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

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;

/**
 * Text field for entering integer values
 *
 * @author Craig Cavanaugh
 */
public class IntegerTextField extends TextField {

    public IntegerTextField() {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("IntegerTextField.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Sets the {@code Integer} value of the field
     *
     * @param value {@code Integer} value, if null, the field will be cleared
     */
    public void setInteger(final Integer value) {
        if (value != null) {
            setText(value.toString());
        } else {
            setText("");
        }
    }

    /**
     * Sets the {@code Long} value of the field
     *
     * @param value {@code Long} value, if null, the field will be cleared
     */
    public void setLong(final Long value) {
        if (value != null) {
            setText(value.toString());
        } else {
            setText("");
        }
    }

    /**
     * Returns the {@code Integer} value of the field
     *
     * @return the {@code Integer}, zero if the field is empty
     */
    public Integer getInteger() {
        if (getText() != null && getText().length() > 0) {
            return Integer.parseInt(getText());
        }

        return 0;
    }

    /**
     * Returns the {@code Long} value of the field
     *
     * @return the {@code Long}, zero if the field is empty
     */
    public Long getLong() {
        if (getText() != null && getText().length() > 0) {
            return Long.parseLong(getText());
        }

        return 0L;
    }

    @Override
    public void deleteText(int start, int end) {
        super.replaceText(start, end, "");
    }

    @Override
    public void replaceText(int start, int end, String text) {
        // If the replaced text would end up being invalid, then simply
        // ignore this call!

        if (text.matches("\\b\\d+\\b")) {
            super.replaceText(start, end, text);
        }
    }

    @Override
    public void replaceSelection(String text) {
        if (text.matches("\\b\\d+\\b")) {
            super.replaceSelection(text);
        }
    }
}
