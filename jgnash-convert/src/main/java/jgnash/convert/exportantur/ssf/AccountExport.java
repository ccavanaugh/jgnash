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
package jgnash.convert.exportantur.ssf;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.text.NumericFormats;
import jgnash.time.DateUtils;
import jgnash.util.FileUtils;
import jgnash.resource.util.ResourceUtils;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * @author Craig Cavanaugh
 */
public class AccountExport {

    private static final String ZERO_WIDTH_SPACE = "\u200B";

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

            // create 2 fonts objects
            final Font defaultFont = wb.createFont();
            final Font headerFont = wb.createFont();

            defaultFont.setFontHeightInPoints((short) 10);
            defaultFont.setColor(IndexedColors.BLACK.getIndex());

            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setColor(IndexedColors.BLACK.getIndex());
            headerFont.setBold(true);

            // create header cell styles
            final CellStyle headerStyle = wb.createCellStyle();

            // Set the other cell style and formatting
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            DataFormat df_header = wb.createDataFormat();

            headerStyle.setDataFormat(df_header.getFormat("text"));
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            final CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("mm/dd/yy"));
            dateStyle.setFont(defaultFont);

            final CellStyle timestampStyle = wb.createCellStyle();
            timestampStyle.setDataFormat(createHelper.createDataFormat().getFormat("YYYY-MM-DD HH:MM:SS"));
            timestampStyle.setFont(defaultFont);

            final CellStyle textStyle = wb.createCellStyle();
            textStyle.setFont(defaultFont);

            final CellStyle amountStyle = wb.createCellStyle();
            amountStyle.setFont(defaultFont);
            amountStyle.setAlignment(HorizontalAlignment.RIGHT);

            final DecimalFormat format = (DecimalFormat) NumericFormats.getFullCommodityFormat(account.getCurrencyNode());
            final String pattern = format.toLocalizedPattern().replace("¤", account.getCurrencyNode().getPrefix());
            final DataFormat df = wb.createDataFormat();
            amountStyle.setDataFormat(df.getFormat(pattern));

            // Create headers
            int row = 0;
            Row r = s.createRow(row);
            for (int i = 0; i < columnNames.length; i++) {
                Cell c = r.createCell(i);
                c.setCellValue(createHelper.createRichTextString(columnNames[i]));
                c.setCellStyle(headerStyle);
            }

            // Dump the transactions
            for (final Transaction transaction : account.getTransactions(startDate, endDate)) {
                r = s.createRow(++row);

                int col = 0;

                // date
                Cell c = r.createCell(col, CellType.STRING);
                c.setCellValue(DateUtils.asDate(transaction.getLocalDate()));
                c.setCellStyle(dateStyle);

                // timestamp
                c = r.createCell(++col, CellType.STRING);
                c.setCellValue(DateUtils.asDate(transaction.getTimestamp()));
                c.setCellStyle(timestampStyle);

                // number
                c = r.createCell(++col, CellType.STRING);
                c.setCellValue(transaction.getNumber());
                c.setCellStyle(textStyle);

                // payee
                c = r.createCell(++col, CellType.STRING);
                c.setCellValue(transaction.getPayee());
                c.setCellStyle(textStyle);

                // memo
                c = r.createCell(++col, CellType.STRING);
                c.setCellValue(transaction.getMemo());
                c.setCellStyle(textStyle);

                // account
                c = r.createCell(++col, CellType.STRING);
                c.setCellValue(getAccountColumnValue(transaction, account));
                c.setCellStyle(textStyle);

                // clr, strip any zero width spaces
                c = r.createCell(++col, CellType.STRING);
                c.setCellValue(transaction.getReconciled(account).toString().replaceAll(ZERO_WIDTH_SPACE, ""));
                c.setCellStyle(textStyle);

                final BigDecimal amount = transaction.getAmount(account);

                // increase
                c = r.createCell(++col, CellType.NUMERIC);
                if (amount.signum() >= 0) {
                    c.setCellValue(amount.doubleValue());
                }
                c.setCellStyle(amountStyle);

                // decrease
                c = r.createCell(++col, CellType.NUMERIC);
                if (amount.signum() < 0) {
                    c.setCellValue(amount.abs().doubleValue());
                }
                c.setCellStyle(amountStyle);

                // balance
                c = r.createCell(++col, CellType.NUMERIC);
                c.setCellValue(account.getBalanceAt(transaction).doubleValue());
                c.setCellStyle(amountStyle);
            }

            // autosize the column widths
            final short columnCount = s.getRow(1).getLastCellNum();

            // autosize all of the columns + 10 pixels
            for (int i = 0; i <= columnCount; i++) {
                s.autoSizeColumn(i);
                s.setColumnWidth(i, s.getColumnWidth(i) + 10);
            }

            Logger.getLogger(AccountExport.class.getName()).log(Level.INFO, "{0} cell styles were used", wb.getNumCellStyles());

            // Save
            final String filename;

            if (wb instanceof XSSFWorkbook) {
                filename = FileUtils.stripFileExtension(file.getAbsolutePath()) + ".xlsx";
            } else {
                filename = FileUtils.stripFileExtension(file.getAbsolutePath()) + ".xls";
            }

            try (final OutputStream out = Files.newOutputStream(Paths.get(filename))) {
                wb.write(out);
            } catch (final Exception e) {
                Logger.getLogger(AccountExport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            }

        }  catch (final IOException e) {
            Logger.getLogger(AccountExport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private static String getAccountColumnValue(final Transaction transaction, final Account account) {
        if (transaction instanceof InvestmentTransaction) {
            return ((InvestmentTransaction) transaction).getInvestmentAccount().getName();
        }
        
		int count = transaction.size();
		
		if (count > 1) {
		    return "[ " + count + " " + ResourceUtils.getString("Button.Splits") + " ]";
		}
		
		final Account creditAccount = transaction.getTransactionEntries().get(0).getCreditAccount();
		
		if (creditAccount != account) {
		   return creditAccount.getName();
		}
		
		return transaction.getTransactionEntries().get(0).getDebitAccount().getName();
    }
}
