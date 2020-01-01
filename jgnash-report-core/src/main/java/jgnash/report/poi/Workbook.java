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
package jgnash.report.poi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.CurrencyNode;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.report.table.ColumnStyle;
import jgnash.report.table.GroupInfo;
import jgnash.time.DateUtils;
import jgnash.util.FileUtils;
import jgnash.util.NotNull;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static jgnash.report.poi.StyleFactory.DEFAULT_HEIGHT;
import static jgnash.report.poi.StyleFactory.GROUP_HEIGHT;
import static jgnash.report.poi.StyleFactory.HEADER_FOOTER_HEIGHT;
import static jgnash.report.poi.StyleFactory.MARGIN;
import static jgnash.report.poi.StyleFactory.TITLE_HEIGHT;

/**
 * Exports a {@code AbstractReportTableModel} to a spreadsheet using POI
 *
 * TODO: Cross tabulation formulas
 *
 * @author Craig Cavanaugh
 */
public class Workbook {

    private Workbook() {
        // utility class
    }

    public static void export(@NotNull final AbstractReportTableModel reportModel, @NotNull final File file) {
        Objects.requireNonNull(reportModel);
        Objects.requireNonNull(file);

        final Logger logger = Logger.getLogger(Workbook.class.getName());

        final String extension = FileUtils.getFileExtension(file.getAbsolutePath());

        try (final org.apache.poi.ss.usermodel.Workbook wb = extension.equals("xlsx") ? new XSSFWorkbook() : new HSSFWorkbook()) {

            final Map<Style, CellStyle> styleMap = buildStyleMap(wb, reportModel.getCurrencyNode());

            // create a new sheet
            final Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName("Sheet1"));

            final Set<GroupInfo> groupInfoSet = GroupInfo.getGroups(reportModel);

            int sheetRow = 0;

            // Add the title
            sheetRow = addReportHeader(reportModel, styleMap, wb, sheet, sheetRow);

            // write all of the groups
            for (final GroupInfo groupInfo : groupInfoSet) {
                sheetRow = addTableSection(reportModel, styleMap, wb, sheet, groupInfo, sheetRow);
            }

            // Add global footer column
            if (reportModel.hasGlobalSummary()) {
                addGlobalFooter(reportModel, styleMap, wb, sheet, sheetRow);
            }


            // autosize the columns
            int col = 0;
            for (int c = 0; c < reportModel.getColumnCount(); c++) {
                if (reportModel.isColumnVisible(c)) {
                    sheet.autoSizeColumn(col);
                    sheet.setColumnWidth(col, sheet.getColumnWidth(col) + 10);

                    col++;
                }
            }

            logger.log(Level.INFO, "{0} cell styles were used", wb.getNumCellStyles());

            // Save the file
            final String filename;

            if (wb instanceof XSSFWorkbook) {
                filename = FileUtils.stripFileExtension(file.getAbsolutePath()) + ".xlsx";
            } else {
                filename = FileUtils.stripFileExtension(file.getAbsolutePath()) + ".xls";
            }

            try (final OutputStream out = Files.newOutputStream(Paths.get(filename))) {
                wb.write(out);
            } catch (final Exception e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }

        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private static int addReportHeader(@NotNull final AbstractReportTableModel reportModel,
                                       @NotNull final Map<Style, CellStyle> styleMap,
                                       @NotNull final org.apache.poi.ss.usermodel.Workbook wb, @NotNull final Sheet s,
                                       final int startRow) {

        int sheetRow = startRow;
        final CreationHelper createHelper = wb.getCreationHelper();

        Row row = s.createRow(sheetRow);

        Cell cell = row.createCell(0);
        cell.setCellStyle(styleMap.get(Style.TITLE));
        cell.setCellValue(createHelper.createRichTextString(reportModel.getTitle()));
        s.addMergedRegion(new CellRangeAddress(sheetRow, sheetRow, 0, reportModel.getVisibleColumnCount() - 1));

        row.setHeightInPoints(TITLE_HEIGHT + MARGIN);

        sheetRow++;

        if (reportModel.getSubTitle() != null && !reportModel.getSubTitle().isBlank()) {
            row = s.createRow(sheetRow);

            cell = row.createCell(0);
            cell.setCellStyle(styleMap.get(Style.SUBTITLE));
            cell.setCellValue(createHelper.createRichTextString(reportModel.getSubTitle()));
            s.addMergedRegion(new CellRangeAddress(sheetRow, sheetRow, 0, reportModel.getVisibleColumnCount() - 1));

            row.setHeightInPoints(DEFAULT_HEIGHT + MARGIN);

            sheetRow++;
        }

        return ++sheetRow;
    }

    private static int addTableSection(@NotNull final AbstractReportTableModel reportModel, @NotNull final Map<Style, CellStyle> styleMap,
                                       @NotNull final org.apache.poi.ss.usermodel.Workbook wb, @NotNull final Sheet s,
                                       @NotNull final GroupInfo groupInfo, final int startRow) {

        final CellStyle headerStyle = styleMap.get(Style.HEADER);

        final CreationHelper createHelper = wb.getCreationHelper();

        final String group = groupInfo.group;

        int sheetRow = startRow;

        int col = 0; // reusable col tracker

        Row row;

        Cell cell;

        // Add group title
        if (!group.equals(AbstractReportTableModel.DEFAULT_GROUP) ) {
            row = s.createRow(sheetRow);
            cell = row.createCell(col);
            cell.setCellStyle(styleMap.get(Style.GROUP));
            cell.setCellValue(createHelper.createRichTextString(group));
            s.addMergedRegion(new CellRangeAddress(sheetRow, sheetRow, 0, reportModel.getVisibleColumnCount() - 1));
            row.setHeightInPoints(GROUP_HEIGHT + MARGIN);

            sheetRow++;
        }

        // Create headers
        row = s.createRow(sheetRow);
        row.setHeightInPoints(HEADER_FOOTER_HEIGHT + MARGIN);

        col = 0; // reusable col tracker

        for (int c = 0; c < reportModel.getColumnCount(); c++) {
            if (reportModel.isColumnVisible(c)) {
                cell = row.createCell(col);

                cell.setCellStyle(headerStyle);
                cell.setCellValue(createHelper.createRichTextString(reportModel.getColumnName(c)));

                col++;
            }
        }

        sheetRow++;

        // add the groups rows
        for (int tableRow = 0; tableRow < reportModel.getRowCount(); tableRow++) {
            final String rowGroup = reportModel.getGroup(tableRow);

            col = 0;

            if (group.equals(rowGroup)) {
                row = s.createRow(sheetRow);   // new row is needed
                row.setHeightInPoints(DEFAULT_HEIGHT + MARGIN);

                for (int tableCol = 0; tableCol < reportModel.getColumnCount(); tableCol++) {
                    if (reportModel.isColumnVisible(tableCol)) {
                        setCellValue(reportModel, styleMap, wb, row, col, tableRow, tableCol);
                        col++;
                    }
                }
                sheetRow++;
            }
        }

        // add the group footer if needed
        if (groupInfo.hasSummation()) {
            col = 0;
            row = s.createRow(sheetRow);   // new row is needed
            row.setHeightInPoints(HEADER_FOOTER_HEIGHT + MARGIN);

            // column zero is assumed to be a total descriptor
            setFooterCellValue(reportModel.getGroupFooterLabel(), ColumnStyle.STRING, styleMap, wb, row, col);

            col++;

            for (int c = 1; c < reportModel.getColumnCount(); c++) {
                if (reportModel.isColumnVisible(c)) {
                    setFooterCellValue(groupInfo.getValue(c), reportModel.getColumnStyle(c), styleMap, wb, row, col);

                    col++;
                }
            }

            sheetRow++;
        }

        return ++sheetRow;
    }

    private static void addGlobalFooter(@NotNull final AbstractReportTableModel reportModel, @NotNull final Map<Style, CellStyle> styleMap,
                                        @NotNull final org.apache.poi.ss.usermodel.Workbook wb, @NotNull final Sheet s,
                                        final int startRow) {
        int col = 0;
        Row row = s.createRow(startRow);   // new row is needed
        row.setHeightInPoints(HEADER_FOOTER_HEIGHT + MARGIN);

        // column zero is assumed to be a total descriptor
        setFooterCellValue(reportModel.getGrandTotalLegend(), ColumnStyle.STRING, styleMap, wb, row, col);

        col++;

        for (int c = 1; c < reportModel.getColumnCount(); c++) {
            if (reportModel.isColumnVisible(c)) {
                setFooterCellValue(reportModel.getGlobalSum(c), reportModel.getColumnStyle(c), styleMap, wb, row, col);

                col++;
            }
        }
    }

    private static void setCellValue(@NotNull final AbstractReportTableModel reportModel, @NotNull final Map<Style, CellStyle> styleMap,
                                     @NotNull final org.apache.poi.ss.usermodel.Workbook wb, @NotNull final Row row, final int wbCol,
                                     final int tableRow, final int tableColumn) {

        if (reportModel.getValueAt(tableRow, tableColumn) != null) {
            final ColumnStyle columnStyle = reportModel.getColumnStyle(tableColumn);

            switch (columnStyle) {
                case SHORT_AMOUNT:
                case BALANCE:
                case BALANCE_WITH_SUM:
                case BALANCE_WITH_SUM_AND_GLOBAL:
                case AMOUNT_SUM: {
                    Cell cell = row.createCell(wbCol, CellType.NUMERIC);
                    cell.setCellStyle(styleMap.get(Style.AMOUNT));
                    cell.setCellValue(((BigDecimal) reportModel.getValueAt(tableRow, tableColumn)).doubleValue());
                }
                break;
                case PERCENTAGE: {
                    Cell cell = row.createCell(wbCol, CellType.NUMERIC);
                    cell.setCellStyle(styleMap.get(Style.PERCENTAGE));
                    cell.setCellValue(((BigDecimal) reportModel.getValueAt(tableRow, tableColumn)).doubleValue());
                }
                break;
                case QUANTITY: {
                    Cell cell = row.createCell(wbCol, CellType.NUMERIC);
                    cell.setCellStyle(styleMap.get(Style.QUANTITY));
                    cell.setCellValue(((BigDecimal) reportModel.getValueAt(tableRow, tableColumn)).doubleValue());
                }
                break;
                case SHORT_DATE: {
                    Cell cell = row.createCell(wbCol, CellType.STRING);
                    cell.setCellStyle(styleMap.get(Style.SHORT_DATE));
                    cell.setCellValue(DateUtils.asDate((LocalDate) reportModel.getValueAt(tableRow, tableColumn)));
                }
                break;
                case TIMESTAMP: {
                    Cell cell = row.createCell(wbCol, CellType.STRING);
                    cell.setCellStyle(styleMap.get(Style.TIMESTAMP));
                    cell.setCellValue(DateUtils.asDate((LocalDateTime) reportModel.getValueAt(tableRow, tableColumn)));
                }
                break;
                case STRING:
                default: {
                    final CreationHelper createHelper = wb.getCreationHelper();

                    Cell cell = row.createCell(wbCol);
                    cell.setCellStyle(styleMap.get(Style.DEFAULT));
                    cell.setCellValue(createHelper.createRichTextString(reportModel.getValueAt(tableRow, tableColumn).toString()));
                }
            }
        }
    }

    private static void setFooterCellValue(@NotNull final Object value, @NotNull ColumnStyle columnStyle, @NotNull final Map<Style, CellStyle> styleMap,
                                           @NotNull final org.apache.poi.ss.usermodel.Workbook wb, @NotNull final Row row,
                                           final int col) {

        switch (columnStyle) {
            case AMOUNT_SUM:
            case BALANCE_WITH_SUM:
            case BALANCE_WITH_SUM_AND_GLOBAL: {
                Cell cell = row.createCell(col, CellType.NUMERIC);
                cell.setCellStyle(styleMap.get(Style.NUMERIC_FOOTER));
                cell.setCellValue(((BigDecimal) value).doubleValue());
            }
            break;
            case BALANCE:
            case PERCENTAGE:
            case QUANTITY: {
                Cell cell = row.createCell(col);
                cell.setCellStyle(styleMap.get(Style.FOOTER));
            }
            break;
            case STRING:
            default: {
                final CreationHelper createHelper = wb.getCreationHelper();

                Cell cell = row.createCell(col);
                cell.setCellStyle(styleMap.get(Style.FOOTER));

                cell.setCellValue(createHelper.createRichTextString(value.toString()));
            }
        }
    }

    private static Map<Style, CellStyle> buildStyleMap(final org.apache.poi.ss.usermodel.Workbook wb, final CurrencyNode currencyNode) {
        final Map<Style, CellStyle> styleMap = new EnumMap<>(Style.class);

        styleMap.put(Style.AMOUNT, StyleFactory.createDefaultAmountStyle(wb, currencyNode));
        styleMap.put(Style.DEFAULT, StyleFactory.createDefaultStyle(wb));
        styleMap.put(Style.FOOTER, StyleFactory.createFooterStyle(wb));
        styleMap.put(Style.GROUP, StyleFactory.createGroupStyle(wb));
        styleMap.put(Style.HEADER, StyleFactory.createHeaderStyle(wb));

        styleMap.put(Style.NUMERIC_FOOTER, StyleFactory.applyCurrencyFormat(wb, currencyNode,
                StyleFactory.createFooterStyle(wb)));

        styleMap.put(Style.QUANTITY, StyleFactory.applySecurityQuantityFormat(wb, StyleFactory.createDefaultStyle(wb)));

        styleMap.put(Style.PERCENTAGE, StyleFactory.applyPercentageFormat(wb, StyleFactory.createDefaultStyle(wb)));

        styleMap.put(Style.SHORT_DATE, StyleFactory.applyShortDateFormat(wb, StyleFactory.createDefaultStyle(wb)));
        styleMap.put(Style.SUBTITLE, StyleFactory.createSubTitleStyle(wb));
        styleMap.put(Style.TIMESTAMP, StyleFactory.applyTimestampFormat(wb, StyleFactory.createDefaultStyle(wb)));
        styleMap.put(Style.TITLE, StyleFactory.createTitleStyle(wb));

        return styleMap;
    }

    private enum Style {
        AMOUNT,
        DEFAULT,
        FOOTER,
        GROUP,
        HEADER,
        NUMERIC_FOOTER,
        PERCENTAGE,
        QUANTITY,
        SHORT_DATE,
        SUBTITLE,
        TIMESTAMP,
        TITLE
    }
}
