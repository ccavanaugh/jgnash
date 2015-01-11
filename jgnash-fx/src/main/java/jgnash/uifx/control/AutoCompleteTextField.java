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
package jgnash.uifx.control;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import jgnash.uifx.control.autocomplete.AutoCompleteModel;

import java.io.IOException;

/**
 * Text field for auto completion of values
 *
 * @author Craig Cavanaugh
 */
public class AutoCompleteTextField<E> extends TextField {

    private final ObjectProperty<AutoCompleteModel<E>> autoCompleteModelObjectProperty = new SimpleObjectProperty<>();

    public AutoCompleteTextField() {
        final FXMLLoader loader = new FXMLLoader(getClass().getResource("AutoCompleteTextField.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void replaceText(final int start, final int end, final String text) {
        // If the replaced text would end up being invalid, then simply
        // ignore this call!

        super.replaceText(start, end, text);

        if (autoCompleteModelObjectProperty.get() != null) {
            final String currText = getText(); // get the full string
            final String newText = autoCompleteModelObjectProperty.get().doLookAhead(currText); // look for a match

            if (newText != null && !currText.isEmpty()) { // found a match and the field is not empty
                clear(); // clear existing text
                super.replaceText(0, 0, currText.substring(0, start + 1) + newText.substring(start + 1)); // replace with the new text string

                // highlight the remainder of the auto-completed text
                positionCaret(start + 1);
                selectEnd();
            }
        }
    }

    @Override
    public void deleteText(final int start, final int end) {
        super.replaceText(start, end, "");  // force call to super super.replaceText to prevent bounds errors
    }

    public ObjectProperty<AutoCompleteModel<E>> getAutoCompleteModelObjectProperty() {
        return autoCompleteModelObjectProperty;
    }
}
