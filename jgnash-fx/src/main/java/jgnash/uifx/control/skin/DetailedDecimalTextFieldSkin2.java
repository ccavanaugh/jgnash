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
import jgnash.uifx.control.DetailedDecimalTextField2;
import jgnash.uifx.control.behavior.DetailedDecimalTextFieldBehavior2;

import com.sun.javafx.scene.control.skin.ComboBoxPopupControl;

/**
 * @author Craig Cavanaugh
 */
public class DetailedDecimalTextFieldSkin2 extends ComboBoxPopupControl<BigDecimal> {

    private final DetailedDecimalTextField2 detailedDecimalTextField;

    private DecimalTextField textField;

    public DetailedDecimalTextFieldSkin2(final DetailedDecimalTextField2 detailedDecimalTextField2) {
        super(detailedDecimalTextField2, new DetailedDecimalTextFieldBehavior2(detailedDecimalTextField2));
        this.detailedDecimalTextField = detailedDecimalTextField2;

        // editable input node
        textField = getInputNode();

        // Fix for RT-29565. Without this the textField does not have a correct
        // pref width at startup, as it is not part of the scenegraph (and therefore
        // has no pref width until after the first measurements have been taken).
        if (textField != null) {
            getChildren().add(textField);
        }

        // move focus in to the text field if the detailedDecimalTextField is editable
        detailedDecimalTextField2.focusedProperty().addListener((ov, t, hasFocus) -> {
            if (detailedDecimalTextField2.isEditable() && hasFocus) {
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

    private BigDecimal initialTextFieldValue = null;

    private DecimalTextField getInputNode() {
        if (textField != null) return textField;

        textField = detailedDecimalTextField.getEditor();
        textField.setFocusTraversable(true);
        textField.promptTextProperty().bindBidirectional(detailedDecimalTextField.promptTextProperty());

        // Fix for RT-21406: ComboBox do not show initial text value
        initialTextFieldValue = textField.getDecimal();
        // End of fix (see updateDisplayNode below for the related code)

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
        BigDecimal value = detailedDecimalTextField.getValue();
        if (initialTextFieldValue != null) {
            // Remainder of fix for RT-21406: ComboBox do not show initial text value
            textField.setDecimal(initialTextFieldValue);
            initialTextFieldValue = null;
            // end of fix
        }
        else {
            detailedDecimalTextField.setValue(value);
        }
    }

    private static PseudoClass CONTAINS_FOCUS_PSEUDO_CLASS_STATE = PseudoClass.getPseudoClass("contains-focus"); //NON-NLS
}
