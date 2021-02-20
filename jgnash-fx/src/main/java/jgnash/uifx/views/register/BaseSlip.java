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

import javafx.beans.property.BooleanProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyEvent;

import jgnash.engine.ReconciledState;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.util.NotNull;

/**
 * Base slip interface.
 *
 * @author Craig Cavanaugh
 */
interface BaseSlip {

    void clearForm();

    void handleCancelAction();

    void handleEnterAction();

    /**
     * Observable value indicating true if the form entry is valid.  Entry acceptance should be bound to this property.
     *
     * @return boolean property
     */
    BooleanProperty validFormProperty();

    @NotNull
    CheckBox getReconcileButton();

    default void setReconciledState(final ReconciledState reconciledState) {
        switch (reconciledState) {
            case NOT_RECONCILED:
                getReconcileButton().setIndeterminate(false);
                getReconcileButton().setSelected(false);
                break;
            case RECONCILED:
                getReconcileButton().setIndeterminate(false);
                getReconcileButton().setSelected(true);
                break;
            case CLEARED:
                getReconcileButton().setIndeterminate(true);
        }
    }

    default ReconciledState getReconciledState() {
        if (getReconcileButton().isIndeterminate()) {
            return ReconciledState.CLEARED;
        } else if (getReconcileButton().isSelected()) {
            return ReconciledState.RECONCILED;
        }
        return ReconciledState.NOT_RECONCILED;
    }

    /**
     * Default implementation for intelligent handling for escape and enter keys within a Slip form.
     * This should be called by the implementing class.
     *
     * @param parent Parent node to attach handler to.
     */
    default void installKeyPressedHandler(final Parent parent) {

        parent.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (JavaFXUtils.ESCAPE_KEY.match(event)) {  // clear the form if an escape key is detected
                clearForm();
            } else if (JavaFXUtils.ENTER_KEY.match(event)) {    // handle an enter key if detected
                if (validFormProperty().get()) {
                    JavaFXUtils.runLater(BaseSlip.this::handleEnterAction);
                } else {
                    JavaFXUtils.runLater(() -> {
                        if (event.getSource() instanceof Node) {
                            JavaFXUtils.focusNext((Node) event.getSource());
                        }
                    });
                }
            }
        });
    }
}
