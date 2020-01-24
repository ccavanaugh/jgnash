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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jgnash.util.LogUtil;

/**
 * Manages the Cookie and Crumb required to connect to the yahoo finance APIs.
 * <p>
 * The cookie and crumb is cached and reused
 *
 * @author Craig Cavanaugh
 */
public class YahooCrumbManager {

    private static final String SET_COOKIE_KEY = "Set-Cookie";

    private static final String CRUMB_REGEX = ".*\"CrumbStore\":\\{\"crumb\":\"(?<crumb>.+?)\"}.*";

    private static final String EXPIRES = "expires";

    private static final String CRUMB = "crumb";

    private static final String COOKIE = "cookie";

    private static final String DATE_FORMAT = "dd-MMM-yyyy HH:mm:ss z";

    private static String cookie;

    private static String crumb;

    private YahooCrumbManager() {
        // utility class
    }

    /**
     * This scrapes the cookie and crumb from a Yahoo web page.
     * <p>
     * This must be call prior to requesting a cookie or crumb.
     *
     * @param symbol Stock to use to scrape value
     */
    public static synchronized boolean authorize(final String symbol) {
        boolean result = false;

        final Preferences preferences = Preferences.userNodeForPackage(YahooCrumbManager.class);

        if (preferences.get(EXPIRES, null) != null) {

            final ZonedDateTime expires = ZonedDateTime.parse(preferences.get(EXPIRES, null));

            if (ZonedDateTime.now().compareTo(expires) < 0) {
                crumb = preferences.get(CRUMB, null);
                cookie = preferences.get(COOKIE, null);

                if (cookie != null && crumb != null) {
                    return true;
                }
            } else {
                clearAuthorization();
            }

        }

        final String url = "https://finance.yahoo.com/quote/" + symbol + "?p=" + symbol;

        final URLConnection connection = ConnectionFactory.openConnection(url);

        if (connection != null) {

            final Map<String, List<String>> headerFields = connection.getHeaderFields();

            for (final Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                if (SET_COOKIE_KEY.equalsIgnoreCase(entry.getKey())) {
                    final List<String> cookieValue = entry.getValue();

                    if (cookieValue != null) {
                        final String[] values = cookieValue.get(0).split(";");
                        cookie = values[0];

                        preferences.put(COOKIE, cookie);

                        final String expires = values[1].trim();

                        if (expires.startsWith("expires")) {    // old, left should Yahoo switch back
                            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
                            final TemporalAccessor accessor = formatter.parse(expires.split(",")[1].trim());
                            preferences.put(EXPIRES, ZonedDateTime.from(accessor).toString());
                        } else if (expires.startsWith("Max-Age")) {
                            final long seconds = Long.parseLong(expires.split("=")[1].trim());
                            ZonedDateTime expireDate = ZonedDateTime.now().plusSeconds(seconds);
                            preferences.put(EXPIRES, expireDate.toString());
                        } else {
                            preferences.put(EXPIRES, null);
                        }

                        final Pattern pattern = Pattern.compile(CRUMB_REGEX);

                        try (final BufferedReader reader =
                                     new BufferedReader(new InputStreamReader(connection.getInputStream(),
                                             StandardCharsets.UTF_8))) {

                            String line;

                            while ((line = reader.readLine()) != null) {
                                final Matcher matcher = pattern.matcher(line);

                                if (matcher.matches()) {
                                    crumb = matcher.group(1);
                                    preferences.put(CRUMB, crumb);
                                    result = true;
                                    break;
                                }
                            }
                        } catch (final IOException e) {
                            LogUtil.logSevere(YahooCrumbManager.class, e);
                        }
                    }
                }
            }
        }

        return result;
    }

    public static synchronized void clearAuthorization() {
        final Preferences preferences = Preferences.userNodeForPackage(YahooCrumbManager.class);

        preferences.remove(CRUMB);
        preferences.remove(COOKIE);
    }

    public static synchronized String getCookie() {
        return cookie;
    }

    public static synchronized String getCrumb() {
        return crumb;
    }

}
