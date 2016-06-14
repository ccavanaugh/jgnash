/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.Authenticator;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import javafx.application.Application;
import javafx.stage.Stage;

import jgnash.engine.Engine;
import jgnash.engine.jpa.JpaNetworkServer;
import jgnash.net.security.YahooParser;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.net.NetworkAuthenticator;
import jgnash.uifx.views.main.MainView;
import jgnash.util.FileUtils;
import jgnash.util.OS;
import jgnash.util.ResourceUtils;
import jgnash.util.prefs.PortablePreferences;

import static java.util.Arrays.asList;

/**
 * Main entry for the application.
 * <p>
 * This bootstraps the JavaFX application and lives in the default class as a workaround for
 * Gnome and OSX menu naming issues.
 *
 * @author Craig Cavanaugh
 */
public class jGnashFx extends Application {

    private static final String FILE_OPTION_SHORT = "f";
    private static final String FILE_OPTION_LONG = "file";
    private static final String VERBOSE_OPTION_SHORT = "v";
    private static final String VERBOSE_OPTION_LONG = "verbose";
    private static final String PORTABLE_FILE_OPTION = "portableFile";
    private static final String PORTABLE_OPTION_SHORT = "p";
    private static final String PORTABLE_OPTION_LONG = "portable";
    private static final String UNINSTALL_OPTION_SHORT = "u";
    private static final String UNINSTALL_OPTION_LONG = "uninstall";
    private static final String HELP_OPTION_SHORT = "h";
    private static final String HELP_OPTION_LONG = "help";
    private static final String PORT_OPTION = "port";
    private static final String HOST_OPTION = "host";
    private static final String PASSWORD_OPTION = "password";
    private static final String SERVER_OPTION = "server";

    private static File dataFile = null;
    private static File serverFile = null;
    private static char[] password = new char[]{};
    private static int port = JpaNetworkServer.DEFAULT_PORT;
    private static String host = null;

    public static void main(final String[] args) throws Exception {
        if (OS.getJavaVersion() < 1.8f) {
            System.out.println(ResourceUtils.getString("Message.JVM8"));
            System.out.println(ResourceUtils.getString("Message.Version") + " "
                    + System.getProperty("java.version") + "\n");

            // try and show a dialog
            JOptionPane.showMessageDialog(null, ResourceUtils.getString("Message.JVM8"),
                    ResourceUtils.getString("Title.Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (OS.getJavaRelease() < OS.JVM_RELEASE_60) {
            JOptionPane.showMessageDialog(null, ResourceUtils.getString("Message.JFX"),
                    ResourceUtils.getString("Title.Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Register the default exception handler
        Thread.setDefaultUncaughtExceptionHandler(new StaticUIMethods.ExceptionHandler());

        configureLogging();

        final OptionParser parser = buildParser();

        try {
            final OptionSet options = parser.parse(args);

            // Does the user want to uninstall and clear their registry settings
            if (options.has(UNINSTALL_OPTION_SHORT)) {
                PortablePreferences.deleteUserPreferences();
                System.exit(0);
            }

            // Needs to be checked for launching
            if (options.has(VERBOSE_OPTION_SHORT)) {
                System.setProperty("javafx.verbose", "true");
            }

            // Check to see if portable preferences are being used
            if (options.has(PORTABLE_FILE_OPTION)) {
                final File file = (File) options.valueOf(PORTABLE_FILE_OPTION);
                if (file.exists()) {
                    PortablePreferences.initPortablePreferences(file.getAbsolutePath());
                }
            } else if (options.has(PORTABLE_OPTION_SHORT)) {  // simple use of portable preferences
                PortablePreferences.initPortablePreferences(null);
            }

            if (options.has(PORT_OPTION)) {
                port = (Integer)options.valueOf(PORT_OPTION);
            }

            if (options.has(PASSWORD_OPTION)) {
                password = ((String)options.valueOf(PASSWORD_OPTION)).toCharArray();
            }

            if (options.has(FILE_OPTION_SHORT)) {
                final File file = (File) options.valueOf(FILE_OPTION_SHORT);
                if (file.exists()) {
                    dataFile = file;
                }
            } else if (!options.nonOptionArguments().isEmpty() && dataFile == null) {
                // Check for no-option version of a file load
                for (Object object : options.nonOptionArguments()) {
                    if (object instanceof String) {
                        if (Files.exists(Paths.get((String)object))) {
                            dataFile = new File((String) object);
                            break;
                        }
                    }
                }
            }

            if (options.has(HOST_OPTION)) {
                host = (String)options.valueOf(HOST_OPTION);
            }

            if (options.has(SERVER_OPTION)) {
                final File file = (File) options.valueOf(SERVER_OPTION);
                if (file.exists()) {
                    serverFile = file;
                }
            }

            //parser.printHelpOn(System.err);

            if (serverFile != null) {
                startServer();
            } else {
                setupNetworking();
                launch(args);
            }
        } catch (final Exception exception) {
            parser.printHelpOn(System.err);
        }
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final MainView mainView = new MainView();
        mainView.start(primaryStage, dataFile, password, host, port);
        password = new char[]{};    // clear the password to protect against malicious code
    }

    private static void startServer() {
        try {
            if (!FileUtils.isFileLocked(serverFile.getAbsolutePath())) {
                JpaNetworkServer networkServer = new JpaNetworkServer();
                networkServer.startServer(serverFile.getAbsolutePath(), port, password);
            } else {
                System.err.println(ResourceUtils.getString("Message.FileIsLocked"));
            }
        } catch (FileNotFoundException e) {
            Logger.getLogger(jGnashFx.class.getName()).log(Level.SEVERE, e.toString(), e);
            System.err.println("File " + serverFile.getAbsolutePath() + " was not found");
        } catch (Exception e) {
            Logger.getLogger(jGnashFx.class.getName()).log(Level.SEVERE, e.toString(), e);
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
        final Handler[] handlers = Logger.getLogger("").getHandlers();
        for (final Handler handler : handlers) {
            handler.setLevel(Level.ALL);
        }

        Engine.getLogger().setLevel(Level.ALL);
        YahooParser.logger.setLevel(Level.ALL);
    }

    private static OptionParser buildParser() {
        final OptionParser parser = new OptionParser() {
            {
                acceptsAll(asList(HELP_OPTION_SHORT, HELP_OPTION_LONG), "This help").forHelp();
                acceptsAll(asList(UNINSTALL_OPTION_SHORT, UNINSTALL_OPTION_LONG), "Remove registry settings");
                acceptsAll(asList(VERBOSE_OPTION_SHORT, VERBOSE_OPTION_LONG), "Enable verbose application messages");
                acceptsAll(asList(FILE_OPTION_SHORT, FILE_OPTION_LONG), "File to load at start").withRequiredArg()
                        .ofType(File.class);
                accepts(PASSWORD_OPTION, "Password for a local File, server or client").withRequiredArg();
                acceptsAll(asList(PORTABLE_OPTION_SHORT, PORTABLE_OPTION_LONG), "Enable portable preferences");
                accepts(PORTABLE_FILE_OPTION, "Enable portable preferences and specify the file")
                        .withRequiredArg().ofType(File.class);
                accepts(PORT_OPTION, "Network port server is running on (default: 5300)").withRequiredArg()
                        .ofType(Integer.class);
                accepts(HOST_OPTION, "Server host name or address").requiredIf(PORT_OPTION).withRequiredArg();
                accepts(SERVER_OPTION, "Runs as a server using the specified file")
                        .withRequiredArg().ofType(File.class);
            }
        };

        parser.allowsUnrecognizedOptions();

        return parser;
    }
}
