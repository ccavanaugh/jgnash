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
package jgnash.uifx.report;

import java.time.LocalDate;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.stage.Stage;

import jgnash.uifx.Options;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.util.InjectFXML;

/**
 * Simple date range input controller.
 *
 * @author Craig Cavanaugh
 */
public class BalanceByMonthOptionsDialogController {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private CheckBox defaultCurrencyCheckBox;

    @FXML
    private RadioButton verticalRadioButton;

    @FXML
    private ButtonBar buttonBar;

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    private LocalDate[] dates = null;

    private final BooleanProperty forceCurrency = new SimpleBooleanProperty();

    @FXML
    private void initialize() {
        buttonBar.buttonOrderProperty().bind(Options.buttonOrderProperty());

        startDatePicker.setValue(endDatePicker.getValue().minusYears(1));

        defaultCurrencyCheckBox.selectedProperty().bindBidirectional(forceCurrency);
    }

    Optional<LocalDate[]> getDates() {
        return Optional.ofNullable(dates);
    }

    boolean isVertical() {
        return verticalRadioButton.isSelected();
    }

    BooleanProperty forceDefaultCurrencyProperty() {
        return forceCurrency;
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parent.get().getWindow()).close();
    }

    @FXML
    private void handleOkAction() {
        dates = new LocalDate[] {startDatePicker.getValue(), endDatePicker.getValue()};

        ((Stage) parent.get().getWindow()).close();
    }
}
