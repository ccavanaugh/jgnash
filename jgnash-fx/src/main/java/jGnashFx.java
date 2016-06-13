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
import java.net.Authenticator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import javafx.application.Application;
import javafx.stage.Stage;

import jgnash.engine.Engine;
import jgnash.net.security.YahooParser;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.net.NetworkAuthenticator;
import jgnash.uifx.views.main.MainView;
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

    private static Path dataFile = null;
    private static char[] password = new char[]{};

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final MainView mainApplication = new MainView();
        mainApplication.start(primaryStage, dataFile, password);
        password = null;
    }

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

        setupNetworking();

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

            if (options.has(FILE_OPTION_SHORT)) {
                final File file = (File) options.valueOf(FILE_OPTION_SHORT);
                if (file.exists()) {
                    dataFile = file.toPath();
                }
            } else if (!options.nonOptionArguments().isEmpty() && dataFile == null) {
                // Check for no-option version of a file load
                for (Object object : options.nonOptionArguments()) {
                    if (object instanceof String) {
                        if (Files.exists(Paths.get((String)object))) {
                            dataFile = Paths.get((String)object);
                            break;
                        }
                    }
                }
            }
        } catch (final Exception exception) {
            parser.printHelpOn(System.err);
        }

        parser.printHelpOn(System.err);

        launch(args);
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
                        .ofType(File.class).describedAs("file");
                acceptsAll(asList(PORTABLE_OPTION_SHORT, PORTABLE_OPTION_LONG), "Enable portable preferences");
                accepts(PORTABLE_FILE_OPTION, "Enable portable preferences and specify the file")
                        .withRequiredArg().ofType(File.class).describedAs("file");
            }
        };

        parser.allowsUnrecognizedOptions();

        return parser;
    }
}
