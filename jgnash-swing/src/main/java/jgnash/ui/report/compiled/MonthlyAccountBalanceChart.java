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
import com.jgoodies.forms.layout.RowSpec;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.FilteredAccountListComboBox;
import jgnash.ui.components.GenericCloseDialog;
import jgnash.ui.register.AccountBalanceDisplayManager;
import jgnash.util.DateUtils;
import jgnash.util.Resource;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * This displays a dialog that shows the monthly balance for a selected account
 * 
 * @author Craig Cavanaugh
 * @author Dany Veilleux
 * @author Peter Vida
 *
 */
public class MonthlyAccountBalanceChart {

    private FilteredAccountListComboBox combo = new FilteredAccountListComboBox(false, false);

    private Resource rb = Resource.get();

    private DatePanel startDateField = new DatePanel();

    private DatePanel endDateField = new DatePanel();

    private JCheckBox subAccountCheckBox;

    private JCheckBox hideLockedAccountCheckBox;

    private JCheckBox hidePlaceholderAccountCheckBox;

    public static void show() {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                Resource rb = Resource.get();

                MonthlyAccountBalanceChart chart = new MonthlyAccountBalanceChart();

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

        Account a = combo.getSelectedAccount();
        JFreeChart chart = createVerticalXYBarChart(a);
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

        builder.append(rb.getString("Label.Account"), combo);
        builder.nextLine();
        builder.append(" ");
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
                    Account account = combo.getSelectedAccount();
                    if (account == null) {
                        return;
                    }

                    updateSubAccountBox();

                    chartPanel.setChart(createVerticalXYBarChart(account));
                    panel.validate();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        };

        combo.addActionListener(listener);

        hideLockedAccountCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                combo.setHideLocked(hideLockedAccountCheckBox.isSelected());
                try {
                    Account account = combo.getSelectedAccount();
                    if (account == null) {
                        return;
                    }

                    updateSubAccountBox();

                    chartPanel.setChart(createVerticalXYBarChart(account));
                    panel.validate();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        });

        hidePlaceholderAccountCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                combo.setHidePlaceholder(hidePlaceholderAccountCheckBox.isSelected());
                try {
                    Account account = combo.getSelectedAccount();
                    if (account == null) {
                        return;
                    }

                    updateSubAccountBox();

                    chartPanel.setChart(createVerticalXYBarChart(account));
                    panel.validate();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        });
        refreshButton.addActionListener(listener);

        updateSubAccountBox();

        return panel;
    }

    private void updateSubAccountBox() {
        Account a = combo.getSelectedAccount();
        if (a == null) {
            return;
        }
        if (a.getChildCount() == 0) {
            subAccountCheckBox.setEnabled(false);
        } else {
            subAccountCheckBox.setEnabled(true);
        }
    }

    private JFreeChart createVerticalXYBarChart(Account a) {
        DateFormat df = new SimpleDateFormat("MM/yy");
        TimeSeriesCollection data = createTimeSeriesCollection(a);

        DateAxis dateAxis = new DateAxis(rb.getString("Column.Date"));
        dateAxis.setTickUnit(new DateTickUnit(DateTickUnitType.MONTH, 1, df));
        dateAxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);

        // if (a.getTransactionCount() > 0) {
        Date start = DateUtils.getFirstDayOfTheMonth(startDateField.getDate());
        Date end = DateUtils.getLastDayOfTheMonth(endDateField.getDate());
        dateAxis.setRange(start, end);
        // }

        NumberAxis valueAxis = new NumberAxis(rb.getString("Column.Balance"));
        StandardXYToolTipGenerator tooltipGenerator = new StandardXYToolTipGenerator("{1}, {2}", df, NumberFormat.getNumberInstance());
        XYBarRenderer renderer = new XYBarRenderer(0.2);
        renderer.setBaseToolTipGenerator(tooltipGenerator);

        XYPlot plot = new XYPlot(data, dateAxis, valueAxis, renderer);
        String title = rb.getString("Title.AccountBalance") + " - " + a.getPathName();

        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(null);

        return chart;
    }

    private TimeSeriesCollection createTimeSeriesCollection(final Account account) {
        List<Date> dates = Collections.emptyList();

        if (subAccountCheckBox.isSelected()) {
            // Getting the dates to calculate
            Date start = DateUtils.getFirstDayOfTheMonth(startDateField.getDate());
            Date stop = DateUtils.getLastDayOfTheMonth(endDateField.getDate());
            dates = DateUtils.getLastDayOfTheMonths(start, stop);
            TimeSeries t = new TimeSeries(rb.getString("Column.Month"), rb.getString("Column.Month"), rb.getString("Column.Balance"));

            // For every month, calculate the total amount
            for (Date date : dates) {
                Date d = DateUtils.getLastDayOfTheMonth(date);
                Date s = DateUtils.getFirstDayOfTheMonth(date);

                // Get the total amount for the account and every sub accounts
                // for the specified date
                BigDecimal bd_TotalAmount = calculateTotal(s, d, account, account.getCurrencyNode());

                // Include it in the graph
                t.add(new Month(date), bd_TotalAmount);
            }

            return new TimeSeriesCollection(t);
        }

        int count = account.getTransactionCount();

        if (count > 0) {
            Date start = account.getTransactionAt(0).getDate();
            Date stop = account.getTransactionAt(count - 1).getDate();
            dates = DateUtils.getLastDayOfTheMonths(start, stop);
        }

        TimeSeries t = new TimeSeries(rb.getString("Column.Month"), rb.getString("Column.Month"), rb.getString("Column.Balance"));

        AccountType type = account.getAccountType();

        for (Date aList : dates) {
            // get balance for the whole month
            Date d = DateUtils.getLastDayOfTheMonth(aList);
            Date s = DateUtils.getFirstDayOfTheMonth(aList);

            BigDecimal balance = AccountBalanceDisplayManager.convertToSelectedBalanceMode(type, account.getBalance(s, d));
            t.add(new Month(aList), balance);
        }

        return new TimeSeriesCollection(t);
    }

    private static BigDecimal calculateTotal(final Date start, final Date end, final Account account, final CurrencyNode baseCurrency) {
        BigDecimal amount;
        AccountType type = account.getAccountType();

        // get the amount for the account                
        amount = AccountBalanceDisplayManager.convertToSelectedBalanceMode(type, account.getBalance(start, end, baseCurrency));

        // add the amount of every sub accounts
        for (Account child : account.getChildren()) {
            amount = amount.add(calculateTotal(start, end, child, baseCurrency));
        }
        return amount;
    }
}
