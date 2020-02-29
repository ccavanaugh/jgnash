/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
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
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseButton;

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
import jgnash.uifx.control.IntegerTreeTableCell;
import jgnash.uifx.skin.StyleClass;
import jgnash.uifx.util.AccountTypeFilter;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.views.AccountBalanceDisplayManager;
import jgnash.uifx.views.AccountBalanceDisplayMode;
import jgnash.uifx.views.register.RegisterActions;
import jgnash.uifx.views.register.RegisterStage;
import jgnash.util.EncodeDecode;

/**
 * Accounts view controller.
 *
 * @author Craig Cavanaugh
 */
public class AccountsViewController implements MessageListener {

    private final static String COLUMN_VISIBILITY = "ColumnVisibility";

    private final Preferences preferences = Preferences.userNodeForPackage(AccountsViewController.class);

    private final AccountTypeFilter typeFilter = new AccountTypeFilter(preferences);

    private final SimpleObjectProperty<Account> selectedAccount = new SimpleObjectProperty<>();

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

    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<AccountBalanceDisplayMode> accountBalanceDisplayModeChangeListener;

    @FXML
    private void initialize() {
        deleteButton.setDisable(true);
        reconcileButton.setDisable(true);

        initializeTreeTableView();

        JavaFXUtils.runLater(this::loadAccountTree);

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM, MessageChannel.ACCOUNT,
                MessageChannel.TRANSACTION);

        // Register invalidation listeners to force a reload
        typeFilter.addListener(observable -> reload());

        selectedAccount.addListener((observable, oldValue, newValue) -> updateButtonStates());

        modifyButton.disableProperty().bind(selectedAccount.isNull());

        accountBalanceDisplayModeChangeListener = (observable, oldValue, newValue) -> treeTableView.refresh();

        AccountBalanceDisplayManager.accountBalanceDisplayMode()
                .addListener(new WeakChangeListener<>(accountBalanceDisplayModeChangeListener));
    }

    @SuppressWarnings("unchecked")
    private void initializeTreeTableView() {
        treeTableView.setShowRoot(false);   // don't show the root
        treeTableView.setEditable(true);    // required for editable columns
        treeTableView.setTableMenuButtonVisible(true);

        treeTableView.setRowFactory(ttv -> getTreeTableRow());

        // force resize policy for better default appearance
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

        // hide the horizontal scrollbar and prevent ghosting
        treeTableView.getStylesheets().addAll(StyleClass.HIDE_HORIZONTAL_CSS);

        final TreeTableColumn<Account, String> nameColumn = new TreeTableColumn<>(resources.getString("Column.Account"));
        nameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getName()));

        final TreeTableColumn<Account, Integer> entriesColumn = new TreeTableColumn<>(resources.getString("Column.Entries"));
        entriesColumn.setCellValueFactory(param -> new SimpleIntegerProperty(param.getValue().getValue().getTransactionCount()).asObject());

        final TreeTableColumn<Account, BigDecimal> balanceColumn = new TreeTableColumn<>(resources.getString("Column.Balance"));
        balanceColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(AccountBalanceDisplayManager.
                convertToSelectedBalanceMode(param.getValue().getValue().getAccountType(),
                        param.getValue().getValue().getTreeBalance())));
        balanceColumn.setCellFactory(cell -> new AccountCommodityFormatTreeTableCell());

        final TreeTableColumn<Account, BigDecimal> reconciledBalanceColumn = new TreeTableColumn<>(resources.getString("Column.ReconciledBalance"));
        reconciledBalanceColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(AccountBalanceDisplayManager.
                convertToSelectedBalanceMode(param.getValue().getValue().getAccountType(),
                        param.getValue().getValue().getReconciledTreeBalance())));
        reconciledBalanceColumn.setCellFactory(cell -> new AccountCommodityFormatTreeTableCell());

        final TreeTableColumn<Account, String> currencyColumn = new TreeTableColumn<>(resources.getString("Column.Currency"));
        currencyColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getCurrencyNode().getSymbol()));

        final TreeTableColumn<Account, String> typeColumn = new TreeTableColumn<>(resources.getString("Column.Type"));
        typeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getAccountType().toString()));

        final TreeTableColumn<Account, Integer> codeColumn = new TreeTableColumn<>(resources.getString("Column.Code"));
        codeColumn.setEditable(true);
        codeColumn.setCellValueFactory(param -> new SimpleIntegerProperty(param.getValue().getValue().getAccountCode()).asObject());
        codeColumn.setCellFactory(param -> new IntegerTreeTableCell<>());
        codeColumn.setOnEditCommit(event -> updateAccountCode(event.getRowValue().getValue(), event.getNewValue()));

        treeTableView.getColumns().addAll(nameColumn, codeColumn, entriesColumn, balanceColumn,
                reconciledBalanceColumn, currencyColumn, typeColumn);

        restoreColumnVisibility();

        installListeners();
    }

    private void installListeners() {
        treeTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectedAccount.set(newValue.getValue());
            } else {
                selectedAccount.set(null);
            }
        });

        for (final TreeTableColumn<?, ?> treeTableColumn : treeTableView.getColumns()) {
            treeTableColumn.visibleProperty().addListener((observable, oldValue, newValue) -> saveColumnVisibility());
        }
    }

    private TreeTableRow<Account> getTreeTableRow() {
        final TreeTableRow<Account> treeTableRow = new TreeTableRow<>();

        // double click handler
        treeTableRow.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                if (selectedAccount.get() != null && !selectedAccount.get().isPlaceHolder()) {
                    JavaFXUtils.runLater(AccountsViewController.this::handleZoomAccountAction);
                }
            }
        });

        final ContextMenu rowMenu = new ContextMenu();

        final MenuItem newItem = new MenuItem(resources.getString("Menu.New.Name"));
        newItem.setOnAction(event -> handleNewAccountAction());

        final MenuItem modifyItem = new MenuItem(resources.getString("Menu.Modify.Name"));
        modifyItem.setOnAction(event -> handleModifyAccountAction());

        final MenuItem deleteItem = new MenuItem(resources.getString("Menu.Delete.Name"));
        deleteItem.setOnAction(event -> handleDeleteAccountAction());
        deleteItem.disableProperty().bind(deleteButton.disabledProperty());

        final MenuItem visibilityMenuItem = new MenuItem(resources.getString("Menu.Hide.Name"));
        visibilityMenuItem.setOnAction(event -> handleModifyAccountAction());
        visibilityMenuItem.setOnAction(event -> new Thread(() -> {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            if (engine != null) {
                engine.toggleAccountVisibility(selectedAccount.get());
            }
        }).start());

        final MenuItem reconcileItem = new MenuItem(resources.getString("Menu.Reconcile.Name"));
        reconcileItem.setOnAction(event -> handleReconcileAction());

        rowMenu.getItems().addAll(newItem, modifyItem, deleteItem, new SeparatorMenuItem(), visibilityMenuItem,
                new SeparatorMenuItem(), reconcileItem);

        rowMenu.setOnShowing(event -> visibilityMenuItem.setText(selectedAccount.get().isVisible()
                ? resources.getString("Menu.Hide.Name") : resources.getString("Menu.Show.Name")));

        treeTableRow.contextMenuProperty().bind(
                Bindings.when(Bindings.isNotNull(treeTableRow.itemProperty()))
                        .then(rowMenu)
                        .otherwise((ContextMenu) null));

        return treeTableRow;
    }

    private void updateButtonStates() {
        JavaFXUtils.runLater(() -> {
            final Account account = selectedAccount.get();

            if (account != null) {
                final int count = account.getTransactionCount();

                deleteButton.setDisable(count > 0 || account.getChildCount() > 0);
                reconcileButton.setDisable(count <= 0);
                zoomButton.setDisable(account.isPlaceHolder());
            } else {
                deleteButton.setDisable(true);
                reconcileButton.setDisable(true);
                zoomButton.setDisable(true);
            }
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

    @FXML
    private void handleFilterAccountAction() {
        StaticAccountsMethods.showAccountFilterDialog(typeFilter);
    }

    @FXML
    private void handleModifyAccountAction() {
        if (selectedAccount.get() != null) {
            StaticAccountsMethods.showModifyAccountProperties(selectedAccount.get());
        }
    }

    @FXML
    private void handleNewAccountAction() {
        StaticAccountsMethods.showNewAccountPropertiesDialog(selectedAccount.get());
    }

    @FXML
    private void handleDeleteAccountAction() {
        new Thread(() -> {   // push off the platform thread to improve performance
            if (selectedAccount.get() != null) {
                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);

                if (!engine.removeAccount(selectedAccount.get())) {
                    StaticUIMethods.displayError(resources.getString("Message.Error.AccountRemove"));
                }
            }
        }).start();
    }

    @FXML
    private void handleZoomAccountAction() {
        RegisterStage.getRegisterStage(selectedAccount.get()).show();
    }

    private void updateAccountCode(final Account account, final Integer code) {
        new Thread(() -> {  // push the change to an external thread to unload the platform thread
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

            Objects.requireNonNull(engine);

            if (!engine.setAccountCode(account, code)) {
                StaticUIMethods.displayError(resources.getString("Message.Error.AccountUpdate"));
            }
        }).start();
    }

    private void loadAccountTree() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            final RootAccount r = engine.getRootAccount();

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
        JavaFXUtils.runLater(this::loadAccountTree);
    }

    @Override
    public void messagePosted(final Message event) {

        switch (event.getEvent()) {
            case ACCOUNT_ADD:
            case ACCOUNT_MODIFY:
            case ACCOUNT_REMOVE:
            case ACCOUNT_VISIBILITY_CHANGE:
                reload();
                break;
            case TRANSACTION_ADD:
            case TRANSACTION_REMOVE:
                JavaFXUtils.runLater(() -> treeTableView.refresh());
                break;
            case FILE_CLOSING:
                JavaFXUtils.runLater(() -> treeTableView.setRoot(null));
                MessageBus.getInstance().unregisterListener(this, MessageChannel.SYSTEM, MessageChannel.ACCOUNT,
                        MessageChannel.TRANSACTION);
                break;
            default:
                break;
        }
    }

    @FXML
    private void handleReconcileAction() {
        RegisterActions.reconcileAccountAction(selectedAccount.get());
    }

    @FXML
    private void handleExport() {
        StaticAccountsMethods.exportAccountTree();
    }
}
