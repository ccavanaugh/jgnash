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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import jgnash.engine.Engine;
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

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * This is the main entry point for the jGnash application.
 *
 * @author Craig Cavanaugh
 */
@SuppressWarnings("CanBeFinal")
public final class Main {

    public static final String VERSION;

    static {
        VERSION = Version.getAppName() + " - " + Version.getAppVersion();
        System.setProperty("awt.useSystemAAFontSettings", "lcd"); // force proper antialias setting
    }

    @Option(name = "-xrender", usage = "Enable the XRender-based Java 2D rendering pipeline")
    private boolean xrender;

    @Option(name = "-opengl", usage = "Enable OpenGL acceleration")
    private boolean opengl;

    @Option(name = "-uninstall", usage = "Remove registry settings")
    private boolean uninstall;

    @Option(name = "-portable", usage = "Enable portable preferences")
    private boolean portable;

    @Option(name = "-portableFile", usage = "Location for portable file")
    private String portableFile;

    @Option(name = "-shutdown", usage = "Issues a shutdown request to a server")
    private boolean shutdown;

    @Option(name = "-port", usage = "Network port server is running on; default is 5300")
    private int port;

    @Option(name = "-host", usage = "Server host name or address")
    private String hostName;

    @Option(name = "-file", usage = "File to load at start")
    private File file;

    @Option(name = "-server", usage = "Act as a server using the specified file")
    private File serverFile;

    @Option(name = "-password", usage = "Client or Server password")
    private String password;

    @Option(name = "-encrypt", usage = "Enable encryption for network communication")
    private boolean encrypt;

    @Option(name = "-enableEDT", usage = "Check for EDT violations")
    private static boolean enableEDT;

    @Option(name = "-verbose", usage = "Enable verbose logging")
    private static boolean verbose;

    @Option(name = "-enableHangDetect", usage = "Enable hang detection on the EDT")
    private static boolean hangDetect;

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
            System.out.println(ResourceUtils.getString("Message.Version") + " " + System.getProperty("java.version") + "\n");

            // try and show a dialog
            JOptionPane.showMessageDialog(null, ResourceUtils.getString("Message.JVM8"), ResourceUtils.getString("Title.Error"), JOptionPane.ERROR_MESSAGE);

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
    @SuppressWarnings("unused")
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

    @SuppressWarnings({ "ResultOfObjectAllocationIgnored", "unused" })
    private void init(final String args[]) {
        configureLogging();

        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);

            /* handle a file name passed in as an argument without use of the -file argument
               assumed behavior for windows users */
            if (args.length == 1 && args[0].charAt(0) != '-') {
                File testFile = new File(args[0]);

                if (testFile.exists()) {
                    file = testFile;
                } else {
                    System.err.println(args[0] + " was not a valid file");
                }
            }

            // Set encrypt as a system property
            //System.getProperties().put(EncryptionManager.ENCRYPTION_FLAG, Boolean.toString(encrypt));
            //System.getProperties().put("ssl", Boolean.toString(encrypt));

            if (port <= 0) {
                port = JpaNetworkServer.DEFAULT_PORT;
            }

            if (password == null) {
                password = JpaNetworkServer.DEFAULT_PASSWORD;
            }

            /* If a shutdown request is found, it trumps any other commandline options */
            if (shutdown) {
                String serverName = "localhost";

                if (hostName != null) {
                    serverName = hostName;
                }

                MessageBus.getInstance().shutDownRemoteServer(serverName, port + 1, password.toCharArray());
            } else if (uninstall) { /* Dump the registry settings if requested */
                PortablePreferences.deleteUserPreferences();
            } else if (serverFile != null) {
                try {
                    if (!FileUtils.isFileLocked(serverFile.getAbsolutePath())) {
                        JpaNetworkServer networkServer = new JpaNetworkServer();
                        networkServer.startServer(serverFile.getAbsolutePath(), port, password.toCharArray());
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
                    PortablePreferences.initPortablePreferences(portableFile);
                }

                enableAntialiasing();

                if (opengl) {
                    System.setProperty("sun.java2d.opengl", "True");
                }

                if (xrender) {
                    System.setProperty("sun.java2d.xrender", "True");
                }

                if (OS.isSystemOSX()) {
                    System.setProperty("apple.laf.useScreenMenuBar", "true");
                }

                setupNetworking();

                if (hostName != null) {
                    new UIApplication(hostName, port, password.toCharArray());
                } else if (file != null && file.exists()) {
                    new UIApplication(file, password.toCharArray());
                } else {
                    new UIApplication(null, null);
                }
            }
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }
}