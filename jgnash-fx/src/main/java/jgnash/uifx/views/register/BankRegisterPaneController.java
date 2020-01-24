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

import java.util.Objects;
import java.util.prefs.Preferences;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionType;
import jgnash.uifx.Options;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.util.NotNull;

/**
 * Register pane controller.
 *
 * @author Craig Cavanaugh
 */
public class BankRegisterPaneController extends RegisterPaneController {

    private static final String RECENT_TAB = "/jgnash/uifx/register/recent/tab";

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

        jumpButton.disableProperty().bind(selectedTransaction.isNull());

        transactionForms.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Load the register table
        final RegisterTableController controller = FXMLUtils.loadFXML(o
                -> registerTablePane.getChildren().add(o), "BasicRegisterTable.fxml", resources);

        registerTableController.set(controller);

        accountProperty().addListener((observable, oldValue, newValue) -> buildTabs());
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
            InvestmentTransactionDialog.show(((InvestmentTransaction) transaction).getInvestmentAccount(), transaction,
                    newTransaction -> {
                        if (newTransaction != null) {
                            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                            Objects.requireNonNull(engine);

                            engine.removeTransaction(transaction);
                            engine.addTransaction(newTransaction);
                        }
                    });
        }
    }

    private void buildTabs() {
        final AccountType accountType = accountProperty().get().getAccountType();

        final String[] tabNames = RegisterFactory.getCreditDebitTabNames(accountType);

        creditTab = buildTab(tabNames[0], SlipType.INCREASE);
        debitTab = buildTab(tabNames[1], SlipType.DECREASE);
        adjustTab = buildAdjustTab();

        transactionForms.getTabs().addAll(creditTab, debitTab, buildTransferTab(), adjustTab);

        if (accountType == AccountType.CHECKING || accountType == AccountType.CREDIT) {
            transactionForms.getSelectionModel().select(debitTab);
        } else if (accountType.getAccountGroup() == AccountGroup.INCOME) {
            transactionForms.getSelectionModel().select(debitTab);
        }

        restoreLastTabUsed();

        transactionForms.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue)
                -> saveLastTabUsed(newValue.intValue()));
    }

    private Tab buildTab(final String tabName, final SlipType slipType) {
        final Tab tab = new Tab(tabName);
        final SlipController slipController = FXMLUtils.loadFXML(tab::setContent, "BankSlip.fxml", resources);

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

    private void saveLastTabUsed(final int index) {
        final Preferences tabPreferences = Preferences.userRoot().node(RECENT_TAB);
        tabPreferences.putInt(accountProperty().get().getUuid().toString(), index);
    }

    private void restoreLastTabUsed() {
        if (Options.restoreLastTabProperty().get()) {
            Preferences tabPreferences = Preferences.userRoot().node(RECENT_TAB);
            String id = accountProperty().get().getUuid().toString();

            final int index = tabPreferences.getInt(id, transactionForms.getSelectionModel().getSelectedIndex());

            JavaFXUtils.runLater(() -> transactionForms.getSelectionModel().select(index));
        }
    }

    @Override
    protected void clearForm() {
        ((Slip) transactionForms.getSelectionModel().getSelectedItem().getUserData()).clearForm();
    }
}
