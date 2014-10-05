/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.ui.checks;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

/**
 * @author Craig Cavanaugh
 */
final class PrintPreviewPanel extends JPanel {

    private Printable printable;

    private PageFormat pageFormat;

    PrintPreviewPanel(final Printable printable, final PageFormat pageFormat) {
        Objects.requireNonNull(printable);
        Objects.requireNonNull(pageFormat);

        setPageFormat(pageFormat);
        this.printable = printable;
        setLayout(null);
    }

    public void setPageFormat(final PageFormat pageFormat) {
        Objects.requireNonNull(pageFormat);

        this.pageFormat = pageFormat;
        repaint();
    }

    public void setPrintable(final Printable printable) {
        this.printable = printable;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        double xOffset; // x offset of page start in window
        double yOffset; // y offset of page start in window
        double scale; // scale factor to fit page in window
        double px = pageFormat.getWidth();
        double py = pageFormat.getHeight();
        double sx = getWidth() - 1;
        double sy = getHeight() - 1;
        if (px / py < sx / sy) { // center horizontally
            scale = sy / py;
            xOffset = 0.5 * (sx - scale * px);
            yOffset = 0;
        } else { // center vertically
            scale = sx / px;
            xOffset = 0;
            yOffset = 0.5 * (sy - scale * py);
        }
        g2.translate((float) xOffset, (float) yOffset);
        g2.scale((float) scale, (float) scale);

        // draw page outline (ignoring margins)
        Rectangle2D page = new Rectangle2D.Double(0, 0, px, py);
        g2.setPaint(Color.WHITE);
        g2.fill(page);
        g2.setPaint(Color.BLACK);
        g2.draw(page);

        // draw the outline of the margin
        Rectangle2D margin = new Rectangle2D.Double(pageFormat.getImageableX(), pageFormat.getImageableY(), pageFormat.getImageableWidth(), pageFormat.getImageableHeight());
        g2.setPaint(Color.DARK_GRAY);
        g2.draw(margin);

        // reset the color
        g2.setPaint(Color.BLACK);

        try {
            printable.print(g2, pageFormat, 0);
        } catch (PrinterException e) {
            Logger.getLogger(PrintPreviewPanel.class.getName()).log(Level.INFO, e.getLocalizedMessage(), e);
        }
    }
}
