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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.KeyEvent;

import jgnash.uifx.control.autocomplete.AutoCompleteModel;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Text field for auto completion of values.
 *
 * @author Craig Cavanaugh
 */
public class AutoCompleteTextField<E> extends TextFieldEx {

    private final ObjectProperty<AutoCompleteModel<E>> autoCompleteModel = new SimpleObjectProperty<>();

    public AutoCompleteTextField() {
        // If the enter key is pressed to accept the auto complete,
        // simulate a tab key press to focus the next field
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (JavaFXUtils.ENTER_KEY.match(event)) {
                JavaFXUtils.runLater(() -> JavaFXUtils.focusNext(AutoCompleteTextField.this));
            }
        });
    }

    @Override
    public void replaceText(final int start, final int end, final String text) {
        super.replaceText(start, end, text);

        if (autoCompleteModel.get() != null) {
            final String currText = getText(); // get the full string
            final String newText = autoCompleteModel.get().doLookAhead(currText); // look for a match

            if (newText != null && !currText.isEmpty()) { // found a match and the field is not empty
                clear(); // clear existing text

                if (start + 1 > currText.length()) {    // delete action has occurred
                    setText(currText.substring(0, start));
                    positionCaret(start);
                } else {
                    // replace with the new text string
                    super.replaceText(0, 0, currText.substring(0, start + 1) + newText.substring(start + 1));

                    // highlight the remainder of the auto-completed text
                    positionCaret(start + 1);
                    selectEnd();
                }
            }
        }
    }

    @Override
    public void deleteText(final int start, final int end) {
        super.replaceText(start, end, "");  // force call to super super.replaceText to prevent bounds errors
    }

    public ObjectProperty<AutoCompleteModel<E>> autoCompleteModelObjectProperty() {
        return autoCompleteModel;
    }
}
