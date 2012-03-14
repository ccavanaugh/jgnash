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
package jgnash.engine.budget;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.text.CommodityFormat;
import jgnash.util.FileUtils;
import jgnash.util.Resource;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Utility class to export a
 * <code>BudgetResultsModel</code>
 *
 * @author Craig Cavanaugh
 * @version $Id$
 */
public class BudgetResultsExport {

    private BudgetResultsExport() {
        // utility class
    }

    public static void exportBudgetResultsModel(final File file, final BudgetResultsModel model) {

        Resource rb = Resource.get();

        Workbook wb;

        String extension = FileUtils.getFileExtension(file.getAbsolutePath());

        if (extension.equals("xlsx")) {
            wb = new XSSFWorkbook();
        } else {
            wb = new HSSFWorkbook();
        }

        CreationHelper createHelper = wb.getCreationHelper();

        // create a new sheet
        Sheet s = wb.createSheet(model.getBudget().getName());

        // create header cell styles
        CellStyle headerStyle = wb.createCellStyle();

        // create 2 fonts objects
        Font amountFont = wb.createFont();
        Font headerFont = wb.createFont();

        amountFont.setFontHeightInPoints((short) 10);
        amountFont.setColor(IndexedColors.BLACK.getIndex());

        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setColor(IndexedColors.BLACK.getIndex());
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);

        DataFormat df = wb.createDataFormat();

        // Set the other cell style and formatting
        headerStyle.setBorderBottom(CellStyle.BORDER_THIN);
        headerStyle.setBorderTop(CellStyle.BORDER_THIN);
        headerStyle.setBorderLeft(CellStyle.BORDER_THIN);
        headerStyle.setBorderRight(CellStyle.BORDER_THIN);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

        headerStyle.setDataFormat(df.getFormat("text"));
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(CellStyle.ALIGN_CENTER);

        int row = 0;
        Row r = s.createRow(row);

        // create period headers
        for (int i = 0; i < model.getDescriptorList().size(); i++) {
            Cell c = r.createCell(i * 3 + 1);
            c.setCellValue(createHelper.createRichTextString(model.getDescriptorList().get(i).getPeriodDescription()));
            c.setCellStyle(headerStyle);
            s.addMergedRegion(new CellRangeAddress(row, row, i * 3 + 1, i * 3 + 3));
        }

        {
            int col = model.getDescriptorList().size() * 3 + 1;
            Cell c = r.createCell(col);
            c.setCellValue(createHelper.createRichTextString(rb.getString("Title.Summary")));
            c.setCellStyle(headerStyle);
            s.addMergedRegion(new CellRangeAddress(row, row, col, col + 2));
        }

        // create results header columns
        row++;
        r = s.createRow(row);

        {
            Cell c = r.createCell(0);
            c.setCellValue(createHelper.createRichTextString(rb.getString("Column.Account")));
            c.setCellStyle(headerStyle);

            for (int i = 0; i <= model.getDescriptorList().size(); i++) {
                c = r.createCell(i * 3 + 1);
                c.setCellValue(createHelper.createRichTextString(rb.getString("Column.Budgeted")));
                c.setCellStyle(headerStyle);

                c = r.createCell(i * 3 + 2);
                c.setCellValue(createHelper.createRichTextString(rb.getString("Column.Change")));
                c.setCellStyle(headerStyle);

                c = r.createCell(i * 3 + 3);
                c.setCellValue(createHelper.createRichTextString(rb.getString("Column.Remaining")));
                c.setCellStyle(headerStyle);
            }
        }

        // must sort the accounts, otherwise child structure is not correct
        List<Account> accounts = new ArrayList<>(model.getAccounts());
        Collections.sort(accounts);

        // create account rows
        for (Account account : accounts) {

            CellStyle amountStyle = wb.createCellStyle();
            amountStyle.setFont(amountFont);

            DecimalFormat format = (DecimalFormat) CommodityFormat.getFullNumberFormat(account.getCurrencyNode());
            String pattern = format.toLocalizedPattern().replace("¤", account.getCurrencyNode().getPrefix());
            amountStyle.setDataFormat(df.getFormat(pattern));

            row++;

            int col = 0;

            r = s.createRow(row);

            CellStyle cs = wb.createCellStyle();
            cs.cloneStyleFrom(headerStyle);
            cs.setAlignment(CellStyle.ALIGN_LEFT);
            cs.setIndention((short) (model.getDepth(account) * 2));

            Cell c = r.createCell(col);
            c.setCellValue(createHelper.createRichTextString(account.getName()));
            c.setCellStyle(cs);

            List<CellReference> budgetedRefList = new ArrayList<>();
            List<CellReference> changeRefList = new ArrayList<>();
            List<CellReference> remainingRefList = new ArrayList<>();

            for (int i = 0; i < model.getDescriptorList().size(); i++) {

                BudgetPeriodResults results = model.getResults(model.getDescriptorList().get(i), account);

                c = r.createCell(++col);
                c.setCellType(Cell.CELL_TYPE_NUMERIC);
                c.setCellValue(results.getBudgeted().doubleValue());
                c.setCellStyle(amountStyle);

                CellReference budgetedRef = new CellReference(row, col);
                budgetedRefList.add(budgetedRef);

                c = r.createCell(++col);
                c.setCellType(Cell.CELL_TYPE_NUMERIC);
                c.setCellValue(results.getChange().doubleValue());
                c.setCellStyle(amountStyle);

                CellReference changeRef = new CellReference(row, col);
                changeRefList.add(changeRef);

                c = r.createCell(++col);
                c.setCellType(Cell.CELL_TYPE_FORMULA);
                c.setCellStyle(amountStyle);
                c.setCellFormula(budgetedRef.formatAsString() + "-" + changeRef.formatAsString());

                CellReference remainingRef = new CellReference(row, col);
                remainingRefList.add(remainingRef);
            }

            // add summary columns                               
            addSummaryCell(r, ++col, budgetedRefList, amountStyle);
            addSummaryCell(r, ++col, changeRefList, amountStyle);
            addSummaryCell(r, ++col, remainingRefList, amountStyle);
        }

        // add group summary rows
        for (AccountGroup group : model.getAccountGroupList()) {

            CellStyle amountStyle = wb.createCellStyle();
            amountStyle.setFont(amountFont);
            amountStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            amountStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
            amountStyle.setBorderBottom(CellStyle.BORDER_THIN);
            amountStyle.setBorderTop(CellStyle.BORDER_THIN);
            amountStyle.setBorderLeft(CellStyle.BORDER_THIN);
            amountStyle.setBorderRight(CellStyle.BORDER_THIN);

            DecimalFormat format = (DecimalFormat) CommodityFormat.getFullNumberFormat(model.getBaseCurrency());
            String pattern = format.toLocalizedPattern().replace("¤", model.getBaseCurrency().getPrefix());
            amountStyle.setDataFormat(df.getFormat(pattern));

            row++;

            int col = 0;

            r = s.createRow(row);

            CellStyle cs = wb.createCellStyle();
            cs.cloneStyleFrom(headerStyle);
            cs.setAlignment(CellStyle.ALIGN_LEFT);

            Cell c = r.createCell(col);
            c.setCellValue(createHelper.createRichTextString(group.toString()));
            c.setCellStyle(cs);

            List<CellReference> budgetedRefList = new ArrayList<>();
            List<CellReference> changeRefList = new ArrayList<>();
            List<CellReference> remainingRefList = new ArrayList<>();

            for (int i = 0; i < model.getDescriptorList().size(); i++) {

                BudgetPeriodResults results = model.getResults(model.getDescriptorList().get(i), group);

                c = r.createCell(++col);
                c.setCellType(Cell.CELL_TYPE_NUMERIC);
                c.setCellValue(results.getBudgeted().doubleValue());
                c.setCellStyle(amountStyle);

                CellReference budgetedRef = new CellReference(row, col);
                budgetedRefList.add(budgetedRef);

                c = r.createCell(++col);
                c.setCellType(Cell.CELL_TYPE_NUMERIC);
                c.setCellValue(results.getChange().doubleValue());
                c.setCellStyle(amountStyle);

                CellReference changeRef = new CellReference(row, col);
                changeRefList.add(changeRef);

                c = r.createCell(++col);
                c.setCellType(Cell.CELL_TYPE_FORMULA);
                c.setCellStyle(amountStyle);
                c.setCellFormula(budgetedRef.formatAsString() + "-" + changeRef.formatAsString());

                CellReference remainingRef = new CellReference(row, col);
                remainingRefList.add(remainingRef);
            }

            // add summary columns                               
            addSummaryCell(r, ++col, budgetedRefList, amountStyle);
            addSummaryCell(r, ++col, changeRefList, amountStyle);
            addSummaryCell(r, ++col, remainingRefList, amountStyle);
        }

        // force evaluation
        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
        evaluator.evaluateAll();

        short columnCount = s.getRow(1).getLastCellNum();

        // autosize all of the columns + 10 pixels
        for (int i = 0; i <= columnCount; i++) {
            s.autoSizeColumn(i);
            s.setColumnWidth(i, s.getColumnWidth(i) + 10);
        }

        // Save
        String filename = file.getAbsolutePath();

        if (wb instanceof XSSFWorkbook) {
            filename = FileUtils.stripFileExtension(filename) + ".xlsx";
        } else {
            filename = FileUtils.stripFileExtension(filename) + ".xls";
        }

        FileOutputStream out;

        try {
            out = new FileOutputStream(filename);
            wb.write(out);
            out.close();
        } catch (Exception e) {
            Logger.getLogger(BudgetResultsExport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

    }

    private static void addSummaryCell(final Row row, final int col, final List<CellReference> cellReferenceList, final CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellType(Cell.CELL_TYPE_FORMULA);
        c.setCellStyle(style);
        c.setCellFormula(buildAddFormula(cellReferenceList));
    }

    private static String buildAddFormula(List<CellReference> cellReferenceList) {
        StringBuilder formula = new StringBuilder(cellReferenceList.get(0).formatAsString());

        for (int i = 1; i < cellReferenceList.size(); i++) {
            formula.append("+");
            formula.append(cellReferenceList.get(i).formatAsString());
        }

        return formula.toString();
    }
}
