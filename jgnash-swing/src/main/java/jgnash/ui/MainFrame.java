/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.ui;

import com.jhlabs.image.BlurFilter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.RepaintManager;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultEditorKit;

import jgnash.Main;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.message.Message;
import jgnash.message.MessageBus;
import jgnash.message.MessageChannel;
import jgnash.message.MessageListener;
import jgnash.net.currency.CurrencyUpdateFactory;
import jgnash.net.security.AbstractYahooParser;
import jgnash.net.security.SecurityUpdateFactory;
import jgnash.plugin.Plugin;
import jgnash.plugin.PluginFactory;
import jgnash.ui.account.ExpandingAccountTablePanel;
import jgnash.ui.actions.AbstractEnabledAction;
import jgnash.ui.actions.OpenAction;
import jgnash.ui.budget.BudgetPanel;
import jgnash.ui.components.MemoryMonitor;
import jgnash.ui.components.SubstanceFontSlider;
import jgnash.ui.components.WaitMessagePanel;
import jgnash.ui.debug.EDTCheckingRepaintManager;
import jgnash.ui.recurring.RecurringPanel;
import jgnash.ui.register.MainRegisterPanel;
import jgnash.ui.register.RegisterEvent;
import jgnash.ui.register.RegisterFrame;
import jgnash.ui.register.RegisterListener;
import jgnash.ui.util.DialogUtils;
import jgnash.ui.util.builder.ActionParser;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.Resource;

import org.jdesktop.jxlayer.plaf.effect.BufferedImageOpEffect;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXStatusBar;

/**
 * The JFrame for the application.
 * 
 * @author Craig Cavanaugh
 * @author Aleksey Trufanov
 *
 */
public class MainFrame extends JFrame implements MessageListener, ActionListener {

    private static final String REGISTER_KEY = "register";

    private static final String REGISTER_FOLLOWS_LIST = "RegisterFollowsList";

    private JMenuBar menuBar;

    private JMenu windowMenu;

    private JMenu viewMenu;

    private JMenu reportMenu;

    private MainViewPanel mainView;

    private final Resource rb = Resource.get();

    private WaitMessagePanel waitPanel;

    private static boolean registerFollowsTree;

    private MainRegisterPanel registerTreePanel;

    private Action editAction;

    private ExpandingAccountTablePanel expandingAccountPanel;

    private JTextField statusField;

    private static final Logger log = Logger.getLogger(MainFrame.class.getName());

    private final PausableThreadPoolExecutor backgroundUpdateExecutor = new PausableThreadPoolExecutor();

    private static final int SCHEDULED_DELAY = 10;

    private JXBusyLabel backgroundOperationLabel;

    private final LogHandler logHandler = new LogHandler();

    private Color infoColor = null;

    private final LockableUI primaryWaitBlurUI = new LockableUI(new BufferedImageOpEffect(new BlurFilter()));

    static {
        registerFollowsTree = doesRegisterFollowTree();
    }

    /**
     * Public constructor
     */
    public MainFrame() {

        /*
         * hook in the theme manager before some of the more complex UI components are created... reduce start up time a
         * touch and avoids a couple UI burps
         */
        ThemeManager themeManager = new ThemeManager(this);

        applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));

        setIconImage(Resource.getImage("/jgnash/resource/gnome-money.png"));

        buildUI();

        // hide on close is critical to correct behavior on Java 7, otherwise a segfault occurs on close
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        if (Main.checkEDT()) {
            log.info("Installing Event Dispatch Thread Checker into RepaintManager");
            RepaintManager.setCurrentManager(new EDTCheckingRepaintManager());
        }

        viewMenu.insertSeparator(viewMenu.getComponentCount() + 1);
        viewMenu.add(themeManager.buildLookAndFeelMenu());
        viewMenu.add(themeManager.buildThemeMenu());

        addWindowListener(new ShutdownAdapter());

        RegisterFrame.addRegisterListener(new RegisterListener() {

            @Override
            public void registerEvent(RegisterEvent e) {
                if (e.getAction() == RegisterEvent.Action.OPEN) {
                    addWindowItem(e);
                } else if (e.getAction() == RegisterEvent.Action.CLOSE) {
                    removeWindowItem(e);
                }
            }
        });

        registerListeners();

        /* add log handlers to listen and display warning and errors */
        registerLogHandler(Engine.class);
        registerLogHandler(EngineFactory.class);
        registerLogHandler(AbstractYahooParser.class);
        registerLogHandler(UIApplication.class);

        SecurityUpdateFactory.getUpdateOnStartup(); // forces the logger to be initialized first
        registerLogHandler(SecurityUpdateFactory.class);

        loadPlugins();

        setBounds();

        log.fine("UI Construction is complete");

        // engine is not null only when a UI restart occurs
        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
            setOpenState(true);
            addViews();
            updateTitle();
        }
    }

    private void registerListeners() {
        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT, MessageChannel.SYSTEM);
    }

    private void setBounds() {
        setMinimumSize(new Dimension(500, 300)); // limit the smallest size

        pack(); //  pack and size the frame

        DialogUtils.addBoundsListener(this); //  add listener to save and restore window bounds
    }

    private void loadPlugins() {
        PluginFactory.get().loadPlugins();       
        PluginFactory.startPlugins();

        for (Plugin plugin : PluginFactory.getPlugins()) {

            JMenuItem[] menuItems = plugin.getMenuItems();

            if (menuItems != null) {
                for (JMenuItem menuItem : menuItems) {
                    Object precedingIdref = menuItem.getClientProperty(Plugin.PRECEDINGMENUIDREF);

                    if (precedingIdref != null && precedingIdref instanceof String) {
                        addMenuItem((String) precedingIdref, menuItem);
                    }
                }
            }
        }
    }

    public static boolean doesRegisterFollowTree() {
        Preferences pref = Preferences.userNodeForPackage(MainFrame.class);
        return pref.getBoolean(REGISTER_FOLLOWS_LIST, true);
    }

    public static void setRegisterFollowsTree(final boolean follow) {
        Preferences pref = Preferences.userNodeForPackage(MainFrame.class);
        registerFollowsTree = follow;
        pref.putBoolean(REGISTER_FOLLOWS_LIST, follow);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        Object source = e.getSource();

        if (source == mainView) {
            mainViewAction();
        }
    }

    private void addViews() {

        registerTreePanel = new MainRegisterPanel();
        expandingAccountPanel = new ExpandingAccountTablePanel();

        mainView.addView(expandingAccountPanel, rb.getString("Button.Accounts"), rb.getString("ToolTip.AccountList"));

        mainView.addView(registerTreePanel, rb.getString("Button.Register"), rb.getString("ToolTip.AccountRegister"));

        mainView.addView(new RecurringPanel(), rb.getString("Button.Reminders"), rb.getString("ToolTip.Reminders"));

        mainView.addView(new BudgetPanel(), rb.getString("Button.Budgeting"), rb.getString("ToolTip.Budgeting"));
    }

    private void addWindowItem(final RegisterEvent e) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                RegisterFrame d = (RegisterFrame) e.getSource();
                JMenuItem mi = new JMenuItem(new WindowAction(d));

                int size = windowMenu.getMenuComponentCount();

                windowMenu.insert(mi, size - 2);
                windowMenu.setEnabled(true);
            }
        });
    }

    private void addMenuItem(final String precedingMenuIdref, final JMenuItem newMenuItem) {
        for (Component component : menuBar.getComponents()) {
            if (component instanceof JMenu) {
                addJMenuItem((JMenu) component, precedingMenuIdref, newMenuItem);
            }
        }
    }

    private void addJMenuItem(final JMenu menu, final String precedingMenuIdref, final JMenuItem newMenuItem) {

        Component[] components = menu.getMenuComponents();

        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JMenu) {
                addJMenuItem((JMenu) components[i], precedingMenuIdref, newMenuItem);
            } else if (components[i] instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) components[i];

                if (precedingMenuIdref.equals(item.getClientProperty(ActionParser.IDREF_ATTRIBUTE))) {
                    menu.add(newMenuItem, i + 1);
                    return;
                }
            }
        }
    }

    private MainViewPanel buildMainView() {

        MainViewPanel panel = new MainViewPanel();
        panel.setBorder(new EmptyBorder(new Insets(2, 6, 0, 2)));

        panel.addActionListener(this);

        return panel;
    }

    private void buildUI() {
        ActionParser actionParser = new ActionParser(this, Resource.get());

        actionParser.preLoadActions("jgnash.ui.actions");

        actionParser.preLoadAction("copy-command", new DefaultEditorKit.CopyAction());
        actionParser.preLoadAction("cut-command", new DefaultEditorKit.CutAction());
        actionParser.preLoadAction("paste-command", new DefaultEditorKit.PasteAction());

        actionParser.preLoadAction("exit-command", new AbstractAction() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                shutDown();
            }
        });

        actionParser.preLoadAction("open-command", new AbstractAction() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                OpenAction.openAction();
                startBackgroundUpdates();
            }
        });

        actionParser.preLoadAction("account-filter-command", new AbstractEnabledAction() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                expandingAccountPanel.showAccountFilterDialog();
            }
        });

        actionParser.preLoadAction("register-filter-command", new AbstractEnabledAction() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                registerTreePanel.showAccountFilterDialog();
            }
        });

        actionParser.preLoadAction("currency-background-update-command", new AbstractEnabledAction() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                backgroundUpdateExecutor.schedule(CurrencyUpdateFactory.getUpdateWorker(), 1, TimeUnit.SECONDS);
            }
        });

        actionParser.preLoadAction("security-background-update-command", new AbstractEnabledAction() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                backgroundUpdateExecutor.schedule(SecurityUpdateFactory.getUpdateWorker(), 1, TimeUnit.SECONDS);
            }
        });

        actionParser.loadFile("/jgnash/resource/main-frame-actions.xml");

        menuBar = actionParser.createMenuBar("main-menu");
        JToolBar toolBar = actionParser.createToolBar("main-toolbar");

        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        viewMenu = (JMenu) actionParser.getJMenuItem("view-menu-command");
        reportMenu = (JMenu) actionParser.getJMenuItem("report-menu-command");
        windowMenu = (JMenu) actionParser.getJMenuItem("window-menu-command");
        windowMenu.setEnabled(false);

        editAction = actionParser.getAction("edit-menu-command");

        if (EngineFactory.getEngine(EngineFactory.DEFAULT) == null) {
            setOpenState(false);
        }

        setTitle(Main.VERSION);

        mainView = buildMainView();

        backgroundOperationLabel = new JXBusyLabel(new Dimension(18, 18));

        statusField = new JTextField();
        statusField.setEditable(false);
        statusField.setFont(statusField.getFont().deriveFont(statusField.getFont().getSize2D() - 1f));

        infoColor = statusField.getForeground();

        JXStatusBar statusBar = new JXStatusBar();
        statusBar.setResizeHandleEnabled(true);

        statusBar.add(statusField, new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL));

        if (ThemeManager.isLookAndFeelSubstance()) {
            statusBar.add(new SubstanceFontSlider());
        }

        statusBar.add(backgroundOperationLabel);

        statusBar.add(new MemoryMonitor(), new JXStatusBar.Constraint(120));

        JPanel contentPanel = new JPanel(new BorderLayout());

        contentPanel.add(toolBar, BorderLayout.NORTH);
        contentPanel.add(mainView, BorderLayout.CENTER);
        contentPanel.add(statusBar, BorderLayout.SOUTH);

        JPanel rootPanel = new JPanel(new BorderLayout());

        rootPanel.add(menuBar, BorderLayout.NORTH);
        rootPanel.add(contentPanel, BorderLayout.CENTER);

        waitPanel = new WaitMessagePanel();

        JLayer<JPanel> rootLayer = new JLayer<>(rootPanel);
        rootLayer.setUI(primaryWaitBlurUI);             

        getContentPane().add(rootLayer, BorderLayout.CENTER);

        setGlassPane(waitPanel);
    }

    public void closeAllWindows() {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                for (Component c : windowMenu.getMenuComponents()) {
                    if (c instanceof JMenuItem) {
                        JMenuItem m = (JMenuItem) c;
                        if (m.getAction() instanceof WindowAction) {
                            RegisterFrame d = (RegisterFrame) m.getAction().getValue(REGISTER_KEY);
                            d.dispatchEvent(new WindowEvent(d, WindowEvent.WINDOW_CLOSING));
                        }
                    }
                }
            }
        });
    }

    final void displayStatus(final String message) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                statusField.setForeground(infoColor);
                statusField.setText(message);
            }
        });
    }

    public void displayWaitMessage(final String message) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                primaryWaitBlurUI.setLocked(true);
                waitPanel.setMessage(message);
                waitPanel.setWaiting(true);
            }
        });

    }

    private void displayWarning(final String message) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                statusField.setForeground(Color.RED);
                statusField.setText(message);
            }
        });
    }

    /**
     * Dispose the UI with an option to prevent complete program shutdown
     * 
     * @param shutDown true if UI should be shutdown when dispose is called
     */
    void dispose(boolean shutDown) {
       
        PluginFactory.stopPlugins();

        if (!shutDown) {
            for (WindowListener listener : getWindowListeners()) {
                if (listener instanceof ShutdownAdapter) {
                    removeWindowListener(listener);
                }
            }
        }

        super.dispose();
    }

    void loadFile(final File file) {
        OpenAction.openAction(file);
        startBackgroundUpdates();
    }

    void loadLast() {
        OpenAction.openLastAction();
        startBackgroundUpdates();
    }

    private void mainViewAction() {
        if (mainView.getVisibleComponent() == registerTreePanel && registerFollowsTree) {
            registerTreePanel.setAccount(expandingAccountPanel.getSelectedAccount());
        }

        // Request focus for the table so it picks up keyboard events without requiring a mouse click
        if (mainView.getVisibleComponent() == expandingAccountPanel) {
            expandingAccountPanel.requestTableFocus();
        }
    }

    @Override
    public void messagePosted(final Message event) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                switch (event.getEvent()) {
                    case FILE_CLOSING:
                        setOpenState(false);
                        updateTitle();
                        removeViews();
                        break;
                    case FILE_NOT_FOUND:
                        break; // ignore for now
                    case FILE_IO_ERROR:
                        //StaticUIMethods.displayError(event.description);
                        break;
                    case FILE_LOAD_FAILED:
                        break; // ignore for now
                    case ACCOUNT_REMOVE_FAILED:
                        StaticUIMethods.displayError(rb.getString("Message.ErrorAccountRemove"));
                        break;
                    case FILE_LOAD_SUCCESS:
                    case FILE_NEW_SUCCESS:
                        setOpenState(true);
                        addViews();
                        updateTitle();
                        break;
                    default:
                        // ignore any other messages that don't belong to us
                        break;
                }
            }
        });

    }

    void openRemote(final String host, final int port, final String user, final String password) {
        OpenAction.openRemote(host, port, user, password);
        startBackgroundUpdates();
    }

    private void registerLogHandler(final Class<?> clazz) {
        Logger.getLogger(clazz.getName()).addHandler(logHandler);
    }

    private void removeViews() {
        mainView.removeAll();
        expandingAccountPanel = null;
        registerTreePanel = null;
    }

    private void removeWindowItem(final RegisterEvent e) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                RegisterFrame d = (RegisterFrame) e.getSource();

                for (Component c : windowMenu.getMenuComponents()) {
                    if (c instanceof JMenuItem) {
                        JMenuItem m = (JMenuItem) c;
                        if (m.getAction() instanceof WindowAction) {
                            if (d == m.getAction().getValue(REGISTER_KEY)) {
                                windowMenu.remove(c);
                                if (windowMenu.getItemCount() < 3) {
                                    windowMenu.setEnabled(false);
                                }
                                return;
                            }
                        }
                    }
                }
            }
        });
    }

    public void setNetworkBusy(final boolean busy) {
        backgroundOperationLabel.setBusy(busy);
    }

    private void setOpenState(boolean state) {
        editAction.setEnabled(state);
        reportMenu.setEnabled(state);
    }

    private void shutDown() {
        closeAllWindows(); // force all windows closed for a clean looking exit

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                dispatchEvent(new WindowEvent(MainFrame.this, WindowEvent.WINDOW_CLOSING));
            }
        });
    }

    private void startBackgroundUpdates() {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {

                if (SecurityUpdateFactory.getUpdateOnStartup()) {
                    backgroundUpdateExecutor.schedule(SecurityUpdateFactory.getUpdateWorker(), SCHEDULED_DELAY, TimeUnit.SECONDS);
                }

                if (CurrencyUpdateFactory.getUpdateOnStartup()) {
                    backgroundUpdateExecutor.schedule(CurrencyUpdateFactory.getUpdateWorker(), SCHEDULED_DELAY, TimeUnit.SECONDS);
                }
            }
        });
    }

    public void pauseBackgroundUpdates() {
        backgroundUpdateExecutor.pause();
    }

    public void resumeBackgroundUpdates() {
        backgroundUpdateExecutor.resume();
    }

    public void stopWaitMessage() {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                waitPanel.setWaiting(false);
                primaryWaitBlurUI.setLocked(false);
            }
        });
    }

    private void updateTitle() {
        new UpdateTitleWorker().execute();
    }

    private class LogHandler extends Handler {

        @Override
        public void close() throws SecurityException {

        }

        @Override
        public void flush() {

        }

        @Override
        public synchronized void publish(final LogRecord record) {

            // update on the event thread to prevent display corruption
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {

                    if (record.getLevel() == Level.WARNING || record.getLevel() == Level.SEVERE) {
                        displayWarning(record.getMessage());
                    } else if (record.getLevel() == Level.INFO) {
                        displayStatus(record.getMessage());
                    }
                }
            });
        }
    }

    private class ShutdownAdapter extends WindowAdapter {

        @Override
        public void windowClosing(final WindowEvent evt) {
            /*
             * force the shut down to the end of the event thread. Lets other listeners do their job
             */
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    closeAllWindows();
                    removeViews();

                    // start thread outside the EDT so the file closure cannot hang the EDT
                    Thread thread = new Thread() {

                        @Override
                        public void run() {
                            EngineFactory.closeEngine(EngineFactory.DEFAULT);
                            System.exit(0); // explicit exit
                        }
                    };

                    thread.start();
                }
            });
        }
    }

    private static class WindowAction extends AbstractAction {

        private static final long serialVersionUID = -598477156303870342L;

        WindowAction(RegisterFrame d) {
            super(d.toString());
            putValue(REGISTER_KEY, d);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final RegisterFrame d = (RegisterFrame) getValue(REGISTER_KEY);

            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (d.getExtendedState() == ICONIFIED) {
                        d.setExtendedState(NORMAL);
                    }
                    d.toFront();
                }
            });
        }
    }

    static class PausableThreadPoolExecutor extends ScheduledThreadPoolExecutor {

        private boolean isPaused;

        private final ReentrantLock pauseLock = new ReentrantLock();

        private final Condition pausedCondition = pauseLock.newCondition();

        public PausableThreadPoolExecutor() {
            super(1, new DefaultDaemonThreadFactory());
        }

        @Override
        protected void beforeExecute(final Thread t, final Runnable r) {
            super.beforeExecute(t, r);
            pauseLock.lock();
            try {
                while (isPaused) {
                    pausedCondition.await();
                }
            } catch (InterruptedException ie) {
                t.interrupt();
            } finally {
                pauseLock.unlock();
            }
        }

        public void pause() {
            pauseLock.lock();
            try {
                isPaused = true;
            } finally {
                pauseLock.unlock();
            }
        }

        public void resume() {
            pauseLock.lock();
            try {
                isPaused = false;
                pausedCondition.signalAll();
            } finally {
                pauseLock.unlock();
            }
        }
    }

    /**
     * The engine could be busy with something else, so run in the background and do not block the EDT
     */
    final class UpdateTitleWorker extends SwingWorker<Engine, Void> {

        @Override
        protected Engine doInBackground() throws Exception {
            return EngineFactory.getEngine(EngineFactory.DEFAULT);
        }

        @Override
        protected void done() {
            try {
                Engine engine = get();

                if (engine != null) {
                    setTitle(new StringBuilder(Main.VERSION).append("  [").append(EngineFactory.getActiveDatabase()).append(']').toString());
                } else {
                    setTitle(Main.VERSION);
                }
            } catch (InterruptedException | ExecutionException exception) {
                setTitle(Main.VERSION);
                log.log(Level.INFO, exception.getMessage(), exception);
            }
        }
    }
}
