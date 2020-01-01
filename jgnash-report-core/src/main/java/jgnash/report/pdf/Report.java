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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import jgnash.engine.MathConstants;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.report.table.ColumnStyle;
import jgnash.report.table.GroupInfo;
import jgnash.report.ui.ReportPrintFactory;
import jgnash.resource.util.ResourceUtils;
import jgnash.text.NumericFormats;
import jgnash.time.DateUtils;
import jgnash.util.NotNull;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import static jgnash.util.LogUtil.logSevere;

/**
 * Base report format definition.
 * <p>
 * Units of measure is in Points
 * <p>
 * The origin of a PDFBox page is the bottom left corner vs. a report being created from the top down.  Report layout
 * logic is from top down with use of a method to convert to PDF coordinate system.
 * <p>
 * This class is abstract to force isolation of Preferences through simple extension of the class
 * <p>
 *
 * @author Craig Cavanaugh
 */
@SuppressWarnings("WeakerAccess")
public abstract class Report implements AutoCloseable {

    private static final int MAX_MEMORY_USAGE = 10_000_000;    // allow 10 meg reports in memory before a scratch file is used

    private static final String BASE_FONT_SIZE = "baseFontSize";

    protected static final ResourceBundle rb = ResourceUtils.getBundle();

    private static final int DEFAULT_BASE_FONT_SIZE = 11;

    private String ellipsis = "...";

    private float baseFontSize;

    private PDFont tableFont;

    private PDFont headerFont;

    private PDFont footerFont;

    private float cellPadding = 3f; // cell padding in points

    final Color footerBackGround = Color.LIGHT_GRAY;

    final Color headerBackground = Color.DARK_GRAY;

    final Color headerTextColor = Color.WHITE;

    static final float FOOTER_SCALE = 0.80f;

    static final float DEFAULT_LINE_WIDTH = 0.20f;

    final PDDocument pdfDocument;

    private PageFormat pageFormat;

    private boolean forceGroupPagination = false;

    public Report() {
        this.pdfDocument = new PDDocument(MemoryUsageSetting.setupMixed(MAX_MEMORY_USAGE));

        setTableFont(loadFont(ReportFactory.getMonoFont(), pdfDocument));
        setHeaderFont(loadFont(ReportFactory.getHeaderFont(), pdfDocument));
        setFooterFont(loadFont(ReportFactory.getProportionalFont(), pdfDocument));

        // restore font size
        baseFontSize = getPreferences().getFloat(BASE_FONT_SIZE, DEFAULT_BASE_FONT_SIZE);

        // restore the page format
        setPageFormat(getPageFormat());
    }

    public void clearReport() {
        for (PDPage pdPage : pdfDocument.getPages()) {
            pdfDocument.removePage(pdPage);
        }
    }

    private static PDFont loadFont(final String name, final PDDocument document) {

        final String path = FontRegistry.getRegisteredFontPath(name);

        if (path != null && !path.isEmpty()) {
            try {
                if (path.toLowerCase(Locale.ROOT).endsWith(".ttf") || path.toLowerCase(Locale.ROOT).endsWith(".otf")
                        || path.toLowerCase(Locale.ROOT).indexOf(".ttc,") > 0) {
                    return PDType0Font.load(document, new FileInputStream(path), false);
                } else if (path.toLowerCase(Locale.ROOT).endsWith(".afm") || path.toLowerCase(Locale.ROOT).endsWith(".pfm")) {
                    return new PDType1Font(document, new FileInputStream(path));
                }
            } catch (final Exception ignored) {
            }
        }

        return PDType1Font.COURIER;
    }

    public int getPageCount() {
        return pdfDocument.getNumberOfPages();
    }

    public final PageFormat getPageFormat() {
        if (pageFormat == null) {
            pageFormat = ReportPrintFactory.getPageFormat(getPreferences());
        }

        return pageFormat;
    }

    public final void setPageFormat(@NotNull final PageFormat pageFormat) {
        Objects.requireNonNull(pageFormat);

        this.pageFormat = pageFormat;
        ReportPrintFactory.savePageFormat(getPreferences(), pageFormat);
    }

    @NotNull
    public PDFont getTableFont() {
        return tableFont;
    }

    public void setTableFont(@NotNull PDFont tableFont) {
        this.tableFont = tableFont;
    }

    public float getBaseFontSize() {
        return baseFontSize;
    }

    public void setBaseFontSize(float tableFontSize) {
        this.baseFontSize = tableFontSize;
        getPreferences().putFloat(BASE_FONT_SIZE, tableFontSize);
    }

    private float getTableRowHeight() {
        return getBaseFontSize() + 2 * getCellPadding();
    }

    public boolean isLandscape() {
        return getPageFormat().getWidth() > getPageFormat().getHeight();
    }

    public PDFont getHeaderFont() {
        return headerFont;
    }

    public void setHeaderFont(final PDFont headerFont) {
        this.headerFont = headerFont;
    }

    public float getCellPadding() {
        return cellPadding;
    }

    public void setCellPadding(final float cellPadding) {
        this.cellPadding = cellPadding;
    }

    private float getAvailableWidth() {
        return (float) getPageFormat().getImageableWidth();
    }

    private float getLeftMargin() {
        return (float) getPageFormat().getImageableX();
    }

    private float getTopMargin() {
        return (float) getPageFormat().getImageableY();
    }

    private float getRightMargin() {
        return (float) getPageFormat().getWidth() - getAvailableWidth() - getLeftMargin();
    }

    private float getBottomMargin() {
        return (float) (getPageFormat().getHeight() - getPageFormat().getImageableHeight() - getTopMargin());
    }

    private float getFooterFontSize() {
        return (float) Math.ceil(getBaseFontSize() * FOOTER_SCALE);
    }

    public PDFont getFooterFont() {
        return footerFont;
    }

    public void setFooterFont(final PDFont footerFont) {
        this.footerFont = footerFont;
    }

    /**
     * Returns the legend for the grand total
     *
     * @return report name
     */
    public String getGrandTotalLegend() {
        return ResourceUtils.getString("Word.Total");
    }

    /**
     * Returns the general label for the group footer
     *
     * @return footer label
     */
    public String getGroupFooterLabel() {
        return ResourceUtils.getString("Word.Subtotal");
    }

    public void addTable(final AbstractReportTableModel reportModel) throws IOException {

        final String title = reportModel.getTitle();
        final String subTitle = reportModel.getSubTitle();

        boolean titleWritten = false;

        final float[] columnWidths = getColumnWidths(reportModel);

        final Set<GroupInfo> groupInfoSet = GroupInfo.getGroups(reportModel);

        // calculate the maximum imageable height of the page before we get too close to the footer
        float imageableBottom = (float) getPageFormat().getHeight() - getBottomMargin() - getTableRowHeight();

        float docY = getTopMargin();   // start at top of the page with the margin

        PDPage page = createPage(); // create the first page

        for (final GroupInfo groupInfo : groupInfoSet) {

            int row = 0;  // tracks the last written row

            while (row < reportModel.getRowCount()) {

                if (docY > imageableBottom || isForceGroupPagination()) {    // if near the bottom of the page
                    docY = getTopMargin();   // start at top of the page with the margin
                    page = createPage();
                }

                try (final PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, page,
                        PDPageContentStream.AppendMode.APPEND, false)) {

                    // add the table title if its not been added
                    if (title != null && !title.isEmpty() && row == 0 && !titleWritten) {
                        docY = addReportTitle(contentStream, title, subTitle, docY);

                        titleWritten = true;
                    }

                    // add the group subtitle if needed
                    if (groupInfoSet.size() > 1) {
                        docY = addTableTitle(contentStream, groupInfo.group, docY);
                    }

                    // write a section of the table and save the last row written for next page if needed
                    final Pair<Integer, Float> pair
                            = addTableSection(reportModel, groupInfo.group, contentStream, row, columnWidths, docY);

                    row = pair.getLeft();
                    docY = pair.getRight();

                } catch (final IOException e) {
                    logSevere(Report.class, e);
                    throw (e);
                }

                // check to see if this table has summation information and add a summation footer
                if (groupInfo.hasSummation() && row == reportModel.getRowCount()) {

                    // TODO, make sure the end of the page has not been reached
                    try (final PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, page,
                            PDPageContentStream.AppendMode.APPEND, false)) {
                        docY = addTableFooter(reportModel, groupInfo, contentStream, columnWidths, docY);
                        docY += getBaseFontSize();  // add some padding
                    } catch (final IOException e) {
                        logSevere(Report.class, e);
                        throw (e);
                    }
                }
            }
        }

        if (reportModel.hasGlobalSummary()) {
            try (final PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, page,
                    PDPageContentStream.AppendMode.APPEND, false)) {
                addGlobalFooter(reportModel, contentStream, columnWidths, docY);
            } catch (final IOException e) {
                logSevere(Report.class, e);
                throw (e);
            }
        }
    }

    /**
     * Simply transform function to convert from a upper origin to a lower pdf page origin.
     *
     * @param y document y position
     * @return returns the pdf page y position
     */
    private float docYToPageY(final float y) {
        return (float) getPageFormat().getHeight() - y;
    }

    /**
     * Writes a table section to the report.
     *
     * @param reportModel   report model
     * @param group         report group
     * @param contentStream PDF content stream
     * @param startRow      starting row
     * @param columnWidths  column widths
     * @param yStart        start location from top of the page
     * @return returns the last reported row of the group and yDoc location
     * @throws IOException IO exception
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private Pair<Integer, Float> addTableSection(final AbstractReportTableModel reportModel, @NotNull final String group,
                                                 final PDPageContentStream contentStream, final int startRow, float[] columnWidths,
                                                 float yStart) throws IOException {

        Objects.requireNonNull(group);

        int rowsWritten = 0;    // the return value of the number of rows written

        // establish start location, use half the row height as the vertical margin between title and table
        final float yTop = (float) getPageFormat().getHeight() - getTableRowHeight() / 2 - yStart;

        float xPos = getLeftMargin() + getCellPadding();
        float yPos = yTop - getTableRowHeight() + getRowTextBaselineOffset();

        contentStream.setFont(getHeaderFont(), getBaseFontSize());

        // add the header
        contentStream.setNonStrokingColor(headerBackground);
        fillRect(contentStream, getLeftMargin(), yTop - getTableRowHeight(), getAvailableWidth(), getTableRowHeight());

        contentStream.setNonStrokingColor(headerTextColor);

        for (int i = 0; i < reportModel.getColumnCount(); i++) {
            if (reportModel.isColumnVisible(i)) {
                float shift = 0;
                float availWidth = columnWidths[i] - getCellPadding() * 2;

                final String text = truncateText(reportModel.getColumnName(i), availWidth,
                        getHeaderFont(), getBaseFontSize());

                if (rightAlign(i, reportModel)) {
                    shift = availWidth - getStringWidth(text, getHeaderFont(), getBaseFontSize());
                }

                drawText(contentStream, xPos + shift, yPos, text);

                xPos += columnWidths[i];
            }
        }

        // add the rows
        contentStream.setFont(getTableFont(), getBaseFontSize());
        contentStream.setNonStrokingColor(Color.BLACK);

        int row = startRow;

        final float bottomMargin = getBottomMargin();

        while (yPos > bottomMargin + getTableRowHeight() && row < reportModel.getRowCount()) {

            final String rowGroup = reportModel.getGroup(row);

            if (group.equals(rowGroup)) {

                xPos = getLeftMargin() + getCellPadding();
                yPos -= getTableRowHeight();

                for (int i = 0; i < reportModel.getColumnCount(); i++) {

                    if (reportModel.isColumnVisible(i)) {

                        final Object value = reportModel.getValueAt(row, i);

                        if (value != null) {
                            float shift = 0;
                            float availWidth = columnWidths[i] - getCellPadding() * 2;

                            final String text = truncateText(formatValue(reportModel.getValueAt(row, i), i, reportModel), availWidth,
                                    getTableFont(), getBaseFontSize());

                            if (rightAlign(i, reportModel)) {
                                shift = availWidth - getStringWidth(text, getTableFont(), getBaseFontSize());
                            }

                            drawText(contentStream, xPos + shift, yPos, text);
                        }

                        xPos += columnWidths[i];
                    }
                }

                rowsWritten++;
            }
            row++;
        }

        // add row lines
        yPos = yTop;
        xPos = getLeftMargin();

        for (int r = 0; r <= rowsWritten + 1; r++) {
            drawLine(contentStream, xPos, yPos, getAvailableWidth() + getLeftMargin(), yPos);
            yPos -= getTableRowHeight();
        }

        // add column lines
        yPos = yTop;
        xPos = getLeftMargin();

        for (int i = 0; i < reportModel.getColumnCount(); i++) {
            if (reportModel.isColumnVisible(i)) {
                drawLine(contentStream, xPos, yPos, xPos, yPos - getTableRowHeight() * (rowsWritten + 1));
                xPos += columnWidths[i];
            }
        }

        // end of last column
        drawLine(contentStream, xPos, yPos, xPos, yPos - getTableRowHeight() * (rowsWritten + 1));

        float yDoc = (float) getPageFormat().getHeight() - (yPos - getTableRowHeight() * (rowsWritten + 1));

        // return the row and docY position
        return new ImmutablePair<>(row, yDoc);
    }

    /**
     * Calculates the offset for row text
     *
     * @return offset
     */
    private float getRowTextBaselineOffset() {
        return (getTableRowHeight()
                - getTableFont().getFontDescriptor().getCapHeight() / 1000 * getBaseFontSize()) / 2f;
    }

    /**
     * Writes a table footer to the report.
     *
     * @param reportModel   report model
     * @param groupInfo     Group info to report on
     * @param contentStream PDF content stream
     * @param columnWidths  column widths
     * @param yStart        start location from top of the page
     * @return returns the y position from the top of the page
     * @throws IOException IO exception
     */
    private float addTableFooter(final AbstractReportTableModel reportModel, final GroupInfo groupInfo,
                                 final PDPageContentStream contentStream, float[] columnWidths,
                                 float yStart) throws IOException {

        float yDoc = yStart + getTableRowHeight();

        // add the footer background
        contentStream.setNonStrokingColor(footerBackGround);
        fillRect(contentStream, getLeftMargin(), docYToPageY(yDoc), getAvailableWidth(), getTableRowHeight());

        drawLine(contentStream, getLeftMargin(), docYToPageY(yDoc), getAvailableWidth() + getLeftMargin(), docYToPageY(yDoc));
        drawLine(contentStream, getLeftMargin(), docYToPageY(yDoc - getTableRowHeight()), getLeftMargin(), docYToPageY(yDoc));
        drawLine(contentStream, getLeftMargin() + getAvailableWidth(), docYToPageY(yDoc - getTableRowHeight()),
                getLeftMargin() + getAvailableWidth(), docYToPageY(yDoc));

        contentStream.setFont(getTableFont(), getBaseFontSize());
        contentStream.setNonStrokingColor(Color.BLACK);

        // draw summation values
        float xPos = getLeftMargin() + getCellPadding();

        // search for first visible column width
        for (int c = 0; c < reportModel.getColumnCount(); c++) {
            if (reportModel.isColumnVisible(c)) {

                // right align the text
                final float availWidth = columnWidths[c] - getCellPadding() * 2;
                final float shift = availWidth - getStringWidth(reportModel.getGroupFooterLabel(), getTableFont(), getBaseFontSize());

                drawText(contentStream, xPos + shift, docYToPageY(yDoc - getRowTextBaselineOffset()),
                        reportModel.getGroupFooterLabel());

                break;
            }
        }


        for (int c = 0; c < reportModel.getColumnCount(); c++) {

            if (reportModel.isColumnVisible(c) && reportModel.isColumnSummed(c)) {

                final Object value = groupInfo.getValue(c);

                if (value != null) {
                    float shift = 0;
                    float availWidth = columnWidths[c] - getCellPadding() * 2;

                    final String text = truncateText(formatValue(groupInfo.getValue(c), c, reportModel), availWidth,
                            getTableFont(), getBaseFontSize());

                    if (rightAlign(c, reportModel)) {
                        shift = availWidth - getStringWidth(text, getTableFont(), getBaseFontSize());
                    }

                    drawText(contentStream, xPos + shift, docYToPageY(yDoc - getRowTextBaselineOffset()), text);
                }
            }

            if (c < reportModel.getColumnCount() - 1) {
                xPos += columnWidths[c];
            }
        }

        return yDoc;
    }

    /**
     * Writes a table footer to the report.
     *
     * @param reportModel   report model
     * @param contentStream PDF content stream
     * @param columnWidths  column widths
     * @param yStart        start location from top of the page
     * @throws IOException IO exception
     */
    private void addGlobalFooter(final AbstractReportTableModel reportModel, final PDPageContentStream contentStream,
                                 float[] columnWidths, float yStart) throws IOException {

        float yDoc = yStart + getTableRowHeight();

        // add the footer background
        contentStream.setNonStrokingColor(footerBackGround);
        fillRect(contentStream, getLeftMargin(), docYToPageY(yDoc), getAvailableWidth(), getTableRowHeight());

        drawLine(contentStream, getLeftMargin(), docYToPageY(yDoc), getAvailableWidth() + getLeftMargin(), docYToPageY(yDoc));
        drawLine(contentStream, getLeftMargin(), docYToPageY(yDoc - getTableRowHeight()), getLeftMargin(), docYToPageY(yDoc));
        drawLine(contentStream, getLeftMargin() + getAvailableWidth(), docYToPageY(yDoc - getTableRowHeight()),
                getLeftMargin() + getAvailableWidth(), docYToPageY(yDoc));

        contentStream.setFont(getTableFont(), getBaseFontSize());
        contentStream.setNonStrokingColor(Color.BLACK);

        // draw summation values
        float xPos = getLeftMargin() + getCellPadding();

        // search for first visible column width
        for (int c = 0; c < reportModel.getColumnCount(); c++) {
            if (reportModel.isColumnVisible(c)) {

                // right align the text
                final float availWidth = columnWidths[c] - getCellPadding() * 2;
                final float shift = availWidth - getStringWidth(reportModel.getGrandTotalLegend(), getTableFont(), getBaseFontSize());

                drawText(contentStream, xPos + shift, docYToPageY(yDoc - getRowTextBaselineOffset()),
                        reportModel.getGrandTotalLegend());

                break;
            }
        }

        for (int c = 0; c < reportModel.getColumnCount(); c++) {

            if (reportModel.isColumnVisible(c) && reportModel.isColumnSummed(c)) {

                final Object value = reportModel.getGlobalSum(c);

                if (value != null) {
                    float shift = 0;
                    float availWidth = columnWidths[c] - getCellPadding() * 2;

                    final String text = truncateText(formatValue(reportModel.getGlobalSum(c), c, reportModel), availWidth,
                            getTableFont(), getBaseFontSize());

                    if (rightAlign(c, reportModel)) {
                        shift = availWidth - getStringWidth(text, getTableFont(), getBaseFontSize());
                    }

                    drawText(contentStream, xPos + shift, docYToPageY(yDoc - getRowTextBaselineOffset()), text);
                }
            }

            if (c < reportModel.getColumnCount() - 1) {
                xPos += columnWidths[c];
            }
        }

        //return yDoc;
    }

    private static String formatValue(final Object value, final int column, final AbstractReportTableModel reportModel) {
        if (value == null) {
            return " ";
        }

        final ColumnStyle columnStyle = reportModel.getColumnStyle(column);

        switch (columnStyle) {
            case TIMESTAMP:
                final DateTimeFormatter dateTimeFormatter = DateUtils.getShortDateTimeFormatter();
                return dateTimeFormatter.format((LocalDateTime) value);
            case SHORT_DATE:
                final DateTimeFormatter dateFormatter = DateUtils.getShortDateFormatter();
                return dateFormatter.format((LocalDate) value);
            case SHORT_AMOUNT:
                final NumberFormat shortNumberFormat = NumericFormats.getShortCommodityFormat(reportModel.getCurrencyNode());
                return shortNumberFormat.format(value);
            case BALANCE:
            case BALANCE_WITH_SUM:
            case BALANCE_WITH_SUM_AND_GLOBAL:
            case AMOUNT_SUM:
                final NumberFormat numberFormat = NumericFormats.getFullCommodityFormat(reportModel.getCurrencyNode());
                return numberFormat.format(value);
            case PERCENTAGE:
                final NumberFormat percentageFormat = NumericFormats.getPercentageFormat();
                return percentageFormat.format(value);
            case QUANTITY:
                final NumberFormat qtyFormat = NumericFormats.getFixedPrecisionFormat(MathConstants.SECURITY_QUANTITY_ACCURACY);
                return qtyFormat.format(value);
            default:
                return value.toString();
        }
    }

    private static boolean rightAlign(final int column, final AbstractReportTableModel reportModel) {
        final ColumnStyle columnStyle = reportModel.getColumnStyle(column);

        switch (columnStyle) {
            case SHORT_AMOUNT:
            case BALANCE:
            case BALANCE_WITH_SUM:
            case BALANCE_WITH_SUM_AND_GLOBAL:
            case AMOUNT_SUM:
            case PERCENTAGE:
            case QUANTITY:
                return true;
            default:
                return false;
        }
    }

    private static void drawText(final PDPageContentStream contentStream, final float xStart, final float yStart,
                                 final String text) throws IOException {
        contentStream.beginText();
        contentStream.newLineAtOffset(xStart, yStart);
        contentStream.showText(text);
        contentStream.endText();
    }


    private static void drawLine(final PDPageContentStream contentStream, final float xStart, final float yStart,
                                 final float xEnd, final float yEnd) throws IOException {
        contentStream.setLineWidth(DEFAULT_LINE_WIDTH);
        contentStream.moveTo(xStart, yStart);
        contentStream.lineTo(xEnd, yEnd);
        contentStream.stroke();
    }

    private static void fillRect(final PDPageContentStream contentStream, final float x, final float y, final float width,
                                 final float height) throws IOException {
        contentStream.addRect(x, y, width, height);
        contentStream.fill();
    }

    /**
     * Adds a title to the table and returns the new document position
     *
     * @param title  title
     * @param yStart table title position from the top of the page
     * @return current y document position
     * @throws IOException exception
     */
    private float addTableTitle(final PDPageContentStream contentStream, final String title, final float yStart)
            throws IOException {

        float docY = yStart + getBaseFontSize() * 1.5f;  // add for font height
        float xPos = getLeftMargin();

        contentStream.setFont(getHeaderFont(), getBaseFontSize() * 1.5f);
        drawText(contentStream, xPos, docYToPageY(docY), title);

        return docY;    // returns new y document position
    }

    /**
     * Adds a Title and subtitle to the document and returns the height consumed
     *
     * @param title  title
     * @param yStart start from the top of the page
     * @return document y position
     * @throws IOException exception
     */
    private float addReportTitle(final PDPageContentStream contentStream, final String title, final String subTitle,
                                 final float yStart) throws IOException {

        float width = getStringWidth(title, getHeaderFont(), getBaseFontSize() * 2);
        float xPos = (getAvailableWidth() / 2f) - (width / 2f) + getLeftMargin();
        float docY = yStart + getBaseFontSize();

        contentStream.setFont(getHeaderFont(), getBaseFontSize() * 2);
        drawText(contentStream, xPos, docYToPageY(docY), title);

        if (subTitle != null && subTitle.length() > 0) {    // subtitle may be empty
            width = getStringWidth(subTitle, getFooterFont(), getFooterFontSize());
            xPos = (getAvailableWidth() / 2f) - (width / 2f) + getLeftMargin();
            docY += getFooterFontSize() * 1.5f;

            contentStream.setFont(getFooterFont(), getFooterFontSize());
            drawText(contentStream, xPos, docYToPageY(docY), subTitle);

            docY += getFooterFontSize() * 2.0f;   // add a margin below the sub title
        }

        return docY;
    }

    public void addFooter() throws IOException {

        final String timeStamp = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(LocalDateTime.now());

        final int pageCount = pdfDocument.getNumberOfPages();
        float yStart = getBottomMargin() * 2 / 3;

        for (int i = 0; i < pageCount; i++) {
            final PDPage page = pdfDocument.getPage(i);
            final String pageText = MessageFormat.format(rb.getString("Pattern.Pages"), i + 1, pageCount);
            final float width = getStringWidth(pageText, getFooterFont(), getFooterFontSize());

            try (final PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, page, PDPageContentStream.AppendMode.APPEND, true)) {
                contentStream.setFont(getFooterFont(), getFooterFontSize());

                drawText(contentStream, getLeftMargin(), yStart, timeStamp);
                drawText(contentStream, (float) getPageFormat().getWidth() - getRightMargin() - width, yStart, pageText);
            } catch (final IOException e) {
                logSevere(Report.class, e);
            }
        }
    }

    private String truncateText(final String text, final float availWidth, final PDFont font, final float fontSize) throws IOException {
        if (text != null) {
            String content = text;

            float width = getStringWidth(content, font, fontSize);

            // munch down the end of the string until it fits
            if (width > availWidth) {
                while (getStringWidth(content + getEllipsis(), font, fontSize) > availWidth && !content.isEmpty()) {
                    content = content.substring(0, content.length() - 1);
                }

                content = content + getEllipsis();
            }

            return content;
        }

        return null;
    }

    private PDPage createPage() {

        final PDPage page = new PDPage(new PDRectangle(0f, 0f, (float) getPageFormat().getWidth(),
                (float) getPageFormat().getHeight()));

        pdfDocument.addPage(page);  // add the page to the document

        return page;
    }

    private static float getStringWidth(final String text, final PDFont font, final float fontSize) throws IOException {
        return (float) Math.ceil(font.getStringWidth(text) / 1000f * fontSize);
    }

    public String getEllipsis() {
        return ellipsis;
    }

    public void setEllipsis(String ellipsis) {
        this.ellipsis = ellipsis;
    }

    private float[] getColumnWidths(final AbstractReportTableModel reportModel) throws IOException {

        int visibleColumnCount = 0;

        for (int i = 0; i < reportModel.getColumnCount(); i++) {
            if (reportModel.isColumnVisible(i)) {
                visibleColumnCount++;
            }
        }

        float[] widths = new float[reportModel.getColumnCount()]; // calculated optimal widths

        float fixedWidth = 0;           // total fixed width
        boolean compressAll = false;    // true if all columns need to be compressed
        float minWidth = 0;             // the minimum width based on column names

        for (int i = 0; i < reportModel.getColumnCount(); i++) {
            if (reportModel.isColumnVisible(i)) {

                final String protoValue = reportModel.getColumnPrototypeValueAt(i);

                float headerWidth = getStringWidth(reportModel.getColumnName(i), getHeaderFont(), getBaseFontSize()) + getCellPadding() * 3f;
                float cellTextWidth = getStringWidth(protoValue, getTableFont(), getBaseFontSize()) + getCellPadding() * 3f;

                widths[i] = Math.max(headerWidth, cellTextWidth);

                if (reportModel.isColumnFixedWidth(i)) {
                    fixedWidth += widths[i];
                }
            } else {
                widths[i] = 0;    // not visible, but a place holder is needed
            }
        }

        // it gets ugly if there is simply not enough room
        if (fixedWidth > getAvailableWidth() || minWidth > getAvailableWidth()) {
            compressAll = true;
            Logger.getLogger(Report.class.getName()).warning("Page width is not wide enough");
        }

        for (int i = 0; i < reportModel.getColumnCount(); i++) {
            if (reportModel.isColumnVisible(i)) {
                if (compressAll) {  // make it ugly
                    widths[i] = getAvailableWidth() / visibleColumnCount;
                } else if (!reportModel.isColumnFixedWidth(i)) {
                    float remainder = getAvailableWidth() - fixedWidth;

                    widths[i] = (reportModel.getColumnWidthWeight(i) / 100f) * remainder;
                }
            }
        }

        return widths;
    }

    public final Preferences getPreferences() {
        return Preferences.userNodeForPackage(getClass()).node(getClass().getSimpleName());
    }

    /**
     * Renders the PDF report to a raster image
     *
     * @param pageIndex page index
     * @param dpi       DPI for the image
     * @return the image
     */
    public BufferedImage renderImage(final int pageIndex, final int dpi) {
        final PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);

        try {
            return pdfRenderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
        } catch (final IOException ioe) {   // occurs when report render is interrupted
            Logger.getLogger(Report.class.getName()).warning(ioe.getLocalizedMessage());
            return new BufferedImage(1,1, BufferedImage.TYPE_INT_RGB);
        }
    }

    /**
     * Saves the report to a PDF file
     *
     * @param path Path to save to
     * @throws IOException exception
     */
    public void saveToFile(final Path path) throws IOException {
        pdfDocument.save(path.toFile());
    }

    @Override
    public void close() throws IOException {
        pdfDocument.close();
        Logger.getLogger(Report.class.getName()).info("Closed the PDDocument cleanly");
    }

    public boolean isForceGroupPagination() {
        return forceGroupPagination;
    }

    public void setForceGroupPagination(boolean forceGroupPagination) {
        this.forceGroupPagination = forceGroupPagination;
    }
}
