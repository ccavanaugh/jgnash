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

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import jgnash.engine.*;
import jgnash.uifx.util.FXMLUtils;
import jgnash.util.NotNull;

import java.util.Objects;

/**
 * Register pane controller
 *
 * @author Craig Cavanaugh
 */
public class BankRegisterPaneController extends RegisterPaneController {

    @FXML
    protected Button jumpButton;

    @FXML
    protected TabPane transactionForms;

    private Tab adjustTab;

    private Tab creditTab;

    private Tab debitTab;

    @FXML
    @Override
    public void initialize() {

        super.initialize();

        jumpButton.disableProperty().bind(selectedTransactionProperty.isNull());

        transactionForms.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Load the register table
        final RegisterTableController controller = FXMLUtils.loadFXML(o -> {
            registerTablePane.getChildren().add(o);
        }, "BasicRegisterTable.fxml", resources);

        registerTableControllerProperty.setValue(controller);

        accountProperty().addListener((observable, oldValue, newValue) -> {
            buildTabs();
        });
    }

    @Override
    protected void modifyTransaction(@NotNull final Transaction transaction) {
        if (!(transaction instanceof InvestmentTransaction)) {
            if (transaction.getTransactionType() == TransactionType.SINGLENTRY) {
                transactionForms.getSelectionModel().select(adjustTab);
                ((Slip) adjustTab.getUserData()).modifyTransaction(transaction);
            } else if (transaction.getAmount(accountProperty().get()).signum() >= 0) {
                transactionForms.getSelectionModel().select(creditTab);
                ((Slip) creditTab.getUserData()).modifyTransaction(transaction);
            } else {
                transactionForms.getSelectionModel().select(debitTab);
                ((Slip) debitTab.getUserData()).modifyTransaction(transaction);
            }
        } else {    // pop a dialog to modify the transaction
            InvestmentTransactionDialog.showAndWait(((InvestmentTransaction) transaction).getInvestmentAccount(),
                    transaction, optional -> {
                if (optional.isPresent()) {
                    final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                    Objects.requireNonNull(engine);

                    engine.removeTransaction(transaction);
                    engine.addTransaction(optional.get());
                }
            });
        }
    }

    private void buildTabs() {
        final AccountType accountType = accountProperty().get().getAccountType();

        final String[] tabNames = RegisterFactory.getCreditDebitTabNames(accountType);

        creditTab = buildTab(tabNames[0], SlipType.INCREASE);
        debitTab = buildTab(tabNames[1], SlipType.DECREASE);
        final Tab transferTab = buildTransferTab();
        adjustTab = buildAdjustTab();

        transactionForms.getTabs().addAll(creditTab, debitTab, transferTab, adjustTab);

        if (accountType == AccountType.CHECKING || accountType == AccountType.CREDIT) {
            transactionForms.getSelectionModel().select(debitTab);
        } else if (accountType.getAccountGroup() == AccountGroup.INCOME) {
            transactionForms.getSelectionModel().select(debitTab);
        }
    }

    private Tab buildTab(final String tabName, final SlipType slipType) {
        final Tab tab = new Tab(tabName);
        final SlipController slipController = FXMLUtils.loadFXML(tab::setContent,
                "BankSlip.fxml", resources);

        slipController.setSlipType(slipType);
        slipController.accountProperty().bind(accountProperty());

        tab.setUserData(slipController); // place a reference to the controller here

        return tab;
    }

    private Tab buildAdjustTab() {
        final Tab tab = new Tab(resources.getString("Tab.Adjust"));

        final AdjustmentSlipController adjustmentSlipController = FXMLUtils.loadFXML(tab::setContent,
                "AdjustmentSlip.fxml", resources);

        adjustmentSlipController.accountProperty().bind(accountProperty());

        tab.setUserData(adjustmentSlipController); // place a reference to the controller here

        return tab;
    }

    private Tab buildTransferTab() {
        final Tab tab = new Tab(resources.getString("Tab.Transfer"));

        final TransferSlipController slipController = FXMLUtils.loadFXML(tab::setContent,
                "TransferSlip.fxml", resources);

        slipController.accountProperty().bind(accountProperty());

        tab.setUserData(slipController); // place a reference to the controller here

        return tab;
    }

    @Override
    protected void clearForm() {
        ((Slip) transactionForms.getSelectionModel().getSelectedItem().getUserData()).clearForm();
    }
}
