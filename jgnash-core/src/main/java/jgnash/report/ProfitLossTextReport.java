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
package jgnash.report;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.Comparators;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.text.NumericFormats;
import jgnash.resource.util.ResourceUtils;

/**
 * Dumps a simple Profit/Loss report to a text file.
 *
 * @author Michael Mueller
 * @author Craig Cavanaugh
 * @author David Robertson
 */
public class ProfitLossTextReport {

    private static final int MAX_LENGTH = 15;  //(-000,000,000.00)

    private static final int MAX_NAME_LEN = 30;

    private static final int HEADER_LENGTH = 54;

    private static final String REPORT_FOOTER = new String(new char[HEADER_LENGTH]).replace("\0", "=");

    private static final String ROW_SEPARATOR = new String(new char[HEADER_LENGTH]).replace("\0", "-");

    private NumberFormat numberFormat;

    private final List<String> reportText = new ArrayList<>();

    private final CurrencyNode baseCommodity;

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private final BiFunction<AccountType, BigDecimal, BigDecimal> balanceConverter;

    private final LocalDate startDate;

    private final LocalDate endDate;

    private final String fileName;

    /**
     * Report constructor.
     *
     * @param fileName         file name to save to
     * @param startDate        start date for the report
     * @param endDate          end date for the report
     * @param currencyNode     CurrencyNode account balances are reported in
     * @param balanceConverter function to adjust account balance sign
     */
    public ProfitLossTextReport(final String fileName, final LocalDate startDate, final LocalDate endDate,
                                final CurrencyNode currencyNode,
                                final BiFunction<AccountType, BigDecimal, BigDecimal> balanceConverter) {

        Objects.requireNonNull(fileName);

        this.fileName = fileName;
        this.baseCommodity = currencyNode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.balanceConverter = balanceConverter;
    }

    public void run() {
        generateReport();
        writeFile();
    }

    private void generateReport() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final Account root = engine.getRootAccount();

        numberFormat = NumericFormats.getFullCommodityFormat(baseCommodity);

        final DateTimeFormatter df = DateTimeFormatter.ISO_DATE;

        // title and dates
        reportText.add(rb.getString("Title.ProfitLoss"));
        reportText.add("");
        reportText.add("From " + df.format(startDate) + " To " + df.format(endDate));
        reportText.add("");
        reportText.add("");

        //Income
        reportText.add(AccountType.INCOME.toString());
        reportText.add(ROW_SEPARATOR);

        //Add up the  Gross Income.
        BigDecimal incomeTotal = BigDecimal.ZERO;

        for (final BigDecimal balance : getBalances(root, AccountType.INCOME)) {
            incomeTotal = incomeTotal.add(balance);
        }

        reportText.add(ROW_SEPARATOR);
        reportText.add(formatAccountName(rb.getString("Word.GrossIncome")) + " " + formatDecimal(incomeTotal));
        reportText.add(ROW_SEPARATOR);
        reportText.add("");
        reportText.add("");

        //Expense
        reportText.add(AccountType.EXPENSE.toString());
        reportText.add(ROW_SEPARATOR);

        //Add up the Gross Expenses
        BigDecimal expenseTotal = BigDecimal.ZERO;

        for (final BigDecimal balance : getBalances(root, AccountType.EXPENSE)) {
            expenseTotal = expenseTotal.add(balance);
        }
        reportText.add(ROW_SEPARATOR);
        reportText.add(formatAccountName(rb.getString("Word.GrossExpense")) + " " + formatDecimal(expenseTotal));
        reportText.add(ROW_SEPARATOR);
        reportText.add("");
        reportText.add("");

        //Net Total
        reportText.add(ROW_SEPARATOR);
        reportText.add(formatAccountName(rb.getString("Word.NetIncome")) + " " + formatDecimal(incomeTotal.add(expenseTotal)));
        reportText.add(REPORT_FOOTER);
    }

    private void writeFile() {
        try (final BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName), StandardCharsets.UTF_8)) {
            for (final String text : reportText) {  //write the array list pl to the file
                writer.write(text);
                writer.newLine();
            }
            writer.newLine();
        } catch (IOException e) {
            Logger.getLogger(ProfitLossTextReport.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private List<BigDecimal> getBalances(final Account a, final AccountType type) {

        final List<BigDecimal> balances = new ArrayList<>();

        for (final Account child : a.getChildren(Comparators.getAccountByCode())) {
            if ((child.getTransactionCount() > 0) && type == child.getAccountType()) {

                final BigDecimal acctBal = balanceConverter.apply(child.getAccountType(),
                        child.getBalance(startDate, endDate, baseCommodity));

                // output account name and balance
                reportText.add(formatAccountName(child.getName()) + " " + formatDecimal(acctBal));

                balances.add(acctBal);
            }

            if (child.isParent()) {
                balances.addAll(getBalances(child, type));
            }
        }

        return balances;
    }

    /**
     * Format decimal amount.
     *
     * @param balance the BigDecimal value to format
     * @return formatted string
     */
    private String formatDecimal(final BigDecimal balance) {
        final StringBuilder stringBuilder = new StringBuilder();
        final String formattedBalance = numberFormat.format(balance);

        // right align amount to pre-defined maximum length
        if (formattedBalance.length() < MAX_LENGTH) {
            stringBuilder.append(" ".repeat(MAX_LENGTH - formattedBalance.length()));
        }

        stringBuilder.append(formattedBalance);

        return stringBuilder.toString();
    }

    /**
     * Format account name.
     *
     * @param name the account name to format
     * @return the formatted account name
     */
    private static String formatAccountName(final String name) {
        final StringBuilder stringBuilder = new StringBuilder(MAX_NAME_LEN);

        stringBuilder.append(name);

        // set name to pre-defined maximum length
        stringBuilder.append(" ".repeat(Math.max(0, MAX_NAME_LEN - name.length())));

        stringBuilder.setLength(MAX_NAME_LEN);

        return stringBuilder.toString();
    }
}
