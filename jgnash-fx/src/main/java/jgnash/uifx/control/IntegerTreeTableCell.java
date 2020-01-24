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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.uifx.control;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.control.TreeTableCell;
import javafx.scene.input.KeyCode;

/**
 * A class containing a {@link TreeTableCell} implementation that draws a {@link IntegerTextField} node inside the cell.
 * <p>
 * By default, the IntegerTreeTableCell is rendered as a {@link javafx.scene.control.Label} when not being edited, and
 * as a IntegerTextField when in editing mode.
 *
 * @author Craig Cavanaugh
 */
public class IntegerTreeTableCell<S> extends TreeTableCell<S, Integer> {

    private IntegerTextField integerTextField = null;

    /**
     * Reference is needed to prevent premature garbage collection.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<Boolean> focusChangeListener;

    @Override
    protected void updateItem(final Integer amount, final boolean empty) {
        super.updateItem(amount, empty);  // required

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                setText(null);
                getIntegerTextField().setInteger(getItem());
                setGraphic(getIntegerTextField());
            } else if (amount != null) {
                setText(amount.toString());
                setGraphic(null);
            } else {
                setText(null);
                setGraphic(null);
            }
        }
    }

    @Override
    public void startEdit() {
        if (!isEditable() || !getTreeTableView().isEditable() || !getTableColumn().isEditable()) {
            return;
        }

        final IntegerTextField decimalTextField = getIntegerTextField();
        decimalTextField.setInteger(getItem());

        super.startEdit();
        setText(null);
        setGraphic(decimalTextField);
        decimalTextField.requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();

        setText(getItem().toString());
        setGraphic(null);
    }

    private IntegerTextField getIntegerTextField() {
        if (integerTextField == null) {
            integerTextField = new IntegerTextField();

            integerTextField.setOnKeyPressed(event -> {
                if (isEditing() && event.getCode() == KeyCode.ENTER) {
                    commitEdit(integerTextField.getInteger());
                }
            });

            focusChangeListener = (observable, oldValue, newValue) -> {
                if (isEditing() && !newValue) {
                    commitEdit(integerTextField.getInteger());
                }
            };

            integerTextField.focusedProperty().addListener(new WeakChangeListener<>(focusChangeListener));
        }

        return integerTextField;
    }
}
