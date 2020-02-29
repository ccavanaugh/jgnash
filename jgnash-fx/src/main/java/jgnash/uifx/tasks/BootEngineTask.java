package jgnash.uifx.tasks;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javafx.concurrent.Task;

import jgnash.engine.EngineFactory;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.util.FileUtils;

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
                
        final String lockedMessage = resources.getString("Message.FileIsLocked") + ": " + localFile;
        String message = resources.getString("Message.FileLoadComplete");

        if (remote) {
            try {
                EngineFactory.bootClientEngine(serverName, port, password, EngineFactory.DEFAULT);
            } catch (final Exception exception) {
                JavaFXUtils.runLater(() -> StaticUIMethods.displayException(exception));
            }
        } else {
            if (!Files.exists(Paths.get(localFile))) {
                message = resources.getString("Message.Error.FileNotFound") + ": " + localFile;
                updateMessage(message);
            } else if (FileUtils.isFileLocked(localFile)) {
                final Runnable UIRunnable = () -> StaticUIMethods.displayError(lockedMessage);

                if (FileUtils.isLockFileStale(localFile)) {
                    // try to remove the lock file first
                    Logger.getLogger(BootEngineTask.class.getName()).info("Attempting to remove stale file lock");

                    if (FileUtils.deleteLockFile(localFile)) {
                        return call();  // recursive call to rerun the task and load the file to keep code simple
                    }

                    JavaFXUtils.runLater(UIRunnable);
                } else {
                    message = lockedMessage;
                    updateMessage(message);
                    JavaFXUtils.runLater(UIRunnable);
                }
            } else  {
                EngineFactory.bootLocalEngine(localFile, EngineFactory.DEFAULT, password);
                updateMessage(resources.getString("Message.FileLoadComplete"));
                Thread.sleep(FORCED_DELAY); // force delay for better visual feedback
            }
        }

        return message;
    }
}
