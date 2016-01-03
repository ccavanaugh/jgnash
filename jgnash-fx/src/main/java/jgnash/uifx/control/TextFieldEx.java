/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import java.io.IOException;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;

import jgnash.uifx.Options;

/**
 * TextField with expanded capabilities
 *
 * @author Craig Cavanaugh
 */
public class TextFieldEx extends TextField {

    private static final BooleanProperty selectOnFocus;

    /**
     * Reference is needed to prevent premature garbage collection
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final ChangeListener<Boolean> focusChangeListener;

    static {
        selectOnFocus = new SimpleBooleanProperty();
        selectOnFocus.bind(Options.selectOnFocusProperty());
    }

    public TextFieldEx() {
        final FXMLLoader loader = new FXMLLoader(TextFieldEx.class.getResource("TextFieldEx.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        // If the select on focus property is enabled, select the text when focus is received
        focusChangeListener = (observable, oldValue, newValue) -> {
            if (selectOnFocus.get()) {
                Platform.runLater(() -> {
                    if (isFocused() && isEditable() && !getText().isEmpty()) {
                        selectAll();
                    }
                });
            }
        };

        focusedProperty().addListener(new WeakChangeListener<>(focusChangeListener));
    }
}
