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
package jgnash.net;

import java.net.Authenticator;
import java.util.prefs.Preferences;

/**
 * An Authenticator that will pop up a dialog and ask for http authentication
 * info if it has not assigned. This does not make authentication information
 * permanent. That must be done using the options configuration for http connect
 *
 * @author Craig Cavanaugh
 */
public class AbstractAuthenticator extends Authenticator {

    public static final String NODEHTTP = "/jgnash/http";

    protected static final String HTTPUSER = "httpuser";

    protected static final String HTTPPASS = "httppass";

    public static final String USEPROXY = "useproxy";

    private static final String USEAUTH = "useauth";

    public static final String PROXYHOST = "proxyhost";

    public static final String PROXYPORT = "proxyport";

    public static void setName(String name) {
        final Preferences pref = Preferences.userRoot().node(NODEHTTP);
        pref.put(HTTPUSER, name);
    }

    public static String getName() {
        final Preferences pref = Preferences.userRoot().node(NODEHTTP);
        return pref.get(HTTPUSER, "user");
    }

    public static void setHost(String host) {
        final Preferences pref = Preferences.userRoot().node(NODEHTTP);
        pref.put(PROXYHOST, host);
    }

    public static String getHost() {
        final Preferences pref = Preferences.userRoot().node(NODEHTTP);
        return pref.get(PROXYHOST, "localhost");
    }

    public static void setPort(int port) {
        final Preferences pref = Preferences.userRoot().node(NODEHTTP);
        pref.putInt(PROXYPORT, port);
    }

    public static int getPort() {
        final Preferences pref = Preferences.userRoot().node(NODEHTTP);
        return pref.getInt(PROXYPORT, 8080);
    }

    public static void setPassword(String password) {
        final Preferences pref = Preferences.userRoot().node(NODEHTTP);
        pref.put(HTTPPASS, password);
    }

    public static String getPassword() {
        final Preferences pref = Preferences.userRoot().node(NODEHTTP);
        return pref.get(HTTPPASS, "");
    }

    public static void setUseAuthentication(boolean use) {
        final Preferences pref = Preferences.userRoot().node(NODEHTTP);
        pref.putBoolean(USEAUTH, use);
    }

    public static void setUseProxy(boolean use) {
        final Preferences pref = Preferences.userRoot().node(NODEHTTP);
        pref.putBoolean(USEPROXY, use);
    }

    public static boolean isProxyUsed() {
        final Preferences pref = Preferences.userRoot().node(NODEHTTP);
        return pref.getBoolean(USEPROXY, false);
    }

    public static boolean isAuthenticationUsed() {
        final Preferences pref = Preferences.userRoot().node(NODEHTTP);
        return pref.getBoolean(USEAUTH, false);
    }

}
