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

import java.util.Objects;
import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.views.accounts.SelectAccountSecuritiesDialog;
import jgnash.util.NotNull;

/**
 * Investment Register pane controller
 *
 * @author Craig Cavanaugh
 */
public class InvestmentRegisterPaneController extends RegisterPaneController {

    @FXML
    private ComboBox<SlipControllerContainer> actionComboBox;

    @FXML
    private StackPane transactionSlips;

    private InvestmentSlipManager investmentSlipManager;

    @FXML
    @Override
    public void initialize() {
        super.initialize();

        // Load the register table
        registerTableControllerProperty.setValue(FXMLUtils.loadFXML(new Consumer<Node>() {
            @Override
            public void accept(final Node node) {
                registerTablePane.getChildren().add(node);
            }
        }, "InvestmentRegisterTable.fxml", resources));

        investmentSlipManager = new InvestmentSlipManager(transactionSlips, actionComboBox);
        investmentSlipManager.accountProperty().bind(accountProperty());
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
        final SelectAccountSecuritiesDialog dialog = new SelectAccountSecuritiesDialog(accountProperty().get(),
                accountProperty().get().getSecurities());

        if (dialog.showAndWait()) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            engine.updateAccountSecurities(accountProperty().get(), dialog.getSelectedSecurities());
        }
    }

    @Override
    protected void modifyTransaction(@NotNull final Transaction transaction) {
        if (transaction.areAccountsLocked()) {
            StaticUIMethods.displayError(resources.getString("Message.TransactionModifyLocked"));
            return;
        }

        investmentSlipManager.modifyTransaction(transaction);
    }
}
