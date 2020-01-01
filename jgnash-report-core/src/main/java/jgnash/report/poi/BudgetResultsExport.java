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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Comparators;
import jgnash.engine.budget.BudgetPeriodResults;
import jgnash.engine.budget.BudgetResultsModel;
import jgnash.resource.util.ResourceUtils;
import jgnash.text.NumericFormats;
import jgnash.util.FileUtils;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Utility class to export a {@code BudgetResultsModel}.
 *
 * @author Craig Cavanaugh
 */
public class BudgetResultsExport {

    private BudgetResultsExport() {
        // utility class
    }

    /**
     * Exports a {@code BudgetResultsModel} to a spreadsheet.
     * 
     * @param file File to save to
     * @param model Results model to export
     * @return Error message
     */
    public static String exportBudgetResultsModel(final Path file, final BudgetResultsModel model) {
        
        String message = null;

        final ResourceBundle rb = ResourceUtils.getBundle();
        
        final String extension = FileUtils.getFileExtension(file.toString());
        
        try (final Workbook wb = extension.equals("xlsx") ? new XSSFWorkbook() : new HSSFWorkbook()) {        	
        	final CreationHelper createHelper = wb.getCreationHelper();

            // create a new sheet
            final Sheet s = wb.createSheet(model.getBudget().getName());

            // create header cell styles, override the defaults
            final CellStyle headerStyle = StyleFactory.createHeaderStyle(wb);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            final Font headerFont = StyleFactory.createHeaderFont(wb);
            headerFont.setColor(IndexedColors.BLACK.index);
            headerStyle.setFont(headerFont);

            // Set the other cell style and formatting
            final DataFormat df_header = wb.createDataFormat();
            headerStyle.setDataFormat(df_header.getFormat("text"));

            // create fonts objects
            final Font amountFont = StyleFactory.createDefaultFont(wb);

            int row = 0;
            Row r = s.createRow(row);

            // fill the corner
            Cell corner = r.createCell(0);
            corner.setCellStyle(headerStyle);


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
                    c.setCellValue(createHelper.createRichTextString(rb.getString("Column.Actual")));
                    c.setCellStyle(headerStyle);

                    c = r.createCell(i * 3 + 3);
                    c.setCellValue(createHelper.createRichTextString(rb.getString("Column.Remaining")));
                    c.setCellStyle(headerStyle);
                }
            }

            // must sort the accounts, otherwise child structure is not correct
            List<Account> accounts = new ArrayList<>(model.getAccounts());
            accounts.sort(Comparators.getAccountByTreePosition(Comparators.getAccountByCode()));

            // create account rows
            for (final Account account : accounts) {
                final CellStyle amountStyle = StyleFactory.createDefaultAmountStyle(wb, account.getCurrencyNode());

                // Sets cell indentation, only impacts display if users changes the cell formatting to be left aligned.
                amountStyle.setIndention((short) (model.getDepth(account) * 2));

                row++;

                int col = 0;

                r = s.createRow(row);

                CellStyle cs = wb.createCellStyle();
                cs.cloneStyleFrom(headerStyle);
                cs.setAlignment(HorizontalAlignment.LEFT);
                cs.setIndention((short) (model.getDepth(account) * 2));

                Cell c = r.createCell(col);
                c.setCellValue(createHelper.createRichTextString(account.getName()));
                c.setCellStyle(cs);

                List<CellReference> budgetedRefList = new ArrayList<>();
                List<CellReference> changeRefList = new ArrayList<>();
                List<CellReference> remainingRefList = new ArrayList<>();

                for (int i = 0; i < model.getDescriptorList().size(); i++) {

                    BudgetPeriodResults results = model.getResults(model.getDescriptorList().get(i), account);

                    c = r.createCell(++col, CellType.NUMERIC);
                    c.setCellValue(results.getBudgeted().doubleValue());
                    c.setCellStyle(amountStyle);

                    CellReference budgetedRef = new CellReference(row, col);
                    budgetedRefList.add(budgetedRef);

                    c = r.createCell(++col, CellType.NUMERIC);
                    c.setCellValue(results.getChange().doubleValue());
                    c.setCellStyle(amountStyle);

                    CellReference changeRef = new CellReference(row, col);
                    changeRefList.add(changeRef);

                    c = r.createCell(++col, CellType.FORMULA);
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
            for (final AccountGroup group : model.getAccountGroupList()) {
                final DataFormat df = wb.createDataFormat();

                // reuse the header style but align right
                final CellStyle amountStyle = StyleFactory.createHeaderStyle(wb);
                amountStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                amountStyle.setAlignment(HorizontalAlignment.RIGHT);
                amountStyle.setFont(amountFont);

                final DecimalFormat format = (DecimalFormat) NumericFormats.getFullCommodityFormat(model.getBaseCurrency());
                final String pattern = format.toLocalizedPattern().replace("¤", model.getBaseCurrency().getPrefix());
                amountStyle.setDataFormat(df.getFormat(pattern));

                row++;

                int col = 0;

                r = s.createRow(row);

                CellStyle cs = wb.createCellStyle();
                cs.cloneStyleFrom(headerStyle);
                cs.setAlignment(HorizontalAlignment.LEFT);

                Cell c = r.createCell(col);
                c.setCellValue(createHelper.createRichTextString(group.toString()));
                c.setCellStyle(cs);

                List<CellReference> budgetedRefList = new ArrayList<>();
                List<CellReference> changeRefList = new ArrayList<>();
                List<CellReference> remainingRefList = new ArrayList<>();

                for (int i = 0; i < model.getDescriptorList().size(); i++) {

                    BudgetPeriodResults results = model.getResults(model.getDescriptorList().get(i), group);

                    c = r.createCell(++col, CellType.NUMERIC);
                    c.setCellValue(results.getBudgeted().doubleValue());
                    c.setCellStyle(amountStyle);

                    CellReference budgetedRef = new CellReference(row, col);
                    budgetedRefList.add(budgetedRef);

                    c = r.createCell(++col, CellType.NUMERIC);
                    c.setCellValue(results.getChange().doubleValue());
                    c.setCellStyle(amountStyle);

                    CellReference changeRef = new CellReference(row, col);
                    changeRefList.add(changeRef);

                    c = r.createCell(++col, CellType.FORMULA);
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

            Logger.getLogger(BudgetResultsExport.class.getName()).log(Level.INFO, "{0} cell styles were used", wb.getNumCellStyles());

            // Save
            String filename = file.toString();

            if (wb instanceof XSSFWorkbook) {
                filename = FileUtils.stripFileExtension(filename) + ".xlsx";
            } else {
                filename = FileUtils.stripFileExtension(filename) + ".xls";
            }
            
            try (final OutputStream out = Files.newOutputStream(Paths.get(filename))) {
                wb.write(out);              
            } catch (final Exception e) {
                Logger.getLogger(BudgetResultsExport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                message = e.getLocalizedMessage();
            }        	        	
        } catch (IOException e) {
        	Logger.getLogger(BudgetResultsExport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
		}                      
        
        return message;
    }

    private static void addSummaryCell(final Row row, final int col, final List<CellReference> cellReferenceList, final CellStyle style) {
        final Cell c = row.createCell(col, CellType.FORMULA);
        c.setCellStyle(style);
        c.setCellFormula(buildAddFormula(cellReferenceList));
    }

    private static String buildAddFormula(final List<CellReference> cellReferenceList) {
        final StringBuilder formula = new StringBuilder(cellReferenceList.get(0).formatAsString());

        for (int i = 1; i < cellReferenceList.size(); i++) {
            formula.append('+');
            formula.append(cellReferenceList.get(i).formatAsString());
        }

        return formula.toString();
    }
}
