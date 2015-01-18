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
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionType;
import jgnash.util.NotNull;

/**
 * Register pane controller
 *
 * @author Craig Cavanaugh
 */
public class BankRegisterPaneController extends RegisterPaneController {

    @FXML
    protected Button jumpButton; // TODO Implement handler

    @FXML
    protected StackPane register;

    @FXML
    protected TabPane transactionForms;

    private Tab adjustTab;

    private Tab creditTab;

    private Tab debitTab;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        super.initialize(location, resources);

        jumpButton.disableProperty().bind(selectedTransactionProperty.isNull());

        transactionForms.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Load the register table
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("BasicRegisterTable.fxml"), resources);
            register.getChildren().add(fxmlLoader.load());
            registerTableControllerProperty.setValue(fxmlLoader.getController());
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        getAccountProperty().addListener((observable, oldValue, newValue) -> {
            buildTabs();
        });
    }

    @Override
    protected void modifyTransaction(@NotNull final Transaction transaction) {
        if (!(transaction instanceof InvestmentTransaction)) {
            if (transaction.getTransactionType() == TransactionType.SINGLENTRY) {
                transactionForms.getSelectionModel().select(adjustTab);
                ((Slip)adjustTab.getUserData()).modifyTransaction(transaction);
            } else if (transaction.getAmount(getAccountProperty().get()).signum() >= 0) {
                transactionForms.getSelectionModel().select(creditTab);
                ((Slip)creditTab.getUserData()).modifyTransaction(transaction);
            } else {
                transactionForms.getSelectionModel().select(debitTab);
                ((Slip)debitTab.getUserData()).modifyTransaction(transaction);
            }
        } /*else {
            // TODO: Show investment transaction dialog
        }*/
    }

    private void buildTabs() {
        final AccountType accountType = getAccountProperty().get().getAccountType();

        final String[] tabNames = RegisterFactory.getCreditDebitTabNames(accountType);

        creditTab = buildTab(tabNames[0], SlipType.INCREASE);
        debitTab = buildTab(tabNames[1], SlipType.DECREASE);
        adjustTab = buildAdjustTab();

        transactionForms.getTabs().addAll(creditTab, debitTab, adjustTab);

        if (accountType == AccountType.CHECKING || accountType == AccountType.CREDIT) {
            transactionForms.getSelectionModel().select(debitTab);
        } else if (accountType.getAccountGroup() == AccountGroup.INCOME) {
            transactionForms.getSelectionModel().select(debitTab);
        }
    }

    private Tab buildTab(final String tabName, final SlipType slipType) {

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("BankSlip.fxml"), resources);
            final Pane pane = fxmlLoader.load();

            final SlipController slipController = fxmlLoader.getController();

            slipController.setSlipType(slipType);
            slipController.getAccountProperty().bind(getAccountProperty());

            final Tab tab = new Tab(tabName);
            tab.setContent(pane);
            tab.setUserData(slipController); // place a reference to the controller here

            return tab;
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
        return new Tab();
    }

    private Tab buildAdjustTab() {
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("BankAdjustmentSlip.fxml"), resources);
            final Pane pane = fxmlLoader.load();

            final BankAdjustmentSlipController transactionPaneController = fxmlLoader.getController();

            transactionPaneController.getAccountProperty().bind(getAccountProperty());

            final Tab tab = new Tab(resources.getString("Tab.Adjust"));
            tab.setContent(pane);
            tab.setUserData(transactionPaneController); // place a reference to the controller here

            return tab;
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
        return new Tab();
    }

    @Override
    protected void clearForm() {
        ((Slip)transactionForms.getSelectionModel().getSelectedItem().getUserData()).clearForm();
    }
}
