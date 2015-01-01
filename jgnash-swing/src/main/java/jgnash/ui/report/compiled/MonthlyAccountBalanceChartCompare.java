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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.Comparators;
import jgnash.engine.CurrencyNode;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.FilteredAccountListComboBox;
import jgnash.ui.components.GenericCloseDialog;
import jgnash.ui.register.AccountBalanceDisplayManager;
import jgnash.util.DateUtils;
import jgnash.util.Resource;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.ClusteredXYBarRenderer;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * This displays a dialog that shows the monthly balance for a selected account
 *
 * @author Craig Cavanaugh
 * @author Dany Veilleux
 * @author Peter Vida
 * @author Pranay Kumar
 */

/**
 * Modified by Pranay Kumar - Compare accounts on the same chart - Show target currency on the chart axis label (must
 * for people working with multiple currencies) - Use same code for recursive and non recursive account balance
 * calculations
 */

public class MonthlyAccountBalanceChartCompare {

    private final FilteredAccountListComboBox combo1 = new FilteredAccountListComboBox(false, false);

    private final FilteredAccountListComboBox combo2 = new FilteredAccountListComboBox(false, false);

    //this is a customisation that can be removed for a more general release

    private final Resource rb = Resource.get();

    private final DatePanel startDateField = new DatePanel();

    private final DatePanel endDateField = new DatePanel();

    private JCheckBox subAccountCheckBox;

    private JCheckBox hideLockedAccountCheckBox;

    private JCheckBox hidePlaceholderAccountCheckBox;

    private JCheckBox jcb_compare;

    public static void show() {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                Resource rb = Resource.get();

                MonthlyAccountBalanceChartCompare chart = new MonthlyAccountBalanceChartCompare();

                JPanel p = chart.createPanel();
                GenericCloseDialog d = new GenericCloseDialog(p, rb.getString("Title.AccountBalance"));
                d.pack();
                d.setModal(false);

                d.setVisible(true);
            }
        });
    }

    private JPanel createPanel() {
        Date end = DateUtils.getLastDayOfTheMonth(endDateField.getDate());
        Date start = DateUtils.subtractYear(end);
        startDateField.setDate(start);

        JButton refreshButton = new JButton(rb.getString("Button.Refresh"));
        refreshButton.setIcon(Resource.getIcon("/jgnash/resource/view-refresh.png"));

        subAccountCheckBox = new JCheckBox(rb.getString("Button.IncludeSubAccounts"));
        subAccountCheckBox.setSelected(true);

        hideLockedAccountCheckBox = new JCheckBox(rb.getString("Button.HideLockedAccount"));
        hidePlaceholderAccountCheckBox = new JCheckBox(rb.getString("Button.HidePlaceholderAccount"));

        jcb_compare = new JCheckBox(rb.getString("Button.Compare"));
        jcb_compare.setSelected(true);

        Account a = combo1.getSelectedAccount();
        Account a2 = combo2.getSelectedAccount();

        JFreeChart chart = createVerticalXYBarChart(a, a2);
        final ChartPanel chartPanel = new ChartPanel(chart);

        FormLayout layout = new FormLayout("p, 4dlu, p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        FormLayout dLayout = new FormLayout("p, 4dlu, p, 8dlu, p, 4dlu, p, 8dlu, p", "");
        DefaultFormBuilder dBuilder = new DefaultFormBuilder(dLayout);

        dBuilder.append(rb.getString("Label.StartDate"), startDateField);
        dBuilder.append(rb.getString("Label.EndDate"), endDateField);
        dBuilder.append(refreshButton);

        FormLayout cbLayout = new FormLayout("p, 4dlu, p, 4dlu, p, 4dlu", "");
        DefaultFormBuilder cbBuilder = new DefaultFormBuilder(cbLayout);

        cbBuilder.append(subAccountCheckBox);
        cbBuilder.append(hideLockedAccountCheckBox);
        cbBuilder.append(hidePlaceholderAccountCheckBox);

        builder.append(rb.getString("Label.Account"), combo1);
        builder.nextLine();
        builder.append(rb.getString("Label.Compare"), combo2);
        builder.nextLine();
        builder.append(jcb_compare);
        builder.append(cbBuilder.getPanel());
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(dBuilder.getPanel(), 3);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();

        builder.appendRow(RowSpec.decode("fill:p:g"));
        builder.append(chartPanel, 3);

        final JPanel panel = builder.getPanel();

        ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (e.getSource() == jcb_compare) {
                        combo2.setEnabled(jcb_compare.isSelected());
                    }

                    Account account = combo1.getSelectedAccount();
                    if (account == null) {
                        return;
                    }

                    Account account2 = combo2.getSelectedAccount();
                    if (jcb_compare.isSelected() && account2 == null) {
                        return;
                    }

                    updateSubAccountBox();

                    chartPanel.setChart(createVerticalXYBarChart(account, account2));
                    panel.validate();
                } catch (final Exception ex) {
                    Logger.getLogger(MonthlyAccountBalanceChartCompare.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
                }
            }
        };

        combo1.addActionListener(listener);
        combo2.addActionListener(listener);
        jcb_compare.addActionListener(listener);
        subAccountCheckBox.addActionListener(listener);

        hideLockedAccountCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                combo1.setHideLocked(hideLockedAccountCheckBox.isSelected());
                combo2.setHideLocked(hideLockedAccountCheckBox.isSelected());
                try {
                    Account account = combo1.getSelectedAccount();
                    if (account == null) {
                        return;
                    }

                    Account account2 = combo2.getSelectedAccount();
                    if (jcb_compare.isSelected() && account2 == null) {
                        return;
                    }

                    updateSubAccountBox();

                    chartPanel.setChart(createVerticalXYBarChart(account, account2));
                    panel.validate();
                } catch (final Exception ex) {
                    Logger.getLogger(MonthlyAccountBalanceChartCompare.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
                }
            }
        });

        hidePlaceholderAccountCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                combo1.setHidePlaceholder(hidePlaceholderAccountCheckBox.isSelected());
                combo2.setHidePlaceholder(hidePlaceholderAccountCheckBox.isSelected());
                try {
                    Account account = combo1.getSelectedAccount();
                    if (account == null) {
                        return;
                    }
                    Account account2 = combo2.getSelectedAccount();
                    if (jcb_compare.isSelected() && account2 == null) {
                        return;
                    }

                    updateSubAccountBox();

                    chartPanel.setChart(createVerticalXYBarChart(account, account2));
                    panel.validate();
                } catch (final Exception ex) {
                    Logger.getLogger(MonthlyAccountBalanceChartCompare.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
                }
            }
        });
        refreshButton.addActionListener(listener);

        updateSubAccountBox();

        return panel;
    }

    private void updateSubAccountBox() {
        Account a = combo1.getSelectedAccount();
        if (a == null) {
            return;
        }
        if (a.getChildCount() == 0) {
            Account a2 = combo2.getSelectedAccount();
            if (combo2.isEnabled() && a2 != null && a2.getChildCount() > 0) {
                subAccountCheckBox.setEnabled(true);
            } else {
                subAccountCheckBox.setEnabled(false);
            }
        } else {
            subAccountCheckBox.setEnabled(true);
        }
    }

    private JFreeChart createVerticalXYBarChart(final Account a, final Account a2) {
        DateFormat df = new SimpleDateFormat("MM/yy");
        TimeSeriesCollection data = createTimeSeriesCollection(a, a2);

        DateAxis dateAxis = new DateAxis(rb.getString("Column.Date"));
        dateAxis.setTickUnit(new DateTickUnit(DateTickUnitType.MONTH, 1, df));
        dateAxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);

        // if (a.getTransactionCount() > 0) {
        Date start = DateUtils.getFirstDayOfTheMonth(startDateField.getDate());
        Date end = DateUtils.getLastDayOfTheMonth(endDateField.getDate());
        dateAxis.setRange(start, end);
        // }

        NumberAxis valueAxis = new NumberAxis(rb.getString("Column.Balance") + "-" + a.getCurrencyNode().getSymbol());
        StandardXYToolTipGenerator tooltipGenerator = new StandardXYToolTipGenerator("{1}, {2}", df, NumberFormat.getNumberInstance());
        ClusteredXYBarRenderer renderer = new ClusteredXYBarRenderer(0.2, false);
        renderer.setBaseToolTipGenerator(tooltipGenerator);

        XYPlot plot = new XYPlot(data, dateAxis, valueAxis, renderer);
        String title;
        if (jcb_compare.isSelected()) {
            title = a.getPathName() + " vs " + a2.getPathName();
        } else {
            title = rb.getString("Title.AccountBalance") + " - " + a.getPathName();
        }

        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(null);

        return chart;
    }

    private TimeSeriesCollection createTimeSeriesCollection(final Account account, final Account a2) {
        //always use this method
        //if (subAccountCheckBox.isSelected()) {
        // Getting the dates to calculate
        Date start = DateUtils.getFirstDayOfTheMonth(startDateField.getDate());
        Date stop = DateUtils.getLastDayOfTheMonth(endDateField.getDate());
        List<Date> list = DateUtils.getLastDayOfTheMonths(start, stop);
        TimeSeries t = new TimeSeries(rb.getString("Column.Month"), rb.getString("Column.Month"), rb.getString("Column.Balance"));
        TimeSeries t2 = new TimeSeries(rb.getString("Column.Month"), rb.getString("Column.Month"), rb.getString("Column.Balance"));

        // For every month, calculate the total amount
        for (Date aList : list) {
            Date d = DateUtils.getLastDayOfTheMonth(aList);
            Date s = DateUtils.getFirstDayOfTheMonth(aList);

            // Get the total amount for the account and every sub accounts
            // for the specified date
            //BigDecimal bd_TotalAmount = calculateTotal(s, d, account, account.getCurrencyNode());
            BigDecimal bd_TotalAmount = calculateTotal(s, d, account, subAccountCheckBox.isSelected(), account.getCurrencyNode());

            // Include it in the graph
            t.add(new Month(aList), totalModulus(bd_TotalAmount, account.getAccountType()));
            if (jcb_compare.isSelected()) {
                bd_TotalAmount = calculateTotal(s, d, a2, subAccountCheckBox.isSelected(), account.getCurrencyNode());
                t2.add(new Month(aList), totalModulus(bd_TotalAmount, a2.getAccountType()));
            }
        }

        TimeSeriesCollection tsc = new TimeSeriesCollection();
        tsc.addSeries(t);
        if (jcb_compare.isSelected()) {
            tsc.addSeries(t2);
        }

        return tsc;
        /*
                return new TimeSeriesCollection(t);
            }

            int count = account.getTransactionCount();

            if (count > 0) {
                Date start = account.getTransactionAt(0).getDate();
                Date stop = account.getTransactionAt(count - 1).getDate();
                list = DateUtils.getLastDayOfTheMonths(start, stop);
            }

            TimeSeries t = new TimeSeries(rb.getString("Column.Month"), rb.getString("Column.Month"), rb.getString("Column.Balance"));

            AccountType type = account.getAccountType();

            for (Date aList : list) {
                // get balance for the whole month
                Date d = DateUtils.getLastDayOfTheMonth(aList);
                Date s = DateUtils.getFirstDayOfTheMonth(aList);

                BigDecimal balance = AccountBalanceDisplayManager.convertToSelectedBalanceMode(type, account.getBalance(s, d));
                t.add(new Month(aList), balance);
            }

            return new TimeSeriesCollection(t);
        */
    }

    private static BigDecimal calculateTotal(final Date start, final Date end, final Account account, boolean recursive, final CurrencyNode baseCurrency) {
        BigDecimal amount;
        AccountType type = account.getAccountType();

        // get the amount for the account                
        amount = AccountBalanceDisplayManager.convertToSelectedBalanceMode(type, account.getBalance(start, end, baseCurrency));

        if (recursive) {
            // add the amount of every sub accounts
            for (final Account child : account.getChildren(Comparators.getAccountByCode())) {
                amount = amount.add(calculateTotal(start, end, child, true, baseCurrency));
            }
        }

        return amount;
    }

    private static BigDecimal totalModulus(BigDecimal amount, final AccountType type) {
        if (type == AccountType.INCOME || type == AccountType.EQUITY || type == AccountType.CREDIT || type == AccountType.LIABILITY) {
            return amount.negate();
        }

        return amount;
    }

}
