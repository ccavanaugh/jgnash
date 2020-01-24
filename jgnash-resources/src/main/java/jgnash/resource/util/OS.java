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

/**
 * OS specific detection code.
 * 
 * @author Craig Cavanaugh
 */
public final class OS {

    private static final boolean IS_OSX;

    private static final boolean IS_WINDOWS;

    public static final String JAVA_VERSION = "java.version";

    private static final String JAVA_SPEC_VERSION = "java.specification.version";

    static {
        final String os = System.getProperty("os.name");

        IS_OSX = os.startsWith("Darwin") || os.startsWith("Mac");
        IS_WINDOWS = os.startsWith("Windows");
    }

    private OS() {
        // utility class
    }

    /**
     * Determines if running on OSX.
     * 
     * @return true if running on OSX
     */
    @SuppressWarnings("unused")
    public static boolean isSystemOSX() {
        return IS_OSX;
    }

    /**
     * Determines if running on Windows.
     * 
     * @return true if running on Windows
     */
    public static boolean isSystemWindows() {
        return IS_WINDOWS;
    }

    /**
     * Returns the version of the JVM.
     *
     * @return returns 1.8 given 1.8.0_60
     */
    public static float getJavaVersion() {
        return Float.parseFloat(System.getProperty(JAVA_SPEC_VERSION));
    }

    /**
     * Returns the release of the JVM.
     *
     * @return returns 60 given 1.8.0_60-ea
     */
    public static int getJavaRelease() {
        String release = System.getProperty(JAVA_VERSION).substring(6);

        release = release.replaceAll("[^\\d.]", "").trim();

        return Integer.parseInt(release);
    }
}
