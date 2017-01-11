package jgnash.uifx.tasks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.concurrent.Task;

import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.jpa.SqlUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.util.FileUtils;
import jgnash.util.ResourceUtils;

/**
 * Boots the engine with a local file or connection to a remote server.
 *
 * @author Craig Cavanaugh
 */
public class BootEngineTask extends Task<String> {

    public static final int FORCED_DELAY = 1500;

    private static final int INDETERMINATE = -1;

    private final boolean remote;
    private final String localFile;
    private final char[] password;
    private final String serverName;
    private final int port;

    private BootEngineTask(final String localFile, final char[] password, final boolean remote,
                           final String serverName, final int port) {
        this.localFile = localFile;
        this.password = password;
        this.remote = remote;
        this.serverName = serverName;
        this.port = port;
    }

    public static void openLast() {
        final BootEngineTask bootTask;

        // must be a remote connection without use of a password
        if (EngineFactory.getLastRemote() && !EngineFactory.usedPassword()) {
            bootTask = new BootEngineTask(null, EngineFactory.EMPTY_PASSWORD, true,
                    EngineFactory.getLastHost(), EngineFactory.getLastPort());
        } else {
            bootTask = new BootEngineTask(EngineFactory.getLastDatabase(), EngineFactory.EMPTY_PASSWORD, false,
                    null, 0);
        }

        new Thread(bootTask).start();

        StaticUIMethods.displayTaskProgress(bootTask);
    }

    public static void initiateBoot(final String localFile, final char[] password, final boolean remote,
                                    final String serverName, final int port) {
        final BootEngineTask bootTask = new BootEngineTask(localFile, password, remote, serverName, port);

        new Thread(bootTask).start();

        StaticUIMethods.displayTaskProgress(bootTask);
    }

    @Override
    protected String call() throws Exception {

        ResourceBundle resources = ResourceUtils.getBundle();

        updateMessage(resources.getString("Message.LoadingFile"));
        updateProgress(INDETERMINATE, Long.MAX_VALUE);

        // Close an open files or connections first
        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
            EngineFactory.closeEngine(EngineFactory.DEFAULT);
        }

        if (remote) {
            try {
                EngineFactory.bootClientEngine(serverName, port, password, EngineFactory.DEFAULT);
            } catch (final Exception exception) {
                Platform.runLater(() -> StaticUIMethods.displayException(exception));
            }
        } else {
            if (FileUtils.isFileLocked(localFile)) {
                Platform.runLater(() -> StaticUIMethods.displayError(resources.getString("Message.FileIsLocked")));
            } else if (checkAndBackupOldVersion(localFile, password)) {
                EngineFactory.bootLocalEngine(localFile, EngineFactory.DEFAULT, password);
                updateMessage(resources.getString("Message.FileLoadComplete"));
                Thread.sleep(FORCED_DELAY); // force delay for better visual feedback
            }
        }

        return resources.getString("Message.FileLoadComplete");
    }

    /**
     * Check and determine if the file is an old format and backup in necessary.
     *
     * @param fileName fileName to verify
     * @param password assumed password
     * @return true if no errors are encountered
     */
    private static boolean checkAndBackupOldVersion(final String fileName, final char[] password) {

        boolean result = false;

        if (Files.exists(new File(fileName).toPath())) {
            final float version = EngineFactory.getFileVersion(new File(fileName), password);
            final DataStoreType type = EngineFactory.getDataStoreByType(fileName);

            boolean oldSchema = false;

            if (type == DataStoreType.H2_DATABASE || type == DataStoreType.HSQL_DATABASE) {
                oldSchema = SqlUtils.useOldPersistenceUnit(fileName, password);
            }

            if (type == DataStoreType.H2_DATABASE && oldSchema) {
                Platform.runLater(()
                        -> StaticUIMethods.displayMessage(ResourceUtils.getString("Message.Info.LongUpgrade")));

                final PackDatabaseTask packDatabaseTask = new PackDatabaseTask(new File(fileName), password);

                new Thread(packDatabaseTask).start();

                Platform.runLater(() -> StaticUIMethods.displayTaskProgress(packDatabaseTask));

                try {   // block until complete
                    packDatabaseTask.get();
                } catch (final InterruptedException | ExecutionException e) {
                    Logger.getLogger(BootEngineTask.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }

            if (type == DataStoreType.HSQL_DATABASE && oldSchema) {
                final String errorMessage = ResourceUtils.getString("Message.Error.OldHsqlFile");

                Platform.runLater(() -> StaticUIMethods.displayError(errorMessage));
            } else if (version <= 0) {
                final String errorMessage = ResourceUtils.getString("Message.Error.InvalidUserPass");

                Platform.runLater(() -> StaticUIMethods.displayError(errorMessage));

            } else {
                result = true;

                // make a versioned backup first
                if (version < Engine.CURRENT_VERSION) {
                    FileUtils.copyFile(Paths.get(fileName), Paths.get(fileName + "." + version));

                    Platform.runLater(() ->
                            StaticUIMethods.displayMessage(ResourceUtils.getString("Message.Info.Upgrade",
                                    fileName + "." + version)));
                }
            }
        }

        return result;
    }
}
