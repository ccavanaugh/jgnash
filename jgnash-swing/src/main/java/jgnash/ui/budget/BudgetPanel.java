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
package jgnash.ui.budget;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import jgnash.Main;
import jgnash.engine.AccountGroup;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.StoredObject;
import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetPeriod;
import jgnash.engine.budget.BudgetPeriodDescriptor;
import jgnash.engine.budget.BudgetPeriodDescriptorFactory;
import jgnash.engine.budget.BudgetResultsExport;
import jgnash.engine.budget.BudgetResultsModel;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.components.RollOverButton;
import jgnash.ui.util.IconUtils;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jidesoft.swing.JideScrollPane;

/**
 * Panel for displaying a budget
 *
 * @author Craig Cavanaugh
 */
public final class BudgetPanel extends JPanel implements ActionListener, MessageListener {

    private static final String COL_VISIBLE = "colVisible";
    private static final String CURRENT_DIR = "cwd";
    private static final String LAST_BUDGET = "lastBudget";
    private static final String ROW_VISIBLE = "rowVisible";
    private final Preferences preferences = Preferences.userNodeForPackage(BudgetPanel.class);

    private static final int COMBO_BOX_WIDTH = 180;
    private Budget activeBudget;
    private BudgetOverviewPanel overviewPanel;
    private int budgetYear;
    private BudgetComboBox budgetCombo;
    private List<BudgetPeriodPanel> panels = new ArrayList<>();
    private JButton budgetManagerButton;
    private JButton budgetPropertiesButton;
    private JButton budgetExportButton;
    private Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
    private ExpandingBudgetTableModel tableModel;
    private JideScrollPane scrollPane;
    private transient AccountRowHeaderResizeHandler rowHeaderResizeHandler;
    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private static final Logger logger = Logger.getLogger(BudgetPanel.class.getName());
    private JCheckBox summaryRowVisibleCheckBox;
    private JCheckBox summaryColVisibleCheckBox;
    private BudgetResultsModel resultsModel;

    public BudgetPanel() {
        if (Main.enableVerboseLogging()) {
            logger.setLevel(Level.ALL);
        } else {
            logger.setLevel(Level.OFF);
        }

        budgetYear = LocalDate.now().getYear();

        layoutMainPanel();
    }

    private void initComponents() {

        initBudgetCombo();

        ResourceBundle rb = ResourceUtils.getBundle();

        budgetExportButton = new RollOverButton(rb.getString("Button.ExportSpreadsheet"),
                IconUtils.getIcon("/jgnash/resource/x-office-spreadsheet.png"));
        budgetExportButton.addActionListener(this);

        budgetManagerButton = new RollOverButton(rb.getString("Button.BudgetMgr"),
                IconUtils.getIcon("/jgnash/resource/document-new.png"));
        budgetManagerButton.setToolTipText(rb.getString("ToolTip.BudgetMgr"));
        budgetManagerButton.addActionListener(this);

        budgetPropertiesButton = new RollOverButton(rb.getString("Button.Properties"),
                IconUtils.getIcon("/jgnash/resource/document-properties.png"));
        budgetPropertiesButton.addActionListener(this);

        summaryRowVisibleCheckBox = new JCheckBox(rb.getString("Button.SumRowVis"));
        summaryRowVisibleCheckBox.setSelected(preferences.getBoolean(ROW_VISIBLE, true));
        summaryRowVisibleCheckBox.setFocusPainted(false);

        summaryColVisibleCheckBox = new JCheckBox(rb.getString("Button.SumColVis"));
        summaryColVisibleCheckBox.setSelected(preferences.getBoolean(COL_VISIBLE, true));
        summaryColVisibleCheckBox.setFocusPainted(false);

        summaryColVisibleCheckBox.addActionListener(this);
        summaryRowVisibleCheckBox.addActionListener(this);

        updateControlsState();
    }

    void setBudgetYear(final int year) {
        budgetYear = year;

        if (activeBudget != null) {
            activeBudget.setWorkingYear(budgetYear);
        }

        refreshDisplay();
    }

    private static JPanel getBudgetPanel(final List<BudgetPeriodPanel> periodPanels) {
        FormLayout layout = new FormLayout("d", "d");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.add(periodPanels.get(0), CC.xy(1, 1));

        for (int i = 1; i < periodPanels.size(); i++) {
            builder.appendColumn("d");

            builder.add(periodPanels.get(i), CC.xy(i + 1, 1));
        }

        return builder.getPanel();
    }

    private void initBudgetCombo() {
        budgetCombo = new BudgetComboBox();

        SwingWorker<StoredObject, Void> worker = new SwingWorker<StoredObject, Void>() {

            @Override
            protected StoredObject doInBackground() throws Exception {
                Preferences preferences = Preferences.userNodeForPackage(BudgetPanel.class);
                String lastBudgetUUID = preferences.get(LAST_BUDGET, null);

                StoredObject o = null;

                if (lastBudgetUUID != null) {
                    o = engine.getBudgetByUuid(lastBudgetUUID);
                }

                return o;
            }

            @Override
            protected void done() {
                try {
                    StoredObject o = get();

                    if (o != null && o instanceof Budget) {
                        budgetCombo.setSelectedBudget((Budget) o);
                        activeBudget = (Budget) o;
                    }

                    if (activeBudget == null) {
                        List<Budget> budgets = engine.getBudgetList();

                        if (!budgets.isEmpty()) {
                            budgetCombo.setSelectedBudget(budgets.get(0));
                            activeBudget = budgets.get(0);
                        }
                    }

                    // the combo takes the full toolbar space unless limited
                    budgetCombo
                            .setMaximumSize(new Dimension(COMBO_BOX_WIDTH, budgetCombo.getPreferredSize().height * 3));

                    budgetCombo.addActionListener(e -> {
                        if (activeBudget != budgetCombo.getSelectedBudget()) {
                            refreshDisplay();
                        }
                    });

                } catch (final InterruptedException | ExecutionException e) {
                    logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }
        };

        worker.execute();
    }

    private void refreshDisplay() {
        logger.entering(BudgetPanel.class.getName(), "refreshDisplay");

        SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {

            @Override
            protected Integer doInBackground() throws Exception {
                return engine.getBudgetList().size(); // could take awhile if the engine is busy
            }

            @Override
            protected void done() {

                try {
                    int size = get();

                    removeBudgetPane();

                    if (size > 0) { // don't even try if a budget does not exist
                        showBudgetPane();
                    }
                } catch (final InterruptedException | ExecutionException e) {
                    logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }
        };

        submit(worker);
    }

    private void updateControlsState() {
        Runnable r = () -> {
            final int budgetCount = engine.getBudgetList().size();

            EventQueue.invokeLater(() -> {
                if (budgetCount > 0) {
                    budgetPropertiesButton.setEnabled(true);

                    // export cannot handle and daily export
                    budgetExportButton.setEnabled(activeBudget.getBudgetPeriod() != BudgetPeriod.DAILY);
                } else {
                    budgetPropertiesButton.setEnabled(false);
                    budgetExportButton.setEnabled(false);
                }
            });
        };

        submit(r);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("fill:p:g, 5dlu", "d, $rg, d, f:p:g, 5dlu");

        PanelBuilder builder = new PanelBuilder(layout, this);

        CellConstraints cc = new CellConstraints();

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        toolBar.add(budgetCombo);
        toolBar.add(budgetPropertiesButton);
        toolBar.addSeparator();
        toolBar.add(budgetManagerButton);
        toolBar.addSeparator();
        toolBar.add(summaryRowVisibleCheckBox);
        toolBar.add(summaryColVisibleCheckBox);
        toolBar.addSeparator();
        toolBar.add(budgetExportButton);

        builder.add(toolBar, cc.xyw(1, 1, 2));

        scrollPane = new JideScrollPane();
        scrollPane.setPreferredSize(new Dimension(1, 1)); // force it something small so it will resize correctly
        scrollPane.setColumnHeadersHeightUnified(true);

        rowHeaderResizeHandler = new AccountRowHeaderResizeHandler(scrollPane);

        overviewPanel = new BudgetOverviewPanel(this);

        add(overviewPanel, cc.xyw(1, 3, 2));
        add(scrollPane, cc.xy(1, 4));

        // listen for budget events
        MessageBus.getInstance().registerListener(this, MessageChannel.BUDGET);
        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM);

        if (budgetCombo.getSelectedBudget() != null) {
            showBudgetPane();
        }
    }

    private void showBudgetPane() {

        logger.entering(BudgetPanel.class.getName(), "showBudgetPane");

        // unregister the listener so we don't leak by listening to stale models
        if (tableModel != null) {
            tableModel.removeMessageListener(this);
        }

        activeBudget = budgetCombo.getSelectedBudget();

        if (activeBudget != null) {

            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            final CurrencyNode baseCurrency = engine.getDefaultCurrency();

            resultsModel = new BudgetResultsModel(activeBudget, budgetYear, baseCurrency);

            tableModel = new ExpandingBudgetTableModel(resultsModel);

            // register the listener
            tableModel.addMessageListener(this);

            activeBudget.setWorkingYear(budgetYear);

            preferences.put(LAST_BUDGET, activeBudget.getUuid());

            List<BudgetPeriodPanel> newPanels = buildPeriodPanels();

            JPanel budgetPanel = getBudgetPanel(newPanels);
            AccountRowHeaderPanel accountPanel = new AccountRowHeaderPanel(activeBudget, tableModel);
            BudgetColumnHeader header = new BudgetColumnHeader(newPanels);

            panels = newPanels;

            for (BudgetPeriodPanel periodPanel : panels) {
                periodPanel.setRowHeight(accountPanel.getRowHeight());
            }

            scrollPane.setViewportView(budgetPanel);
            scrollPane.setRowHeaderView(accountPanel);
            scrollPane.setColumnHeaderView(header);
            scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, accountPanel.getTableHeader());

            if (summaryRowVisibleCheckBox.isSelected()) {
                addSummaryRows();
            }

            if (activeBudget.getBudgetPeriod() != BudgetPeriod.YEARLY && summaryColVisibleCheckBox.isSelected()) { // summary is redundant for a yearly view
                addSummaryColumn();

                if (summaryRowVisibleCheckBox.isSelected()) {
                    addSummaryCorner();
                }
            }

            rowHeaderResizeHandler.attachListeners();

            showCurrentPeriod();

            overviewPanel.updateSparkLines();
        }

        logger.exiting(BudgetPanel.class.getName(), "showBudgetPane");
    }

    private void removeBudgetPane() {
        logger.entering(BudgetPanel.class.getName(), "removeBudgetPane");

        rowHeaderResizeHandler.detachListeners();

        scrollPane.setRowHeaderView(null);
        scrollPane.setViewportView(null);
        scrollPane.setColumnHeaderView(null);
        scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, null);

        removeSummaryColumn();
        removeSummaryRows();

        logger.exiting(BudgetPanel.class.getName(), "removeBudgetPane");
    }

    private void addSummaryColumn() {
        int rowHeight = ((AccountRowHeaderPanel) scrollPane.getRowHeader().getView()).getRowHeight();

        AccountRowFooterPanel rowFooter = new AccountRowFooterPanel(tableModel);
        rowFooter.setRowHeight(rowHeight);

        scrollPane.setRowFooterView(rowFooter);
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, rowFooter.getTableHeader());
    }

    private void removeSummaryColumn() {
        if (scrollPane.getRowFooter() != null) {
            if (scrollPane.getRowFooter().getView() != null) {
                ((AccountRowFooterPanel) scrollPane.getRowFooter().getView()).unregisterListeners();
            }

            scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, null);
            scrollPane.setCorner(JScrollPane.LOWER_RIGHT_CORNER, null);
            scrollPane.setRowFooterView(null);
        }
    }

    private void addSummaryRows() {
        BudgetColumnFooter footer = new BudgetColumnFooter(panels);
        scrollPane.setColumnFooterView(footer);

        JComponent corner = ((AccountRowHeaderPanel) scrollPane.getRowHeader().getView()).getFooter();

        scrollPane.setCorner(JScrollPane.LOWER_LEFT_CORNER, corner);
    }

    private void removeSummaryRows() {
        scrollPane.setColumnFooterView(null);
        scrollPane.setCorner(JScrollPane.LOWER_LEFT_CORNER, null);
    }

    private void addSummaryCorner() {
        JComponent footer = ((AccountRowFooterPanel) scrollPane.getRowFooter().getView()).getFooter();
        scrollPane.setCorner(JScrollPane.LOWER_RIGHT_CORNER, footer);
    }

    private void removeSummaryCorner() {
        scrollPane.setCorner(JScrollPane.LOWER_RIGHT_CORNER, null);
    }

    private void updateSummaryColumnVisibility() {

        Runnable runnable = () -> {
            preferences.putBoolean(COL_VISIBLE, summaryColVisibleCheckBox.isSelected());

            EventQueue.invokeLater(() -> {
                if (summaryColVisibleCheckBox.isSelected()) {
                    if (activeBudget.getBudgetPeriod() != BudgetPeriod.YEARLY) { // summary is redundant for a yearly view
                        addSummaryColumn();

                        if (summaryRowVisibleCheckBox.isSelected()) {
                            addSummaryCorner();
                        }
                    }
                } else {
                    removeSummaryColumn();
                    removeSummaryCorner();
                }
            });
        };

        submit(runnable);
    }

    private void updateSummaryRowVisibility() {

        Runnable runnable = () -> {
            preferences.putBoolean(ROW_VISIBLE, summaryRowVisibleCheckBox.isSelected());

            EventQueue.invokeLater(() -> {
                if (summaryRowVisibleCheckBox.isSelected()) {
                    addSummaryRows();

                    if (summaryColVisibleCheckBox.isSelected()
                            && activeBudget.getBudgetPeriod() != BudgetPeriod.YEARLY) {
                        addSummaryCorner();
                    }
                } else {
                    removeSummaryRows();
                    removeSummaryCorner();
                }
            });
        };

        submit(runnable);
    }

    private List<BudgetPeriodPanel> buildPeriodPanels() {
        final List<BudgetPeriodDescriptor> descriptors = BudgetPeriodDescriptorFactory.getDescriptors(budgetYear,
                activeBudget.getBudgetPeriod());

        final List<BudgetPeriodPanel> newPanels = new ArrayList<>();

        for (BudgetPeriodDescriptor descriptor : descriptors) {
            BudgetPeriodPanel panel = new BudgetPeriodPanel(new BudgetPeriodModel(descriptor, tableModel));

            newPanels.add(panel);
        }

        return newPanels;
    }

    /**
     * Makes the current {@code BudgetPeriodPanel} visible
     */
    private void showCurrentPeriod() {

        Runnable r = () -> {

            final LocalDate today = LocalDate.now();

            for (int i = 0; i < panels.size(); i++) {
                if (panels.get(i).isBetween(today)) {
                    final BudgetPeriodPanel panel;

                    if (i > 2) {
                        panel = panels.get(i - 2);
                    } else if (i > 1) {
                        panel = panels.get(i - 1);
                    } else {
                        panel = panels.get(i);
                    }

                    EventQueue.invokeLater(() -> panel.scrollRectToVisible(panel.getBounds()));
                }
            }
        };

        submit(r);
    }

    void submit(final Runnable r) {
        pool.submit(r);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == budgetManagerButton) {
            showManagerDialog();
        } else if (e.getSource() == budgetPropertiesButton) {
            showPropertiesDialog();
        } else if (e.getSource() == summaryColVisibleCheckBox) {
            updateSummaryColumnVisibility();
        } else if (e.getSource() == summaryRowVisibleCheckBox) {
            updateSummaryRowVisibility();
        } else if (e.getSource() == budgetExportButton) {
            exportBudgetAction();
        }
    }

    private void showManagerDialog() {
        Runnable r = BudgetManagerDialog::showDialog;

        submit(r);
    }

    private void showPropertiesDialog() {
        Runnable r = () -> EventQueue.invokeLater(() -> {
            BudgetPropertiesDialog d = new BudgetPropertiesDialog(activeBudget);
            d.setVisible(true);
        });

        submit(r);
    }

    @Override
    public void messagePosted(final Message event) {
        switch (event.getEvent()) {
            case BUDGET_ADD:
                EventQueue.invokeLater(() -> {
                    int budgetCount = engine.getBudgetList().size();

                    if (budgetCount == 1) {
                        showBudgetPane();
                    }

                    updateControlsState();
                });
                break;
            case BUDGET_REMOVE:
                EventQueue.invokeLater(() -> {
                    if (engine.getBudgetList().isEmpty()) {
                        removeBudgetPane();
                        overviewPanel.updateSparkLines();
                    }

                    if (activeBudget.equals(event.getObject(MessageProperty.BUDGET))) {
                        refreshDisplay();
                    }

                    updateControlsState();
                });
                break;
            case BUDGET_UPDATE:
                EventQueue.invokeLater(() -> {
                    if (activeBudget.equals(event.getObject(MessageProperty.BUDGET))) {
                        refreshDisplay();
                        updateControlsState();
                    }
                });

                break;
            case ACCOUNT_ADD:
            case ACCOUNT_REMOVE:
            case ACCOUNT_MODIFY:
            case TRANSACTION_ADD:
            case TRANSACTION_REMOVE:
            case BUDGET_GOAL_UPDATE:
                overviewPanel.updateSparkLines();
                break;
            case FILE_CLOSING:
                MessageBus.getInstance().unregisterListener(this, MessageChannel.BUDGET);
                MessageBus.getInstance().unregisterListener(this, MessageChannel.SYSTEM);
                EventQueue.invokeLater(BudgetPanel.this::removeBudgetPane);
                engine = null;
                break;
            default:
                break;
        }
    }

    List<AccountGroup> getAccountGroups() {
        return tableModel.getAccountGroups();
    }

    Icon getSparkLineIcon(final AccountGroup group) {
        List<BigDecimal> remaining = resultsModel.getDescriptorList().parallelStream().map(descriptor ->
                resultsModel.getResults(descriptor, group).getRemaining()).collect(Collectors.toList());

        return BudgetSparkline.getSparklineImage(remaining);
    }

    private void exportBudgetAction() {
        final Preferences pref = Preferences.userNodeForPackage(BudgetPanel.class);

        final ResourceBundle rb = ResourceUtils.getBundle();

        JFileChooser chooser = new JFileChooser(pref.get(CURRENT_DIR, null));
        chooser.setMultiSelectionEnabled(false);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(rb.getString("Label.SpreadsheetFiles")
                + " (*.xls, *.xlsx)", "xls", "xlsx"));

        if (chooser.showSaveDialog(UIApplication.getFrame()) == JFileChooser.APPROVE_OPTION) {
            pref.put(CURRENT_DIR, chooser.getCurrentDirectory().getAbsolutePath());

            final File file = new File(chooser.getSelectedFile().getAbsolutePath());

            final class Export extends SwingWorker<String, Void> {

                @Override
                protected String doInBackground() throws Exception {
                    UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));

                    return BudgetResultsExport.exportBudgetResultsModel(file, resultsModel);
                }

                @Override
                protected void done() {
                    UIApplication.getFrame().stopWaitMessage();
                    try {
                        String message = get();

                        // display any errors that may have occurred
                        if (message != null) {
                            StaticUIMethods.displayError(message);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                    }
                }
            }

            new Export().execute();
        }
    }
}
