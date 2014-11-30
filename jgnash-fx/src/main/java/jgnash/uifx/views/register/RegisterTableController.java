/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.uifx.views.register;

import java.math.BigDecimal;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import jgnash.engine.Account;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.text.CommodityFormat;
import jgnash.util.DateUtils;
import jgnash.util.EncodeDecode;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;

/**
 * Register Table with stats controller
 *
 * @author Craig Cavanaugh
 */
public class RegisterTableController implements Initializable {

    //private static final String PREF_NODE_REG_POS = "/jgnash/uifx/views/register/positions";
    private static final String PREF_NODE_REG_WIDTH = "/jgnash/uifx/views/register/widths";

    private static final String PREF_NODE_REG_VIS = "/jgnash/uifx/views/register/visibility";

    private final AccountPropertyWrapper accountPropertyWrapper = new AccountPropertyWrapper();

    /**
     * Limits number of processed resize events ensuring the most recent is executed
     */
    private ThreadPoolExecutor updateColumnSizeExecutor;

    /**
     * Limits number of processed visibility change events ensuring the most recent is executed
     */
    private ThreadPoolExecutor updateColumnVisibilityExecutor;

    private ResourceBundle resources;

    @FXML
    private TableView<Transaction> tableView;

    @FXML
    private Label reconciledBalanceLabel;

    @FXML
    private Label balanceLabel;

    @FXML
    private Label accountNameLabel;

    /**
     * Active account for the pane
     */
    private ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private ColumnVisibilityListener visibilityListener = new ColumnVisibilityListener();

    private ColumnWidthListener widthListener = new ColumnWidthListener();

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    AccountPropertyWrapper getAccountPropertyWrapper() {
        return accountPropertyWrapper;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.resources = resources;

        /* At least 2 updates need to be allowed.  The update in process and any potential updates requested
         * that occur when an update is already in process.  Limited to 1 thread
         *
         * Excess execution requests will be silently discarded
         */
        updateColumnSizeExecutor = new ThreadPoolExecutor(0, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
        updateColumnSizeExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        updateColumnVisibilityExecutor = new ThreadPoolExecutor(0, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
        updateColumnVisibilityExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        tableView.setTableMenuButtonVisible(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Bind the account property
        getAccountPropertyWrapper().getAccountProperty().bind(accountProperty);

        // Bind label test to the account property wrapper
        accountNameLabel.textProperty().bind(getAccountPropertyWrapper().getAccountNameProperty());
        balanceLabel.textProperty().bind(getAccountPropertyWrapper().getAccountBalanceProperty());
        reconciledBalanceLabel.textProperty().bind(getAccountPropertyWrapper().getReconciledAmountProperty());

        buildTable();

        // Load the table on change
        getAccountProperty().addListener((observable, oldValue, newValue) -> loadTable());
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        final TableColumn<Transaction, Date> dateColumn = new TableColumn<>(resources.getString("Column.Date"));
        dateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDate()));
        dateColumn.setCellFactory(cell -> new TransactionDateTableCell());
        dateColumn.setMinWidth(50);

        final TableColumn<Transaction, String> numberColumn = new TableColumn<>(resources.getString("Column.Num"));
        numberColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getNumber()));
        numberColumn.setCellFactory(cell -> new TransactionStringTableCell());
        numberColumn.setMinWidth(50);

        final TableColumn<Transaction, String> payeeColumn = new TableColumn<>(resources.getString("Column.Payee"));
        payeeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getPayee()));
        payeeColumn.setCellFactory(cell -> new TransactionStringTableCell());
        payeeColumn.setMinWidth(75);

        final TableColumn<Transaction, String> memoColumn = new TableColumn<>(resources.getString("Column.Memo"));
        memoColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getMemo()));
        memoColumn.setCellFactory(cell -> new TransactionStringTableCell());
        memoColumn.setMinWidth(75);

        final TableColumn<Transaction, String> accountColumn = new TableColumn<>(resources.getString("Column.Account"));
        accountColumn.setCellValueFactory(param -> new AccountNameWrapper(param.getValue()));
        accountColumn.setCellFactory(cell -> new TransactionStringTableCell());
        accountColumn.setMinWidth(75);

        final TableColumn<Transaction, String> reconciledColumn = new TableColumn<>(resources.getString("Column.Clr"));
        reconciledColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getReconciled(accountProperty.getValue()).toString()));
        reconciledColumn.setCellFactory(cell -> new TransactionStringTableCell());
        reconciledColumn.setMinWidth(35);

        final TableColumn<Transaction, BigDecimal> increaseColumn = new TableColumn<>(resources.getString("Column.Increase"));
        increaseColumn.setCellValueFactory(param -> new IncreaseAmountProperty(param.getValue().getAmount(getAccountProperty().getValue())));
        increaseColumn.setCellFactory(cell -> new TransactionCommodityFormatTableCell(CommodityFormat.getShortNumberFormat(accountProperty.get().getCurrencyNode())));
        increaseColumn.setMinWidth(60);

        final TableColumn<Transaction, BigDecimal> decreaseColumn = new TableColumn<>(resources.getString("Column.Decrease"));
        decreaseColumn.setCellValueFactory(param -> new DecreaseAmountProperty(param.getValue().getAmount(getAccountProperty().getValue())));
        decreaseColumn.setCellFactory(cell -> new TransactionCommodityFormatTableCell(CommodityFormat.getShortNumberFormat(accountProperty.get().getCurrencyNode())));
        decreaseColumn.setMinWidth(60);

        final TableColumn<Transaction, BigDecimal> balanceColumn = new TableColumn<>(resources.getString("Column.Balance"));
        balanceColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(accountProperty.get().getBalanceAt(param.getValue())));
        balanceColumn.setCellFactory(cell -> new TransactionCommodityFormatTableCell(CommodityFormat.getFullNumberFormat(accountProperty.get().getCurrencyNode())));
        balanceColumn.setMinWidth(60);

        tableView.getColumns().addAll(dateColumn, numberColumn, payeeColumn, memoColumn, accountColumn, reconciledColumn, increaseColumn, decreaseColumn, balanceColumn);
    }

    private void loadTable() {
        tableView.getItems().clear();

        // TODO, will need to wrap the account transactions for correct running balance calculation when sorted
        if (accountProperty.get() != null) {
            tableView.getItems().addAll(accountProperty.get().getSortedTransactionList());

            // Remove listeners while state is being restored so states are not saved during state changes
            removeColumnListeners();

            restoreColumnVisibility();
            restoreColumnWidths();

            // Install listeners and save column states
            installColumnListeners();
        }
    }

    private void saveColumnWidths() {
        final Account account = accountProperty.getValue();

        if (account != null) {
            final String uuid = account.getUuid();
            final Preferences preferences = Preferences.userRoot().node(PREF_NODE_REG_WIDTH);

            final double[] columnWidths = new double[tableView.getColumns().size()];

            for (int i = 0; i < columnWidths.length; i++) {
                columnWidths[i] = tableView.getColumns().get(i).getWidth();
            }

            preferences.put(uuid, EncodeDecode.encodeDoubleArray(columnWidths));
        }
    }

    private void restoreColumnWidths() {
        final Account account = accountProperty.getValue();

        if (account != null) {
            final String uuid = account.getUuid();
            final Preferences preferences = Preferences.userRoot().node(PREF_NODE_REG_WIDTH);

            final String widths = preferences.get(uuid, null);
            if (widths != null) {
                final double[] columnWidths = EncodeDecode.decodeDoubleArray(widths);

                tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

                if (columnWidths.length == tableView.getColumns().size()) {
                    for (int i = 0; i < columnWidths.length; i++) {
                        tableView.getColumns().get(i).prefWidthProperty().setValue(columnWidths[i]);
                    }
                }

                tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            }
        }
    }

    private void saveColumnVisibility() {
        final Account account = accountProperty.getValue();

        if (account != null) {
            final String uuid = account.getUuid();
            final Preferences preferences = Preferences.userRoot().node(PREF_NODE_REG_VIS);

            final boolean[] columnVisibility = new boolean[tableView.getColumns().size()];

            for (int i = 0; i < columnVisibility.length; i++) {
                columnVisibility[i] = tableView.getColumns().get(i).isVisible();
            }

            preferences.put(uuid, EncodeDecode.encodeBooleanArray(columnVisibility));
        }
    }

    private void restoreColumnVisibility() {
        final Account account = accountProperty.getValue();

        if (account != null) {
            final String uuid = account.getUuid();
            final Preferences preferences = Preferences.userRoot().node(PREF_NODE_REG_VIS);

            final String result = preferences.get(uuid, null);
            if (result != null) {
                final boolean[] columnVisibility = EncodeDecode.decodeBooleanArray(result);

                tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

                if (columnVisibility.length == tableView.getColumns().size()) {
                    for (int i = 0; i < columnVisibility.length; i++) {
                        tableView.getColumns().get(i).visibleProperty().setValue(columnVisibility[i]);
                    }
                }

                tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            }
        }
    }

    private void installColumnListeners() {
        for (final TableColumn<Transaction, ?> tableColumn : tableView.getColumns()) {
            tableColumn.visibleProperty().addListener(visibilityListener);
            tableColumn.widthProperty().addListener(widthListener);
        }
    }

    private void removeColumnListeners() {
        for (final TableColumn<Transaction, ?> tableColumn : tableView.getColumns()) {
            tableColumn.visibleProperty().removeListener(visibilityListener);
            tableColumn.widthProperty().removeListener(widthListener);
        }
    }

    public void packTable() {

        final double[] PREF_COLUMN_WEIGHTS = {0, 0, 20, 20, 20, 0, 0, 0, 0};

        // Check for mismatch of column count and bail if needed
        if (tableView.getColumns().size() != PREF_COLUMN_WEIGHTS.length) {
            return;
        }

        // Create a list of visible columns and column weights
        final List<TableColumn<Transaction, ?>> visibleColumns = new ArrayList<>();
        final List<Double> visibleColumnWeights = new ArrayList<>();

        for (int i = 0; i < tableView.getColumns().size(); i++) {
            if(tableView.getColumns().get(i).isVisible()) {
                visibleColumns.add(tableView.getColumns().get(i));
                visibleColumnWeights.add(PREF_COLUMN_WEIGHTS[i]);
            }
        }

        /*
         * The calculated width of all visible columns, tableView.getWidth() does not allocate for the scroll bar if
         * visible within the TableView
         */
        double tableWidth = 0;

        for (final TableColumn<Transaction, ?> column : visibleColumns) {
            tableWidth += Math.rint(column.getWidth());
        }

        double calculatedWidths[] = new double[visibleColumns.size()];
        double calculatedWidth = 0;

        for (int i = 0; i < calculatedWidths.length; i++) {
            calculatedWidths[i] = getCalculatedColumnWidth(visibleColumns.get(i));
            calculatedWidth += calculatedWidths[i];
        }

        double[] optimizedWidths = calculatedWidths.clone();

        if (calculatedWidth > tableWidth) { // calculated width is wider than the page... need to compress columns
            Double[] columnWeights = visibleColumnWeights.toArray(new Double[visibleColumnWeights.size()]);

            double fixedWidth = 0; // total fixed width of columns

            for (int i = 0; i < optimizedWidths.length; i++) {
                if (columnWeights[i] == 0) {
                    fixedWidth += optimizedWidths[i];
                }
            }

            double diff = tableWidth - fixedWidth; // remaining non fixed width that must be compressed
            double totalWeight = 0; // used to calculate percentages

            for (double columnWeight : columnWeights) {
                totalWeight += columnWeight;
            }

            int i = 0;
            while (i < columnWeights.length) {
                if (columnWeights[i] > 0) {
                    double adj = (columnWeights[i] / totalWeight * diff);

                    if (optimizedWidths[i] > adj) { // only change if necessary
                        optimizedWidths[i] = adj;
                    } else {
                        diff -= optimizedWidths[i]; // available difference is reduced
                        totalWeight -= columnWeights[i]; // adjust the weighting
                        optimizedWidths = calculatedWidths.clone(); // reset widths
                        columnWeights[i] = 0d; // do not try to adjust width again
                        i = -1; // restart the loop from the beginning
                    }
                }
                i++;
            }
        }

        final double[] finalWidths = optimizedWidths.clone();

        Platform.runLater(() -> {
            removeColumnListeners();
            tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

            for (int i = 0; i < finalWidths.length; i++) {
                visibleColumns.get(i).prefWidthProperty().setValue(finalWidths[i]);
            }

            tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            installColumnListeners();

            System.out.println(EncodeDecode.encodeDoubleArray(finalWidths));
        });
    }

    /**
     * Determines the preferred width of the column including contents
     *
     * @param column {@code TableColumn} to measure content
     * @return preferred width
     */
    private double getCalculatedColumnWidth(final TableColumnBase<?, ?> column) {
        DateFormat dateFormatter = DateUtils.getShortDateFormat();

        double maxWidth = column.getMinWidth(); // init with the minimum column width

        for (int i = 0; i < tableView.getItems().size(); i++) {

            final Object object = column.getCellData(i);

            if (object != null) {
                String displayString;

                if (object instanceof Date) {
                    displayString = dateFormatter.format(object);
                } else {
                    displayString = object.toString();
                }

                if (!displayString.isEmpty()) {    // ignore empty strings
                    final Text text = new Text(displayString);
                    new Scene(new Group(text));

                    text.applyCss();
                    maxWidth = Math.max(maxWidth, text.getLayoutBounds().getWidth());
                }
            }
        }

        return Math.ceil(maxWidth + 4);
    }


    private static class IncreaseAmountProperty extends SimpleObjectProperty<BigDecimal> {
        IncreaseAmountProperty(final BigDecimal value) {
            if (value.signum() >= 0) {
                setValue(value);
            } else {
                setValue(null);
            }
        }
    }

    private static class DecreaseAmountProperty extends SimpleObjectProperty<BigDecimal> {
        DecreaseAmountProperty(final BigDecimal value) {
            if (value.signum() < 0) {
                setValue(value.abs());
            } else {
                setValue(null);
            }
        }
    }

    private class AccountNameWrapper extends SimpleStringProperty {

        final String split = resources.getString("Button.Splits");

        AccountNameWrapper(final Transaction t) {
            super();

            if (t instanceof InvestmentTransaction) {
                setValue(((InvestmentTransaction) t).getInvestmentAccount().getName());
            } else {
                int count = t.size();
                if (count > 1) {
                    setValue("[ " + count + " " + split + " ]");
                } else {
                    Account creditAccount = t.getTransactionEntries().get(0).getCreditAccount();
                    if (creditAccount != accountProperty.get()) {
                        setValue(creditAccount.getName());
                    } else {
                        setValue(t.getTransactionEntries().get(0).getDebitAccount().getName());
                    }
                }
            }
        }
    }

    private final class ColumnVisibilityListener implements ChangeListener<Boolean> {
        @Override
        public void changed(final ObservableValue<? extends Boolean> observable, final Boolean oldValue, final Boolean newValue) {
            updateColumnVisibilityExecutor.execute(RegisterTableController.this::saveColumnVisibility);
        }
    }

    private final class ColumnWidthListener implements ChangeListener<Number> {
        @Override
        public void changed(final ObservableValue<? extends Number> observable, final Number oldValue, final Number newValue) {
            updateColumnSizeExecutor.execute(RegisterTableController.this::saveColumnWidths);
        }
    }
}
