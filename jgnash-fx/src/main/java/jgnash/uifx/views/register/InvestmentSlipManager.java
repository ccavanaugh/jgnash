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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import jgnash.engine.Account;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionType;
import jgnash.uifx.StaticUIMethods;
import jgnash.util.NotNull;
import jgnash.resource.util.ResourceUtils;

/**
 * Manager for investment transaction forms.
 */
class InvestmentSlipManager {
    private final ComboBox<SlipControllerContainer> actionComboBox;

    private final StackPane transactionSlips;

    private final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private final ResourceBundle resources = ResourceUtils.getBundle();

    InvestmentSlipManager(final StackPane transactionSlips, final ComboBox<SlipControllerContainer> actionComboBox) {
        this.transactionSlips = transactionSlips;
        this.actionComboBox = actionComboBox;

        initialize();
    }

    private void initialize() {
        actionComboBox.setEditable(false);

        actionComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.getController().clearForm();
            }
            transactionSlips.getChildren().clear();
            transactionSlips.getChildren().addAll(newValue.getPane());
        });

        accountProperty().addListener((observable, oldValue, newValue) -> buildTabs());
    }

    ObjectProperty<Account> accountProperty() {
        return accountProperty;
    }

    private void buildTabs() {
        final String[] actions = new String[]{resources.getString("Transaction.BuyShare"),
                resources.getString("Transaction.SellShare"), resources.getString("Transaction.TransferIn"),
                resources.getString("Transaction.TransferOut"), resources.getString("Transaction.AddShare"),
                resources.getString("Transaction.RemoveShare"), resources.getString("Transaction.ReinvestDiv"),
                resources.getString("Transaction.Dividend"), resources.getString("Transaction.SplitShare"),
                resources.getString("Transaction.MergeShare"), resources.getString("Transaction.ReturnOfCapital")};

        final List<SlipControllerContainer> transactionPanes = new ArrayList<>();

        transactionPanes.add(buildBuyShareTab(actions[0]));
        transactionPanes.add(buildSellShareTab(actions[1]));
        transactionPanes.add(buildCashTransferTab(actions[2], SlipType.INCREASE));
        transactionPanes.add(buildCashTransferTab(actions[3], SlipType.DECREASE));
        transactionPanes.add(buildAdjustShareTab(actions[4], TransactionType.ADDSHARE));
        transactionPanes.add(buildAdjustShareTab(actions[5], TransactionType.REMOVESHARE));
        transactionPanes.add(buildReinvestDividendTab(actions[6]));
        transactionPanes.add(buildDividendTab(actions[7]));
        transactionPanes.add(buildSplitMergeTab(actions[8], TransactionType.SPLITSHARE));
        transactionPanes.add(buildSplitMergeTab(actions[9], TransactionType.MERGESHARE));
        transactionPanes.add(buildReturnOfCapitalTab(actions[10]));

        actionComboBox.getItems().addAll(transactionPanes);

        actionComboBox.getSelectionModel().select(0);    // force default selection
    }

    void modifyTransaction(@NotNull final Transaction transaction) {
        if (transaction.areAccountsLocked()) {
            StaticUIMethods.displayError(resources.getString("Message.TransactionModifyLocked"));
            return;
        }

        if (transaction instanceof InvestmentTransaction) {
            switch (transaction.getTransactionType()) {
                case BUYSHARE:
                    actionComboBox.getSelectionModel().select(0);
                    break;
                case SELLSHARE:
                    actionComboBox.getSelectionModel().select(1);
                    break;
                case ADDSHARE:
                    actionComboBox.getSelectionModel().select(4);
                    break;
                case REMOVESHARE:
                    actionComboBox.getSelectionModel().select(5);
                    break;
                case REINVESTDIV:
                    actionComboBox.getSelectionModel().select(6);
                    break;
                case DIVIDEND:
                    actionComboBox.getSelectionModel().select(7);
                    break;
                case SPLITSHARE:
                    actionComboBox.getSelectionModel().select(8);
                    break;
                case MERGESHARE:
                    actionComboBox.getSelectionModel().select(9);
                    break;
                case RETURNOFCAPITAL:
                    actionComboBox.getSelectionModel().select(10);
                    break;
                default:
                    break;
            }
        } else {
            if (transaction.getAmount(accountProperty().get()).signum() >= 0) {
                actionComboBox.getSelectionModel().select(2);  // transferIn
            } else {
                actionComboBox.getSelectionModel().select(3);  // transferOut
            }
        }

        actionComboBox.getSelectionModel().getSelectedItem().getController().modifyTransaction(transaction);
    }


    private SlipControllerContainer buildCashTransferTab(final String name, final SlipType slipType) {

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("InvestmentTransactionPane.fxml"), resources);
            final Pane pane = fxmlLoader.load();

            final SlipController slipController = fxmlLoader.getController();

            slipController.setSlipType(slipType);
            slipController.accountProperty().bind(accountProperty());

            return new SlipControllerContainer(name, slipController, pane);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SlipControllerContainer buildAdjustShareTab(final String name, final TransactionType transactionType) {

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AdjustSharesSlip.fxml"), resources);
            final Pane pane = fxmlLoader.load();

            final AdjustSharesSlipController slipController = fxmlLoader.getController();

            slipController.setTransactionType(transactionType);
            slipController.accountProperty().bind(accountProperty());

            return new SlipControllerContainer(name, slipController, pane);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SlipControllerContainer buildBuyShareTab(final String name) {

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("BuyShareSlip.fxml"), resources);
            final Pane pane = fxmlLoader.load();

            final BuyShareSlipController slipController = fxmlLoader.getController();

            slipController.accountProperty().bind(accountProperty());

            return new SlipControllerContainer(name, slipController, pane);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SlipControllerContainer buildSellShareTab(final String name) {

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SellShareSlip.fxml"), resources);
            final Pane pane = fxmlLoader.load();

            final SellShareSlipController slipController = fxmlLoader.getController();

            slipController.accountProperty().bind(accountProperty());

            return new SlipControllerContainer(name, slipController, pane);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SlipControllerContainer buildReinvestDividendTab(final String name) {

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ReinvestDividendSlip.fxml"), resources);
            final Pane pane = fxmlLoader.load();

            final ReinvestDividendSlipController slipController = fxmlLoader.getController();

            slipController.accountProperty().bind(accountProperty());

            return new SlipControllerContainer(name, slipController, pane);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SlipControllerContainer buildDividendTab(final String name) {

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("DividendSlip.fxml"), resources);
            final Pane pane = fxmlLoader.load();

            final DividendSlipController slipController = fxmlLoader.getController();

            slipController.accountProperty().bind(accountProperty());

            return new SlipControllerContainer(name, slipController, pane);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SlipControllerContainer buildSplitMergeTab(final String name, final TransactionType transactionType) {

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SplitMergeSharesSlip.fxml"), resources);
            final Pane pane = fxmlLoader.load();

            final SplitMergeSharesSlipController slipController = fxmlLoader.getController();

            slipController.setTransactionType(transactionType);
            slipController.accountProperty().bind(accountProperty());

            return new SlipControllerContainer(name, slipController, pane);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SlipControllerContainer buildReturnOfCapitalTab(final String name) {

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ReturnOfCapitalSlip.fxml"), resources);
            final Pane pane = fxmlLoader.load();

            final ReturnOfCapitalSlipController slipController = fxmlLoader.getController();

            slipController.accountProperty().bind(accountProperty());

            return new SlipControllerContainer(name, slipController, pane);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
