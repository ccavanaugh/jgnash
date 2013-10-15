/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.components.OpenDatabaseDialog;
import jgnash.ui.util.SimpleSwingWorker;
import jgnash.util.FileUtils;
import jgnash.util.Resource;

/**
 * UI Action to open a database
 *
 * @author Craig Cavanaugh
 */
public class OpenAction {

    private static final Logger logger = Logger.getLogger(OpenAction.class.getName());

    private static boolean remoteConnectionFailed = false;

    static {
        logger.setLevel(Level.ALL);
    }

    private OpenAction() {
    }

    public static void openAction() {

        final class BootEngine extends SimpleSwingWorker {

            private OpenDatabaseDialog dialog;

            BootEngine(final OpenDatabaseDialog dialog) {
                this.dialog = dialog;
            }

            @Override
            protected Void doInBackground() throws Exception {
                Resource rb = Resource.get();

                UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));

                EngineFactory.closeEngine(EngineFactory.DEFAULT);

                Engine engine = null;

                final char[] password = dialog.getPassword();

                if (dialog.isRemote()) {
                    String host = dialog.getHost();
                    int port = dialog.getPort();

                    engine = EngineFactory.bootClientEngine(host, port, password, EngineFactory.DEFAULT);

                    if (engine == null) {
                        remoteConnectionFailed = true;
                    }
                } else {
                    try {
                        if (FileUtils.isFileLocked(dialog.getDatabasePath())) {
                            StaticUIMethods.displayError(Resource.get().getString("Message.FileIsLocked"));
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
                    StaticUIMethods.displayError(Resource.get().getString("Message.ErrorServerConnection"));
                }
            }
        }

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {

                OpenDatabaseDialog d = new OpenDatabaseDialog(UIApplication.getFrame());

                d.setDatabasePath(EngineFactory.getLastDatabase());
                d.setPort(EngineFactory.getLastPort());
                d.setHost(EngineFactory.getLastHost());
                d.setRemote(EngineFactory.getLastRemote());

                d.setVisible(true);

                boolean result = d.getResult();

                if (result) {
                    new BootEngine(d).execute();
                }

            }
        });
    }

    public static void openAction(final File file, final char[] password) {

        String database = file.getAbsolutePath();

        final class BootEngine extends SimpleSwingWorker {

            @Override
            protected Void doInBackground() throws Exception {
                Resource rb = Resource.get();
                UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));
                logger.fine("Booting the engine");

                // Disk IO is heavy so delay and allow the UI to react before starting the boot operation
                Thread.sleep(750);

                if (checkAndBackupOldVersion(file.getAbsolutePath(), password)) {
                    Engine e = EngineFactory.bootLocalEngine(file.getAbsolutePath(), EngineFactory.DEFAULT, password);
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
                    new BootEngine().execute();
                } else {
                    StaticUIMethods.displayError(Resource.get().getString("Message.FileIsLocked"));
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

        final class BootEngine extends SimpleSwingWorker {

            @Override
            protected Void doInBackground() throws Exception {
                Resource rb = Resource.get();
                UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));
                logger.fine("Booting the engine");

                // Disk IO is heavy so delay and allow the UI to react before starting the boot operation
                Thread.sleep(750);

                Engine engine = null;


                if (EngineFactory.getLastRemote() && !EngineFactory.usedPassword()) {  // must be a remote connection without use of a password

                    String host = EngineFactory.getLastHost();
                    int port = EngineFactory.getLastPort();

                    engine = EngineFactory.bootClientEngine(host, port, new char[]{}, EngineFactory.DEFAULT);

                    if (engine == null) {
                        appLogger.warning(rb.getString("Message.ErrorServerConnection"));
                    }
                } else {    // must be a local file with a user name and password
                    if (checkAndBackupOldVersion(EngineFactory.getLastDatabase(), new char[]{})) {
                        engine = EngineFactory.bootLocalEngine(EngineFactory.getLastDatabase(), EngineFactory.DEFAULT, new char[]{});
                    }

                    if (engine == null) {
                        appLogger.warning(rb.getString("Message.ErrorLoadingFile"));
                    }
                }

                if (engine != null) {
                    engine.getRootAccount(); // prime the engine
                    logger.fine("Engine boot complete");
                }

                return null;
            }

            @Override
            protected void done() {
                logger.info("openLastAction() done");
                UIApplication.getFrame().stopWaitMessage();
            }
        }


        if (!EngineFactory.getLastRemote()) {
            String database = EngineFactory.getLastDatabase();

            // if local and a password was used last time, don't even try
            if (EngineFactory.usedPassword()) {
                return;
            }

            // check for locked file before trying the boot
            if (EngineFactory.doesDatabaseExist(database)) {
                try {
                    if (!FileUtils.isFileLocked(database)) {
                        new BootEngine().execute();
                    } else {
                        StaticUIMethods.displayError(Resource.get().getString("Message.FileIsLocked"));
                    }
                } catch (final IOException e) {
                    appLogger.log(Level.SEVERE, e.toString(), e);
                }
            }
        }
    }

    public static void openRemote(final String host, final int port, final char[] password) {

        final class BootEngine extends SimpleSwingWorker {

            @Override
            protected Void doInBackground() throws Exception {
                Resource rb = Resource.get();
                UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));
                logger.fine("Booting the engine");

                Thread.sleep(750);

                EngineFactory.bootClientEngine(host, port, password, EngineFactory.DEFAULT);

                EngineFactory.getEngine(EngineFactory.DEFAULT).getRootAccount(); // prime the engine
                logger.fine("Engine boot complete");
                return null;
            }

            @Override
            protected void done() {
                UIApplication.getFrame().stopWaitMessage();
            }
        }

        new BootEngine().execute();
    }

    private static boolean checkAndBackupOldVersion(final String fileName, final char[] password) {

        boolean result = false;

        if (Files.exists(new File(fileName).toPath())) {
            float version = EngineFactory.getFileVersion(new File(fileName), password);

            if (version <= 0) {
                final String errorMessage = Resource.get().getString("Message.Error.InvalidUserPass");

                UIApplication.getLogger().warning(errorMessage);

                new Thread() {  // pop an error dialog with the warning for immediate feedback
                    public void run() {
                        StaticUIMethods.displayError(errorMessage);
                    }
                }.start();

            } else {
                result = true;

                // make a versioned backup first
                if (version < Engine.CURRENT_VERSION) {
                    FileUtils.copyFile(new File(fileName), new File(fileName + "." + version));
                }
            }
        }

        return result;
    }
}
