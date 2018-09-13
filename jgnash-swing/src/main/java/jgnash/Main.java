/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.*;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.resource.util.OS;
import jgnash.resource.util.ResourceUtils;
import jgnash.resource.util.Version;
import jgnash.ui.MainFrame;
import jgnash.ui.UIApplication;
import jgnash.ui.actions.OpenAction;
import jgnash.ui.net.NetworkAuthenticator;
import jgnash.util.LogUtil;

/**
 * This is the main entry point for the jGnash application.
 *
 * @author Craig Cavanaugh
 */
public final class Main {

    public static final String VERSION;

    static {
        VERSION = Version.getAppName() + " - " + Version.getAppVersion();
        System.setProperty("awt.useSystemAAFontSettings", "lcd"); // force proper antialias setting
    }

    private File file = null;

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

        LogUtil.configureLogging();

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

    @SuppressWarnings("unused")
    private void init(final String args[]) {
        configureLogging();

        if (args.length > 0) {
            if (Files.exists(Paths.get(args[0]))) {
                file = new File(args[0]);
            }
            System.err.println(args[0] + " was not a valid file");
        }

        setupNetworking();

        enableAntialiasing();

        if (file != null && file.exists()) {
            new UIApplication(file.toPath(), EngineFactory.EMPTY_PASSWORD);
        } else {
            new UIApplication(null, null);
        }
    }
}
