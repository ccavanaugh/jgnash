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
package jgnash.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for application version
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

            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    builder.append(line);
                }
            }

            final Pattern pattern = Pattern.compile(REGEX);
            final Matcher matcher = pattern.matcher(builder.toString());

            while (matcher.find()) {
                if (matcher.group(1).equals(JSON_NAME)) {
                    if (matcher.find()) {
                        return Optional.ofNullable(matcher.group(1));
                    }
                }
            }

            return Optional.empty();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static boolean isReleaseCurrent() {
        Optional<String> release = getLatestGitHubRelease();
        if (release.isPresent()) {

            // quick check
            if (getAppVersion().equals( release.get())) {
                return true;
            } else {
                String gitVersion[] = release.get().split(".");
                String thisVersion[] = getAppVersion().split(".");
            }

        }
        return true;
    }

    public static void main(final String[] args) {
        System.out.println(isReleaseCurrent());
    }
}
