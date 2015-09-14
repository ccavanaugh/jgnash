/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.convert.exports.xssf;

import jgnash.engine.Account;
import jgnash.util.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Craig Cavanaugh
 */
public class AccountExport {

    private AccountExport() {
        // Utility class
    }

    public static void exportAccount(final Account account, final String[] columnNames, final LocalDate startDate, final LocalDate endDate, final File file) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(startDate);
        Objects.requireNonNull(endDate);
        Objects.requireNonNull(file);
        Objects.requireNonNull(columnNames);

        final String extension = FileUtils.getFileExtension(file.getAbsolutePath());

        try (final Workbook wb = extension.equals("xlsx") ? new XSSFWorkbook() : new HSSFWorkbook()) {
            final CreationHelper createHelper = wb.getCreationHelper();

            // create a new sheet
            final Sheet s = wb.createSheet(account.getName());

            // create header cell styles
            final CellStyle headerStyle = wb.createCellStyle();

            // create 2 fonts objects
            final Font amountFont = wb.createFont();
            final Font headerFont = wb.createFont();

            amountFont.setFontHeightInPoints((short) 10);
            amountFont.setColor(IndexedColors.BLACK.getIndex());

            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setColor(IndexedColors.BLACK.getIndex());
            headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);

            // Set the other cell style and formatting
            headerStyle.setBorderBottom(CellStyle.BORDER_THIN);
            headerStyle.setBorderTop(CellStyle.BORDER_THIN);
            headerStyle.setBorderLeft(CellStyle.BORDER_THIN);
            headerStyle.setBorderRight(CellStyle.BORDER_THIN);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

            DataFormat df_header = wb.createDataFormat();

            headerStyle.setDataFormat(df_header.getFormat("text"));
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(CellStyle.ALIGN_CENTER);

            // Create headers
            int row = 0;
            Row r = s.createRow(row);
            for (int i = 0; i < columnNames.length; i++) {
                Cell c = r.createCell(i);
                c.setCellValue(createHelper.createRichTextString(columnNames[i]));
                c.setCellStyle(headerStyle);
            }


            Logger.getLogger(AccountExport.class.getName()).log(Level.INFO, "{0} cell styles were used", wb.getNumCellStyles());

            // Save
            final String filename;

            if (wb instanceof XSSFWorkbook) {
                filename = FileUtils.stripFileExtension(file.getAbsolutePath()) + ".xlsx";
            } else {
                filename = FileUtils.stripFileExtension(file.getAbsolutePath()) + ".xls";
            }

            try (final FileOutputStream out = new FileOutputStream(filename)) {
                wb.write(out);
            } catch (final Exception e) {
                Logger.getLogger(AccountExport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            }

        }  catch (IOException e) {
            Logger.getLogger(AccountExport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
