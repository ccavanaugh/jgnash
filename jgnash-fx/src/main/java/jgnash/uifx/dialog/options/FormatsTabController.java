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
package jgnash.uifx.dialog.options;

import jgnash.text.NumericFormats;
import jgnash.time.DateUtils;
import jgnash.uifx.Options;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

/**
 * Controls formats used to display information
 *
 * @author Craig Cavanaugh
 */
public class FormatsTabController {

    @FXML
    private ComboBox<String> fullNumberFormatComboBox;

    @FXML
    private ComboBox<String> shortNumberFormatComboBox;

    @FXML
    private ComboBox<String> dateFormatComboBox;

    @FXML
    private void initialize() {
        dateFormatComboBox.getItems().setAll(DateUtils.getAvailableShortDateFormats());
        dateFormatComboBox.setValue(Options.shortDateFormatProperty().getValue());

        fullNumberFormatComboBox.getItems().setAll(NumericFormats.getKnownFullPatterns());
        fullNumberFormatComboBox.setValue(Options.fullNumericFormatProperty().getValue());

        shortNumberFormatComboBox.getItems().setAll(NumericFormats.getKnownShortPatterns());
        shortNumberFormatComboBox.setValue(Options.shortNumericFormatProperty().getValue());

        dateFormatComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                Options.shortDateFormatProperty().setValue(newValue);
            }
        });

        fullNumberFormatComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                Options.fullNumericFormatProperty().setValue(newValue);
            }
        });

        shortNumberFormatComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                Options.shortNumericFormatProperty().setValue(newValue);
            }
        });
    }
}
