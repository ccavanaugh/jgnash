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
package jgnash.uifx.control.skin;

import java.math.BigDecimal;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.DetailedDecimalTextField;
import jgnash.uifx.control.behavior.DetailedDecimalTextFieldBehavior;

import com.sun.javafx.scene.control.skin.ComboBoxPopupControl;

/**
 * @author Craig Cavanaugh
 */
public class DetailedDecimalTextFieldSkin extends ComboBoxPopupControl<BigDecimal> {

    private final DetailedDecimalTextField detailedDecimalTextField;

    private DecimalTextField textField;

    public DetailedDecimalTextFieldSkin(final DetailedDecimalTextField detailedDecimalTextField) {
        super(detailedDecimalTextField, new DetailedDecimalTextFieldBehavior(detailedDecimalTextField));
        this.detailedDecimalTextField = detailedDecimalTextField;

        // editable input node
        textField = getInputNode();

        if (textField != null) {
            getChildren().add(textField);
        }

        // move focus in to the text field if the detailedDecimalTextField is editable
        detailedDecimalTextField.focusedProperty().addListener((ov, t, hasFocus) -> {
            if (detailedDecimalTextField.isEditable() && hasFocus) {
                Platform.runLater(textField::requestFocus);
            }
        });
    }

    @Override
    protected Node getPopupContent() {
        return null;
    }

    @Override
    protected TextField getEditor() {
        return null;
    }

    @Override
    protected StringConverter<BigDecimal> getConverter() {
        return null;
    }

    @Override
    public Node getDisplayNode() {
        Node displayNode = getInputNode();
        updateDisplayNode();
        return displayNode;
    }

    private BigDecimal initialDecimalFieldValue = null;

    private DecimalTextField getInputNode() {
        if (textField != null) return textField;

        textField = detailedDecimalTextField.getEditor();
        textField.setFocusTraversable(true);
        textField.promptTextProperty().bindBidirectional(detailedDecimalTextField.promptTextProperty());

        initialDecimalFieldValue = textField.getDecimal();

        textField.focusedProperty().addListener((ov, t, hasFocus) -> {
            // RT-21454 starts here
            if (!hasFocus) {
                setTextFromTextFieldIntoComboBoxValue();
                pseudoClassStateChanged(CONTAINS_FOCUS_PSEUDO_CLASS_STATE, false);
            }
            else {
                pseudoClassStateChanged(CONTAINS_FOCUS_PSEUDO_CLASS_STATE, true);
            }
        });

        return textField;
    }

    protected void updateDisplayNode() {
        final BigDecimal value = detailedDecimalTextField.getValue();

        if (initialDecimalFieldValue != null) {
            textField.setDecimal(initialDecimalFieldValue);
            initialDecimalFieldValue = null;
        }
        else {
            detailedDecimalTextField.setValue(value);
        }
    }

    private static PseudoClass CONTAINS_FOCUS_PSEUDO_CLASS_STATE = PseudoClass.getPseudoClass("contains-focus"); //NON-NLS
}
