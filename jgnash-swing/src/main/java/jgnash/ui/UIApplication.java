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

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.help.JHelp;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingConstants;

import jgnash.Main;
import jgnash.engine.EngineFactory;
import jgnash.message.ChannelEvent;
import jgnash.message.Message;
import jgnash.message.MessageBus;
import jgnash.message.MessageChannel;
import jgnash.ui.components.ExceptionDialog;
import jgnash.ui.report.FontRegistry;
import jgnash.ui.splash.AboutDialog;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * GUI version of the jGnash program. This Class creates and provides access to the MainFrame.
 *
 * @author Craig Cavanaugh
 *
 */
public class UIApplication implements Thread.UncaughtExceptionHandler {

    private static final String helpHS = "default/jhelpset.hs";

    private static final String ACCEPT_LICENSE = "licenseaccepted";

    /**
     * Help ID
     */
    public static final String NEWACCOUNT_ID = "NewAccount";

    /**
     * Help ID
     */
    public static final String INTRODUCTION_ID = "Introduction";

    /**
     * Help ID
     */
    public static final String REPORTS_ID = "Reports";

    private final Preferences pref = Preferences.userNodeForPackage(UIApplication.class);

    private static MainFrame mainFrame;

    private static volatile HelpBroker helpBroker;

    private static final Logger logger = Logger.getLogger(UIApplication.class.getName());

    private static JDialog helpDialog;

    private static JHelp jHelp;

    public UIApplication(final File file) {

        if (initFrame()) {

            FontRegistry.registerFonts(); // pre-register report fonts

            // try to load the last open file
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (file != null) {
                        mainFrame.loadFile(file);
                    } else if (EngineFactory.openLastOnStartup()) {
                        mainFrame.loadLast();
                    }
                }
            });
        }
    }

    public UIApplication(final String host, final int port, final String user, final String password) {
        if (initFrame()) {

            FontRegistry.registerFonts(); // pre-register report fonts

            // try to connect to the remove host
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    mainFrame.openRemote(host, port, user, password);
                }
            });
        }
    }

    private boolean initFrame() {
        boolean result = false;

        // install default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(this);

        if (!pref.getBoolean(ACCEPT_LICENSE, false)) {
            if (AboutDialog.showAcceptLicenseDialog()) {
                pref.putBoolean(ACCEPT_LICENSE, true);
            } else {
                System.err.println(Resource.get().getString("Message.ErrorLicense"));
            }
        }

        if (pref.getBoolean(ACCEPT_LICENSE, false)) {
            result = true;

            try {
                EventQueue.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {
                        mainFrame = new MainFrame();
                        mainFrame.setVisible(true);
                    }
                });
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        if (result) {
            if (Main.enableHangDetection()) {
                Logger.getLogger(UIApplication.class.getName()).info("Installing Event Dispatch Thread Hang Monitor");
                jgnash.ui.debug.EventDispatchThreadHangMonitor.initMonitoring();
            }
        }

        return result;
    }

    public static MainFrame getFrame() {
        return mainFrame;
    }

    public synchronized static HelpBroker getHelpBroker() {
        if (helpBroker == null) {
            ClassLoader cl = UIApplication.class.getClassLoader();
            try {
                URL hsURL = HelpSet.findHelpSet(cl, helpHS);
                HelpSet hs = new HelpSet(null, hsURL);
                helpBroker = hs.createHelpBroker();
            } catch (Exception ee) {
                System.err.println("HelpSet " + ee.getMessage());
                System.err.println("HelpSet " + helpHS + " not found");
            }
        }

        return helpBroker;
    }

    public static void enableHelpOnButton(final JButton button, final String id) {

        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                showHelp(id);
            }
        });
    }

    public static synchronized void showHelp(final String id) {
        HelpBroker broker = UIApplication.getHelpBroker();

        if (broker != null) {

            if (jHelp == null) {
                jHelp = new JHelp(broker.getHelpSet());
            }

            try {
                if (id != null) {
                    jHelp.setCurrentID(id);
                }
            } catch (javax.help.BadIDException e) {
                logger.log(Level.INFO, "Invalid help ID: {0}", id);
            }

            if (helpDialog == null) {

                helpDialog = new JDialog((Frame) null, Resource.get().getString("Title.Help"), false);

                helpDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                helpDialog.getContentPane().add(jHelp, SwingConstants.CENTER);
                helpDialog.setMinimumSize(new Dimension(500, 400));
                helpDialog.setSize(new Dimension(500, 640));
                helpDialog.setLocationRelativeTo(UIApplication.getFrame());
                helpDialog.setModalExclusionType(Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);

                DialogUtils.addBoundsListener(helpDialog);

                // remove the reference when closed
                helpDialog.addWindowListener(new WindowAdapter() {

                    @Override
                    public void windowClosing(WindowEvent evt) {
                        helpDialog = null;
                        jHelp = null;
                    }
                });
            }

            helpDialog.setVisible(true);
        }
    }

    public static void repaint() {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                mainFrame.repaint();
            }
        });
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {

        // ignore any exceptions thrown by the help plaf
        if (e.getStackTrace()[0].getClassName().contains("help.plaf")) {
            return;
        }

        logger.log(Level.SEVERE, e.getMessage(), e);

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                new ExceptionDialog(getFrame(), e).setVisible(true);
            }
        });
    }

    /**
     * Forces a restart of the UI without having to reload the engine. Useful when changing the look and feel
     */
    public static void restartUI() {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {

                if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {

                    MessageBus messageBus = MessageBus.getInstance(EngineFactory.getEngine(EngineFactory.DEFAULT).getName());
                    Message message = new Message(MessageChannel.SYSTEM, ChannelEvent.UI_RESTARTING, EngineFactory.getEngine(EngineFactory.DEFAULT));

                    messageBus.fireEvent(message);
                }

                helpBroker = null;

                mainFrame.setVisible(false);
                mainFrame.dispose(false);

                // recreate the main UI twice to flush look and feel info from the JXSwing components
                mainFrame = new MainFrame();
                mainFrame.dispose(false);

                mainFrame = new MainFrame();
                mainFrame.setVisible(true);

                if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {

                    MessageBus messageBus = MessageBus.getInstance(EngineFactory.getEngine(EngineFactory.DEFAULT).getName());
                    Message message = new Message(MessageChannel.SYSTEM, ChannelEvent.UI_RESTARTED, EngineFactory.getEngine(EngineFactory.DEFAULT));

                    messageBus.fireEvent(message);
                }
            }
        });
    }

    /**
     * Returns a common application level logger. Intended for user readable information and warnings
     *
     * @return Logger
     */
    public static Logger getLogger() {
        return logger;
    }
}
