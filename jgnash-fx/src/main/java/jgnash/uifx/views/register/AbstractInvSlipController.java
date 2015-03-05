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
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ReconcileManager;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;

/**
 * @author Craig Cavanaugh
 */
public abstract class AbstractInvSlipController implements Slip {

    @FXML
    ResourceBundle resources;

    @FXML
    CheckBox reconciledButton;

    /**
     * Holds a reference to a transaction being modified
     */
    Transaction modTrans = null;

    final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    ObjectProperty<Account> accountProperty() {
        return accountProperty;
    }

    void initialize() {

        // Lazy init when account property is set
        accountProperty.addListener((observable, oldValue, newValue) -> {
            if (!newValue.memberOf(AccountGroup.INVEST)) {
                throw new RuntimeException(resources.getString("Message.Error.InvalidAccountGroup"));
            }
        });
    }

    @Override
    public void handleEnterAction() {
        if (validateForm()) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

            Objects.requireNonNull(engine);

            if (modTrans == null) {
                final Transaction newTrans = buildTransaction();

                // Need to set the reconciled state
                ReconcileManager.reconcileTransaction(accountProperty().get(), newTrans,
                        reconciledButton.isSelected() ? ReconciledState.CLEARED : ReconciledState.NOT_RECONCILED);

                engine.addTransaction(newTrans);
            } else {
                final Transaction newTrans = buildTransaction();

                newTrans.setDateEntered(modTrans.getDateEntered());

                /* Need to preserve the reconciled state of the opposite side
                 * if both sides are not automatically reconciled
                 */
                ReconcileManager.reconcileTransaction(accountProperty().get(), newTrans,
                        reconciledButton.isSelected() ? ReconciledState.CLEARED : ReconciledState.NOT_RECONCILED);

                if (engine.isTransactionValid(newTrans)) {
                    if (engine.removeTransaction(modTrans)) {
                        engine.addTransaction(newTrans);
                    }
                }
            }
            clearForm();
            focusFirstComponent();
        }
    }

    abstract protected void focusFirstComponent();
}
