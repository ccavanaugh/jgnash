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

import java.math.BigDecimal;
import java.text.NumberFormat;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.control.TableCell;
import javafx.scene.input.KeyCode;

/**
 * A class containing a {@link TableCell} implementation that draws a {@link DecimalTextField} node inside the cell.
 * <p>
 * By default, the BigDecimalTableCell is rendered as a {@link javafx.scene.control.Label} when not being edited, and
 * as a DecimalTextField when in editing mode.
 *
 * @author Craig Cavanaugh
 */
public class BigDecimalTableCell<S> extends TableCell<S, BigDecimal> {

    private final SimpleObjectProperty<NumberFormat> numberFormat = new SimpleObjectProperty<>();

    private DecimalTextField decimalTextField = null;

    /**
     * Reference is needed to prevent premature garbage collection.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<Boolean> focusChangeListener;

    public BigDecimalTableCell(final ObjectProperty<NumberFormat> numberFormatProperty) {
        setStyle("-fx-alignment: center-right;");  // Right align
        numberFormatProperty().bind(numberFormatProperty);
    }

    public BigDecimalTableCell(final NumberFormat numberFormat) {
        setStyle("-fx-alignment: center-right;");  // Right align
        numberFormatProperty().set(numberFormat);
    }

    @Override
    protected void updateItem(final BigDecimal amount, final boolean empty) {
        super.updateItem(amount, empty);  // required

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                setText(null);
                getDecimalTextField().setDecimal(getItem());
                setGraphic(getDecimalTextField());
            } else if (amount != null) {
                setText(numberFormat.get().format(amount));
                setGraphic(null);
            } else {
                setText(null);
                setGraphic(null);
            }
        }
    }

    private SimpleObjectProperty<NumberFormat> numberFormatProperty() {
        return numberFormat;
    }

    @Override
    public void startEdit() {
        if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
            return;
        }

        final DecimalTextField decimalTextField = getDecimalTextField();
        decimalTextField.setDecimal(getItem());

        super.startEdit();
        setText(null);
        setGraphic(decimalTextField);
        decimalTextField.requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();

        setText(numberFormat.get().format(getItem()));
        setGraphic(null);
    }

    private DecimalTextField getDecimalTextField() {
        if (decimalTextField == null) {
            decimalTextField = new DecimalTextField();
            decimalTextField.scaleProperty().set(numberFormat.get().getMaximumFractionDigits());

            decimalTextField.setOnKeyPressed(event -> {
                if (isEditing() && event.getCode() == KeyCode.ENTER) {
                    commitEdit(decimalTextField.getDecimal());
                }
            });

            focusChangeListener = (observable, oldValue, newValue) -> {
                if (isEditing() && !newValue) {
                    commitEdit(decimalTextField.getDecimal());
                }
            };

            decimalTextField.focusedProperty().addListener(new WeakChangeListener<>(focusChangeListener));
        }

        return decimalTextField;
    }
}
