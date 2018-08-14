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
package jgnash.report.ui.jasper;

import ar.com.fdvs.dj.domain.constants.Font;

import java.text.NumberFormat;
import java.util.prefs.Preferences;

import jgnash.report.ui.FontRegistry;

/**
 * Factory methods to help with report configuration and generation
 * 
 * @author Craig Cavanaugh
 */
public class ReportFactory {

    /**
     * Preferences key for mono spaced font
     */
    private final static String MONOSPACE = "monospace";

    /**
     * Preferences key for proportional spaced font
     */
    private final static String PROPORTIONAL = "proportional";

    private final static String[] DEFAULT_MONO_FONTS = { "Courier New", "Andale Mono", "Bitstream Vera Sans Mono",
                    "Luxi Mono", "Liberation Mono" };

    private final static String[] DEFAULT_PROPORTIONAL_FONTS = { "Times New Roman", "Bitstream Vera Serif", "Luxi Serif",
                    "Liberation Serif" };

    private ReportFactory() {
    }

    /**
     * Returns a font name for a mono spaced, PDF embeddable font
     * 
     * @return The font name for a mono spaced font
     */
    private static String getDefaultMonoFont() {
        for (String knownFont : DEFAULT_MONO_FONTS) {
            java.awt.Font f = new java.awt.Font(knownFont, java.awt.Font.PLAIN, 1);
            if (f.getFamily().equalsIgnoreCase(knownFont)) {
                return knownFont; // it found it!
            }
        }

        return "Monospaced"; // fail safe
    }

    /**
     * Returns a font name for a proportional, PDF embeddable font
     * 
     * @return The font name for a proportional spaced font
     */
    private static String getDefaultProportionalFont() {
        for (String knownFont : DEFAULT_PROPORTIONAL_FONTS) {
            java.awt.Font f = new java.awt.Font(knownFont, java.awt.Font.PLAIN, 1);
            if (f.getFamily().equalsIgnoreCase(knownFont)) {
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
     * Sets the name of the mono spaced font to use
     * 
     * @param font font name to use
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

    private static Font buildFont(final int size, final String font, final boolean bold, final boolean italic, final boolean underline) {
        Font f = new Font(size, font, bold, italic, underline);
        f.setFontName(font);

        String fontPath = FontRegistry.getRegisteredFontPath(font);
        if (fontPath != null) {
            f.setPdfFontEmbedded(true);
            f.setPdfFontName(fontPath);
            f.setPdfFontEncoding(Font.PDF_ENCODING_Identity_H_Unicode_with_horizontal_writing);
        }

        return f;
    }

    public static Font getDefaultProportionalFont(final int size) {
        return buildFont(size, getProportionalFont(), false, false, false);
    }

    public static Font getDefaultProportionalFont(final int size, final boolean bold) {
        return buildFont(size, getProportionalFont(), bold, false, false);
    }

    public static Font getDefaultProportionalFont(final int size, final boolean bold, final boolean italic) {
        return buildFont(size, getProportionalFont(), bold, italic, false);
    }

    public static Font getDefaultMonoFont(final int size) {
        return buildFont(size, getMonoFont(), false, false, false);
    }

    public static Font getDefaultMonoFont(final int size, final boolean bold) {
        return buildFont(size, getMonoFont(), bold, false, false);
    }

    public static NumberFormat getQuantityFormat() {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(4);
        nf.setMinimumFractionDigits(4);

        return nf;
    }

    public static NumberFormat getPercentageFormat() {
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(2);

        return nf;
    }
}
