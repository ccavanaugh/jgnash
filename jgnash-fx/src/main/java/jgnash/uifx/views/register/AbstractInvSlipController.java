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
import java.util.ResourceBundle;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.ColumnConstraints;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ReconcileManager;
import jgnash.engine.Transaction;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.util.InjectFXML;
import jgnash.util.NotNull;

/**
 * Base Slip controller for investment transactions
 *
 * @author Craig Cavanaugh
 */
abstract class AbstractInvSlipController implements Slip {

    @InjectFXML
    private final ObjectProperty<Parent> parent = new SimpleObjectProperty<>();

    @FXML
    ResourceBundle resources;

    @FXML
    private CheckBox reconciledButton;

    @FXML
    DatePickerEx datePicker;

    @FXML
    TransactionTagPane tagPane;

    @FXML
    protected ColumnConstraints dateColumnConstraint;

    /**
     * Holds a reference to a transaction being modified.
     */
    Transaction modTrans = null;

    final ObjectProperty<Account> account = new SimpleObjectProperty<>();

    ObjectProperty<Account> accountProperty() {
        return account;
    }

    final BooleanProperty validFormProperty = new SimpleBooleanProperty();

    void initialize() {

        // Needed to support tri-state capability
        reconciledButton.setAllowIndeterminate(true);

        // Lazy init when account property is set
        account.addListener((observable, oldValue, newValue) -> {
            if (!newValue.memberOf(AccountGroup.INVEST)) {
                throw new RuntimeException(resources.getString("Message.Error.InvalidAccountGroup"));
            }
        });

        // Install an event handler when the parent has been set via injection
        parent.addListener((observable, oldValue, newValue) -> installKeyPressedHandler(newValue));

        if (dateColumnWidth.get() == 0) {
            dateColumnWidth.bind(getDateColumnWidth(datePicker.getStyle()));
        }

        dateColumnConstraint.minWidthProperty().bindBidirectional(dateColumnWidth);
        dateColumnConstraint.maxWidthProperty().bindBidirectional(dateColumnWidth);
    }

    @Override
    public BooleanProperty validFormProperty() {
        return validFormProperty;
    }

    @Override
    @NotNull
    public CheckBox getReconcileButton() {
        return reconciledButton;
    }

    @Override
    public void handleEnterAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        Objects.requireNonNull(engine);

        if (modTrans == null) {
            final Transaction newTrans = buildTransaction();

            // Need to set the reconciled state
            ReconcileManager.reconcileTransaction(accountProperty().get(), newTrans, getReconciledState());

            engine.addTransaction(newTrans);
        } else {
            final Transaction newTrans = buildTransaction();

                /* Need to preserve the reconciled state of the opposite side
                 * if both sides are not automatically reconciled
                 */
            ReconcileManager.reconcileTransaction(accountProperty().get(), newTrans, getReconciledState());

            if (engine.isTransactionValid(newTrans)) {
                if (engine.removeTransaction(modTrans)) {
                    engine.addTransaction(newTrans);
                }
            }
        }
        clearForm();
        focusFirstComponent();
    }

    @Override
    public void handleCancelAction() {
        clearForm();
        focusFirstComponent();
    }

    @Override
    public void clearForm() {
        modTrans = null;

        reconciledButton.setDisable(false);
        reconciledButton.setSelected(false);
        reconciledButton.setIndeterminate(false);

        tagPane.clearSelectedTags();
    }

    protected abstract void focusFirstComponent();
}
