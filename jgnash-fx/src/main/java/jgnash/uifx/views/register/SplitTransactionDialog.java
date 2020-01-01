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

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.WindowEvent;

import jgnash.engine.TransactionEntry;
import jgnash.uifx.Options;
import jgnash.uifx.util.FXMLUtils;
import jgnash.util.NotNull;
import jgnash.resource.util.ResourceUtils;

/**
 * Split Transaction entry dialog.
 *
 * @author Craig Cavanaugh
 */
class SplitTransactionDialog extends AbstractTransactionEntryDialog {

    private static final String PREF_NODE_USER_ROOT = "/jgnash/uifx/views/register/splits";

    @FXML
    private TabPane tabPane;

    @FXML
    private CheckBox concatenateMemosCheckBox;

    private Tab creditTab;

    private Tab debitTab;

    SplitTransactionDialog() {
        FXMLUtils.loadFXML(this, "SplitTransactionDialog.fxml", ResourceUtils.getBundle());
        setTitle(ResourceUtils.getString("Title.SpitTran"));
    }

    @Override
    String getPrefNode() {
        return PREF_NODE_USER_ROOT;
    }

    @Override
    void newAction() {
        ((SplitTransactionSlipController) creditTab.getUserData()).clearForm();
        ((SplitTransactionSlipController) debitTab.getUserData()).clearForm();
        tableView.getSelectionModel().clearSelection();
    }

    @Override
    void deleteAction() {
        final TransactionEntry entry = tableView.getSelectionModel().getSelectedItem();
        if (entry != null) {
            tableView.getSelectionModel().clearSelection();
            ((SplitTransactionSlipController) tabPane.getSelectionModel().getSelectedItem().getUserData()).clearForm();
            getTransactionEntries().remove(entry);
        }
    }


    @Override
    void modifyTransactionEntry(@NotNull final TransactionEntry transactionEntry) {
        if (transactionEntry.getCreditAccount() == accountProperty().get()) { // this is a credit
            tabPane.getSelectionModel().select(creditTab);
            ((SplitTransactionSlipController) creditTab.getUserData()).modifyTransactionEntry(transactionEntry);
        } else {
            tabPane.getSelectionModel().select(debitTab);
            ((SplitTransactionSlipController) debitTab.getUserData()).modifyTransactionEntry(transactionEntry);
        }
    }

    @Override
    void initForm() {
        concatenateMemosCheckBox.selectedProperty().bindBidirectional(Options.concatenateMemosProperty());

        final String[] tabNames = RegisterFactory.getCreditDebitTabNames(accountProperty().get().getAccountType());

        creditTab = new Tab(tabNames[0]);

        final SplitTransactionSlipController creditController = FXMLUtils.loadFXML(o -> creditTab.setContent(o),
                "SplitTransactionSlip.fxml", resources);

        creditTab.setUserData(creditController);

        creditController.setSlipType(SlipType.INCREASE);
        creditController.accountProperty().set(accountProperty().getValue());
        creditController.transactionEntryListProperty().set(getTransactionEntries());
        creditController.comparatorProperty().bind(tableView.comparatorProperty());

        debitTab = new Tab(tabNames[1]);

        final SplitTransactionSlipController debitController = FXMLUtils.loadFXML(o -> debitTab.setContent(o),
                "SplitTransactionSlip.fxml", resources);

        debitTab.setUserData(debitController);

        debitController.setSlipType(SlipType.DECREASE);
        debitController.accountProperty().set(accountProperty().getValue());
        debitController.transactionEntryListProperty().set(getTransactionEntries());
        debitController.comparatorProperty().bind(tableView.comparatorProperty());

        tabPane.getTabs().addAll(creditTab, debitTab);

        // Install a listener to unbind from the Options to prevent leaks
        if (getScene() != null) {
            getScene().windowProperty().get().addEventHandler(WindowEvent.WINDOW_HIDING,
                    event -> concatenateMemosCheckBox.selectedProperty()
                            .unbindBidirectional(Options.concatenateMemosProperty()));
        }
    }

    void show(final SlipType slipType, final Runnable runnable) {
        switch (slipType) {
            case DECREASE:
                tabPane.getSelectionModel().select(debitTab);
                break;
            case INCREASE:
                tabPane.getSelectionModel().select(creditTab);
                break;
        }

        show(runnable);
    }
}
