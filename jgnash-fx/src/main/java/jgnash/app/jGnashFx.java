/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
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
package jgnash.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.Authenticator;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import javafx.application.Application;
import javafx.stage.Stage;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.jpa.JpaNetworkServer;
import jgnash.engine.message.MessageBus;
import jgnash.resource.util.OS;
import jgnash.resource.util.ResourceUtils;
import jgnash.resource.util.Version;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.net.NetworkAuthenticator;
import jgnash.uifx.views.main.MainView;
import jgnash.util.FileUtils;
import jgnash.util.LogUtil;
import jgnash.util.prefs.PortablePreferences;

import picocli.CommandLine;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Help;
import static picocli.CommandLine.IVersionProvider;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParseResult;

import static jgnash.util.LogUtil.logSevere;

/**
 * Main entry point for the application.
 * <p>
 * This bootstraps the JavaFX application and lives in the default class as a workaround for
 * Gnome and OSX menu naming issues.
 *
 * @author Craig Cavanaugh
 */
public class jGnashFx extends Application {

    private static File dataFile = null;

    private static File serverFile = null;

    private static char[] password = EngineFactory.EMPTY_PASSWORD;

    private static int port = JpaNetworkServer.DEFAULT_PORT;

    private static String hostName = null;

    public static void main(final String[] args) {

        if (OS.getJavaVersion() < 11f) {
            System.err.println(ResourceUtils.getString("Message.JVM11"));
            System.err.println(ResourceUtils.getString("Message.Version") + " "
                    + System.getProperty("java.version") + "\n");

            // show a swing based dialog
            JOptionPane.showMessageDialog(null, ResourceUtils.getString("Message.JVM11"),
                    ResourceUtils.getString("Title.Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Register the default exception handler
        Thread.setDefaultUncaughtExceptionHandler(new StaticUIMethods.ExceptionHandler());

        final CommandLine commandLine = new CommandLine(new CommandLineOptions());
        commandLine.setToggleBooleanFlags(false);
        commandLine.setUsageHelpWidth(80);

        try {

            final ParseResult pr = commandLine.parseArgs(args);
            final CommandLineOptions options = commandLine.getCommand();

            if (CommandLine.printHelpIfRequested(pr)) {
                System.exit(0);
            }

            // check for bad server file and hostName combination... can't do both
            if (serverFile != null && options.hostName != null) {
                commandLine.usage(System.err, Help.Ansi.AUTO);
                System.exit(1);
            }

            configureLogging();

            if (options.uninstall) {
                PortablePreferences.deleteUserPreferences();
                System.exit(0);
            }


            //System.getProperties().put(EncryptionManager.ENCRYPTION_FLAG, Boolean.toString(options.ssl));
            //System.getProperties().put("ssl", Boolean.toString(options.ssl));

            port = options.port;
            hostName = options.hostName;
            password = options.password;

            if (options.shutdown) {
                if (hostName == null) {
                    hostName = EngineFactory.LOCALHOST;
                }
                MessageBus.getInstance().shutDownRemoteServer(hostName, port + 1, password);
                System.exit(0);
            }

            if (options.verbose) {
                System.setProperty("javafx.verbose", "true");
            }

            if (options.portableFile != null) {
                PortablePreferences.initPortablePreferences(options.portableFile.getAbsolutePath());
            } else if (options.portable) {
                PortablePreferences.initPortablePreferences(null);
            }

            if (options.zeroArgFile != null && options.zeroArgFile.exists()) {
                jGnashFx.dataFile = options.zeroArgFile;
            }

            if (options.dataFile != null && options.dataFile.exists()) {
                jGnashFx.dataFile = options.dataFile;
            }

            if (options.serverFile != null && options.serverFile.exists()) {
                jGnashFx.serverFile = options.serverFile;
                startServer();
            } else {
                setupNetworking();
                launch(args);
            }
        } catch (final Exception e) {
            logSevere(jGnashFx.class, e);
            commandLine.usage(System.err, Help.Ansi.AUTO);
            System.exit(1);
        }
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final MainView mainView = new MainView();
        mainView.start(primaryStage, dataFile, password, hostName, port);

        if (password != null) {
            Arrays.fill(password, (char) 0);    // clear the password to protect against malicious code
        }
    }

    @Override
    public void stop() {
        System.exit(0); // Platform.exit() is not always enough for a complete shutdown, force closure
    }

    private static void startServer() {
        try {
            if (!FileUtils.isFileLocked(serverFile.getAbsolutePath())) {
                JpaNetworkServer networkServer = new JpaNetworkServer();
                networkServer.startServer(serverFile.getAbsolutePath(), port, password);
                Arrays.fill(password, (char) 0);    // clear the password to protect against malicious code
            } else {
                System.err.println(ResourceUtils.getString("Message.FileIsLocked"));
            }
        } catch (final FileNotFoundException e) {
            logSevere(jGnashFx.class, e);
            System.err.println("File " + serverFile.getAbsolutePath() + " was not found");
        } catch (final Exception e) {
            logSevere(jGnashFx.class, e);
        }
    }

    private static void setupNetworking() {
        final Preferences auth = Preferences.userRoot().node(NetworkAuthenticator.NODEHTTP);

        if (auth.getBoolean(NetworkAuthenticator.USEPROXY, false)) {

            final String proxyHost = auth.get(NetworkAuthenticator.PROXYHOST, "");
            final String proxyPort = auth.get(NetworkAuthenticator.PROXYPORT, "");

            System.getProperties().put("http.proxyHost", proxyHost);
            System.getProperties().put("http.proxyPort", proxyPort);

            // this will deal with any authentication requests properly
            Authenticator.setDefault(new NetworkAuthenticator());

            System.out.println(ResourceUtils.getString("Message.Proxy") + proxyHost + ":" + proxyPort);
        }
    }

    private static void configureLogging() {
        LogUtil.configureLogging();

        final Handler[] handlers = Logger.getLogger("").getHandlers();
        for (final Handler handler : handlers) {
            handler.setLevel(Level.ALL);
        }

        Engine.getLogger().setLevel(Level.ALL);
    }

    @SuppressWarnings({"CanBeFinal", "FieldMayBeFinal"})
    @Command(mixinStandardHelpOptions = true, versionProvider = CommandLineOptions.VersionProvider.class, name = "jGnash", separator = " ", sortOptions = false)
    public static class CommandLineOptions {

        private static final String FILE_OPTION_SHORT = "-f";
        private static final String FILE_OPTION_LONG = "--file";
        private static final String VERBOSE_OPTION_SHORT = "-v";
        private static final String VERBOSE_OPTION_LONG = "--verbose";
        private static final String PORTABLE_FILE_OPTION = "--portableFile";
        private static final String PORTABLE_OPTION_SHORT = "-p";
        private static final String PORTABLE_OPTION_LONG = "--portable";
        private static final String UNINSTALL_OPTION_SHORT = "-u";
        private static final String UNINSTALL_OPTION_LONG = "--uninstall";
        private static final String PORT_OPTION = "--port";
        private static final String HOST_OPTION = "--hostName";
        private static final String PASSWORD_OPTION = "--password";
        private static final String SERVER_OPTION = "--server";
        private static final String SHUTDOWN_OPTION = "--shutdown";
        private static final String BYPASS_BOOTLOADER = "--bypassBootloader";
        //private static final String SSL_OPTION = "--ssl";

        @CommandLine.Parameters(index = "0", arity = "0")
        private File zeroArgFile = null;

        @Option(names = {FILE_OPTION_SHORT, FILE_OPTION_LONG}, paramLabel = "<File>", description = "File to load at start")
        private File dataFile = null;

        @Option(names = {PASSWORD_OPTION}, description = "Password for a local File, server or client")
        private char[] password = EngineFactory.EMPTY_PASSWORD; // must not be null

        @Option(names = {PORTABLE_OPTION_SHORT, PORTABLE_OPTION_LONG}, description = "Enable portable preferences")
        private boolean portable = false;

        @Option(names = {PORTABLE_FILE_OPTION}, paramLabel = "<File>", description = "Enable portable preferences and specify the file")
        private File portableFile = null;

        @Option(names = {HOST_OPTION}, description = "Server host name or address")
        private String hostName = null;

        @Option(names = {PORT_OPTION}, description = "Network port server is running on (default: " + JpaNetworkServer.DEFAULT_PORT + ")")
        private int port = JpaNetworkServer.DEFAULT_PORT;

        //@Option(names = {SSL_OPTION}, description = "Enable SSL for network communication")
        //private boolean ssl = false;

        @Option(names = {SERVER_OPTION}, paramLabel = "<File>", description = "Runs as a server using the specified file")
        private File serverFile = null;

        @Option(names = {SHUTDOWN_OPTION}, description = "Issues a shutdown request to a server")
        private boolean shutdown = false;

        @Option(names = {UNINSTALL_OPTION_SHORT, UNINSTALL_OPTION_LONG}, description = "Remove registry settings (uninstall)")
        private boolean uninstall = false;

        @Option(names = {VERBOSE_OPTION_SHORT, VERBOSE_OPTION_LONG}, description = "Enable verbose application messages")
        private boolean verbose = false;

        @CommandLine.Option(names = {BYPASS_BOOTLOADER}, description = "Bypasses the bootloader and requires manual installation of OS specific files")
        public boolean bypassBootloader = false;

        static class  VersionProvider implements IVersionProvider {
            @Override
            public String[] getVersion() {
                return new String[] {Version.getAppName() + " " + Version.getAppVersion()};
            }
        }
    }
}
