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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.uifx.StaticUIMethods;
import jgnash.util.NotNull;

/**
 * Investment Register pane controller
 *
 * @author Craig Cavanaugh
 */
public class InvestmentRegisterPaneController extends RegisterPaneController {

    @FXML
    private ComboBox<TransactionPane> actionComboBox;

    @FXML
    private StackPane register;

    @FXML
    private StackPane transactionForms;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        super.initialize(location, resources);

        actionComboBox.setEditable(false);

        // Load the register table
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("InvestmentRegisterTable.fxml"), resources);
            register.getChildren().add(fxmlLoader.load());
            registerTableControllerProperty.setValue(fxmlLoader.getController());
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        getAccountProperty().addListener((observable, oldValue, newValue) -> {
            buildTabs();
        });

        actionComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.getController().clearForm();
            }
            transactionForms.getChildren().clear();
            transactionForms.getChildren().addAll(newValue.getPane());
        });
    }

    private void buildTabs() {
        final String[] actions = new String[]{resources.getString("Transaction.BuyShare"),
                resources.getString("Transaction.SellShare"), resources.getString("Transaction.TransferIn"),
                resources.getString("Transaction.TransferOut"), resources.getString("Transaction.AddShare"),
                resources.getString("Transaction.RemoveShare"), resources.getString("Transaction.ReinvestDiv"),
                resources.getString("Transaction.Dividend"), resources.getString("Transaction.SplitShare"),
                resources.getString("Transaction.MergeShare"), resources.getString("Transaction.ReturnOfCapital")};

        final List<TransactionPane> transactionPanes = new ArrayList<>();

        transactionPanes.add(buildCashTransferTab(actions[2], PanelType.INCREASE));
        transactionPanes.add(buildCashTransferTab(actions[3], PanelType.DECREASE));

        actionComboBox.getItems().addAll(transactionPanes);

        actionComboBox.getSelectionModel().select(0);    // force selection
    }

    private TransactionPane buildCashTransferTab(final String name, final PanelType panelType) {

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("InvestmentTransactionPane.fxml"), resources);
            final Pane pane = fxmlLoader.load();

            final TransactionPaneController transactionPaneController = fxmlLoader.getController();

            transactionPaneController.setPanelType(panelType);
            transactionPaneController.getAccountProperty().bind(getAccountProperty());

            return new TransactionPane(name, transactionPaneController, pane);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void handleEnterAction() {
        actionComboBox.getSelectionModel().selectedItemProperty().get().getController().handleEnterAction();
    }

    @FXML
    private void handleCancelAction() {
        actionComboBox.getSelectionModel().selectedItemProperty().get().getController().handleCancelAction();
    }

    @FXML
    private void handleSecuritiesAction() {
        //TODO: Implement
    }

    @Override
    protected void modifyTransaction(@NotNull final Transaction transaction) {
        if (transaction.areAccountsLocked()) {
            StaticUIMethods.displayError(resources.getString("Message.TransactionModifyLocked"));
            return;
        }

        if (transaction instanceof InvestmentTransaction) {
            switch (transaction.getTransactionType()) {
                default:
                    // TODO: investment forms
                    break;
            }
        } else {
            if (transaction.getAmount(getAccountProperty().get()).signum() >= 0) {
                actionComboBox.getSelectionModel().select(2);  // transferIn
            } else {
                actionComboBox.getSelectionModel().select(3);  // transferOut
            }
            actionComboBox.getSelectionModel().getSelectedItem().getController().modifyTransaction(transaction);
        }
    }

    /**
     * Utility class to hold the controller, form, and form name
     */
    private static class TransactionPane {

        private final String name;
        private final TransactionEntryController controller;
        private final Pane pane;

        private TransactionPane(final String name, final TransactionEntryController controller, final Pane pane) {
            this.name = name;
            this.controller = controller;
            this.pane = pane;
        }

        public TransactionEntryController getController() {
            return controller;
        }

        public Pane getPane() {
            return pane;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
