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
package jgnash;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.jpa.JpaNetworkServer;
import jgnash.engine.message.MessageBus;
import jgnash.net.security.YahooParser;
import jgnash.ui.MainFrame;
import jgnash.ui.UIApplication;
import jgnash.ui.actions.OpenAction;
import jgnash.ui.net.NetworkAuthenticator;
import jgnash.util.FileUtils;
import jgnash.util.OS;
import jgnash.util.ResourceUtils;
import jgnash.util.Version;
import jgnash.util.prefs.PortablePreferences;

import static java.util.Arrays.asList;

/**
 * This is the main entry point for the jGnash application.
 *
 * @author Craig Cavanaugh
 */
public final class Main {

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
    private static final String XRENDER_OPTION = "xrender";
    private static final String OPEN_GL_OPTION = "opengl";
    private static final String EDT_OPTION = "enableEDT";
    private static final String HANG_DETECT_OPTION = "enableHangDetect";
    private static final String SHUTDOWN_OPTION = "shutdown";
    private static final String ENCRYPT_OPTION = "encrypt";

    public static final String VERSION;

    static {
        VERSION = Version.getAppName() + " - " + Version.getAppVersion();
        System.setProperty("awt.useSystemAAFontSettings", "lcd"); // force proper antialias setting
    }

    private boolean portable = false;

    private File portableFile = null;

    private int port = JpaNetworkServer.DEFAULT_PORT;

    private String hostName = null;

    private File file = null;

    private File serverFile = null;

    private char[] password = EngineFactory.EMPTY_PASSWORD;

    private static boolean enableEDT = false;

    private static boolean verbose = false;

    private static boolean hangDetect = false;

    public static boolean checkEDT() {
        return enableEDT;
    }

    public static boolean enableVerboseLogging() {
        return verbose;
    }

    public static boolean enableHangDetection() {
        return hangDetect;
    }

    private static boolean checkJVMVersion() {
        final float version = OS.getJavaVersion();
        boolean result = true;

        System.out.println(version);

        if (version < 1.8f) {
            System.out.println(ResourceUtils.getString("Message.JVM8"));
            System.out.println(ResourceUtils.getString("Message.Version") + " "
                    + System.getProperty("java.version") + "\n");

            // try and show a dialog
            JOptionPane.showMessageDialog(null, ResourceUtils.getString("Message.JVM8"),
                    ResourceUtils.getString("Title.Error"), JOptionPane.ERROR_MESSAGE);

            result = false;
        }

        return result;
    }

    private static void configureLogging() {
        final Handler[] handlers = Logger.getLogger("").getHandlers();
        for (Handler handler : handlers) {
            handler.setLevel(Level.ALL);
        }

        Engine.getLogger().setLevel(Level.ALL);
        MainFrame.logger.setLevel(Level.ALL);
        OpenAction.logger.setLevel(Level.ALL);
        YahooParser.logger.setLevel(Level.ALL);
    }

    private static void enableAntialiasing() {
        System.out.println(ResourceUtils.getString("Message.AntiAlias"));
        System.setProperty("swing.aatext", "true");
    }

    /**
     * main method.
     *
     * @param args command line arguments
     */
    public static void main(final String args[]) {
        if (checkJVMVersion()) {
            Main main = new Main();
            main.init(args);
        }
    }

    /**
     * Setup the networking to handle authentication requests and work http proxies correctly.
     */
    private static void setupNetworking() {
        final Preferences auth = Preferences.userRoot().node(NetworkAuthenticator.NODEHTTP);

        if (auth.getBoolean(NetworkAuthenticator.USEPROXY, false)) {

            String proxyHost = auth.get(NetworkAuthenticator.PROXYHOST, "");
            String proxyPort = auth.get(NetworkAuthenticator.PROXYPORT, "");

            System.getProperties().put("http.proxyHost", proxyHost);
            System.getProperties().put("http.proxyPort", proxyPort);

            // this will deal with any authentication requests properly
            java.net.Authenticator.setDefault(new NetworkAuthenticator());

            System.out.println(ResourceUtils.getString("Message.Proxy") + proxyHost + ":" + proxyPort);
        }
    }

    private void init(final String args[]) {
        configureLogging();

        final OptionParser parser = buildParser();

        try {
            final OptionSet options = parser.parse(args);

            /* handle a file name passed in as an argument without use of the -file argument
               assumed behavior for windows users */
            if (!options.nonOptionArguments().isEmpty()) {
                // Check for no-option version of a file load
                for (final Object object : options.nonOptionArguments()) {
                    if (object instanceof String) {
                        if (Files.exists(Paths.get((String)object))) {
                            file = new File((String) object);
                            break;
                        } else {
                            System.err.println(object + " was not a valid file");
                        }
                    }
                }
            }

            if (options.has(EDT_OPTION)) {
                enableEDT = true;
            }

            if (options.has(VERBOSE_OPTION_SHORT)) {
                verbose = true;
            }

            if (options.has(HANG_DETECT_OPTION)) {
                hangDetect = true;
            }

            /*if (options.has(ENCRYPT_OPTION)) {
                // Set encrypt as a system property
                System.getProperties().put(EncryptionManager.ENCRYPTION_FLAG, Boolean.toString(encrypt));
                System.getProperties().put("ssl", Boolean.toString(encrypt));
            }*/

            if (options.has(PORT_OPTION)) {
                port = (Integer)options.valueOf(PORT_OPTION);
            }

            if (options.has(PASSWORD_OPTION)) {
                password = ((String)options.valueOf(PASSWORD_OPTION)).toCharArray();
            }

            if (options.has(HOST_OPTION)) {
                hostName = (String)options.valueOf(HOST_OPTION);
            }

            if (options.has(FILE_OPTION_SHORT) && file == null) {
                file = (File) options.valueOf(FILE_OPTION_SHORT);
                if (!file.exists()) {
                    file = null;
                }
            }

            if (options.has(SERVER_OPTION)) {
                final File file = (File) options.valueOf(SERVER_OPTION);
                if (file.exists()) {
                    serverFile = file;
                }
            }

            // Check to see if portable preferences are being used
            if (options.has(PORTABLE_FILE_OPTION)) {
                final File file = (File) options.valueOf(PORTABLE_FILE_OPTION);
                if (file.exists()) {
                    portableFile = file;
                }
            } else if (options.has(PORTABLE_OPTION_SHORT)) {  // simple use of portable preferences
                portable = true;
            }

            /* If a shutdown request is found, it trumps any other commandline options */
            if (options.has(SHUTDOWN_OPTION)) {
                if (hostName == null) {
                    hostName = EngineFactory.LOCALHOST;
                }
                MessageBus.getInstance().shutDownRemoteServer(hostName, port + 1, password);
            } else if (options.has(UNINSTALL_OPTION_SHORT)) { /* Dump the registry settings if requested */
                PortablePreferences.deleteUserPreferences();
            } else if (serverFile != null) {
                try {
                    if (!FileUtils.isFileLocked(serverFile.getAbsolutePath())) {
                        JpaNetworkServer networkServer = new JpaNetworkServer();
                        networkServer.startServer(serverFile.getAbsolutePath(), port, password);
                    } else {
                        System.err.println(ResourceUtils.getString("Message.FileIsLocked"));
                    }
                } catch (FileNotFoundException e) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.toString(), e);
                    System.err.println("File " + serverFile.getAbsolutePath() + " was not found");
                } catch (Exception e) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.toString(), e);
                }
            } else { // start the UI
                if (portable || portableFile != null) { // must hook in the preferences implementation first
                    // for best operation
                    PortablePreferences.initPortablePreferences(portableFile.getAbsolutePath());
                }

                enableAntialiasing();

                if (options.has(OPEN_GL_OPTION)) {
                    System.setProperty("sun.java2d.opengl", "True");
                }

                if (options.has(XRENDER_OPTION)) {
                    System.setProperty("sun.java2d.xrender", "True");
                }

                if (OS.isSystemOSX()) {
                    System.setProperty("apple.laf.useScreenMenuBar", "true");
                }

                setupNetworking();

                if (hostName != null) {
                    new UIApplication(hostName, port, password);
                } else if (file != null && file.exists()) {
                    new UIApplication(file, password);
                } else {
                    new UIApplication(null, null);
                }
            }
        } catch (final Exception e) {
            try {
                parser.printHelpOn(System.err);
            } catch (final IOException ioe) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ioe.toString(), ioe);
            }

        }
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
                accepts(PORT_OPTION, "Network port server is running on (default: " + JpaNetworkServer.DEFAULT_PORT
                        + ")").withRequiredArg().ofType(Integer.class);
                accepts(HOST_OPTION, "Server host name or address").requiredIf(PORT_OPTION).withRequiredArg();
                accepts(SERVER_OPTION, "Runs as a server using the specified file")
                        .withRequiredArg().ofType(File.class);
                accepts(XRENDER_OPTION, "Enable the XRender-based Java 2D rendering pipeline");
                accepts(OPEN_GL_OPTION, "Enable OpenGL acceleration");
                accepts(HANG_DETECT_OPTION, "Enable hang detection on the EDT");
                accepts(SHUTDOWN_OPTION, "Issues a shutdown request to a server");
                accepts(EDT_OPTION, "Check for EDT violations");
                accepts(ENCRYPT_OPTION, "Enable encryption for network communication");
            }
        };

        parser.allowsUnrecognizedOptions();

        return parser;
    }
}