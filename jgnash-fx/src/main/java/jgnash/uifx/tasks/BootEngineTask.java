package jgnash.uifx.tasks;

import java.io.File;
import java.nio.file.Files;
import java.util.ResourceBundle;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.StaticUIMethods;
import jgnash.util.FileUtils;
import jgnash.util.Resource;
import jgnash.util.ResourceUtils;

import javafx.application.Platform;
import javafx.concurrent.Task;

/**
 * Boots the engine with a local file or connection to a remote server
 *
 * @author Craig Cavanaugh
 */
public class BootEngineTask extends Task<String> {

    private static final int FORCED_DELAY = 1500;

    private static final int INDETERMINATE = -1;

    private boolean remote;
    private String localFile;
    private char[] password;
    private String serverName;
    private int port;

    private BootEngineTask(final String localFile, final char[] password, final boolean remote, final String serverName, final int port) {
        this.localFile = localFile;
        this.password = password;
        this.remote = remote;
        this.serverName = serverName;
        this.port = port;
    }

    public static void initiateBoot(final String localFile, final char[] password, final boolean remote, final String serverName, final int port) {
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
     * Check and determine if the file is an old format and backup in necessary
     * @param fileName fileName to verify
     * @param password assumed password
     *
     * @return true if no errors are encountered
     */
    private static boolean checkAndBackupOldVersion(final String fileName, final char[] password) {

        boolean result = false;

        if (Files.exists(new File(fileName).toPath())) {
            float version = EngineFactory.getFileVersion(new File(fileName), password);

            if (version <= 0) {
                final String errorMessage = ResourceUtils.getBundle().getString("Message.Error.InvalidUserPass");

                Platform.runLater(() -> StaticUIMethods.displayError(errorMessage));

            } else {
                result = true;

                // make a versioned backup first
                if (version < Engine.CURRENT_VERSION) {
                    FileUtils.copyFile(new File(fileName), new File(fileName + "." + version));

                    Platform.runLater(() -> {
                        final Resource rb = Resource.get();
                        final String message = rb.getString("Message.Info.Upgrade", fileName + "." + version);

                        StaticUIMethods.displayMessage(message);
                    });
                }
            }
        }

        return result;
    }
}
