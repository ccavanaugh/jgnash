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

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.controllers.AbstractAccountTreeController;
import jgnash.uifx.skin.StyleClass;
import jgnash.uifx.util.AccountTypeFilter;
import jgnash.uifx.views.accounts.StaticAccountsMethods;
import jgnash.util.DefaultDaemonThreadFactory;

/**
 * Top level view for account registers
 *
 * @author Craig Cavanaugh
 */
public class RegisterViewController implements Initializable {

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
    private TreeView<Account> treeView;

    @FXML
    private StackPane registerPane;

    final static ExecutorService executorService = Executors.newSingleThreadExecutor(new DefaultDaemonThreadFactory());

    private final Preferences preferences = Preferences.userNodeForPackage(RegisterViewController.class);

    private final AccountTypeFilter typeFilter = new AccountTypeFilter(Preferences.userNodeForPackage(getClass()));

    private RegisterPaneController registerPaneController;

    private ResourceBundle resources;

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

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.resources = resources;

        accountTreeController.initialize(); // must initialize the account controller

        // Filter changes should force a reload of the tree
        typeFilter.addListener(observable -> accountTreeController.reload());

        // Remember the last divider location
        splitPane.getDividers().get(0).positionProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(final ObservableValue<? extends Number> observable, final Number oldValue, final Number newValue) {
                executorService.submit(() -> preferences.putDouble(DIVIDER_POSITION, (Double) newValue));
            }
        });

        // Remember the last account selection
        accountTreeController.getSelectedAccountProperty().addListener(new ChangeListener<Account>() {
            @Override
            public void changed(final ObservableValue<? extends Account> observable, final Account oldValue, final Account newValue) {
                executorService.submit(() -> preferences.put(LAST_ACCOUNT, newValue.getUuid()));
                showAccount();
            }
        });

        // Restore divider location
        splitPane.setDividerPosition(0, preferences.getDouble(DIVIDER_POSITION, DEFAULT_DIVIDER_POSITION));

        restoreLastSelectedAccount();
    }

    private void showAccount() {
        if (registerPaneController != null) {
            registerPaneController.getAccountProperty().unbind();
            registerPane.getChildren().clear();
        }

        final String formResource;

        if (accountTreeController.getSelectedAccountProperty().get().memberOf(AccountGroup.INVEST)) {
            formResource = "InvestmentRegisterPane.fxml";
        } else {
            formResource = "BasicRegisterPane.fxml";
        }

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(formResource), resources);
            registerPane.getChildren().add(fxmlLoader.load());
            registerPaneController = fxmlLoader.getController();

            // Push the account to the controller at the end of the application thread
            Platform.runLater(() -> registerPaneController.getAccountProperty().setValue(accountTreeController.getSelectedAccountProperty().get()));
        } catch (final IOException e) {
            Logger.getLogger(RegisterViewController.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private void restoreLastSelectedAccount() {
        executorService.submit(() -> {
            final String lastAccountUuid = preferences.get(LAST_ACCOUNT, null);
            if (lastAccountUuid != null) {
                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                if (engine != null) {
                    final Account account = engine.getAccountByUuid(lastAccountUuid);
                    if (account != null) {
                        accountTreeController.setSelectedAccount(account);
                    }
                }
            }
        });
    }

    @FXML
    public void handleFilterAccountAction() {
        StaticAccountsMethods.showAccountFilterDialog(typeFilter);
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
