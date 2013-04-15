/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
package jgnash.net;

import java.awt.GridLayout;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * An Authenticator that will pop up a dialog and ask for http authentication
 * info if it has not assigned. This does not make authentication information
 * permanent. That must be done using the options configuration for http connect
 *
 * @author Craig Cavanaugh
 */
public class NetworkAuthenticator extends Authenticator {

    public static final String NODEHTTP = "/jgnash/http";

    private static final String HTTPUSER = "httpuser";

    private static final String HTTPPASS = "httppass";

    public static final String USEPROXY = "useproxy";

    private static final String USEAUTH = "useauth";

    public static final String PROXYHOST = "proxyhost";

    public static final String PROXYPORT = "proxyport";

    private final Preferences auth = Preferences.userRoot().node(NODEHTTP);

    public static void setName(String name) {
        Preferences pref = Preferences.userRoot().node(NODEHTTP);
        pref.put(HTTPUSER, name);
    }

    public static String getName() {
        Preferences pref = Preferences.userRoot().node(NODEHTTP);
        return pref.get(HTTPUSER, "user");
    }

    public static void setHost(String host) {
        Preferences pref = Preferences.userRoot().node(NODEHTTP);
        pref.put(PROXYHOST, host);
    }

    public static String getHost() {
        Preferences pref = Preferences.userRoot().node(NODEHTTP);
        return pref.get(PROXYHOST, "localhost");
    }

    public static void setPort(int port) {
        Preferences pref = Preferences.userRoot().node(NODEHTTP);
        pref.putInt(PROXYPORT, port);
    }

    public static int getPort() {
        Preferences pref = Preferences.userRoot().node(NODEHTTP);
        return pref.getInt(PROXYPORT, 8080);
    }

    public static void setPassword(String password) {
        Preferences pref = Preferences.userRoot().node(NODEHTTP);
        pref.put(HTTPPASS, password);
    }

    public static String getPassword() {
        Preferences pref = Preferences.userRoot().node(NODEHTTP);
        return pref.get(HTTPPASS, "");
    }

    public static void setUseAuthentication(boolean use) {
        Preferences pref = Preferences.userRoot().node(NODEHTTP);
        pref.putBoolean(USEAUTH, use);
    }

    public static void setUseProxy(boolean use) {
        Preferences pref = Preferences.userRoot().node(NODEHTTP);
        pref.putBoolean(USEPROXY, use);
    }

    public static boolean isProxyUsed() {
        Preferences pref = Preferences.userRoot().node(NODEHTTP);
        return pref.getBoolean(USEPROXY, false);
    }

    public static boolean isAuthenticationUsed() {
        Preferences pref = Preferences.userRoot().node(NODEHTTP);
        return pref.getBoolean(USEAUTH, false);
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        char pass[] = null;
        String user;

        // get the password
        String _pass = auth.get(HTTPPASS, null);
        if (_pass != null) {
            if (!_pass.isEmpty()) {
                pass = _pass.toCharArray();
            }
        }

        // get the user
        user = auth.get(HTTPUSER, null);
        if (user != null) {
            if (user.length() <= 0) {
                user = null;
            }
        }

        // if either returns null, pop a dialog
        if (user == null || pass == null) {
            JTextField username = new JTextField();
            JPasswordField password = new JPasswordField();
            JPanel panel = new JPanel(new GridLayout(2, 2));
            panel.add(new JLabel("User Name"));
            panel.add(username);
            panel.add(new JLabel("Password"));
            panel.add(password);
            int option = JOptionPane.showConfirmDialog(null, new Object[]{"Site: " + getRequestingHost(),
                    "Realm: " + getRequestingPrompt(), panel}, "Enter Network Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option == JOptionPane.OK_OPTION) {
                user = username.getText();
                pass = password.getPassword();
            } else {
                return null;
            }
        }

        return new PasswordAuthentication(user, pass);
    }
}
