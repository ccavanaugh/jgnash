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
package jgnash.uifx.views.register;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.uifx.control.AbstractAccountTreeController;
import jgnash.uifx.report.ReportActions;
import jgnash.uifx.skin.StyleClass;
import jgnash.uifx.util.AccountTypeFilter;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.views.accounts.StaticAccountsMethods;
import jgnash.util.DefaultDaemonThreadFactory;

/**
 * Top level view for account registers.
 *
 * @author Craig Cavanaugh
 */
public class RegisterViewController {

    private static final String DIVIDER_POSITION = "DividerPosition";

    private static final String LAST_ACCOUNT = "LastAccount";

    private static final double DEFAULT_DIVIDER_POSITION = 0.2;

    @FXML
    public SplitPane splitPane;

    @FXML
    private Button reconcileButton;

    @FXML
    private Button filterButton;

    @FXML
    private Button zoomButton;

    @FXML
    private Button packColumnsButton;

    @FXML
    private TreeView<Account> treeView;

    @FXML
    private StackPane registerPane;

    @FXML
    private ResourceBundle resources;

    private final static ExecutorService executorService =
            Executors.newSingleThreadExecutor(new DefaultDaemonThreadFactory("Register View Controller Executor"));

    private final Preferences preferences = Preferences.userNodeForPackage(RegisterViewController.class);

    private final AccountTypeFilter typeFilter = new AccountTypeFilter(Preferences.userNodeForPackage(getClass()));

    private RegisterPaneController registerPaneController;

    private final AbstractAccountTreeController accountTreeController = new AbstractAccountTreeController() {
        @Override
        protected TreeView<Account> getTreeView() {
            return treeView;
        }

        @Override
        protected boolean isAccountVisible(Account account) {
            return typeFilter.isAccountVisible(account);
        }

        @Override
        protected boolean isAccountSelectable(final Account account) {
            return !account.isPlaceHolder();
        }

        @Override
        public void initialize() {
            super.initialize();

            // Install a cell that disables selection if the account is a placeholder
            getTreeView().setCellFactory(param -> new DisabledTreeCell());
        }
    };

    @FXML
    private void initialize() {
        accountTreeController.initialize(); // must initialize the account controller

        // Filter changes should force a reload of the tree
        typeFilter.addListener(observable -> accountTreeController.reload());

        // Remember the last divider location
        splitPane.getDividers().get(0).positionProperty().addListener((observable, oldValue, newValue)
                -> executorService.submit(() -> preferences.putDouble(DIVIDER_POSITION, (Double) newValue)));

        // Remember the last account selection
        accountTreeController.getSelectedAccountProperty().addListener((observable, oldValue, newValue) -> {
            executorService.submit(() -> preferences.put(LAST_ACCOUNT, newValue.getUuid().toString()));
            showAccount();
        });

        // Restore divider location
        splitPane.setDividerPosition(0, preferences.getDouble(DIVIDER_POSITION, DEFAULT_DIVIDER_POSITION));

        restoreLastSelectedAccount();
    }

    private void showAccount() {
        if (registerPaneController != null) {
            registerPaneController.accountProperty().unbind();
            registerPane.getChildren().clear();
        }

        final Account account = accountTreeController.getSelectedAccountProperty().get();
        final String formResource;

        if (account.isLocked()) {
            if (account.memberOf(AccountGroup.INVEST)) {
                formResource = "LockedInvestmentRegisterPane.fxml";
            } else {
                formResource = "LockedBasicRegisterPane.fxml";
            }
        } else {
            if (account.memberOf(AccountGroup.INVEST)) {
                formResource = "InvestmentRegisterPane.fxml";
            } else if (account.getAccountType() == AccountType.LIABILITY) {
                formResource = "LiabilityRegisterPane.fxml";
            } else {
                formResource = "BasicRegisterPane.fxml";
            }
        }

        registerPaneController = FXMLUtils.loadFXML(scene -> registerPane.getChildren().add(scene), formResource,
                resources);

        // Push the account to the controller at the end of the application thread
        JavaFXUtils.runLater(() -> registerPaneController.accountProperty()
                .set(accountTreeController.getSelectedAccountProperty().get()));
    }

    private void restoreLastSelectedAccount() {
        executorService.submit(() -> {
            final String lastAccountUuid = preferences.get(LAST_ACCOUNT, "");
            if (!lastAccountUuid.isEmpty()) {
                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                if (engine != null) {
                    final Account account = engine.getAccountByUuid(UUID.fromString(lastAccountUuid));
                    if (account != null) {
                        accountTreeController.setSelectedAccount(account);
                    }
                }
            }
        });
    }

    @FXML
    private void handleFilterAccountAction() {
        StaticAccountsMethods.showAccountFilterDialog(typeFilter);
    }

    @FXML
    private void handleZoomAction() {
        if (accountTreeController.getSelectedAccountProperty().get() != null) {
            RegisterStage.getRegisterStage(accountTreeController.getSelectedAccountProperty().get()).show();
        }
    }

    @FXML
    private void handleReconcileAction() {
        RegisterActions.reconcileAccountAction(accountTreeController.getSelectedAccountProperty().get());
    }

    @FXML
    private void handleAccountExport() {
        RegisterTableController registerTableController = registerPaneController.registerTableController.get();

        final Account account = registerPaneController.accountProperty().get();

        if (account != null && account.getTransactionCount() > 1) {
            LocalDate startDate = account.getTransactionAt(0).getLocalDate();
            LocalDate endDate = account.getTransactionAt(account.getTransactionCount() - 1).getLocalDate();

            final List<Transaction> selected = new ArrayList<>(registerTableController.getSelectedTransactions());

            if (selected.size() > 1) {
                Collections.sort(selected);
                startDate = selected.get(0).getLocalDate();
                endDate = selected.get(selected.size() - 1).getLocalDate();
            }

            RegisterActions.exportTransactions(account, startDate, endDate);
        }
    }

    @FXML
    private void handleAccountReport() {
        ReportActions.displayAccountRegisterReport(registerPaneController.accountProperty().get());
    }

    @FXML
    private void handleTableColumnPack() {
        registerPaneController.registerTableController.get().manuallyPackTable();
    }

    private static final class DisabledTreeCell extends TreeCell<Account> {
        @Override
        public void updateItem(final Account account, final boolean empty) {
            super.updateItem(account, empty);   // required

            if (!empty && account != null) {
                setText(account.getName());

                if (account.isPlaceHolder()) {
                    setId(StyleClass.DISABLED_CELL_ID);
                } else {
                    setId(StyleClass.ENABLED_CELL_ID);
                }
            }
        }
    }
}
