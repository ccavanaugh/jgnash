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
package jgnash.uifx.control;

import java.math.BigDecimal;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import jgnash.uifx.resource.font.MaterialDesignLabel;
import jgnash.util.NotNull;

/**
 * A {@code DecimalTextField} composite that supports use of a popup or dialog by overriding {@code show()}.
 *
 * @author Craig Cavanaugh
 */
public class DetailedDecimalTextField extends GridPane {

    /**
     * The editor for the ComboBox. It is used for both editable text field and non-editable text field.
     */
    private ReadOnlyObjectWrapper<DecimalTextField> editor;

    public ObjectProperty<BigDecimal> valueProperty() {
        return value;
    }

    private final ObjectProperty<BigDecimal> value = new SimpleObjectProperty<>(this, "value");

    /**
     * Specifies whether the numeric field allows for user input.
     *
     * @return the editable property
     */
    protected final BooleanProperty editableProperty() {
        return editable;
    }

    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", true) {
        @Override
        protected void invalidated() {
            pseudoClassStateChanged(PSEUDO_CLASS_EDITABLE, get());
        }
    };


    public DetailedDecimalTextField() {

        // apply the choice box and  date picker styles to the pane
        getStyleClass().setAll("choice-box", "date-picker");

        final Button arrowButton = new Button("", new MaterialDesignLabel(MaterialDesignLabel.MDIcon.PENCIL));
        arrowButton.getStyleClass().setAll("button", "arrow-button");
        arrowButton.setFocusTraversable(false);
        arrowButton.setOnAction(event -> show());

        final ColumnConstraints col = new ColumnConstraints();
        col.setHgrow(Priority.ALWAYS);

        getColumnConstraints().addAll(col);

        // add the controls
        add(getEditor(), 0, 0);
        add(arrowButton, 1, 0);

        // we want the GridPane to wrap right around the controls
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // use the focused state of the text field and apply to the pane
        getEditor().focusedProperty().addListener((observable, oldValue, newValue)
                -> pseudoClassStateChanged(FOCUSED_PSEUDO_CLASS, newValue));

    }

    private DecimalTextField getEditor() {
        return editorProperty().get();
    }

    private ReadOnlyObjectProperty<DecimalTextField> editorProperty() {
        if (editor == null) {
            editor = new ReadOnlyObjectWrapper<>(this, "editor"); //NON-NLS
            DecimalTextField field = new DecimalTextField();
            field.decimalProperty().bindBidirectional(valueProperty());
            field.editableProperty().bindBidirectional(editableProperty());
            editor.set(field);
        }
        return editor.getReadOnlyProperty();
    }

    /**
     * Decimal property.
     *
     * @return BigDecimal object property
     * @see DecimalTextField#decimalProperty()
     */
    public ObjectProperty<BigDecimal> decimalProperty() {
        return getEditor().decimalProperty();
    }

    /**
     * Gets the decimal value.
     *
     * @return BigDecimal value
     * @see DecimalTextField#getDecimal()
     */
    public @NotNull
    BigDecimal getDecimal() {
        return getEditor().getDecimal();
    }

    /**
     * Sets the value for the field.
     *
     * @param decimal BigDecimal value to display.  May not be null
     * @see DecimalTextField#setDecimal(BigDecimal)
     */
    protected void setDecimal(@NotNull final BigDecimal decimal) {
        Objects.requireNonNull(decimal);

        getEditor().setDecimal(decimal);
    }

    /**
     * Called when the button is clicked.
     */
    public void show() {
        // does nothing by default
    }

    private static final PseudoClass FOCUSED_PSEUDO_CLASS = PseudoClass.getPseudoClass("focused");

    private static final PseudoClass PSEUDO_CLASS_EDITABLE = PseudoClass.getPseudoClass("editable");
}
