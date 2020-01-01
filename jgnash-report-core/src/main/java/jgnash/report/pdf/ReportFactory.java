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
package jgnash.report.pdf;

import java.util.List;
import java.util.prefs.Preferences;

/**
 * Factory methods to help with report configuration and generation
 * 
 * @author Craig Cavanaugh
 */
public class ReportFactory {

    /**
     * Preferences key for mono spaced report
     */
    private final static String MONOSPACE = "monospace";

    /**
     * Preferences key for proportional spaced report
     */
    private final static String PROPORTIONAL = "proportional";

    /**
     * Preferences key for headers / footers / titles
     */
    private final static String HEADER = "header";

    private final static String[] DEFAULT_MONO_FONTS = { "Courier New", "Andale Mono", "Noto Sans Mono Regular",
                    "Luxi Mono", "Liberation Mono", "Comic Sans MS" };

    private final static String[] DEFAULT_PROPORTIONAL_FONTS = { "Times New Roman", "Noto Sans Mono Regular", "Luxi Serif",
                    "Liberation Serif" };

    private final static String[] DEFAULT_HEADER_FONTS = { "Arial Bold", "Noto Sans Bold", "Luxi Serif",
            "Liberation Serif" };

    private ReportFactory() {
    }

    /**
     * Returns a report name for a mono spaced, PDF embeddable report
     * 
     * @return The report name for a mono spaced report
     */
    private static String getDefaultMonoFont() {

        final List<String> fonts = FontRegistry.getFontList();

        for (String knownFont : DEFAULT_MONO_FONTS) {
            if (fonts.contains(knownFont)) {
                return knownFont; // it found it!
            }
        }
        return "Monospaced"; // fail safe
    }

    /**
     * Returns a report name for a proportional, PDF embeddable report
     * 
     * @return The report name for a proportional spaced report
     */
    private static String getDefaultProportionalFont() {

        final List<String> fonts = FontRegistry.getFontList();

        for (String knownFont : DEFAULT_PROPORTIONAL_FONTS) {
            if (fonts.contains(knownFont)) {
                return knownFont; // it found it!
            }
        }
        return "SansSerif"; // fail safe
    }

    /**
     * Returns a font name for a proportional, PDF embeddable report
     *
     * @return The font name for headers
     */
    private static String getDefaultHeaderFont() {

        final List<String> fonts = FontRegistry.getFontList();

        for (String knownFont : DEFAULT_HEADER_FONTS) {
            if (fonts.contains(knownFont)) {
                return knownFont; // it found it!
            }
        }
        return "SansSerif"; // fail safe
    }

    /**
     * Returns the name of the mono spaced font to use
     * 
     * @return name of the mono spaced font to use
     */
    public static String getMonoFont() {
        Preferences p = Preferences.userNodeForPackage(ReportFactory.class);
        return p.get(MONOSPACE, getDefaultMonoFont());
    }

    /**
     * Returns the name of the proportional spaced font to use
     * 
     * @return name of the proportional spaced font to use
     */
    public static String getProportionalFont() {
        Preferences p = Preferences.userNodeForPackage(ReportFactory.class);
        return p.get(PROPORTIONAL, getDefaultProportionalFont());
    }

    /**
     * Returns the name of the proportional spaced font to use
     *
     * @return name of the proportional spaced font to use
     */
    public static String getHeaderFont() {
        Preferences p = Preferences.userNodeForPackage(ReportFactory.class);
        return p.get(HEADER, getDefaultHeaderFont());
    }

    /**
     * Sets the name of the mono spaced font to use
     * 
     * @param font report name to use
     */
    public static void setMonoFont(final String font) {
        Preferences p = Preferences.userNodeForPackage(ReportFactory.class);
        p.put(MONOSPACE, font);
    }

    /**
     * Sets the name of the proportional spaced font to use
     * 
     * @param font font name to use
     */
    public static void setProportionalFont(final String font) {
        Preferences p = Preferences.userNodeForPackage(ReportFactory.class);
        p.put(PROPORTIONAL, font);
    }

    /**
     * Sets the name of the header font to use
     *
     * @param font font name to use
     */
    public static void setHeaderFont(final String font) {
        Preferences p = Preferences.userNodeForPackage(ReportFactory.class);
        p.put(HEADER, font);
    }
}
