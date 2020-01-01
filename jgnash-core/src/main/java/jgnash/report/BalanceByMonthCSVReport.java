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
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.Comparators;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.time.DateUtils;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

/**
 * Export monthly balance information as a CSV (comma-separated variable) file.
 *
 * @author Craig Cavanaugh
 * @author Tom Edelson
 */
public class BalanceByMonthCSVReport {

    private final List<Account> accountList = new ArrayList<>();

    private final List<BigDecimal[]> balanceList = new ArrayList<>();

    private final boolean vertical;

    private final BiFunction<AccountType, BigDecimal, BigDecimal> balanceConverter;

    private final LocalDate startDate;

    private final LocalDate endDate;

    private final String fileName;

    private final CurrencyNode baseCommodity;

    /**
     * Report constructor.
     *
     * @param fileName         file name to save to
     * @param startDate        start date for the report
     * @param endDate          end date for the report
     * @param currencyNode     CurrencyNode account balances are reported in
     * @param vertical         {@code true} if the export is oriented vertically instead of horizontally
     * @param balanceConverter function to adjust account balance sign
     */
    public BalanceByMonthCSVReport(@NotNull final String fileName, @NotNull final LocalDate startDate,
                                   @NotNull final LocalDate endDate, @Nullable final CurrencyNode currencyNode,
                                   boolean vertical,
                                   @NotNull final BiFunction<AccountType, BigDecimal, BigDecimal> balanceConverter) {

        Objects.requireNonNull(fileName);

        this.fileName = fileName;
        this.baseCommodity = currencyNode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.vertical = vertical;
        this.balanceConverter = balanceConverter;
    }

    public void run() {

        final Logger logger = Logger.getLogger(BalanceByMonthCSVReport.class.getName());

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final LocalDate[] dates = getLastDays(startDate, endDate);

        buildLists(engine.getRootAccount(), dates);

        try {
            logger.info("Writing file");
            if (vertical) {
                writeVerticalCSVFileFormat(fileName, dates);
            } else {
                writeHorizontalFormatCSVFile(fileName, dates);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, null, e);
        }
    }

    private static LocalDate[] getLastDays(final LocalDate startDate, final LocalDate stopDate) {
        final ArrayList<LocalDate> list = new ArrayList<>();

        LocalDate t = DateUtils.getLastDayOfTheMonth(startDate);

        // add a month at a time to the previous date until all of the months
        // have been captured
        while (DateUtils.before(t, stopDate)) {
            list.add(t);
            t = t.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        }

        return list.toArray(new LocalDate[0]);

    }

    private void buildLists(final Account account, final LocalDate[] dates) {
        for (final Account child : account.getChildren(Comparators.getAccountByCode())) {
            if (child.getTransactionCount() > 0) {
                accountList.add(child); // add the account
                final BigDecimal[] bigDecimals = new BigDecimal[dates.length];
                for (int i = 0; i < dates.length; i++) {
                    if (baseCommodity != null) {
                        bigDecimals[i] = balanceConverter.apply(child.getAccountType(), child.getBalance(dates[i],
                                baseCommodity));
                    } else {
                        bigDecimals[i] = balanceConverter.apply(child.getAccountType(), child.getBalance(dates[i]));
                    }
                }
                balanceList.add(bigDecimals);
            }
            if (child.isParent()) {
                buildLists(child, dates);
            }
        }
    }

    /*
     * ,A1,A2,A3 Jan,455,30,80 Feb,566,70,90 March,678,200,300
     */
    private void writeHorizontalFormatCSVFile(final String fileName, final LocalDate[] dates) throws IOException {

        if (fileName == null || dates == null) {
            return;
        }

        try (final BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName), StandardCharsets.UTF_8)) {

            // write out the account names with full path
            final int length = accountList.size();

            for (final Account a : accountList) {
                writer.write(",");
                writer.write(a.getPathName());
            }

            writer.newLine();

            // write out the month, and then balance for that month
            for (int i = 0; i < dates.length; i++) {
                writer.write(dates[i].getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
                for (int j = 0; j < length; j++) {
                    BigDecimal[] bigDecimals = balanceList.get(j);
                    writer.write(",");
                    writer.write(bigDecimals[i].toString());
                }
                writer.newLine();
            }
        }

    }

    /*
     * ,Jan,Feb,Mar A1,30,80,100 A2,70,90,120 A3,200,300,400
     */
    private void writeVerticalCSVFileFormat(final String fileName, final LocalDate[] dates) throws IOException {

        if (fileName == null || dates == null) {
            return;
        }

        try (final BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName), StandardCharsets.UTF_8)) {

            // write out the month header, the first column is empty
            for (final LocalDate date : dates) {
                writer.write(",");
                writer.write(date.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
            }

            writer.newLine();

            // write out the account balance info
            for (int i = 0; i < accountList.size(); i++) {
                writer.write(accountList.get(i).getPathName());

                for (final BigDecimal bigDecimal : balanceList.get(i)) {
                    writer.write(",");
                    writer.write(bigDecimal.toString());
                }
                writer.newLine();
            }
        }
    }
}
