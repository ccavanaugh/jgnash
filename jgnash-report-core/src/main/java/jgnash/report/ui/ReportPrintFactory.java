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
package jgnash.report.ui;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.util.Arrays;
import java.util.Locale;
import java.util.prefs.Preferences;

import jgnash.report.pdf.PageSize;

/**
 * Factory class for handling printing preferences
 * 
 * @author Craig Cavanaugh
 */
public class ReportPrintFactory {
    private static final String HEIGHT = "height";
    private static final String WIDTH = "width";
    private static final String ORIENTATION = "orientation";
    private static final String IMAGEABLE_HEIGHT = "imageableHeight";
    private static final String IMAGEABLE_WIDTH = "imageableWidth";
    private static final String IMAGEABLE_X = "imageableX";
    private static final String IMAGEABLE_Y = "imageableY";

    private static final int DEFAULT_MARGIN = 43; // ~.60" margin

    private ReportPrintFactory() {
    }

    /**
     * Returns the default paper size for a report
     * 
     * @return page format
     */
    public static PageFormat getDefaultPage() {

        /* A4 is the assumed default */
        PageSize defaultPageSize = PageSize.A4;

        /* US and Canada use letter size as the default */
        final Locale[] letterLocales = new Locale[] { Locale.US, Locale.CANADA, Locale.CANADA_FRENCH };
        if (Arrays.asList(letterLocales).contains(Locale.getDefault())) {
            defaultPageSize = PageSize.LETTER;
        }

        /* Create the default paper size with a default margin */
        final Paper paper = new Paper();

        double width = defaultPageSize.width;
        double height = defaultPageSize.height;

        paper.setSize(width, height);
        paper.setImageableArea(DEFAULT_MARGIN, DEFAULT_MARGIN, width - (2 * DEFAULT_MARGIN),
                height - (2 * DEFAULT_MARGIN));

        final PageFormat format = new PageFormat();
        format.setPaper(paper);

        return format;
    }

    /**
     * Save a {@code PageFormat} to preferences
     * 
     * @param p the specific report {@code Preferences}
     * @param format {@code PageFormat} to save
     */
    public static void savePageFormat(final Preferences p, final PageFormat format) {
        p.putInt(ORIENTATION, format.getOrientation());

        final Paper paper = format.getPaper();

        p.putDouble(HEIGHT, paper.getHeight());
        p.putDouble(WIDTH, paper.getWidth());

        p.putDouble(IMAGEABLE_HEIGHT, paper.getImageableHeight());
        p.putDouble(IMAGEABLE_WIDTH, paper.getImageableWidth());
        p.putDouble(IMAGEABLE_X, paper.getImageableX());
        p.putDouble(IMAGEABLE_Y, paper.getImageableY());
    }

    /**
     * Generates a {@code PageFormat} given a {@code Preferences} node
     * @param p {@code Preferences} node
     * @return restored {@code PageFormat} or the default if it has not been saved before
     */
    public static PageFormat getPageFormat(final Preferences p) {
        double height = p.getDouble(HEIGHT, 0);
        double width = p.getDouble(WIDTH, 0);
        int orientation = p.getInt(ORIENTATION, 0);
        double imageableHeight = p.getDouble(IMAGEABLE_HEIGHT, 0);
        double imageableWidth = p.getDouble(IMAGEABLE_WIDTH, 0);
        double imageableX = p.getDouble(IMAGEABLE_X, 0);
        double imageableY = p.getDouble(IMAGEABLE_Y, 0);

        if (height == 0 || width == 0 || imageableHeight == 0 || imageableWidth == 0) {
            return getDefaultPage();
        }

        final PageFormat pf = new PageFormat();

        pf.setOrientation(orientation);

        Paper paper = pf.getPaper();
        paper.setSize(width, height);
        paper.setImageableArea(imageableX, imageableY, imageableWidth, imageableHeight);

        pf.setPaper(paper);

        return pf;
    }
}
