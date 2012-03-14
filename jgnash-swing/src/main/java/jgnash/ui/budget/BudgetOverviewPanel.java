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
package jgnash.ui.budget;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jgnash.Main;
import jgnash.engine.AccountGroup;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.util.DateUtils;
import jgnash.util.Resource;

/**
 * Overview panel for the displayed period
 *
 * @author Craig Cavanaugh
 * @version $Id: BudgetOverviewPanel.java 3074 2012-01-07 16:16:04Z ccavanaugh $
 */
class BudgetOverviewPanel extends JPanel implements ChangeListener {

    private JSpinner yearSpinner;

    private SpinnerNumberModel model;

    private BudgetPanel budgetPanel;

    private JPanel sparklinePanel;

    /**
     * Used to limit the number of sparkline updates that occur
     */
    private final ThreadPoolExecutor updateIconExecutor;

    private static final Logger logger = Logger.getLogger(BudgetPanel.class.getName());

    public BudgetOverviewPanel(final BudgetPanel budgetPanel) {
        if (Main.enableVerboseLogging()) {
            logger.setLevel(Level.ALL);
        } else {
            logger.setLevel(Level.OFF);
        }

        this.budgetPanel = budgetPanel;

        layoutMainPanel();

        setupSpinnerModel();

        /* At least 2 updates need to be allowed.  The update in process and any potential updates requested
         * that occur when an update is already in process.  Limited to 1 thread
         *
         * Excess execution requests will be silently discarded 
         */
        updateIconExecutor = new ThreadPoolExecutor(0, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(2));
        updateIconExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    private void initComponents() {

        sparklinePanel = new JPanel();

        FormLayout layout = new FormLayout("d", "d");
        sparklinePanel.setLayout(layout);

        model = new SpinnerNumberModel();

        model.setValue(DateUtils.getCurrentYear());
        model.setStepSize(1);

        yearSpinner = new JSpinner(model);

        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(yearSpinner, "####");
        yearSpinner.setEditor(editor);
        yearSpinner.setEnabled(false);
    }

    private void layoutMainPanel() {
        Resource rb = Resource.get();

        initComponents();

        FormLayout layout = new FormLayout("2dlu, right:d, $lcgap, max(40dlu;d), $lcgap, d", "min");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);
        setLayout(layout);

        setBorder(Borders.EMPTY_BORDER);

        builder.add(new JLabel(rb.getString("Label.Year")), CC.xy(2, 1));
        builder.add(yearSpinner, CC.xy(4, 1));
        builder.add(sparklinePanel, CC.xy(6, 1));
    }

    /**
     * Run the model setup in the background because it can be expensive
     */
    private void setupSpinnerModel() {

        Runnable r = new Runnable() {

            @Override
            public void run() {
                Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                int minYear = DateUtils.getCurrentYear();
                int maxYear = DateUtils.getCurrentYear() + 1;

                for (Transaction transaction : engine.getTransactions()) {

                    int year = DateUtils.getYear(transaction.getDate());

                    minYear = Math.min(minYear, year);
                    maxYear = Math.max(maxYear, year);
                }

                final int _minYear = minYear;
                final int _maxYear = maxYear;

                // changes to the spinner must be pushed to the EDT
                EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        if (model != null) {
                            model.setMinimum(_minYear);
                            model.setMaximum(_maxYear);
                        }

                        yearSpinner.setEnabled(true);

                        // register the listener after the model has been updated, otherwise unnecessary updates will occur
                        yearSpinner.addChangeListener(BudgetOverviewPanel.this);
                    }
                });
            }
        };

        budgetPanel.submit(r);
    }

    @Override
    public void stateChanged(final ChangeEvent e) {
        logger.entering(BudgetOverviewPanel.class.getName(), "stateChanged");

        if (e.getSource() == yearSpinner) {
            int year = (Integer) model.getNumber();

            budgetPanel.setBudgetYear(year);
        }

        logger.exiting(BudgetOverviewPanel.class.getName(), "stateChanged");
    }

    void updateSparkLines() {

        Runnable r = new Runnable() {

            @Override
            public void run() {

                final List<AccountGroup> groups = new ArrayList<>(budgetPanel.getAccountGroups());

                final List<Icon> icons = new ArrayList<>();

                for (AccountGroup group : groups) {
                    icons.add(budgetPanel.getSparkLineIcon(group));
                }

                EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        FormLayout layout = (FormLayout) sparklinePanel.getLayout();

                        // remove all components and columns
                        sparklinePanel.removeAll();

                        int columnCount = layout.getColumnCount();

                        for (int i = columnCount; i >= 1; i--) {
                            layout.removeColumn(i);
                        }

                        // create components and columns and add
                        if (icons.size() > 0) {
                            layout.appendColumn(ColumnSpec.decode("d"));
                            sparklinePanel.add(getLabel(groups.get(0), icons.get(0)), CC.xy(1, 1));

                            for (int i = 1; i < icons.size(); i++) {
                                layout.appendColumn(ColumnSpec.decode("2dlu"));
                                layout.appendColumn(ColumnSpec.decode("d"));

                                sparklinePanel.add(getLabel(groups.get(i), icons.get(i)), CC.xy(i * 2 + 1, 1));
                            }
                        }

                        // force the complete panel to update so icons show
                        invalidate();
                        validate();
                    }
                });
            }
        };

        updateIconExecutor.execute(r);
    }

    private JLabel getLabel(final AccountGroup group, final Icon icon) {
        JLabel label = new JLabel(group.toString(), icon, JLabel.LEFT);
        label.setHorizontalTextPosition(JLabel.LEFT);

        label.setFont(label.getFont().deriveFont(label.getFont().getSize2D() - 2f));
        return label;
    }
}
