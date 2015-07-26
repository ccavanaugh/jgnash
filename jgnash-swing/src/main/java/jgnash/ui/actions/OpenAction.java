/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.ui.actions;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.components.OpenDatabaseDialog;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.FileUtils;
import jgnash.util.ResourceUtils;

/**
 * UI Action to open a database
 *
 * @author Craig Cavanaugh
 */
public class OpenAction {

    public static final Logger logger = Logger.getLogger(OpenAction.class.getName());

    private static boolean remoteConnectionFailed = false;

    /* The internal SwingWorker thread pool may be full at startup because of all the threads being launched
       which can lead to random deadlocks at startup.  Use an internal thread pool to prevent issues.
       This also reduces startup time if multiple cores are available. */
    private static final ExecutorService pool = Executors.newSingleThreadExecutor(new DefaultDaemonThreadFactory());

    static {
        logger.setLevel(Level.ALL);
    }

    private OpenAction() {
    }

    public static void openAction() {

        final class BootEngine extends SwingWorker<Void, Void> {

            final private OpenDatabaseDialog dialog;

            BootEngine(final OpenDatabaseDialog dialog) {
                this.dialog = dialog;
            }

            @Override
            protected Void doInBackground() throws Exception {
                final ResourceBundle rb = ResourceUtils.getBundle();

                UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));

                EngineFactory.closeEngine(EngineFactory.DEFAULT);

                Engine engine = null;

                final char[] password = dialog.getPassword();

                if (dialog.isRemote()) {
                    final String host = dialog.getHost();
                    final int port = dialog.getPort();

                    engine = EngineFactory.bootClientEngine(host, port, password, EngineFactory.DEFAULT);

                    if (engine == null) {
                        remoteConnectionFailed = true;
                    }
                } else {
                    try {
                        if (FileUtils.isFileLocked(dialog.getDatabasePath())) {
                            StaticUIMethods.displayError(ResourceUtils.getString("Message.FileIsLocked"));
                        } else if (checkAndBackupOldVersion(dialog.getDatabasePath(), password)) {
                            engine = EngineFactory.bootLocalEngine(dialog.getDatabasePath(), EngineFactory.DEFAULT, password);
                        }
                    } catch (final Exception e) {
                        StaticUIMethods.displayError(e.getLocalizedMessage());
                    }
                }

                if (engine != null) {
                    engine.getRootAccount(); // prime the engine
                }

                return null;
            }

            @Override
            protected void done() {
                logger.info("openAction() done");
                UIApplication.getFrame().stopWaitMessage();

                if (remoteConnectionFailed) {
                    StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.ServerConnection"));
                }
            }
        }

        EventQueue.invokeLater(() -> {

            final OpenDatabaseDialog d = new OpenDatabaseDialog(UIApplication.getFrame());

            d.setDatabasePath(EngineFactory.getLastDatabase());
            d.setPort(EngineFactory.getLastPort());
            d.setHost(EngineFactory.getLastHost());
            d.setRemote(EngineFactory.getLastRemote());

            d.setVisible(true);

            if (d.getResult()) {
                pool.execute(new BootEngine(d));
            }

        });
    }

    public static void openAction(final File file, final char[] password) {

        final String database = file.getAbsolutePath();

        final class BootEngine extends SwingWorker<Void, Void> {

            @Override
            protected Void doInBackground() throws Exception {
                final ResourceBundle rb = ResourceUtils.getBundle();

                UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));
                logger.fine("Booting the engine");

                // Disk IO is heavy so delay and allow the UI to react before starting the boot operation
                Thread.sleep(750);

                if (checkAndBackupOldVersion(file.getAbsolutePath(), password)) {
                    final Engine e = EngineFactory.bootLocalEngine(file.getAbsolutePath(), EngineFactory.DEFAULT, password);
                    if (e != null) {
                        e.getRootAccount(); // prime the engine
                    }

                    logger.fine("Engine boot complete");
                }

                return null;
            }

            @Override
            protected void done() {
                logger.info("openAction(final File file) done");
                UIApplication.getFrame().stopWaitMessage();
            }
        }

        if (EngineFactory.doesDatabaseExist(database)) {
            try {
                if (!FileUtils.isFileLocked(database)) {
                    pool.execute(new BootEngine());
                } else {
                    StaticUIMethods.displayError(ResourceUtils.getString("Message.FileIsLocked"));
                }
            } catch (final IOException e) {
                logger.log(Level.SEVERE, e.toString(), e);
            }
        }
    }

    /**
     * Opens the last local file or remote connection automatically only if a password was not used last time
     */
    public static void openLastAction() {
        final Logger appLogger = UIApplication.getLogger();

        final class BootEngine extends SwingWorker<Long, Void> {

            @Override
            protected Long doInBackground() throws Exception {

                final long startTime = System.currentTimeMillis();

                final ResourceBundle rb = ResourceUtils.getBundle();

                UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));
                logger.fine("Booting the engine");

                Engine engine = null;

                if (EngineFactory.getLastRemote() && !EngineFactory.usedPassword()) {  // must be a remote connection without use of a password

                    final String host = EngineFactory.getLastHost();
                    final int port = EngineFactory.getLastPort();

                    engine = EngineFactory.bootClientEngine(host, port, new char[]{}, EngineFactory.DEFAULT);

                    if (engine == null) {
                        appLogger.warning(rb.getString("Message.Error.ServerConnection"));
                    }
                } else {    // must be a local file with a user name and password
                    if (checkAndBackupOldVersion(EngineFactory.getLastDatabase(), new char[]{})) {
                        engine = EngineFactory.bootLocalEngine(EngineFactory.getLastDatabase(), EngineFactory.DEFAULT, new char[]{});
                    }

                    if (engine == null) {
                        appLogger.warning(rb.getString("Message.Error.LoadingFile"));
                    }
                }

                if (engine != null) {
                    logger.fine("Engine boot complete");
                }

                return System.currentTimeMillis() - startTime;
            }

            @Override
            protected void done() {
                logger.info("openLastAction() done");

                try {
                    long time = get(5, TimeUnit.SECONDS);
                    logger.log(Level.INFO, "Boot time was: {0} seconds", ((float)time) / 1000f);
                } catch (final InterruptedException | ExecutionException | TimeoutException e) {
                    logger.log(Level.SEVERE, e.toString(), e);
                } finally {
                    UIApplication.getFrame().stopWaitMessage();
                }
            }
        }


        if (!EngineFactory.getLastRemote()) {
            final String database = EngineFactory.getLastDatabase();

            // if local and a password was used last time, don't even try
            if (EngineFactory.usedPassword()) {
                return;
            }

            // check for locked file before trying the boot
            if (EngineFactory.doesDatabaseExist(database)) {
                try {
                    if (!FileUtils.isFileLocked(database)) {
                        pool.execute(new BootEngine());
                    } else {
                        StaticUIMethods.displayError(ResourceUtils.getString("Message.FileIsLocked"));
                    }
                } catch (final IOException e) {
                    appLogger.log(Level.SEVERE, e.toString(), e);
                }
            }
        }
    }

    public static void openRemote(final String host, final int port, final char[] password) {

        final class BootEngine extends  SwingWorker<Void, Void> {

            @Override
            protected Void doInBackground() throws Exception {
                final ResourceBundle rb = ResourceUtils.getBundle();

                UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));
                logger.fine("Booting the engine");

                Thread.sleep(750);

                EngineFactory.bootClientEngine(host, port, password, EngineFactory.DEFAULT);

                logger.fine("Engine boot complete");
                return null;
            }

            @Override
            protected void done() {
                UIApplication.getFrame().stopWaitMessage();
            }
        }

        pool.execute(new BootEngine());
    }

    private static boolean checkAndBackupOldVersion(final String fileName, final char[] password) {

        boolean result = false;

        if (Files.exists(new File(fileName).toPath())) {
            final float version = EngineFactory.getFileVersion(new File(fileName), password);

            if (version <= 0) {
                final String errorMessage = ResourceUtils.getString("Message.Error.InvalidUserPass");

                UIApplication.getLogger().warning(errorMessage);

                new Thread() {  // pop an error dialog with the warning for immediate feedback
                    @Override
                    public void run() {
                        StaticUIMethods.displayError(errorMessage);
                    }
                }.start();

            } else {
                result = true;

                // make a versioned backup first
                if (version < Engine.CURRENT_VERSION) {
                    FileUtils.copyFile(new File(fileName), new File(fileName + "." + version));

                    new Thread() {  // pop an information dialog about the backup file
                        @Override
                        public void run() {

                            final String message = ResourceUtils.getString("Message.Info.Upgrade", fileName + "."
                                    + version);

                            StaticUIMethods.displayMessage(message, ResourceUtils.getString("Title.Information"),
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                    }.start();

                }
            }
        }

        return result;
    }
}
