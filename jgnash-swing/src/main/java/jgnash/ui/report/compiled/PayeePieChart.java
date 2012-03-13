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
package jgnash.ui.report.compiled;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.EventQueue;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Transaction;
import jgnash.engine.search.PayeeMatcher;
import jgnash.text.CommodityFormat;
import jgnash.ui.components.AccountListComboBox;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.GenericCloseDialog;
import jgnash.util.DateUtils;
import jgnash.util.Resource;

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
 * @version $Id: PayeePieChart.java 3070 2012-01-04 11:00:43Z ccavanaugh $
 */
public class PayeePieChart {

    private static final String STARTDATE = "startDate";

    private static final String FILTERTAG = "filter:";

    private static final Pattern POUND_DELIMITER_PATTERN = Pattern.compile("#");

    private final Resource rb = Resource.get();

    private Preferences pref = Preferences.userNodeForPackage(PayeePieChart.class);

    private AccountListComboBox combo;

    private JCheckBox useFilters;

    private JComboBox filterCombo;

    private List<String> filterList;

    private TextField txtAddFilter;

    private boolean filtersChanged;

    private JCheckBox showPercentCheck;

    private Account currentAccount; // because the list may not be showing children

    private ChartPanel chartPanel;

    // Fields to select the dates
    private DatePanel startField;

    private DatePanel endField;

    private PieSectionLabelGenerator defaultLabels;

    private PieSectionLabelGenerator percentLabels;

    public static void show() {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {

                Resource rb = Resource.get();

                PayeePieChart chart = new PayeePieChart();

                JPanel p = chart.createPanel();
                GenericCloseDialog d = new GenericCloseDialog(p, rb.getString("Title.AccountBalance"));
                d.pack();
                d.setModal(false);

                d.setVisible(true);
            }
        });
    }

    private JPanel createPanel() {

        JButton refreshButton = new JButton(rb.getString("Button.Refresh"));

        JButton addFilterButton = new JButton(rb.getString("Button.AddFilter"));
        final JButton saveButton = new JButton(rb.getString("Button.SaveFilters"));
        JButton deleteFilterButton = new JButton(rb.getString("Button.DeleteFilter"));
        JButton clearPrefButton = new JButton(rb.getString("Button.MasterDelete"));

        filterCombo = new JComboBox();

        startField = new DatePanel();
        endField = new DatePanel();

        txtAddFilter = new TextField();

        filterList = new ArrayList<String>();
        filtersChanged = false;
        useFilters = new JCheckBox(rb.getString("Label.UseFilters"));
        useFilters.setSelected(true);

        showPercentCheck = new JCheckBox(rb.getString("Label.ShowPercentValues"));

        combo = AccountListComboBox.getFullInstance();

        Date dStart = DateUtils.subtractYear(DateUtils.getFirstDayOfTheMonth(new Date()));

        long start = pref.getLong(STARTDATE, dStart.getTime());

        startField.setDate(new Date(start));

        currentAccount = combo.getSelectedAccount();
        JFreeChart chart = createPieChart(currentAccount);
        chartPanel = new ChartPanel(chart, true, true, true, false, true);
        //                         (chart, properties, save, print, zoom, tooltips)

        FormLayout layout = new FormLayout("p, 4dlu, 70dlu, 8dlu, p, 4dlu, 70dlu, 8dlu, p, 4dlu:g, left:p", "f:d, 3dlu, f:d, 6dlu, f:p:g");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        layout.setRowGroups(new int[][] { { 1, 3 } });

        builder.append(combo, 9);
        builder.append(useFilters);
        builder.nextLine();

        builder.nextLine();

        builder.append(rb.getString("Label.StartDate"), startField);
        builder.append(rb.getString("Label.EndDate"), endField);
        builder.append(refreshButton);

        builder.append(showPercentCheck);
        builder.nextLine();
        builder.nextLine();

        builder.append(chartPanel, 11);
        builder.nextLine();

        builder.append(txtAddFilter, 3);
        builder.append(addFilterButton, 3);
        builder.append(saveButton);
        builder.nextLine();
        builder.append(filterCombo, 3);
        builder.append(deleteFilterButton, 3);
        builder.append(clearPrefButton);
        builder.nextLine();

        JPanel panel = builder.getPanel();

        combo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                Account newAccount = combo.getSelectedAccount();

                if (filtersChanged) {
                    int result = JOptionPane.showConfirmDialog(null, rb.getString("Message.SaveFilters"), "Warning", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        saveButton.doClick();
                    }
                }
                filtersChanged = false;

                String[] list = POUND_DELIMITER_PATTERN.split(pref.get(FILTERTAG + newAccount.hashCode(), ""));
                filterList.clear();
                for (String filter : list) {
                    if (filter.length() > 0) {
                        //System.out.println("Adding filter: #" + filter + "#");
                        filterList.add(filter);
                    }
                }
                refreshFilters();

                setCurrentAccount(newAccount);
                pref.putLong(STARTDATE, startField.getDate().getTime());
            }
        });

        refreshButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                setCurrentAccount(currentAccount);
                pref.putLong(STARTDATE, startField.getDate().getTime());
            }
        });

        clearPrefButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(null, rb.getString("Message.MasterDelete"), "Warning", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    try {
                        pref.clear();
                    } catch (Exception ex) {
                        System.out.println("Exception clearing preferences" + ex);
                    }
                }
            }
        });

        saveButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                StringBuilder sb = new StringBuilder();

                for (String filter : filterList) {
                    sb.append(filter);
                    sb.append('#');
                }
                //System.out.println("Save = " + FILTERTAG + currentAccount.hashCode() + " = " + sb.toString());
                pref.put(FILTERTAG + currentAccount.hashCode(), sb.toString());
                filtersChanged = false;
            }
        });

        addFilterButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                String newFilter = txtAddFilter.getText();
                filterList.remove(newFilter);
                if (newFilter.length() > 0) {
                    filterList.add(newFilter);
                    filtersChanged = true;
                    txtAddFilter.setText("");
                }
                refreshFilters();
            }
        });

        deleteFilterButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                if (filterList.size() > 0) {
                    String filter = (String) filterCombo.getSelectedItem();
                    filterList.remove(filter);
                    filtersChanged = true;
                }
                refreshFilters();
            }
        });

        useFilters.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                setCurrentAccount(currentAccount);
            }
        });

        showPercentCheck.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                ((PiePlot) chartPanel.getChart().getPlot()).setLabelGenerator(showPercentCheck.isSelected() ? percentLabels : defaultLabels);
            }
        });

        return panel;
    }

    private void refreshFilters() {
        filterCombo.removeAllItems();
        for (String aFilterList : filterList) {
            filterCombo.addItem(aFilterList);
        }
    }

    private void setCurrentAccount(final Account a) {
        try {
            if (a == null) {
                return;
            }
            currentAccount = a;

            chartPanel.setChart(createPieChart(a));
            chartPanel.validate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private JFreeChart createPieChart(Account a) {
        PieDataset data = createPieDataset(a);
        PiePlot plot = new PiePlot(data);

        // rebuilt each time because they're based on the account's commodity
        NumberFormat valueFormat = CommodityFormat.getFullNumberFormat(a.getCurrencyNode());
        NumberFormat percentFormat = new DecimalFormat("0.0#%");
        defaultLabels = new StandardPieSectionLabelGenerator("{0} = {1}", valueFormat, percentFormat);
        percentLabels = new StandardPieSectionLabelGenerator("{0} = {1}\n{2}", valueFormat, percentFormat);

        plot.setLabelGenerator(showPercentCheck.isSelected() ? percentLabels : defaultLabels);

        plot.setLabelGap(.02);
        plot.setInteriorGap(.1);

        String title;

        // pick an appropriate title
        if (a.getAccountType() == AccountType.EXPENSE) {
            title = rb.getString("Title.PercentExpense");
        } else if (a.getAccountType() == AccountType.INCOME) {
            title = rb.getString("Title.PercentIncome");
        } else {
            title = rb.getString("Title.PercentDist");
        }

        title = title + " - " + a.getPathName();

        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);

        BigDecimal total = a.getTreeBalance(startField.getDate(), endField.getDate()).abs();

        chart.addSubtitle(new TextTitle(valueFormat.format(total)));

        chart.setBackgroundPaint(null);

        return chart;
    }

    private static class TranTuple {

        Account account;

        Transaction transaction;

        public TranTuple(Account account, Transaction transaction) {
            this.account = account;
            this.transaction = transaction;
        }
    }

    private List<TranTuple> getTransactions(final Account account, final List<TranTuple> transactions, final Date startDate, final Date endDate) {

        for (Transaction t : account.getTransactions(startDate, endDate)) {
            TranTuple touple = new TranTuple(account, t);
            transactions.add(touple);
        }

        for (Account child : account.getChildren()) {
            getTransactions(child, transactions, startDate, endDate);
        }

        return transactions;
    }

    private PieDataset createPieDataset(final Account a) {
        DefaultPieDataset returnValue = new DefaultPieDataset();
        if (a != null) {
            //System.out.print("Account = "); System.out.println(a);
            Map<String, BigDecimal> names = new HashMap<String, BigDecimal>();

            List<TranTuple> list = getTransactions(a, new ArrayList<TranTuple>(), startField.getDate(), endField.getDate());

            CurrencyNode currency = a.getCurrencyNode();

            for (TranTuple touple : list) {

                Transaction tran = touple.transaction;
                Account account = touple.account;

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

            for (String key : names.keySet()) {
                String sKey = key;
                BigDecimal value = names.get(key);

                if (value.compareTo(BigDecimal.ZERO) == -1) {
                    value = value.negate();
                    sKey = sKey + "(-ve)";
                }

                returnValue.setValue(sKey, value);
            }
        }
        return returnValue;
    }

}
