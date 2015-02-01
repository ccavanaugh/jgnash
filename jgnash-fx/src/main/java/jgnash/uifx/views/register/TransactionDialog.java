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
package jgnash.uifx.views.register;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.uifx.MainApplication;
import jgnash.uifx.util.StageUtils;
import jgnash.util.ResourceUtils;

/**
 * A Dialog for creating and editing new transactions
 *
 * @author Craig Cavanaugh
 */
public class TransactionDialog extends Stage {

    @FXML
    private TabPane tabPane;

    @FXML
    private ResourceBundle resources;

    private final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private Optional<Transaction> transactionOptional = Optional.empty();

    private Tab creditTab;
    private Tab debitTab;

    private TransactionDialog() {
        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TransactionDialog.fxml"), ResourceUtils.getBundle());
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
        }

        initOwner(MainApplication.getPrimaryStage());
        initStyle(StageStyle.DECORATED);
        initModality(Modality.APPLICATION_MODAL);
        setTitle(ResourceUtils.getBundle().getString("Title.NewTrans"));

        StageUtils.addBoundsListener(this, TransactionDialog.class);
    }

    private ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    @FXML
    private void initialize() {
        getAccountProperty().addListener((observable, oldValue, newValue) -> {
            buildTabs();
        });
    }

    private Tab buildTab(final String tabName, final SlipType slipType) {

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("BankSlip.fxml"), resources);
            final Pane pane = fxmlLoader.load();

            final SlipController slipController = fxmlLoader.getController();

            // Override the default event handler for the enter and cancel buttons
            slipController.enterButton.setOnAction(event -> handleEnterAction(slipController));
            slipController.cancelButton.setOnAction(event -> tabPane.getScene().getWindow().hide());

            slipController.setSlipType(slipType);
            slipController.getAccountProperty().bind(getAccountProperty());

            final Tab tab = new Tab(tabName);
            tab.setContent(pane);
            tab.setUserData(slipController);

            return tab;
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
        return new Tab();
    }

    private void buildTabs() {
        final AccountType accountType = getAccountProperty().get().getAccountType();
        final String[] tabNames = RegisterFactory.getCreditDebitTabNames(accountType);

        creditTab = buildTab(tabNames[0], SlipType.INCREASE);
        debitTab = buildTab(tabNames[1], SlipType.DECREASE);

        tabPane.getTabs().addAll(creditTab, debitTab);

        if (accountType == AccountType.CHECKING || accountType == AccountType.CREDIT) {
            tabPane.getSelectionModel().select(debitTab);
        } else if (accountType.getAccountGroup() == AccountGroup.INCOME) {
            tabPane.getSelectionModel().select(debitTab);
        }
    }

    private void handleEnterAction(final SlipController controller) {
        if (controller.validateForm()) {
            transactionOptional = Optional.ofNullable(controller.buildTransaction());
            tabPane.getScene().getWindow().hide();
        }
    }

    private Optional<Transaction> getTransactionOptional() {
        return transactionOptional;
    }

    private void setTransaction(final Transaction transaction) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        if (!engine.isStored(transaction)) { // must not be a persisted transaction
            if (transaction.getAmount(accountProperty.get()).signum() >= 0) {
                tabPane.getSelectionModel().select(creditTab);
                ((SlipController)creditTab.getUserData()).modifyTransaction(transaction);
            } else {
                tabPane.getSelectionModel().select(debitTab);
                ((SlipController)debitTab.getUserData()).modifyTransaction(transaction);
            }
        }
    }

    public static Optional<Transaction> showAndWait(final Account account, final Transaction transaction) {
        TransactionDialog transactionDialog = new TransactionDialog();
        transactionDialog.getAccountProperty().setValue(account);
        transactionDialog.setTransaction(transaction);

        // Lock the height of the dialog
        transactionDialog.setMinHeight(transactionDialog.getHeight());
        transactionDialog.setMaxHeight(transactionDialog.getHeight());

        transactionDialog.showAndWait();

        return transactionDialog.getTransactionOptional();
    }
}
