/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import jgnash.Main;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.ui.components.ExceptionDialog;
import jgnash.ui.splash.AboutDialog;
import jgnash.util.ResourceUtils;

/**
 * GUI version of the jGnash program. This Class creates and provides access to the MainFrame.
 *
 * @author Craig Cavanaugh
 */
public class UIApplication implements Thread.UncaughtExceptionHandler {

    private static final String ACCEPT_LICENSE = "licenseaccepted";

    private final Preferences pref = Preferences.userNodeForPackage(UIApplication.class);

    private static MainFrame mainFrame;

    private static final Logger logger = Logger.getLogger(UIApplication.class.getName());

    public UIApplication(final Path file, final char[] password) {

        if (initFrame()) {
        
            // try to load the last open file
            try {
                EventQueue.invokeAndWait(() -> {
                    if (file != null) {
                        MainFrame.loadFile(file, password);
                    } else if (EngineFactory.openLastOnStartup()) {
                        MainFrame.loadLast();
                    }
                });
            } catch (final InterruptedException | InvocationTargetException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    public UIApplication(final String host, final int port, final char[] password) {
        if (initFrame()) {         

            // try to connect to the remove host
            EventQueue.invokeLater(() -> MainFrame.openRemote(host, port, password));
        }
    }

    private boolean initFrame() {
        boolean result = false;

        StaticUIMethods.fixWindowManager();

        // install default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(this);

        if (!pref.getBoolean(ACCEPT_LICENSE, false)) {
            if (AboutDialog.showAcceptLicenseDialog()) {
                pref.putBoolean(ACCEPT_LICENSE, true);
            } else {
                System.err.println(ResourceUtils.getString("Message.Error.License"));
            }
        }

        if (pref.getBoolean(ACCEPT_LICENSE, false)) {
            result = true;

            try {
                EventQueue.invokeAndWait(() -> {
                    mainFrame = new MainFrame();
                    mainFrame.setVisible(true);
                });
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        if (result) {
            if (Main.enableHangDetection()) {
                logger.info("Installing Event Dispatch Thread Hang Monitor");
                jgnash.ui.debug.EventDispatchThreadHangMonitor.initMonitoring();
            }
        }

        return result;
    }

    public static MainFrame getFrame() {
        return mainFrame;
    }

    public static void repaint() {
        EventQueue.invokeLater(mainFrame::repaint);
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {

        // ignore any exceptions thrown by the help plaf
        if (e.getStackTrace()[0].getClassName().contains("help.plaf")) {
            return;
        }

        logger.log(Level.SEVERE, e.getMessage(), e);

        EventQueue.invokeLater(() -> new ExceptionDialog(getFrame(), e).setVisible(true));
    }

    /**
     * Forces a restart of the UI without having to reload the engine. Useful when changing the look and feel
     */
    public static void restartUI() {

        EventQueue.invokeLater(() -> {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

            if (engine != null) {
                MessageBus messageBus = MessageBus.getInstance(engine.getName());
                Message message = new Message(MessageChannel.SYSTEM, ChannelEvent.UI_RESTARTING, engine);

                messageBus.fireEvent(message);
            }

            mainFrame.setVisible(false);
            mainFrame.doPartialDispose();

            // recreate the main UI twice to flush look and feel info from the JXSwing components
            mainFrame = new MainFrame();
            mainFrame.doPartialDispose();

            mainFrame = new MainFrame();
            mainFrame.setVisible(true);

            if (engine != null) {

                MessageBus messageBus = MessageBus.getInstance(engine.getName());
                Message message = new Message(MessageChannel.SYSTEM, ChannelEvent.UI_RESTARTED, engine);

                messageBus.fireEvent(message);
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
