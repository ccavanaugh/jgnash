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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;

import jgnash.convert.imports.BayesImportClassifier;
import jgnash.convert.imports.GenericImport;
import jgnash.convert.imports.ImportBank;
import jgnash.convert.imports.ImportTransaction;
import jgnash.engine.Account;
import jgnash.resource.font.FontAwesomeLabel;
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

    private static final double[] PREF_COLUMN_WEIGHTS = {0, 0, 0, 50, 50, 0, 0};

    @FXML
    private void initialize() {
        textFlow.getChildren().addAll(new Text(TextResource.getString("ImportTwo.txt")));

        deleteButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());

        tableView.setTableMenuButtonVisible(false);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setEditable(true);

        tableView.getItems().addListener((ListChangeListener<ImportTransaction>) c ->
                valid.setValue(tableView.getItems().size() > 0));

        final TableColumn<ImportTransaction, ImportTransaction.ImportState> stateColumn = new TableColumn<>();
        stateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getState()));

        stateColumn.setCellFactory(new Callback<TableColumn<ImportTransaction, ImportTransaction.ImportState>,
                TableCell<ImportTransaction, ImportTransaction.ImportState>>() {
            @Override
            public TableCell<ImportTransaction, ImportTransaction.ImportState> call(TableColumn<ImportTransaction,
                    ImportTransaction.ImportState> param) {
                TableCell<ImportTransaction, ImportTransaction.ImportState> cell =
                        new TableCell<ImportTransaction, ImportTransaction.ImportState>() {
                            @Override
                            public void updateItem(final ImportTransaction.ImportState item, final boolean empty) {
                                super.updateItem(item, empty);

                                if (empty) {
                                    setText(null);
                                    setGraphic(null);
                                } else if (item != null) {
                                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                                    setText(null);

                                    switch (item) {
                                        case IGNORE:
                                            setGraphic(new StackPane(new FontAwesomeLabel(FontAwesomeIcon.MINUS_CIRCLE)));
                                            break;
                                        case NEW:
                                            setGraphic(new StackPane(new FontAwesomeLabel(FontAwesomeIcon.PLUS_CIRCLE)));
                                            break;
                                        case EQUAL:
                                            setGraphic(new StackPane(new Label("=")));
                                            break;
                                        case NOT_EQUAL:
                                            setGraphic(new StackPane(new FontAwesomeLabel(FontAwesomeIcon.PLUS_CIRCLE)));
                                            break;
                                    }
                                }
                            }
                        };

                cell.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                    if (event.getClickCount() > 1) {                       
                        final ImportTransaction t = tableView.getItems().get(((TableCell)event.getSource()).getTableRow().getIndex());

                        if (t.getState() == ImportTransaction.ImportState.EQUAL) {
                            t.setState(ImportTransaction.ImportState.NOT_EQUAL);
                        } else if (t.getState() == ImportTransaction.ImportState.NOT_EQUAL) {
                            t.setState(ImportTransaction.ImportState.EQUAL);
                        } else if (t.getState() == ImportTransaction.ImportState.NEW) {
                            t.setState(ImportTransaction.ImportState.IGNORE);
                        } else if (t.getState() == ImportTransaction.ImportState.IGNORE) {
                            t.setState(ImportTransaction.ImportState.NEW);
                        }

                        Platform.runLater(tableView::refresh);
                    }
                });
                return cell;
            }
        });

        tableView.getColumns().add(stateColumn);

        final TableColumn<ImportTransaction, LocalDate> dateColumn =
                new TableColumn<>(resources.getString("Column.Date"));
        dateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().datePosted));
        dateColumn.setCellFactory(param -> new ShortDateTableCell<>());
        tableView.getColumns().add(dateColumn);

        final TableColumn<ImportTransaction, String> numberColumn =
                new TableColumn<>(resources.getString("Column.Num"));
        numberColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().checkNumber));
        tableView.getColumns().add(numberColumn);

        final TableColumn<ImportTransaction, String> payeeColumn =
                new TableColumn<>(resources.getString("Column.Payee"));
        payeeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().payee));
        tableView.getColumns().add(payeeColumn);

        final TableColumn<ImportTransaction, String> memoColumn =
                new TableColumn<>(resources.getString("Column.Memo"));
        memoColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().memo));
        tableView.getColumns().add(memoColumn);

        final TableColumn<ImportTransaction, Account> accountColumn =
                new TableColumn<>(resources.getString("Column.Account"));
        accountColumn.setCellValueFactory(param -> {
            if (param.getValue() != null && param.getValue().account != null) {
                return new SimpleObjectProperty<>(param.getValue().account);
            }
            return null;
        });
        accountColumn.setCellFactory(param -> new AccountComboBoxTableCell<>());
        accountColumn.setEditable(true);

        accountColumn.setOnEditCommit(event -> {
            event.getTableView().getItems().get(event.getTablePosition().getRow()).account = event.getNewValue();
            Platform.runLater(tableViewManager::packTable);
        });
        tableView.getColumns().add(accountColumn);

        final TableColumn<ImportTransaction, BigDecimal> amountColumn =
                new TableColumn<>(resources.getString("Column.Amount"));
        amountColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().amount));
        amountColumn.setCellFactory(param -> new BigDecimalTableCell<>(NumberFormat.getNumberInstance()));
        tableView.getColumns().add(amountColumn);

        tableViewManager = new TableViewManager<>(tableView, PREF_NODE);
        tableViewManager.setColumnWeightFactory(column -> PREF_COLUMN_WEIGHTS[column]);

        updateDescriptor();
    }

    @Override
    public void putSettings(final Map<ImportWizard.Settings, Object> map) {
        map.put(ImportWizard.Settings.TRANSACTIONS, new ArrayList<>(tableView.getItems()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void getSettings(final Map<ImportWizard.Settings, Object> map) {

        final ImportBank<ImportTransaction> bank = (ImportBank<ImportTransaction>) map.get(ImportWizard.Settings.BANK);

        if (bank != null) {
            final List<ImportTransaction> list = bank.getTransactions();

            final Account account = (Account) map.get(ImportWizard.Settings.ACCOUNT);

            // set to sane account assuming it's going to be a single entry
            for (final ImportTransaction t : list) {
                t.account = account;
                t.setState(ImportTransaction.ImportState.NEW);  // force reset
            }

            // match up any pre-existing transactions
            GenericImport.matchTransactions(list, account);

            // classify the transactions
            BayesImportClassifier.classifyTransactions(list, account);

            tableView.getItems().setAll(list);
            FXCollections.sort(tableView.getItems());

            tableViewManager.restoreLayout();
        }

        Platform.runLater(tableViewManager::packTable);

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
        tableView.getItems().removeAll(tableView.getSelectionModel().getSelectedItems());
    }
}
