/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterGraphics;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;

import jgnash.engine.Transaction;
import jgnash.engine.checks.CheckLayout;
import jgnash.engine.checks.CheckObject;
import jgnash.text.BigDecimalToWords;
import jgnash.time.DateUtils;

/**
 * Check layout object
 *
 * @author Craig Cavanaugh
 */
class PrintableCheckLayout implements Printable {

    private CheckLayout checkLayout;

    private Transaction[] transactions;

    private PageFormat pageFormat;

    private boolean testPrint = false;

    private Font font;

    private Font testPrintFont;

    private FontRenderContext frc;

    private PrinterJob job = PrinterJob.getPrinterJob();

    private static final float space = 4.5f;

    PrintableCheckLayout(final CheckLayout layout) {
        this.checkLayout = layout;
        pageFormat = getPrinterJob().defaultPage();
    }

    public void setCheckLayout(final CheckLayout layout) {
        this.checkLayout = layout;
    }

    public CheckLayout getCheckLayout() {
        return checkLayout;
    }

    private PrinterJob getPrinterJob() {
        if (job == null) {
            job = PrinterJob.getPrinterJob();
        }
        return job;
    }

    PageFormat pageSetup() {
        pageFormat = getPrinterJob().pageDialog(checkLayout.getPrintAttributes());
        return pageFormat;
    }

    /**
     * This will print the sheet checks (Transactions)
     */
    void print() {
        getPrinterJob().setPrintable(this);
        if (getPrinterJob().printDialog(checkLayout.getPrintAttributes())) {
            try {
                getPrinterJob().print(checkLayout.getPrintAttributes());
            } catch (PrinterException pe) {
                Logger.getLogger(PrintableCheckLayout.class.getName()).log(Level.SEVERE, null, pe);
            }
        }
    }

    /**
     * This will print the sheet checks (Transactions)
     *
     * @param t transactions to print
     */
    void print(final Transaction[] t) {
        Objects.requireNonNull(t);

        assert t.length == checkLayout.getNumberOfChecks();
        this.transactions = t;
        print();
    }

    public CheckObject[] getCheckObjects() {

        List<CheckObject> checkObjects = checkLayout.getCheckObjects();

        return checkObjects.toArray(new CheckObject[checkObjects.size()]);
    }

    public double getCheckHeight() {
        return checkLayout.getCheckHeight();
    }

    public void setNumChecks(final int count) {
        checkLayout.setNumberOfChecks(count);
    }

    public int getNumChecks() {
        return checkLayout.getNumberOfChecks();
    }

    public PageFormat getPageFormat() {

        PrintRequestAttributeSet printAttributes = checkLayout.getPrintAttributes();

        if (pageFormat == null) { // create a default pageFormat
            PageFormat format = getPrinterJob().defaultPage();
            Paper paper = format.getPaper();

            MediaSizeName media = (MediaSizeName) printAttributes.get(Media.class);
            MediaSize ms = MediaSize.getMediaSizeForName(media);

            MediaPrintableArea ma = (MediaPrintableArea) printAttributes.get(MediaPrintableArea.class);

            if (ma != null) {
                int INCH = MediaPrintableArea.INCH;
                paper.setImageableArea((ma.getX(INCH) * 72), (ma.getY(INCH) * 72), (ma.getWidth(INCH) * 72), (ma.getHeight(INCH) * 72));
            }

            if (ms != null) {
                paper.setSize((ms.getX(Size2DSyntax.INCH) * 72), (ms.getY(Size2DSyntax.INCH) * 72));
            }

            format.setPaper(paper);

            OrientationRequested or = (OrientationRequested) printAttributes.get(OrientationRequested.class);
            if (or != null) {
                if (or == OrientationRequested.LANDSCAPE) {
                    format.setOrientation(PageFormat.LANDSCAPE);
                } else if (or == OrientationRequested.REVERSE_LANDSCAPE) {
                    format.setOrientation(PageFormat.REVERSE_LANDSCAPE);
                } else if (or == OrientationRequested.PORTRAIT) {
                    format.setOrientation(PageFormat.PORTRAIT);
                } else if (or == OrientationRequested.REVERSE_PORTRAIT) {
                    format.setOrientation(PageFormat.PORTRAIT);
                }
            }
            pageFormat = format;
        }
        return pageFormat;
    }

    public void setTestPrint(final boolean testPrint) {
        this.testPrint = testPrint;
    }

    private void drawCheck(final Graphics2D g2, final Rectangle2D bounds, final BigDecimal amount, final LocalDate date, final String memo, final String payee) {

        font = new Font("Serif", Font.PLAIN, 10);
        testPrintFont = new Font("Serif", Font.BOLD, 9);
        float offset = (float) bounds.getY();
        frc = g2.getFontRenderContext();

        // print the check bounds
        if (testPrint) {
            g2.draw(bounds);
        }

        for (CheckObject o : checkLayout.getCheckObjects()) {
            switch (o.getType()) {
                case ADDRESS:
                case AMOUNT:
                    drawAmount(g2, o, offset, amount);
                    break;
                case AMOUNT_TEXT:
                    drawAmountText(g2, o, offset, amount);
                    break;
                case DATE:
                    drawDate(g2, o, offset, date);
                    break;
                case MEMO:
                    drawMemo(g2, o, offset, memo);
                    break;
                case PAYEE:
                    drawPayee(g2, o, offset, payee);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Paint a sheet on checks on the graphics device given an array of
     * transactions. If the array is null, it is assumed that a test print is
     * being requested, otherwise, only non null transactions will be printed.
     * The array must equal getNumChecks.  Check position will not be assumed.
     *
     * @param g2     graphics context to draw on
     * @param format page format to use
     * @param list   array of transactions
     */
    private void drawSheet(final Graphics2D g2, final PageFormat format, final Transaction[] list) {

        double checkHeight = checkLayout.getCheckHeight();
        int numChecks = checkLayout.getNumberOfChecks();

        for (int i = 0; i < numChecks; i++) {
            Rectangle2D bounds = new Rectangle2D.Double(0, i * checkHeight, format.getWidth(), checkHeight);

            if (list != null) {
                if (list.length != getNumChecks()) {
                    return;
                }
                if (list[i] != null) { // skip null transactions

                    String payee = list[i].getPayee();
                    String memo = list[i].getMemo();
                    BigDecimal amount = list[i].getAmount(list[i].getCommonAccount()).abs();
                    LocalDate date = list[i].getLocalDate();

                    drawCheck(g2, bounds, amount, date, memo, payee);
                }
            } else {

                String payee = "Expensive bank USA";
                String memo = "I spent too much on the widget";
                BigDecimal amount = new BigDecimal("12345.67");
                LocalDate date = LocalDate.now();

                drawCheck(g2, bounds, amount, date, memo, payee);
            }
        }
    }

    private void drawDate(final Graphics2D g2, final CheckObject object, final float offset, final LocalDate date) {
        final DateTimeFormatter df = DateUtils.getShortDateFormatter();

        float dateX = object.getX();
        float dateY = object.getY() + offset;
        TextLayout textLayout = new TextLayout(df.format(date), font, frc);
        textLayout.draw(g2, dateX, dateY);

        if (testPrint) {
            TextLayout dateText = new TextLayout("DATE", testPrintFont, frc);
            double width = dateText.getBounds().getWidth();
            dateText.draw(g2, (float) (dateX - width - space), dateY);
        }
    }

    private void drawMemo(final Graphics2D g2, final CheckObject object, final float offset, final String memo) {
        TextLayout text;
        float x = object.getX();
        float y = object.getY() + offset;

        if (memo != null && !memo.isEmpty()) {
            text = new TextLayout(memo, font, frc);
            text.draw(g2, x, y);
        }

        if (testPrint) {
            text = new TextLayout("MEMO", testPrintFont, frc);
            double width = text.getBounds().getWidth();
            text.draw(g2, (float) (x - width - space), y);
        }
    }

    private void drawAmount(final Graphics2D g2, final CheckObject object, final float offset, final BigDecimal amount) {
        float x = object.getX();
        float y = object.getY() + offset;

        TextLayout text = new TextLayout("**" + amount + "**", font, frc);

        text.draw(g2, x, y);

        if (testPrint) {
            text = new TextLayout("$", testPrintFont, frc);
            double width = text.getBounds().getWidth();
            text.draw(g2, (float) (x - width - space), y);
        }
    }

    private void drawAmountText(final Graphics2D g2, final CheckObject object, final float offset, final BigDecimal amount) {
        String amountText = BigDecimalToWords.convert(amount);
        float x = object.getX();
        float y = object.getY() + offset;
        TextLayout text = new TextLayout(amountText, font, frc);
        text.draw(g2, x, y);
    }

    private void drawPayee(final Graphics2D g2, final CheckObject object, final float offset, final String payee) {
        float payeeX = object.getX();
        float payeeY = object.getY() + offset;

        if (payee != null && !payee.isEmpty()) {
            TextLayout textLayout = new TextLayout(payee, font, frc);
            textLayout.draw(g2, payeeX, payeeY);
        }

        if (testPrint) {
            TextLayout payeeText = new TextLayout("ORDER OF", testPrintFont, frc);
            double width = payeeText.getBounds().getWidth();
            payeeText.draw(g2, (float) (payeeX - width - space), payeeY);
            LineMetrics metrics = testPrintFont.getLineMetrics("PAY TO THE", frc);
            float y = payeeY - (float) payeeText.getBounds().getHeight() - metrics.getDescent() - metrics.getLeading();
            payeeText = new TextLayout("PAY TO THE", testPrintFont, frc);
            payeeText.draw(g2, (float) (payeeX - width - space), y);
        }
    }

    /**
     * Prints the page at the specified index into the specified
     * {@link Graphics} context in the specified
     * format.  A {@code PrinterJob} calls the
     * {@code Printable} interface to request that a page be
     * rendered into the context specified by
     * {@code graphics}.  The format of the page to be drawn is
     * specified by {@code pageFormat}.  The zero based index
     * of the requested page is specified by {@code pageIndex}.
     * If the requested page does not exist then this method returns
     * NO_SUCH_PAGE; otherwise PAGE_EXISTS is returned.
     * The {@code Graphics} class or subclass implements the
     * {@link PrinterGraphics} interface to provide additional
     * information.  If the {@code Printable} object
     * aborts the print job then it throws a {@link PrinterException}.
     *
     * @param graphics  the context into which the page is drawn
     * @param pf        the size and orientation of the page being drawn
     * @param pageIndex the zero based index of the page to be drawn
     * @return PAGE_EXISTS if the page is rendered successfully
     *         or NO_SUCH_PAGE if {@code pageIndex} specifies a
     *         non-existent page.     *
     */
    @Override
    public int print(final Graphics graphics, final PageFormat pf, final int pageIndex) {
        if (pageIndex == 0) {
            Graphics2D g2 = (Graphics2D) graphics;
            if (g2.getDeviceConfiguration().getDevice().getType() == GraphicsDevice.TYPE_PRINTER) {
                g2.translate(pf.getImageableX(), pf.getImageableY());
            }
            drawSheet(g2, pf, transactions);
            return PAGE_EXISTS;
        }
        return NO_SUCH_PAGE;
    }

}