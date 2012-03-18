/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
package jgnash.ui.report;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.util.prefs.Preferences;

import javax.print.PrintService;

import jgnash.ui.report.jasper.DynamicJasperReport;

/**
 * Factory class for handling printing preferences
 *
 * @author Craig Cavanaugh
 *
 */
public class ReportPrintFactory {
    private static final String HEIGHT = "height";
    private static final String WIDTH = "width";
    private static final String ORIENTATION = "orientation";
    private static final String IMAGEABLE_HEIGHT = "imageableHeight";
    private static final String IMAGEABLE_WIDTH = "imageableWidth";
    private static final String IMAGEABLE_X = "imageableX";
    private static final String IMAGEABLE_Y = "imageableY";

    private ReportPrintFactory() {
    }

    /**
     * Gets the default page format based on the default printer.  If no printers are found
     * a generic default is created.
     *
     * @return page format
     */
    private static PageFormat getDefaultPage() {

        PrintService[] services = PrinterJob.lookupPrintServices();
        if (services.length == 0) { // no printers found on the system.
            return new PageFormat();
        }

        return PrinterJob.getPrinterJob().defaultPage();
    }

    /**
     * Save a <code>PageFormat</code> to preferences
     *
     * @param report report
     * @param format <code>PageFormat</code> to save
     */
    public static void savePageFormat(final DynamicJasperReport report, final PageFormat format) {
        Preferences p = report.getPreferences();

        p.putInt(ORIENTATION, format.getOrientation());

        Paper paper = format.getPaper();

        p.putDouble(HEIGHT, paper.getHeight());
        p.putDouble(WIDTH, paper.getWidth());

        p.putDouble(IMAGEABLE_HEIGHT, paper.getImageableHeight());
        p.putDouble(IMAGEABLE_WIDTH, paper.getImageableWidth());
        p.putDouble(IMAGEABLE_X, paper.getImageableX());
        p.putDouble(IMAGEABLE_Y, paper.getImageableY());
    }

    public static PageFormat getPageFormat(final DynamicJasperReport report) {
        Preferences p = report.getPreferences();

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

        PrinterJob job = PrinterJob.getPrinterJob();

        PageFormat pf = job.defaultPage();

        pf.setOrientation(orientation);

        Paper paper = pf.getPaper();
        paper.setSize(width, height);
        paper.setImageableArea(imageableX, imageableY, imageableWidth, imageableHeight);

        pf.setPaper(paper);

        return pf;
    }
}
