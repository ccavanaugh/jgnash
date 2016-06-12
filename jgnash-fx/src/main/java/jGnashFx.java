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

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final MainView mainApplication = new MainView();
        mainApplication.start(primaryStage);
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
            if (options.has("u")) {
                PortablePreferences.deleteUserPreferences();
                System.exit(0);
            }

            // Needs to be checked for launching
            if (options.has("v")) {
                System.setProperty("javafx.verbose", "true");
            }

            // Check to see if portable preferences are being used
            if (options.has("portableFile")) {
                final File file = (File) options.valueOf("portableFile");
                if (file.exists()) {
                    PortablePreferences.initPortablePreferences(file.getAbsolutePath());
                }
            } else if (options.has("p")) {  // simple use of portable preferences
                PortablePreferences.initPortablePreferences(null);
            }

            if (!options.nonOptionArguments().isEmpty()) {
                // TODO, try to load a file
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

        OptionParser parser = new OptionParser() {
            {
                accepts("help", "This help").forHelp();
                acceptsAll(asList("u", "uninstall"), "Remove registry settings");
                acceptsAll(asList("v", "verbose"), "Enable verbose application messages");
                acceptsAll(asList("p", "portable"), "Enable portable preferences");
                accepts("portableFile", "Enable portable preferences and specify the file")
                        .withRequiredArg().ofType(File.class).describedAs("file");
                //nonOptions("File to load").ofType(File.class);
            }
        };

        parser.allowsUnrecognizedOptions();

        return parser;
    }
}
