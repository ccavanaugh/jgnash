/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.report.table.AbstractReportTableModel;
import jgnash.report.table.GroupInfo;
import jgnash.resource.util.ResourceUtils;
import jgnash.util.FileUtils;
import jgnash.util.NotNull;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Exports a {@code AbstractReportTableModel} to a spreadsheet using POI
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

            final Map<Style, CellStyle> styleMap = buildStyleMap(wb);

            // create a new sheet
            final Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName("Sheet1"));

            final Set<GroupInfo> groupInfoSet = GroupInfo.getGroups(reportModel);

            int sheetRow = 0;

            // write all of the groups
            for (final GroupInfo groupInfo : groupInfoSet) {
                sheetRow = addTableSection(reportModel, styleMap, wb, sheet, groupInfo, sheetRow) + 1;
            }

            // TODO: Add global footer column

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

    private static int addTableSection(@NotNull final AbstractReportTableModel reportModel, final Map<Style, CellStyle> styleMap,
                                       @NotNull final org.apache.poi.ss.usermodel.Workbook wb, @NotNull final Sheet s,
                                       @NotNull final GroupInfo groupInfo, final int startRow) {
        Objects.requireNonNull(groupInfo);
        Objects.requireNonNull(reportModel);
        Objects.requireNonNull(wb);
        Objects.requireNonNull(s);

        final CellStyle headerStyle = styleMap.get(Style.HEADER);
        final CellStyle defaultStyle = styleMap.get(Style.DEFAULT);

        final CreationHelper createHelper = wb.getCreationHelper();

        final String group = groupInfo.group;

        int sheetRow = startRow;

        // Create headers
        Row row = s.createRow(sheetRow);

        int col = 0; // reusable col tracker

        for (int c = 0; c < reportModel.getColumnCount(); c++) {
            if (reportModel.isColumnVisible(c)) {
                final Cell cell = row.createCell(col);

                cell.setCellStyle(headerStyle);
                cell.setCellValue(createHelper.createRichTextString(reportModel.getColumnName(c)));

                col++;
            }
        }

        sheetRow++;

        // add the groups rows
        for (int r = 0; r < reportModel.getRowCount(); r++) {
            final String rowGroup = reportModel.getGroup(r);

            col = 0;

            if (group.equals(rowGroup)) {
                row = s.createRow(sheetRow);   // new row is needed

                for (int c = 0; c < reportModel.getColumnCount(); c++) {
                    if (reportModel.isColumnVisible(c)) {
                        final Cell cell = row.createCell(col);
                        cell.setCellStyle(defaultStyle);
                        cell.setCellValue(createHelper.createRichTextString(reportModel.getValueAt(r, c).toString()));

                        col++;
                    }
                }
                sheetRow++;
            }
        }

        // add the group footer if needed
        if (groupInfo.hasSummation()) {

            final CellStyle footerStyle = styleMap.get(Style.FOOTER);

            col = 0;
            row = s.createRow(sheetRow);   // new row is needed

            Cell cell = row.createCell(col);
            cell.setCellStyle(footerStyle);
            cell.setCellValue(createHelper.createRichTextString(ResourceUtils.getString("Word.Subtotal")));

            col++;

            for (int c = 0; c < reportModel.getColumnCount(); c++) {
                if (reportModel.isColumnVisible(c) && reportModel.isColumnSummed(c)) {
                    cell = row.createCell(col);
                    cell.setCellStyle(footerStyle);
                    cell.setCellValue(createHelper.createRichTextString(groupInfo.getValue(c).toString()));

                    col++;
                }
            }

            sheetRow++;
        }

        return sheetRow;
    }

    private static Map<Style, CellStyle> buildStyleMap(final org.apache.poi.ss.usermodel.Workbook wb) {
        final Map<Style, CellStyle> styleMap = new EnumMap<>(Style.class);

        final Font defaultFont = StyleFactory.createDefaultFont(wb);
        final CellStyle defaultStyle = wb.createCellStyle();
        defaultStyle.setFont(defaultFont);
        styleMap.put(Style.DEFAULT, defaultStyle);

        final Font footerFont = StyleFactory.createFooterFont(wb);
        final CellStyle footerStyle = StyleFactory.createFooterStyle(wb);
        footerStyle.setFont(footerFont);
        styleMap.put(Style.FOOTER, footerStyle);

        final CellStyle headerStyle = StyleFactory.createHeaderStyle(wb);
        final Font headerFont = StyleFactory.createHeaderFont(wb);
        headerStyle.setFont(headerFont);
        styleMap.put(Style.HEADER, headerStyle);

        return styleMap;
    }

    private enum Style {
        DEFAULT,
        FOOTER,
        HEADER
    }
}
