/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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

import jgnash.Main;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.net.security.UpdateFactory;
import jgnash.plugin.Plugin;
import jgnash.plugin.PluginFactory;
import jgnash.plugin.SwingPlugin;
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
import jgnash.ui.util.DialogUtils;
import jgnash.ui.util.IconUtils;
import jgnash.ui.util.builder.ActionParser;
import jgnash.util.ResourceUtils;
import jgnash.util.Version;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXStatusBar;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * The JFrame for the application.
 *
 * @author Craig Cavanaugh
 * @author Aleksey Trufanov
 */
public class MainFrame extends JFrame implements MessageListener, ActionListener {

    private static final String REGISTER_KEY = "register";

    private static final String REGISTER_FOLLOWS_LIST = "RegisterFollowsList";

    private JMenuBar menuBar;

    private JMenu windowMenu;

    private JMenu viewMenu;

    private JMenu reportMenu;

    private MainViewPanel mainView;

    private final transient ResourceBundle rb = ResourceUtils.getBundle();

    private WaitMessagePanel waitPanel;

    private static boolean registerFollowsTree;

    private MainRegisterPanel registerTreePanel;

    private transient Action editAction;

    private ExpandingAccountTablePanel expandingAccountPanel;

    private JTextField statusField;

    public static final Logger logger = Logger.getLogger(MainFrame.class.getName());

    private JXBusyLabel backgroundOperationLabel;

    private Color infoColor = null;

    private BusyLayerUI busyLayerUI;

    static {
        registerFollowsTree = doesRegisterFollowTree();
    }

    MainFrame() {
        /*
         * hook in the theme manager before some of the more complex UI components are created... reduce start up time a
         * touch and avoids a couple UI burps
         */
        ThemeManager themeManager = new ThemeManager(this);

        applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));

        setIconImage(IconUtils.getImage("/jgnash/resource/gnome-money.png"));

        buildUI();

        // do nothing by default, let the shutdown adapter do all the work
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        if (Main.checkEDT()) {
            logger.info("Installing Event Dispatch Thread Checker into RepaintManager");
            RepaintManager.setCurrentManager(new EDTCheckingRepaintManager());
        }

        viewMenu.insertSeparator(viewMenu.getComponentCount() + 1);
        viewMenu.add(themeManager.buildLookAndFeelMenu());
        viewMenu.add(themeManager.buildThemeMenu());

        addWindowListener(new ShutdownAdapter());

        RegisterFrame.addRegisterListener(e -> {
            if (e.getAction() == RegisterEvent.Action.OPEN) {
                addWindowItem(e);
            } else if (e.getAction() == RegisterEvent.Action.CLOSE) {
                removeWindowItem(e);
            }
        });

        registerListeners();

        /* add logger handlers to listen and display warning and errors */
        LogHandler logHandler = new LogHandler();

        Engine.addLogHandler(logHandler);
        EngineFactory.addLogHandler(logHandler);
        UpdateFactory.addLogHandler(logHandler);

        loadPlugins();

        setBounds();

        logger.fine("UI Construction is complete");

        // engine is not null only when a UI restart occurs
        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
            setOpenState(true);
            addViews();
            updateTitle();
        }

        new Thread(() -> {

            try {
                Thread.sleep(20_000); // force a 10 second delay
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }

            boolean current = Version.isReleaseCurrent();

            if (!current) {
                EventQueue.invokeLater(()
                        -> StaticUIMethods.displayMessage(ResourceUtils.getString("Message.NewVersion")));
            }
        }).start();
    }

    /**
     * Performs a controlled shutdown to ensure the file is written and closed
     * before the UI disappears.
     */
    private void performControlledShutdown() {

        closeAllWindows(); // close any open windows first

        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {

            displayWaitMessage(ResourceUtils.getString("Message.StoreWait"));

            try {
                Thread.sleep(1000); // lets the UI start and get the users attention
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            }

            EngineFactory.closeEngine(EngineFactory.DEFAULT);

            stopWaitMessage();

            try {
                Thread.sleep(1000);
            } catch (final InterruptedException ex) {
                logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            }
        }

        System.exit(0); // explicit exit  
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
        PluginFactory.loadPlugins(plugin -> plugin instanceof SwingPlugin); // Only load Swing based plugins
        PluginFactory.startPlugins(Plugin.PluginPlatform.Swing);

        for (final Plugin plugin : PluginFactory.getPlugins()) {
            final JMenuItem[] menuItems = ((SwingPlugin) plugin).getMenuItems();

            if (menuItems != null) {
                for (JMenuItem menuItem : menuItems) {
                    final Object precedingId = menuItem.getClientProperty(SwingPlugin.PRECEDING_MENU_IDREF);

                    if (precedingId instanceof String) {
                        addMenuItem((String) precedingId, menuItem);
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
        EventQueue.invokeLater(() -> {
            RegisterFrame d = (RegisterFrame) e.getSource();
            JMenuItem mi = new JMenuItem(new WindowAction(d));

            int size = windowMenu.getMenuComponentCount();

            windowMenu.insert(mi, size - 2);
            windowMenu.setEnabled(true);
        });
    }

    private void addMenuItem(final String precedingMenuId, final JMenuItem newMenuItem) {
        for (Component component : menuBar.getComponents()) {
            if (component instanceof JMenu) {
                addJMenuItem((JMenu) component, precedingMenuId, newMenuItem);
            }
        }
    }

    private void addJMenuItem(final JMenu menu, final String precedingMenuId, final JMenuItem newMenuItem) {

        final Component[] components = menu.getMenuComponents();

        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JMenu) {
                addJMenuItem((JMenu) components[i], precedingMenuId, newMenuItem);
            } else if (components[i] instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) components[i];

                if (precedingMenuId.equals(item.getClientProperty(ActionParser.ID_REF_ATTRIBUTE))) {
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
        ActionParser actionParser = new ActionParser(this);

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

        actionParser.preLoadAction("open-command", new OpenFileAction());
        actionParser.preLoadAction("open-command-tb", new OpenFileAction());

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

        actionParser.preLoadAction("currency-background-update-command", new UpdateExchangeRateAction());

        actionParser.preLoadAction("security-background-update-command", new UpdateSecuritiesAction());

        try (final InputStream stream = MainFrame.class.getResourceAsStream("/jgnash/resource/main-frame-actions.xml")) {
            actionParser.loadFile(stream);
        } catch (final IOException exception) {
            logger.log(Level.SEVERE, exception.getMessage(), exception);
        }

        menuBar = actionParser.createMenuBar("main-menu");
        JToolBar toolBar = actionParser.createToolBar("main-toolbar");

        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        viewMenu = (JMenu) actionParser.getJMenuItem("view-menu-command");
        reportMenu = (JMenu) actionParser.getJMenuItem("report-menu-command");
        windowMenu = (JMenu) actionParser.getJMenuItem("window-menu-command");

        Objects.requireNonNull(windowMenu);

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

        busyLayerUI = new BusyLayerUI();
        JLayer<JPanel> rootLayer = new JLayer<>(rootPanel, busyLayerUI);

        getContentPane().add(rootLayer, BorderLayout.CENTER);

        setGlassPane(waitPanel);
    }

    public void closeAllWindows() {
        EventQueue.invokeLater(() -> {
            for (Component c : windowMenu.getMenuComponents()) {
                if (c instanceof JMenuItem) {
                    JMenuItem m = (JMenuItem) c;
                    if (m.getAction() instanceof WindowAction) {
                        RegisterFrame d = (RegisterFrame) m.getAction().getValue(REGISTER_KEY);
                        d.dispatchEvent(new WindowEvent(d, WindowEvent.WINDOW_CLOSING));
                    }
                }
            }
        });
    }

    private void displayStatus(final String message) {
        EventQueue.invokeLater(() -> {
            statusField.setForeground(infoColor);
            statusField.setText(message);
        });
    }

    public void displayWaitMessage(final String message) {

        // If not on the EDT, invoke on the EDT and wait block until complete... prevents a race condition
        if (!EventQueue.isDispatchThread()) {
            try {
                EventQueue.invokeAndWait(() -> {
                    busyLayerUI.start();
                    waitPanel.setMessage(message);
                    waitPanel.setWaiting(true);
                });
            } catch (final InterruptedException | InvocationTargetException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        } else {
            EventQueue.invokeLater(() -> {
                busyLayerUI.start();
                waitPanel.setMessage(message);
                waitPanel.setWaiting(true);
            });
        }
    }

    public void stopWaitMessage() {
        if (EventQueue.isDispatchThread()) {
            waitPanel.setWaiting(false);
            busyLayerUI.stop();
        } else {
            EventQueue.invokeLater(() -> {
                waitPanel.setWaiting(false);
                busyLayerUI.stop();
            });
        }
    }

    private void displayWarning(final String message) {
        EventQueue.invokeLater(() -> {
            statusField.setForeground(Color.RED);
            statusField.setText(message);
        });
    }

    /**
     * Dispose the UI without a complete program shutdown.
     */
    void doPartialDispose() {

        PluginFactory.stopPlugins();

        for (final WindowListener listener : getWindowListeners()) {
            if (listener instanceof ShutdownAdapter) {
                removeWindowListener(listener);
            }
        }

        super.dispose();
    }

    static void loadFile(final Path file, final char[] password) {
        OpenAction.openAction(file, password);
    }

    static void loadLast() {
        OpenAction.openLastAction();
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
        EventQueue.invokeLater(() -> {
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
                    StaticUIMethods.displayError(rb.getString("Message.Error.AccountRemove"));
                    break;
                case FILE_LOAD_SUCCESS:
                    setOpenState(true);
                    addViews();
                    updateTitle();
                    break;
                case BACKGROUND_PROCESS_STARTED:
                    setNetworkBusy(true);
                    break;
                case BACKGROUND_PROCESS_STOPPED:
                    setNetworkBusy(false);
                    break;
                default:
                    // ignore any other messages that don't belong to us
                    break;
            }
        });

    }

    static void openRemote(final String host, final int port, final char[] password) {
        OpenAction.openRemote(host, port, password);
    }

    private void removeViews() {
        mainView.removeAll();
        expandingAccountPanel = null;
        registerTreePanel = null;
    }

    private void removeWindowItem(final RegisterEvent e) {
        EventQueue.invokeLater(() -> {
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
        });
    }

    private void setNetworkBusy(final boolean busy) {
        backgroundOperationLabel.setBusy(busy);
    }

    private void setOpenState(boolean state) {
        editAction.setEnabled(state);
        reportMenu.setEnabled(state);
    }

    private void shutDown() {
        closeAllWindows(); // force all windows closed for a clean looking exit

        EventQueue.invokeLater(() -> dispatchEvent(new WindowEvent(MainFrame.this, WindowEvent.WINDOW_CLOSING)));
    }

    private void updateTitle() {
        new UpdateTitleWorker().execute();
    }

    private static class UpdateExchangeRateAction extends AbstractEnabledAction {
        @Override
        public void actionPerformed(final ActionEvent e) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

            if (engine != null) {
                engine.startExchangeRateUpdate(0);
            }
        }
    }

    private static class UpdateSecuritiesAction extends AbstractEnabledAction {
        @Override
        public void actionPerformed(final ActionEvent e) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

            if (engine != null) {
                engine.startSecuritiesUpdate(0);
            }
        }
    }

    private static class OpenFileAction extends AbstractAction {
        @Override
        public void actionPerformed(final ActionEvent e) {
            OpenAction.openAction();
        }
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
            EventQueue.invokeLater(() -> {

                if (record.getLevel() == Level.WARNING || record.getLevel() == Level.SEVERE) {
                    displayWarning(record.getMessage());
                } else if (record.getLevel() == Level.INFO) {
                    displayStatus(record.getMessage());
                }
            });
        }
    }

    private class ShutdownAdapter extends WindowAdapter {

        @Override
        public void windowClosing(final WindowEvent evt) {

            // push the shutdown process outside the EDT so the UI effects work correctly
            Thread t = new Thread(MainFrame.this::performControlledShutdown);

            t.start();
        }
    }

    private static class WindowAction extends AbstractAction {

        WindowAction(RegisterFrame d) {
            super(d.toString());
            putValue(REGISTER_KEY, d);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final RegisterFrame d = (RegisterFrame) getValue(REGISTER_KEY);

            EventQueue.invokeLater(() -> {
                if (d.getExtendedState() == ICONIFIED) {
                    d.setExtendedState(NORMAL);
                }
                d.toFront();
            });
        }
    }

    /**
     * The engine could be busy with something else, so run in the background
     * and do not block the EDT.
     */
    private final class UpdateTitleWorker extends SwingWorker<Engine, Void> {

        @Override
        protected Engine doInBackground() {
            return EngineFactory.getEngine(EngineFactory.DEFAULT);
        }

        @Override
        protected void done() {
            try {
                Engine engine = get();

                if (engine != null) {
                    setTitle(Main.VERSION + "  [" + EngineFactory.getActiveDatabase() + ']');
                } else {
                    setTitle(Main.VERSION);
                }
            } catch (InterruptedException | ExecutionException exception) {
                setTitle(Main.VERSION);
                logger.log(Level.INFO, exception.getMessage(), exception);
            }
        }
    }
}
