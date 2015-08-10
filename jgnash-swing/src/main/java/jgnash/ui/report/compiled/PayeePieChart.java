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
package jgnash.ui.report.compiled;

import java.awt.EventQueue;
import java.awt.TextField;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Transaction;
import jgnash.engine.search.PayeeMatcher;
import jgnash.text.CommodityFormat;
import jgnash.ui.components.AccountListComboBox;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.GenericCloseDialog;
import jgnash.ui.util.DialogUtils;
import jgnash.util.DateUtils;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

/**
 * This displays a dialog that shows a pie chart with transactions from the selected account Filters can be setup on a
 * per account basis to group by payee Name.
 * 
 * @author Pranay Kumar
 */
public class PayeePieChart {

    private static final String START_DATE = "startDate";

    private static final String FILTER_TAG = "filter:";

    private static final Pattern POUND_DELIMITER_PATTERN = Pattern.compile("#");

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private final Preferences pref = Preferences.userNodeForPackage(PayeePieChart.class);

    private AccountListComboBox combo;

    private JCheckBox useFilters;

    private JComboBox<String> filterCombo;

    private List<String> filterList;

    private TextField txtAddFilter;

    private boolean filtersChanged;

    private JCheckBox showPercentCheck;

    private Account currentAccount; // because the list may not be showing children

    private ChartPanel chartPanelCredit;
    private ChartPanel chartPanelDebit;

    // Fields to select the dates
    private DatePanel startField;

    private DatePanel endField;

    private PieSectionLabelGenerator defaultLabels;

    private PieSectionLabelGenerator percentLabels;

    public static void show() {

        EventQueue.invokeLater(() -> {
            PayeePieChart chart = new PayeePieChart();

            JPanel p = chart.createPanel();
            GenericCloseDialog d = new GenericCloseDialog(p, ResourceUtils.getString("Title.AccountBalance"));
            d.pack();

            d.setMinimumSize(d.getSize());

            DialogUtils.addBoundsListener(d);

            d.setModal(false);

            d.setVisible(true);
        });
    }

    private JPanel createPanel() {

        JButton refreshButton = new JButton(rb.getString("Button.Refresh"));

        JButton addFilterButton = new JButton(rb.getString("Button.AddFilter"));
        final JButton saveButton = new JButton(rb.getString("Button.SaveFilters"));
        JButton deleteFilterButton = new JButton(rb.getString("Button.DeleteFilter"));
        JButton clearPrefButton = new JButton(rb.getString("Button.MasterDelete"));

        filterCombo = new JComboBox<>();

        startField = new DatePanel();
        endField = new DatePanel();

        txtAddFilter = new TextField();

        filterList = new ArrayList<>();
        filtersChanged = false;
        useFilters = new JCheckBox(rb.getString("Label.UseFilters"));
        useFilters.setSelected(true);

        showPercentCheck = new JCheckBox(rb.getString("Label.ShowPercentValues"));

        combo = AccountListComboBox.getFullInstance();

        Date dStart = DateUtils.subtractYear(DateUtils.getFirstDayOfTheMonth(new Date()));

        long start = pref.getLong(START_DATE, dStart.getTime());

        startField.setDate(new Date(start));

        currentAccount = combo.getSelectedAccount();
        PieDataset[] data = createPieDataSet(currentAccount);
        JFreeChart chartCredit = createPieChart(currentAccount, data, 0);
        chartPanelCredit = new ChartPanel(chartCredit, true, true, true, false, true);
        //                         (chart, properties, save, print, zoom, tooltips)
        JFreeChart chartDebit = createPieChart(currentAccount, data, 1);
        chartPanelDebit = new ChartPanel(chartDebit, true, true, true, false, true);

        FormLayout layout = new FormLayout("p, $lcgap, 70dlu, 8dlu, p, $lcgap, 70dlu, $ugap, p, $lcgap:g, left:p", "d, $rgap, d, $ugap, f:p:g, $rgap, d");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        layout.setRowGroups(new int[][] { { 1, 3 } });

        // row 1
        builder.append(combo, 9);
        builder.append(useFilters);
        builder.nextLine();
        builder.nextLine();

        // row 3
        builder.append(rb.getString("Label.StartDate"), startField);
        builder.append(rb.getString("Label.EndDate"), endField);
        builder.append(refreshButton);
        builder.append(showPercentCheck);
        builder.nextLine();
        builder.nextLine();

        // row 5
        FormLayout subLayout = new FormLayout("180dlu:g, 1dlu, 180dlu:g", "f:180dlu:g");
        DefaultFormBuilder subBuilder = new DefaultFormBuilder(subLayout);
        subBuilder.append(chartPanelCredit);
        subBuilder.append(chartPanelDebit);
        
        builder.append(subBuilder.getPanel(), 11);
        builder.nextLine();
        builder.nextLine();

        // row 7
        builder.append(txtAddFilter, 3);
        builder.append(addFilterButton, 3);
        builder.append(saveButton);
        builder.nextLine();
        
        // row
        builder.append(filterCombo, 3);
        builder.append(deleteFilterButton, 3);
        builder.append(clearPrefButton);
        builder.nextLine();

        JPanel panel = builder.getPanel();

        combo.addActionListener(e -> {
            Account newAccount = combo.getSelectedAccount();

            if (filtersChanged) {
                int result = JOptionPane.showConfirmDialog(null, rb.getString("Message.SaveFilters"), "Warning", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    saveButton.doClick();
                }
            }
            filtersChanged = false;

            String[] list = POUND_DELIMITER_PATTERN.split(pref.get(FILTER_TAG + newAccount.hashCode(), ""));
            filterList.clear();
            for (String filter : list) {
                if (!filter.isEmpty()) {
                    //System.out.println("Adding filter: #" + filter + "#");
                    filterList.add(filter);
                }
            }
            refreshFilters();

            setCurrentAccount(newAccount);
            pref.putLong(START_DATE, startField.getDate().getTime());
        });

        refreshButton.addActionListener(e -> {
            setCurrentAccount(currentAccount);
            pref.putLong(START_DATE, startField.getDate().getTime());
        });

        clearPrefButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(null, rb.getString("Message.MasterDelete"), "Warning", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                try {
                    pref.clear();
                } catch (Exception ex) {
                    System.out.println("Exception clearing preferences" + ex);
                }
            }
        });

        saveButton.addActionListener(e -> {
            final StringBuilder sb = new StringBuilder();

            for (String filter : filterList) {
                sb.append(filter);
                sb.append('#');
            }
            //System.out.println("Save = " + FILTER_TAG + currentAccount.hashCode() + " = " + sb.toString());
            pref.put(FILTER_TAG + currentAccount.hashCode(), sb.toString());
            filtersChanged = false;
        });

        addFilterButton.addActionListener(e -> {
            String newFilter = txtAddFilter.getText();
            filterList.remove(newFilter);
            if (!newFilter.isEmpty()) {
                filterList.add(newFilter);
                filtersChanged = true;
                txtAddFilter.setText("");
            }
            refreshFilters();
        });

        deleteFilterButton.addActionListener(e -> {
            if (!filterList.isEmpty()) {
                String filter = (String) filterCombo.getSelectedItem();
                filterList.remove(filter);
                filtersChanged = true;
            }
            refreshFilters();
        });

        useFilters.addActionListener(e -> setCurrentAccount(currentAccount));

        showPercentCheck.addActionListener(e -> {
            ((PiePlot) chartPanelCredit.getChart().getPlot()).setLabelGenerator(showPercentCheck.isSelected() ? percentLabels : defaultLabels);
            ((PiePlot) chartPanelDebit.getChart().getPlot()).setLabelGenerator(showPercentCheck.isSelected() ? percentLabels : defaultLabels);
        });

        return panel;
    }

    private void refreshFilters() {
        filterCombo.removeAllItems();
        filterList.forEach(filterCombo::addItem);
    }

    private void setCurrentAccount(final Account a) {
        try {
            if (a == null) {
                return;
            }
            currentAccount = a;

            PieDataset[] data = createPieDataSet(a);
            chartPanelCredit.setChart(createPieChart(a, data, 0));
            chartPanelCredit.validate();
            chartPanelDebit.setChart(createPieChart(a, data, 1));
            chartPanelDebit.validate();
        } catch (final Exception ex) {           
            Logger.getLogger(PayeePieChart.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        }
    }

    private JFreeChart createPieChart(Account a, PieDataset[] data, int index) {
        PiePlot plot = new PiePlot(data[index]);

        // rebuilt each time because they're based on the account's commodity
        NumberFormat valueFormat = CommodityFormat.getFullNumberFormat(a.getCurrencyNode());
        NumberFormat percentFormat = new DecimalFormat("0.0#%");
        defaultLabels = new StandardPieSectionLabelGenerator("{0} = {1}", valueFormat, percentFormat);
        percentLabels = new StandardPieSectionLabelGenerator("{0} = {1}\n{2}", valueFormat, percentFormat);

        plot.setLabelGenerator(showPercentCheck.isSelected() ? percentLabels : defaultLabels);

        plot.setLabelGap(.02);
        plot.setInteriorGap(.1);

        BigDecimal thisTotal = BigDecimal.ZERO;
        for(int i=0; i<data[index].getItemCount(); i++ ) {
            thisTotal = thisTotal.add( (BigDecimal)(data[index].getValue(i)) );
        }
        BigDecimal acTotal = a.getTreeBalance(startField.getLocalDate(), endField.getLocalDate()).abs();

        String title = "";
        String subtitle = "";

        // pick an appropriate title(s)
        if (index == 0) {
            title = a.getPathName();
            subtitle = rb.getString("Column.Credit") + " : " + valueFormat.format(thisTotal);
        } else if (index == 1) {
            title = rb.getString("Column.Balance") + " : " + valueFormat.format(acTotal);
            subtitle = rb.getString("Column.Debit") + " : " + valueFormat.format(thisTotal);
        }


        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);

        chart.addSubtitle(new TextTitle(subtitle));
        chart.setBackgroundPaint(null);

        return chart;
    }

    private static class TranTuple {

        final Account account;

        final Transaction transaction;

        public TranTuple(Account account, Transaction transaction) {
            this.account = account;
            this.transaction = transaction;
        }
    }

    private List<TranTuple> getTransactions(final Account account, final List<TranTuple> transactions, final LocalDate startDate, final LocalDate endDate) {

        for (final Transaction t : account.getTransactions(startDate, endDate)) {
            TranTuple tuple = new TranTuple(account, t);
            transactions.add(tuple);
        }

        for (final Account child : account.getChildren(Comparators.getAccountByCode())) {
            getTransactions(child, transactions, startDate, endDate);
        }

        return transactions;
    }

    private PieDataset[] createPieDataSet(final Account a) {
        DefaultPieDataset[] returnValue = new DefaultPieDataset[2];
        returnValue[0] = new DefaultPieDataset();
        returnValue[1] = new DefaultPieDataset();

        if (a != null) {
            //System.out.print("Account = "); System.out.println(a);
            Map<String, BigDecimal> names = new HashMap<>();

            List<TranTuple> list = getTransactions(a, new ArrayList<>(), startField.getLocalDate(), endField.getLocalDate());

            CurrencyNode currency = a.getCurrencyNode();

            for (final TranTuple tranTuple : list) {

                Transaction tran = tranTuple.transaction;
                Account account = tranTuple.account;

                String payee = tran.getPayee();
                BigDecimal sum = tran.getAmount(account);

                sum = sum.multiply(account.getCurrencyNode().getExchangeRate(currency));

                //System.out.print("   Transaction = "); System.out.print(payee); System.out.print(" "); System.out.println(sum);

                if (useFilters.isSelected()) {
                    for (String aFilterList : filterList) {

                        PayeeMatcher pm = new PayeeMatcher(aFilterList, false);

                        if (pm.matches(tran)) {
                            payee = aFilterList;
                            //System.out.println(filterList.get(i));
                            break;
                        }
                    }
                }

                if (names.containsKey(payee)) {
                    sum = sum.add(names.get(payee));
                }

                names.put(payee, sum);
            }

            for (final Map.Entry<String, BigDecimal> entry : names.entrySet()) {
                BigDecimal value = entry.getValue();

                if (value.compareTo(BigDecimal.ZERO) == -1) {
                    value = value.negate();
                    returnValue[1].setValue(entry.getKey(), value);
                }
                else {
                    returnValue[0].setValue(entry.getKey(), value);
                }
            }
        }
        return returnValue;
    }

}
