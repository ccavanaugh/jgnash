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

import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import jgnash.engine.TransactionEntry;
import jgnash.uifx.util.FXMLUtils;
import jgnash.util.NotNull;
import jgnash.util.ResourceUtils;

/**
 * Gains / Loss entry dialog
 *
 * @author Craig Cavanaugh
 */
class GainLossDialog extends AbstractTransactionEntryDialog {

    private static final String PREF_NODE_USER_ROOT = "/jgnash/uifx/views/register/gainLoss";

    @FXML
    private StackPane formPane;

    @FXML
    private ResourceBundle resources;

    private
    GainLossTransactionEntrySlipController gainLossController;

    GainLossDialog() {
        FXMLUtils.loadFXML(this, "GainLossDialog.fxml", ResourceUtils.getBundle());
        setTitle(ResourceUtils.getBundle().getString("Title.InvGainsLoss"));
    }

    @Override
    String getPrefNode() {
        return PREF_NODE_USER_ROOT;
    }

    @Override
    void newAction() {
        gainLossController.clearForm();
        tableView.getSelectionModel().clearSelection();
    }

    @Override
    void deleteAction() {
        final TransactionEntry entry = tableView.getSelectionModel().getSelectedItem();
        if (entry != null) {
            tableView.getSelectionModel().clearSelection();
            gainLossController.clearForm();
            getTransactionEntries().remove(entry);
        }
    }

    @Override
    void modifyTransactionEntry(@NotNull final TransactionEntry transactionEntry) {
        gainLossController.modifyTransactionEntry(transactionEntry);
    }

    @Override
   void initForm() {
        gainLossController = FXMLUtils.loadFXML(o -> formPane.getChildren().addAll((Node) o),
                "GainLossTransactionEntrySlip.fxml", resources);

        gainLossController.accountProperty().bind(accountProperty());
        gainLossController.transactionEntryListProperty().setValue(getTransactionEntries());
        gainLossController.comparatorProperty().bind(tableView.comparatorProperty());

        gainLossController.setSlipType(SlipType.INCREASE);
    }
}
