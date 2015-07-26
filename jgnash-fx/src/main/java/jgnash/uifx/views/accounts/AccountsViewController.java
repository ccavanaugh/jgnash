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
package jgnash.uifx.views.accounts;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.RootAccount;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.util.AccountTypeFilter;
import jgnash.util.EncodeDecode;

/**
 * Accounts view controller
 *
 * @author Craig Cavanaugh
 */
public class AccountsViewController implements MessageListener {

    private final static String COLUMN_VISIBILITY = "ColumnVisibility";

    private final Preferences preferences = Preferences.userNodeForPackage(AccountsViewController.class);

    private final AccountTypeFilter typeFilter = new AccountTypeFilter(preferences);

    @FXML
    private ResourceBundle resources;

    @FXML
    private TreeTableView<Account> treeTableView;

    @FXML
    private Button newButton;

    @FXML
    private Button modifyButton;

    @FXML
    private Button reconcileButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button filterButton;

    @FXML
    private Button zoomButton;

    @FXML
    private void initialize() {

        modifyButton.setDisable(true);
        deleteButton.setDisable(true);
        reconcileButton.setDisable(true);
        zoomButton.setDisable(true);

        initializeTreeTableView();

        Platform.runLater(this::loadAccountTree);

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM, MessageChannel.ACCOUNT, MessageChannel.TRANSACTION);

        // Register invalidation listeners to force a reload
        typeFilter.addListener(observable -> reload());
    }

    @SuppressWarnings("unchecked")
    private void initializeTreeTableView() {
        treeTableView.setShowRoot(false);   // don't show the root
        treeTableView.setEditable(true);    // required for editable columns
        treeTableView.setTableMenuButtonVisible(true);

        // force resize policy for better default appearance
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

        final TreeTableColumn<Account, String> nameColumn = new TreeTableColumn<>(resources.getString("Column.Account"));
        nameColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getName()));

        final TreeTableColumn<Account, Integer> entriesColumn = new TreeTableColumn<>(resources.getString("Column.Entries"));
        entriesColumn.setCellValueFactory(param -> new SimpleIntegerProperty(param.getValue().getValue().getTransactionCount()).asObject());

        final TreeTableColumn<Account, BigDecimal> balanceColumn = new TreeTableColumn<>(resources.getString("Column.Balance"));
        balanceColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getValue().getTreeBalance()));
        balanceColumn.setCellFactory(cell -> new AccountCommodityFormatTreeTableCell());

        final TreeTableColumn<Account, BigDecimal> reconciledBalanceColumn = new TreeTableColumn<>(resources.getString("Column.ReconciledBalance"));
        reconciledBalanceColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getValue().getReconciledTreeBalance()));
        reconciledBalanceColumn.setCellFactory(cell -> new AccountCommodityFormatTreeTableCell());

        final TreeTableColumn<Account, String> currencyColumn = new TreeTableColumn<>(resources.getString("Column.Currency"));
        currencyColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getCurrencyNode().getSymbol()));

        final TreeTableColumn<Account, String> typeColumn = new TreeTableColumn<>(resources.getString("Column.Type"));
        typeColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getAccountType().toString()));

        final TreeTableColumn<Account, Integer> codeColumn = new TreeTableColumn<>(resources.getString("Column.Code"));
        codeColumn.setEditable(true);
        codeColumn.setCellValueFactory(param -> new SimpleIntegerProperty(param.getValue().getValue().getAccountCode()).asObject());
        codeColumn.setCellFactory(param -> new IntegerEditingTreeTableCell());
        codeColumn.setOnEditCommit(event -> updateAccountCode(event.getRowValue().getValue(), event.getNewValue()));

        treeTableView.getColumns().addAll(nameColumn, codeColumn, entriesColumn, balanceColumn, reconciledBalanceColumn, currencyColumn, typeColumn);

        restoreColumnVisibility();

        installListeners();
    }

    private void installListeners() {
        treeTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateButtonStates(newValue.getValue());
            } else {
                updateButtonStates(null);
            }
        });

        for (final TreeTableColumn<?,?> treeTableColumn : treeTableView.getColumns()) {
            treeTableColumn.visibleProperty().addListener((observable, oldValue, newValue) -> saveColumnVisibility());
        }
    }

    private void updateButtonStates(final Account account) {
        Platform.runLater(() -> {
            if (account != null) {
                final int count = account.getTransactionCount();

                deleteButton.setDisable(count > 0 || account.getChildCount() > 0);
                reconcileButton.setDisable(count <= 0);
            } else {
                deleteButton.setDisable(true);
                reconcileButton.setDisable(true);
            }

            modifyButton.setDisable(account == null);
            zoomButton.setDisable(account == null);
        });
    }

    private void saveColumnVisibility() {
        final boolean[] columnVisibility = new boolean[treeTableView.getColumns().size()];

        for (int i = 0; i < columnVisibility.length; i++) {
            columnVisibility[i] = treeTableView.getColumns().get(i).isVisible();
        }

        preferences.put(COLUMN_VISIBILITY, EncodeDecode.encodeBooleanArray(columnVisibility));
    }

    private void restoreColumnVisibility() {
        final String result = preferences.get(COLUMN_VISIBILITY, null);

        if (result != null) {
            boolean[] columnVisibility = EncodeDecode.decodeBooleanArray(result);

            if (columnVisibility.length == treeTableView.getColumns().size()) {
                for (int i = 0; i < columnVisibility.length; i++) {
                    treeTableView.getColumns().get(i).setVisible(columnVisibility[i]);
                }
            }
        }
    }

    private Optional<Account> getSelectedAccount() {
        final TreeItem<Account> treeItem = treeTableView.getSelectionModel().getSelectedItem();

        if (treeItem != null) {
            return Optional.ofNullable(treeItem.getValue());
        }

        return Optional.empty();
    }

    @FXML
    private void handleFilterAccountAction() {
        StaticAccountsMethods.showAccountFilterDialog(typeFilter);
    }

    @FXML
    private void handleModifyAccountAction() {
        if (getSelectedAccount().isPresent()) {
            StaticAccountsMethods.showModifyAccountProperties(getSelectedAccount().get());
        }
    }

    @FXML
    private void handleNewAccountAction() {
        StaticAccountsMethods.showNewAccountPropertiesDialog();
    }

    @FXML
    private void handleDeleteAccountAction() {
        if (getSelectedAccount().isPresent()) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            if (!engine.removeAccount(getSelectedAccount().get())) {
                StaticUIMethods.displayError(resources.getString("Message.Error.AccountRemove"));
            }
        }
    }

    private void updateAccountCode(final Account account, final Integer code) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            try {
                final Account template = (Account) account.clone();
                template.setAccountCode(code);

                engine.modifyAccount(template, account);
            } catch (final CloneNotSupportedException e) {
                Logger.getLogger(AccountsViewController.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        }
    }

    private void loadAccountTree() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            RootAccount r = engine.getRootAccount();

            final TreeItem<Account> root = new TreeItem<>(r);
            root.setExpanded(true);

            treeTableView.setRoot(root);
            loadChildren(root);
        } else {
            treeTableView.setRoot(null);
        }
    }

    private synchronized void loadChildren(final TreeItem<Account> parentItem) {
        parentItem.getValue().getChildren(Comparators.getAccountByCode()).stream().filter(typeFilter::isAccountVisible).forEach(child -> {
            TreeItem<Account> childItem = new TreeItem<>(child);
            childItem.setExpanded(true);

            parentItem.getChildren().add(childItem);

            if (child.getChildCount() > 0) {
                loadChildren(childItem);
            }
        });
    }

    private synchronized void reload() {
        Platform.runLater(this::loadAccountTree);
    }

    @Override
    public void messagePosted(final Message event) {

        switch (event.getEvent()) {
            case ACCOUNT_ADD:
            case ACCOUNT_MODIFY:
            case ACCOUNT_REMOVE:
                reload();
                break;
            case TRANSACTION_ADD:
            case TRANSACTION_REMOVE:
                treeTableView.refresh();    //TODO implement a better model that listens to account balance changes
                break;
            case FILE_CLOSING:
                Platform.runLater(() -> treeTableView.setRoot(null));
                MessageBus.getInstance().unregisterListener(this, MessageChannel.SYSTEM, MessageChannel.ACCOUNT, MessageChannel.TRANSACTION);
                break;
            default:
                break;
        }
    }

}
