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

/**
 * Utility to checks with Github for the latest available release
 */
public class ReleaseCheck {

    private static final String TAG_URL = "https://api.github.com/repos/ccavanaugh/jgnash/tags";

    public static Optional<String> getLatestRelease() {

        try {

            final StringBuilder builder = new StringBuilder();

            final URL url = new URL(TAG_URL);

            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    builder.append(line);
                }
            }

            return Optional.of(builder.toString());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static void main(final String[] args) {
        final Optional<String> result = getLatestRelease();

        if (result.isPresent()) {
            System.out.println(result.get());
        }
    }
}
