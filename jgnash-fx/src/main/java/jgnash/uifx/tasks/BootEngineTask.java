package jgnash.uifx.tasks;

import java.util.ResourceBundle;

import jgnash.engine.EngineFactory;
import jgnash.uifx.StaticUIMethods;
import jgnash.util.ResourceUtils;

import javafx.application.Platform;
import javafx.concurrent.Task;

/**
 * Boots the engine with a local file or connection to a remote server
 *
 * @author Craig Cavanaugh
 */
public class BootEngineTask extends Task<String> {

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
        BootEngineTask bootTask = new BootEngineTask(localFile, password, remote, serverName, port);

        Thread thread = new Thread(bootTask);
        thread.setDaemon(true);
        thread.start();

        StaticUIMethods.displayTaskProgress(bootTask);
    }

    @Override
    protected String call() throws Exception {

        ResourceBundle resources = ResourceUtils.getBundle();

        updateMessage(resources.getString("Message.LoadingFile"));
        updateProgress(INDETERMINATE, Long.MAX_VALUE);

        if (remote) {
            try {
                EngineFactory.bootClientEngine(serverName, port, password, EngineFactory.DEFAULT);
            } catch (final Exception exception) {
                Platform.runLater(() -> StaticUIMethods.displayException(exception));
            }
        } else {
            EngineFactory.bootLocalEngine(localFile, EngineFactory.DEFAULT, password);
        }

        updateMessage(resources.getString("Message.FileLoadComplete"));

        return resources.getString("Message.FileLoadComplete");
    }
}
