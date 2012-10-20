/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import jgnash.engine.Engine;
import jgnash.engine.db4o.Db4oNetworkServer;
import jgnash.net.NetworkAuthenticator;
import jgnash.net.security.AbstractYahooParser;
import jgnash.ui.MainFrame;
import jgnash.ui.UIApplication;
import jgnash.ui.actions.OpenAction;
import jgnash.util.FileUtils;
import jgnash.util.OS;
import jgnash.util.Resource;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * This is the main entry point for the jGnash application.
 * 
 * @author Craig Cavanaugh
 */

public final class Main {

    public static final String VERSION;

    static {
        VERSION = Resource.getAppName() + " - " + Resource.getAppVersion();
    }

    @Option(name = "-opengl", usage = "Enable OpenGL acceleration")
    private boolean opengl;

    @Option(name = "-uninstall", usage = "Remove registry settings")
    private boolean uninstall;

    @Option(name = "-portable", usage = "Enable portable preferences")
    private boolean portable;

    @Option(name = "-portableFile", usage = "Location for portable file")
    private String portableFile;

    @Option(name = "-port", usage = "Network port")
    private int port;

    @Option(name = "-client", usage = "Server host name or address")
    private String client;

    @Option(name = "-file", usage = "File to load at start")
    private File file;

    @Option(name = "-server", usage = "Act as a server using the specified file")
    private File server;

    @Option(name = "-user", usage = "Client or Server user name")
    private String user;

    @Option(name = "-password", usage = "Client or Server password")
    private String password;

    @Option(name = "-enableEDT", usage = "Check for EDT violations")
    private static boolean enableEDT;

    @Option(name = "-verbose", usage = "Enable verbose logging")
    private static boolean verbose;

    @Option(name = "-enableHangDetect", usage = "Enable hang detection on the EDT")
    private static boolean hangDetect;

    final private static int DEFAULT_PORT = 5300;
    
    final private static String DEFAULT_USER = "sa";
    
    final private static String DEFAULT_PASSWORD = "pass";

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
        final float version = getJVMVersion();
        boolean result = true;

        System.out.println(version);

        if (version < 1.7f) {
            System.out.println(Resource.get().getString("Message.JVM7"));
            System.out.println(Resource.get().getString("Message.Version") + " " + System.getProperty("java.version") + "\n");

            // try and show a dialog
            JOptionPane.showMessageDialog(null,Resource.get().getString("Message.JVM7"), Resource.get().getString("Title.Error"), JOptionPane.ERROR_MESSAGE);

            result = false;
        }

        return result;
    }

    private static float getJVMVersion() {
        return Float.parseFloat(System.getProperty("java.version").substring(0, 3));
    }

    private static void configureLogging() {
        final Handler[] handlers = Logger.getLogger("").getHandlers();
        for (Handler handler : handlers) {
            handler.setLevel(Level.ALL);
        }

        Engine.getLogger().setLevel(Level.ALL);
        Logger.getLogger(MainFrame.class.getName()).setLevel(Level.ALL);
        Logger.getLogger(OpenAction.class.getName()).setLevel(Level.ALL);
        Logger.getLogger(AbstractYahooParser.class.getName()).setLevel(Level.ALL);
    }

    private static void enableAntialiasing() {
        System.out.println(Resource.get().getString("Message.Antialias"));
        System.setProperty("swing.aatext", "true");
    }

    /**
     * main method
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
     * Setup the networking to handle authentication requests and work http proxies correctly
     */
    private static void setupNetworking() {
        final Preferences auth = Preferences.userRoot().node(NetworkAuthenticator.NODEHTTP);

        if (auth.getBoolean(NetworkAuthenticator.USEPROXY, false)) {

            String proxyHost = auth.get(NetworkAuthenticator.PROXYHOST, "");
            String proxyPort = auth.get(NetworkAuthenticator.PROXYPORT, "");

            System.getProperties().put("http.proxyHost", proxyHost);
            System.getProperties().put("http.proxyPort", proxyPort);

            // this will deal with any authentication requests properly
            java.net.Authenticator.setDefault(new jgnash.net.NetworkAuthenticator());

            System.out.println(Resource.get().getString("Message.Proxy") + proxyHost + ":" + proxyPort);
        }
    }

    private static void deleteUserPreferences() {
        try {
            Preferences prefs = Preferences.userRoot();
            if (prefs.nodeExists("/jgnash")) {
                Preferences jgnash = prefs.node("/jgnash");
                jgnash.removeNode();
                prefs.flush();
            } else {
                System.err.println(Resource.get().getString("Message.PrefFail"));
            }
        } catch (BackingStoreException bse) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, bse.toString(), bse);
            System.err.println(Resource.get().getString("Message.UninstallBad"));
        }
    }    
    
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    private void init(final String args[]) {        
        configureLogging();

        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);

            /* handle a file name passed in as an argument without use of the -file argument
               assumed behavior for windows users */
            if (args.length == 1 && !args[0].startsWith("-")) {
                File testFile = new File(args[0]);

                if (testFile.exists()) {
                    file = testFile;
                } else {
                    System.err.println(args[0] + " was not a valid file");
                }
            }

            if (port <= 0) {
                port = DEFAULT_PORT;
            }
            
            if (user == null) {
                user = DEFAULT_USER;
            }
            
            if (password == null) {
                password = DEFAULT_PASSWORD;
            }

            /* Dump the registry settings if requested */
            if (uninstall) {
                deleteUserPreferences();
            } else if (server != null) {
                try {
                    if (!FileUtils.isFileLocked(server.getAbsolutePath())) {
                        Db4oNetworkServer netserver = new Db4oNetworkServer(); // Start the db4o server
                        netserver.runServer(server.getAbsolutePath(), port, user, password);
                    } else {
                        System.err.println(Resource.get().getString("Message.FileIsLocked"));
                    }
                } catch (FileNotFoundException e) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.toString(), e);
                    System.err.println("File " + server.getAbsolutePath() + " was not found");
                }
            } else { // start the UI
                if (portable || portableFile != null) { // must hook in the preferences implementation first for best operation
                    System.setProperty("java.util.prefs.PreferencesFactory", "jgnash.util.prefs.MapPreferencesFactory");

                    try {
                        importPreferences();
                    } catch (FileNotFoundException e) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.toString(), e);
                        System.err.println("Preferences file " + getPreferenceFile().getAbsolutePath() + " was not found");
                    }

                    // add a shutdown hook to export user preferences
                    Runtime.getRuntime().addShutdownHook(new ExportPreferencesThread());
                }

                enableAntialiasing();

                if (opengl) {
                    System.out.println(Resource.get().getString("Message.OpenGL"));
                    System.setProperty("sun.java2d.opengl", "True");
                }

                if (OS.isSystemOSX()) {
                    System.setProperty("apple.laf.useScreenMenuBar", "true");
                }

                setupNetworking();

                if (client != null) {
                    new UIApplication(client, port, user, password);
                } else if (file != null && file.exists()) {
                    new UIApplication(file);
                } else {
                    new UIApplication(null);
                }
            }
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }        
    }

    private File getPreferenceFile() {

        File exportFile;

        if (portableFile != null && !portableFile.isEmpty()) {
            exportFile = new File(portableFile);
        } else {
            String base = System.getProperty("user.dir");
            String filesep = System.getProperty("file.separator");
            exportFile = new File(base + filesep + "pref.xml");
        }
        return exportFile;
    }

    /**
     * Import jGnash preferences from the file system
     * 
     * @throws FileNotFoundException thrown if defined preferences file cannot be found
     */
    private void importPreferences() throws FileNotFoundException {
        File importFile = getPreferenceFile();

        if (importFile.canRead()) {
            Logger.getLogger(Main.class.getName()).info("Importing preferences");        

            try(FileInputStream is = new FileInputStream(importFile)) {
                Preferences.importPreferences(is);
            } catch (InvalidPreferencesFormatException | IOException e) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.toString(), e);
            } 
        }
    }

    /* Exports preferences to an XML file */

    private class ExportPreferencesThread extends Thread {
        @Override
        public void run() {
            Logger.getLogger(Main.class.getName()).info("Exporting preferences");

            File exportFile = getPreferenceFile();

            Preferences prefs = Preferences.userRoot();
    
            try( FileOutputStream os = new FileOutputStream(exportFile)) {              
                try {
                    if (prefs.nodeExists("/jgnash")) {
                        Preferences p = prefs.node("/jgnash");
                        p.exportSubtree(os);
                    }
                    deleteUserPreferences();
                } catch (BackingStoreException e) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.toString(), e);
                } 
            } catch (IOException e) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.toString(), e);
            }
        }
    }
}