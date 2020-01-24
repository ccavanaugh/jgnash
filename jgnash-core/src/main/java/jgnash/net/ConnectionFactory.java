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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.prefs.Preferences;

import jgnash.util.Nullable;

import static jgnash.util.LogUtil.logSevere;

/**
 * Factory methods for setting network connection timeout and creating a URLConnection with timeout set correctly.
 * 
 * @author Craig Cavanaugh
 */
public class ConnectionFactory {

    private static final String TIMEOUT = "timeout";

    private static final int DEFAULT_TIMEOUT = 30;

    public static final int MAX_TIMEOUT = 30;

    public static final int MIN_TIMEOUT = 1;

    public static final int MILLIS_PER_SECOND = 1000;

    private ConnectionFactory() {
    }

    /**
     * Sets the network timeout in seconds.
     * 
     * @param seconds time in seconds before a timeout occurs
     */
    public synchronized static void setConnectionTimeout(final int seconds) {

        if (seconds < MIN_TIMEOUT || seconds > MAX_TIMEOUT) {
            throw new IllegalArgumentException("Invalid timeout connection");
        }

        Preferences pref = Preferences.userNodeForPackage(ConnectionFactory.class);
        pref.putInt(TIMEOUT, seconds);
    }

    /**
     * Return the network connection timeout in seconds.
     * 
     * @return timeout in seconds
     */
    public synchronized static int getConnectionTimeout() {
        Preferences pref = Preferences.userNodeForPackage(ConnectionFactory.class);
        return pref.getInt(TIMEOUT, DEFAULT_TIMEOUT);
    }

    @Nullable
    public synchronized static URLConnection openConnection(final String url) {
        try {
            return openConnection(new URL(url));
        } catch (final MalformedURLException ex) {
            logSevere(ConnectionFactory.class, ex);
        }
        return null;
    }

    @Nullable
    private synchronized static URLConnection openConnection(final URL url) {
        URLConnection connection = null;

        try {
            connection = url.openConnection();
        } catch (final IOException ex) {
            logSevere(ConnectionFactory.class, ex);
        }

        /* Set the connection timeout */
        if (connection != null) {
            connection.setConnectTimeout(getConnectionTimeout() * 1000);
            connection.setReadTimeout(getConnectionTimeout() * 1000);
        }

        return connection;
    }
}
