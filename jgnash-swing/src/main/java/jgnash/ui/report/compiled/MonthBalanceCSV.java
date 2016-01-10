/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

/*
 * 2008-10-10, Tom Edelson:
 * Created this new Java class, consisting of most of the code which used to
 * be in jgnash/report/scripts/MonthBalanceCSV.bsh.
 * Very minor changes were made to get it to compile without error or warning.
 * This makes the report run successfully on Mac OS MARKED, where it didn't
 * before; as a side effect, it should make it run faster on all platforms.
 */

package jgnash.ui.report.compiled;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.filechooser.FileNameExtensionFilter;

import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.ui.components.DatePanel;
import jgnash.ui.register.AccountBalanceDisplayManager;
import jgnash.util.DateUtils;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Export monthly balance information as a CSV (comma-separated variable) file
 *
 * @author Craig Cavanaugh
 * @author Tom Edelson
 */
public final class MonthBalanceCSV {

    private final List<Account> accountList = new ArrayList<>();

    private final List<BigDecimal[]> balanceList = new ArrayList<>();

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private boolean vertical = true;

    private MonthBalanceCSV() {

        final LocalDate[] dates = getDates();

        if (dates != null) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            Account root = engine.getRootAccount();
            buildLists(root, dates);

            try {
                System.out.println("writing file");
                if (vertical) {
                    writeVerticalCSVFileFormat(getFileName(), dates);
                } else {
                    writeHorizontalFormatCSVFile(getFileName(), dates);
                }
            } catch (IOException e) {
                Logger.getLogger(MonthBalanceCSV.class.getName()).log(Level.SEVERE, null, e);
            }
        }

    } // end constructor

    private LocalDate[] getDates() {

        LocalDate[] dates;

        DatePanel startField = new DatePanel();
        DatePanel endField = new DatePanel();

        ButtonGroup group = new ButtonGroup();
        JRadioButton vButton = new JRadioButton(rb.getString("Button.Vertical"));
        JRadioButton hButton = new JRadioButton(rb.getString("Button.Horizontal"));

        group.add(vButton);
        group.add(hButton);
        vButton.setSelected(true);

        FormLayout layout = new FormLayout("right:p, 4dlu, p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.rowGroupingEnabled(true);

        builder.append(rb.getString("Label.StartDate"), startField);
        builder.append(rb.getString("Label.EndDate"), endField);
        builder.append(rb.getString("Label.Layout"), vButton);
        builder.append("", hButton);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();

        JPanel panel = builder.getPanel();

        int option = JOptionPane.showConfirmDialog(null, new Object[]{panel}, rb.getString("Message.StartEndDate"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            dates = getLastDays(startField.getLocalDate(), endField.getLocalDate());
        } else {
            dates = null;
        }

        vertical = vButton.isSelected();

        return dates;

    } // end method getDates

    private static LocalDate[] getLastDays(final LocalDate startDate, final LocalDate stopDate) {
        final ArrayList<LocalDate> list = new ArrayList<>();

        LocalDate t = DateUtils.getLastDayOfTheMonth(startDate);

        // add a month at a time to the previous date until all of the months
        // have been captured
        while (DateUtils.before(t, stopDate)) {
            list.add(t);

            t = t.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        }

        return list.toArray(new LocalDate[list.size()]);

    } // end method getLastDays

    private void buildLists(final Account account, final LocalDate[] dates) {
        for (final Account child : account.getChildren(Comparators.getAccountByCode())) {
            if (child.getTransactionCount() > 0) {
                accountList.add(child); // add the account
                BigDecimal[] bigDecimals = new BigDecimal[dates.length];
                for (int i = 0; i < dates.length; i++) {
                    bigDecimals[i] = AccountBalanceDisplayManager.convertToSelectedBalanceMode(child.getAccountType(),
                            child.getBalance(dates[i]));
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

        try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName),
                StandardCharsets.UTF_8))) {

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

        try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName),
                StandardCharsets.UTF_8))) {

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
            } // end outer for loop
        }

    }

    private String getFileName() {
        final JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(rb.getString("Message.CSVFile"), "csv"));

        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            String fileName = chooser.getSelectedFile().getAbsolutePath();
            if (!fileName.endsWith(".csv")) {
                fileName = fileName + ".csv";
            }
            return fileName;
        }
        return null;
    } // end method getFileName

    @SuppressWarnings("unused")
    public static void run() {
        new MonthBalanceCSV();
    }

}
