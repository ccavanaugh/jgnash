/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.uifx.util.FXMLUtils;
import jgnash.util.NotNull;
import jgnash.util.Nullable;
import jgnash.util.ResourceUtils;

/**
 * A Dialog for creating and editing new transactions.
 *
 * @author Craig Cavanaugh
 */
public class TransactionDialog extends Stage {

    @FXML
    private TabPane tabPane;

    @FXML
    private ResourceBundle resources;

    private final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private final ObjectProperty<Consumer<Transaction>> transactionConsumer = new SimpleObjectProperty<>();

    private Tab creditTab;
    private Tab debitTab;

    private TransactionDialog() {
        FXMLUtils.loadFXML(this, "TransactionDialog.fxml", ResourceUtils.getBundle());

        setTitle(ResourceUtils.getBundle().getString("Title.NewTrans"));
    }

    private ObjectProperty<Account> accountProperty() {
        return accountProperty;
    }

    private void setTransactionConsumer(final Consumer<Transaction> consumer) {
        transactionConsumer.setValue(consumer);
    }

    @FXML
    private void initialize() {
        accountProperty().addListener((observable, oldValue, newValue) -> buildTabs());
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
            slipController.accountProperty().bind(accountProperty());

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
        final AccountType accountType = accountProperty().get().getAccountType();
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
            transactionConsumer.get().accept(controller.buildTransaction());

            tabPane.getScene().getWindow().hide();
        }
    }

    private void setTransaction(final Transaction transaction) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        if (!engine.isStored(transaction)) { // must not be a persisted transaction
            if (transaction.getAmount(accountProperty.get()).signum() >= 0) {
                tabPane.getSelectionModel().select(creditTab);
                ((SlipController) creditTab.getUserData()).modifyTransaction(transaction);
            } else {
                tabPane.getSelectionModel().select(debitTab);
                ((SlipController) debitTab.getUserData()).modifyTransaction(transaction);
            }
        }
    }

    public static void showAndWait(@NotNull final Account account, @Nullable final Transaction transaction,
                                   final Consumer<Transaction> consumer) {

        final TransactionDialog transactionDialog = new TransactionDialog();
        transactionDialog.accountProperty().setValue(account);
        transactionDialog.setTransactionConsumer(consumer);

        if (transaction != null) {
            transactionDialog.setTransaction(transaction);
        }

        Platform.runLater(() -> {
            transactionDialog.show();

            // Lock the height of the dialog after it has been shown
            Platform.runLater(() -> {
                transactionDialog.setMinHeight(transactionDialog.getHeight());
                //transactionDialog.setMaxHeight(transactionDialog.getHeight());
            });

            // TODO: Silly hack to tickle the layout and force it to expand on Windows OS
            Platform.runLater(() -> transactionDialog.setWidth(transactionDialog.getWidth() + 1));

            //Platform.runLater(() -> StageUtils.addBoundsListener(transactionDialog, TransactionDialog.class));
        });
    }
}
