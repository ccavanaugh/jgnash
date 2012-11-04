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

import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import jgnash.engine.CurrencyNode;
import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.EngineFactory;
import jgnash.engine.RootAccount;
import jgnash.text.CommodityFormat;
import jgnash.ui.components.AccountListComboBox;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.GenericCloseDialog;
import jgnash.ui.util.CursorUtils;
import jgnash.util.DateUtils;
import jgnash.util.Resource;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.PieSectionEntity;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

/**
 * This displays a dialog that shows a pie chart with account distribution for Income and Expense accounts only.
 * 
 * @author Jeff Prickett
 * @author Craig Cavanaugh
 * @author Chad McHenry
 * @author Dany Veilleux
 * @author Pranay Kumar
 */
public class IncomeExpensePieChart {

    private static final String STARTDATE = "startDate";

    private final Resource rb = Resource.get();

    private final Preferences pref = Preferences.userNodeForPackage(IncomeExpensePieChart.class);

    private AccountListComboBox combo;

    private JCheckBox showEmptyCheck;

    private JCheckBox showPercentCheck;

    private final Cursor ZOOM_IN = CursorUtils.getCursor(CursorUtils.ZOOM_IN);

    private final Cursor ZOOM_OUT = CursorUtils.getCursor(CursorUtils.ZOOM_OUT);

    private Point lastPoint;

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

                IncomeExpensePieChart chart = new IncomeExpensePieChart();

                JPanel p = chart.createPanel();
                GenericCloseDialog d = new GenericCloseDialog(p, rb.getString("Title.AccountBalance"));
                d.pack();
                d.setModal(false);

                d.setVisible(true);
            }
        });
    }

    private JPanel createPanel() {
        EnumSet<AccountType> set = EnumSet.of(AccountType.INCOME, AccountType.EXPENSE);

        JButton refreshButton = new JButton(rb.getString("Button.Refresh"));

        startField = new DatePanel();

        endField = new DatePanel();

        showEmptyCheck = new JCheckBox(rb.getString("Label.ShowEmptyAccounts"));

        showPercentCheck = new JCheckBox(rb.getString("Label.ShowPercentValues"));

        combo = AccountListComboBox.getParentTypeInstance(EngineFactory.getEngine(EngineFactory.DEFAULT).getRootAccount(), set);

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
        builder.append(showEmptyCheck);
        builder.nextLine();

        builder.nextLine();

        builder.append(rb.getString("Label.StartDate"), startField);
        builder.append(rb.getString("Label.EndDate"), endField);
        builder.append(refreshButton);

        builder.append(showPercentCheck);
        builder.nextLine();
        builder.nextLine();

        builder.append(chartPanel, 11);

        JPanel panel = builder.getPanel();

        combo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                setCurrentAccount(combo.getSelectedAccount());
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

        showEmptyCheck.addActionListener(new ActionListener() {

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

        ChartMouseListener mouseListener = new ChartMouseListener() {

            @Override
            public void chartMouseClicked(final ChartMouseEvent event) {
                MouseEvent me = event.getTrigger();
                if (me.getID() == MouseEvent.MOUSE_CLICKED && me.getClickCount() == 1) {
                    try {
                        ChartEntity entity = event.getEntity();
                        // expand sections if interesting, back out if in nothing
                        if (entity instanceof PieSectionEntity) {
                            Account a = (Account) ((PieSectionEntity) entity).getSectionKey();
                            if (a.getChildCount() > 0) {
                                setCurrentAccount(a);
                            }
                        } else if (entity == null) {
                            Account parent = currentAccount;
                            if (parent == null) {
                                return;
                            }
                            parent = parent.getParent();
                            if (parent == null || parent instanceof RootAccount) {
                                return;
                            }
                            setCurrentAccount(parent);
                        }
                    } catch (Exception ex) {
                        System.err.println(ex);
                    }
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent event) {
                setChartCursor(chartPanel, event.getEntity(), event.getTrigger().getPoint());
            }
        };

        chartPanel.addChartMouseListener(mouseListener);

        return panel;
    }

    private void setCurrentAccount(Account a) {
        try {
            if (a == null) {
                return;
            }
            currentAccount = a;

            chartPanel.setChart(createPieChart(a));
            chartPanel.validate();
            // refresh the cursor for changed display
            if (lastPoint != null) {
                setChartCursor(chartPanel, null, lastPoint);
            }
        } catch (Exception e) {
            Logger.getLogger(IncomeExpensePieChart.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private void setChartCursor(final ChartPanel chartPanel, final ChartEntity e, final Point point) {
        lastPoint = point;

        EventQueue.invokeLater(new Runnable() {

            ChartEntity entity = e;

            @Override
            public void run() {
                if (entity == null && point != null) {
                    entity = chartPanel.getEntityForPoint(lastPoint.x, lastPoint.y);
                }
                Account parent = currentAccount;
                if (entity instanceof PieSectionEntity) {
                    // change cursor if section is interesting
                    Account a = (Account) ((PieSectionEntity) entity).getSectionKey();
                    if (a.getChildCount() > 0 && a != parent) {
                        chartPanel.setCursor(ZOOM_IN);
                    } else {
                        chartPanel.setCursor(Cursor.getDefaultCursor());
                    }
                    return;
                } else if (entity == null && parent != null) {
                    parent = parent.getParent();
                    if (parent != null && !(parent instanceof RootAccount)) {
                        chartPanel.setCursor(ZOOM_OUT);
                        return;
                    }
                }
                chartPanel.setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    private JFreeChart createPieChart(Account a) {
        PieDataset data = createPieDataset(a);
        PiePlot plot = new PiePlot(data);

        // rebuilt each time because they're based on the account's commodity
        CurrencyNode defaultCurrency = EngineFactory.getEngine(EngineFactory.DEFAULT).getDefaultCurrency();
        NumberFormat valueFormat = CommodityFormat.getFullNumberFormat(a.getCurrencyNode());
        NumberFormat percentFormat = new DecimalFormat("0.0#%");
        defaultLabels = new StandardPieSectionLabelGenerator("{0} = {1}", valueFormat, percentFormat);
        percentLabels = new StandardPieSectionLabelGenerator("{0} = {1}\n{2}", valueFormat, percentFormat);

        plot.setLabelGenerator(showPercentCheck.isSelected() ? percentLabels : defaultLabels);

        plot.setLabelGap(.02);
        plot.setInteriorGap(.1);

        // if we had to add a section for the account (because it has it's
        // own transactions, not just child accounts), separate it from children.
        if (data.getIndex(a) != -1) {
            plot.setExplodePercent(a, .10);
        }

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

        String subtitle = valueFormat.format(total);
        if (!defaultCurrency.equals(a.getCurrencyNode()))
        {
            BigDecimal totalDefaultCurrency = total.multiply(a.getCurrencyNode().getExchangeRate(defaultCurrency));
            NumberFormat defaultValueFormat = CommodityFormat.getFullNumberFormat(defaultCurrency);
            subtitle += "  -  " + defaultValueFormat.format(totalDefaultCurrency);
        }
        chart.addSubtitle( new TextTitle(subtitle) );
        chart.setBackgroundPaint(null);

        return chart;
    }

    private PieDataset createPieDataset(Account a) {
        DefaultPieDataset returnValue = new DefaultPieDataset();
        if (a != null) {

            BigDecimal total = a.getTreeBalance(startField.getDate(), endField.getDate(), a.getCurrencyNode());

            // abs() on all values won't work if children aren't of uniform sign,
            // then again, this chart is not right to display those trees
            boolean negate = total != null && total.floatValue() < 0;

            // accounts may have balances independent of their children
            BigDecimal value = a.getBalance(startField.getDate(), endField.getDate());

            if (value.compareTo(BigDecimal.ZERO) != 0) {
                returnValue.setValue(a, negate ? value.negate() : value);
            }

            for (Account child : a.getChildren()) {
                value = child.getTreeBalance(startField.getDate(), endField.getDate(), a.getCurrencyNode());

                if (showEmptyCheck.isSelected() || value.compareTo(BigDecimal.ZERO) != 0) {
                    returnValue.setValue(child, negate ? value.negate() : value);
                }
            }
        }
        return returnValue;
    }
}
