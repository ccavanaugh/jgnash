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
package jgnash.uifx.views.accounts;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TreeTableCell;

import jgnash.engine.Account;
import jgnash.uifx.control.IntegerTextField;

/**
* {@code TreeTableCell} editor for Integers
 *
 * @author Craig Cavanaugh
*/
class IntegerEditingTreeTableCell extends TreeTableCell<Account, Integer> {

    private IntegerTextField integerTextField;

    @Override
    public void startEdit() {
        if (!isEmpty()) {
            super.startEdit();
            createIntegerTextField();
            setText(null);
            setGraphic(integerTextField);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            integerTextField.requestFocus();
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();

        setText(getItem() == null ? "" : getItem().toString());
        setContentDisplay(ContentDisplay.TEXT_ONLY);
    }

    @Override
    public void updateItem(final Integer integer, final boolean empty) {
        super.updateItem(integer, empty);

        if (!empty) {
            if (isEditing()) {
                if (integerTextField != null) {
                    integerTextField.setInteger(integer);
                }
                setGraphic(integerTextField);

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            } else {
                setText(getItem() == null ? "" : integer.toString());

                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }
        } else {
            setText(null);
            setGraphic(null);
        }
    }

    private void createIntegerTextField() {
        integerTextField = new IntegerTextField();
        integerTextField.setInteger(getItem());
        integerTextField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);

        integerTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                commitEdit(integerTextField.getInteger());
            }
        });
    }
}
