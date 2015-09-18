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
package jgnash.uifx.wizard.imports;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import jgnash.convert.imports.BayesImportClassifier;
import jgnash.convert.imports.GenericImport;
import jgnash.convert.imports.ImportBank;
import jgnash.convert.imports.ImportTransaction;
import jgnash.engine.Account;
import jgnash.uifx.control.AccountComboBoxTableCell;
import jgnash.uifx.control.BigDecimalTableCell;
import jgnash.uifx.control.ShortDateTableCell;
import jgnash.uifx.control.wizard.AbstractWizardPaneController;
import jgnash.uifx.util.TableViewManager;
import jgnash.util.TextResource;

/**
 * Import Wizard, imported transaction wizard
 *
 * @author Craig Cavanaugh
 */
public class ImportPageTwoController extends AbstractWizardPaneController<ImportWizard.Settings> {

    @FXML
    private TextFlow textFlow;

    @FXML
    private TableView<ImportTransaction> tableView;

    @FXML
    private Button deleteButton;

    @FXML
    private ResourceBundle resources;

    private final SimpleBooleanProperty valid = new SimpleBooleanProperty(false);

    private TableViewManager<ImportTransaction> tableViewManager;

    private static final String PREF_NODE = "/jgnash/uifx/wizard/imports";

    private static final double[] PREF_COLUMN_WEIGHTS = {0, 0, 0, 33, 33, 33, 0};

    @FXML
    private void initialize() {
        textFlow.getChildren().addAll(new Text(TextResource.getString("ImportTwo.txt")));

        deleteButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());

        tableView.setTableMenuButtonVisible(false);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setEditable(true);

        tableView.getItems().addListener((ListChangeListener<ImportTransaction>) c ->
                valid.setValue(tableView.getItems().size() > 0));

        final TableColumn<ImportTransaction, String> stateColumn = new TableColumn<>();
        stateColumn.setCellValueFactory(param -> {
            switch (param.getValue().getState()) {
                case NOT_EQUAL:
                    return new SimpleStringProperty("\u2260");
                case EQUAL:
                    return new SimpleStringProperty("=");
                case NEW:
                    return new SimpleStringProperty("+");
                case IGNORE:
                    return new SimpleStringProperty("-");
            }
            return null;
        });
        tableView.getColumns().add(stateColumn);

        final TableColumn<ImportTransaction, LocalDate> dateColumn = new TableColumn<>(resources.getString("Column.Date"));
        dateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().datePosted));
        dateColumn.setCellFactory(param -> new ShortDateTableCell<>());
        tableView.getColumns().add(dateColumn);

        final TableColumn<ImportTransaction, String> numberColumn = new TableColumn<>(resources.getString("Column.Num"));
        numberColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().checkNumber));
        tableView.getColumns().add(numberColumn);

        final TableColumn<ImportTransaction, String> payeeColumn = new TableColumn<>(resources.getString("Column.Payee"));
        payeeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().payee));
        tableView.getColumns().add(payeeColumn);

        final TableColumn<ImportTransaction, String> memoColumn = new TableColumn<>(resources.getString("Column.Memo"));
        memoColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().memo));
        tableView.getColumns().add(memoColumn);

        final TableColumn<ImportTransaction, Account> accountColumn = new TableColumn<>(resources.getString("Column.Account"));
        accountColumn.setCellValueFactory(param -> {
            if (param.getValue() != null && param.getValue().account != null) {
                return new SimpleObjectProperty<>(param.getValue().account);
            }
            return null;
        });
        accountColumn.setCellFactory(param -> new AccountComboBoxTableCell<>());
        accountColumn.setEditable(true);
        accountColumn.setOnEditCommit(event ->
                event.getTableView().getItems().get(event.getTablePosition().getRow()).account = event.getNewValue());

        tableView.getColumns().add(accountColumn);

        final TableColumn<ImportTransaction, BigDecimal> amountColumn = new TableColumn<>(resources.getString("Column.Amount"));
        amountColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().amount));
        amountColumn.setCellFactory(param -> new BigDecimalTableCell<>(NumberFormat.getNumberInstance()));
        tableView.getColumns().add(amountColumn);

        tableViewManager = new TableViewManager<>(tableView, PREF_NODE);
        tableViewManager.setColumnWeightFactory(column-> PREF_COLUMN_WEIGHTS[column]);

        updateDescriptor();
    }

    @Override
    public void putSettings(final Map<ImportWizard.Settings, Object> map) {
       // map.put(ImportWizard.Settings.ACCOUNT, accountComboBox.getValue());
    }

    @Override
    public void getSettings(final Map<ImportWizard.Settings, Object> map) {

        if (tableView.getItems().isEmpty()) {   // don't flush old settings
            ImportBank bank = (ImportBank) map.get(ImportWizard.Settings.BANK);

            if (bank != null) {
                List<ImportTransaction> list = bank.getTransactions();

                Account account = (Account) map.get(ImportWizard.Settings.ACCOUNT);

                // set to sane account assuming it's going to be a single entry
                for (final ImportTransaction t : list) {
                    t.account = account;
                    t.setState(ImportTransaction.ImportState.NEW);
                }

                // match up any pre-existing transactions
                GenericImport.matchTransactions(list, account);

                // classify the transactions
                BayesImportClassifier.classifyTransactions(list, account);

                tableView.getItems().setAll(list);
                FXCollections.sort(tableView.getItems());

                tableViewManager.restoreLayout();
            }
        }

        updateDescriptor();
    }

    @Override
    public boolean isPaneValid() {
        return valid.getValue();
    }

    @Override
    public String toString() {
        return "2. " + resources.getString("Title.ModImportTrans");
    }

    @FXML
    private void handleDeleteAction() {

    }
}
