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
package jgnash.resource.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for application version.
 *
 * @author Craig Cavanaugh
 */
public class Version {

    private static final String JGNASH_RESOURCE_CONSTANTS = "jgnash/resource/constants";

    private static final String VERSION = "version";

    private static final String NAME = "name";

    private static final String JSON_NAME = "name";

    private static final String TAG_URL = "https://api.github.com/repos/ccavanaugh/jgnash/tags";

    private static final String REGEX = "\"(.+?)\"";

    private static final int CONNECT_TIMEOUT = 5000;

    private Version() {
        // Utility class
    }

    public static String getAppVersion() {
        return ResourceBundle.getBundle(JGNASH_RESOURCE_CONSTANTS).getString(VERSION);
    }

    public static String getAppName() {
        return ResourceBundle.getBundle(JGNASH_RESOURCE_CONSTANTS).getString(NAME);
    }

    public static Optional<String> getLatestGitHubRelease() {

        try {
            final StringBuilder builder = new StringBuilder();

            final URL url = new URL(TAG_URL);
            final URLConnection connection = url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);

            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(),
                    StandardCharsets.UTF_8))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    builder.append(line);
                }
            }

            final Pattern pattern = Pattern.compile(REGEX);
            final Matcher matcher = pattern.matcher(builder.toString());

            while (matcher.find()) {
                if (matcher.group(1).equals(JSON_NAME) && matcher.find()) {
                    return Optional.ofNullable(matcher.group(1));
                }
            }

            return Optional.empty();
        } catch (IOException ioe) {
            if (ioe.getLocalizedMessage().contains("403 for URL")) {  // known github error, return the current version
                return Optional.of(getAppVersion());
            }
            return Optional.empty();
        } catch (final Exception e) {
            Logger.getLogger(Version.class.getName()).log(Level.WARNING, e.getLocalizedMessage(), e);
            return Optional.empty();
        }
    }

    public static boolean isReleaseCurrent() {
        return isReleaseCurrent(getAppVersion());
    }

    public static boolean isReleaseCurrent(final String version) {
        boolean currentRelease = true;

        final Optional<String> release = getLatestGitHubRelease();

        if (release.isPresent()) {
            // quick check
            if (version.equals(release.get())) {
                return true;
            } else if (version.startsWith("${")) {   // running withing a development environment
                return true;
            } else {
                final String[] gitVersion = release.get().split("\\.");
                final String[] thisVersion = version.split("\\.");

                for (int i = 0; i < 3; i++) {   // x.x.x is tested for
                    if (i < gitVersion.length && i < thisVersion.length) {
                        try {
                            final int current = Integer.parseInt(thisVersion[i]);
                            final int git = Integer.parseInt(gitVersion[i]);

                            if (git > current) {
                                currentRelease = false;
                                break;
                            } else if (current > git) {    // newer release than git
                                break;
                            }
                        } catch (final NumberFormatException e) {
                            Logger.getLogger(Version.class.getName()).log(Level.INFO, e.getLocalizedMessage(), e);
                        }
                    }
                }

                if (gitVersion.length > thisVersion.length && currentRelease) {
                    currentRelease = false;
                }
            }
        }
        return currentRelease;
    }
}
