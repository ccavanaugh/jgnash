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
package jgnash.uifx.views.recurring;

import java.time.LocalDate;

import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;

import jgnash.engine.recurring.Reminder;
import jgnash.uifx.control.DatePickerEx;
import jgnash.util.NotNull;

/**
 * Abstract repeating reminder controller.
 *
 * @author Craig Cavanaugh
 */
class AbstractTabController implements RecurringTabController {

    @FXML
    private RadioButton noEndDateToggleButton;

    @FXML
    private RadioButton dateToggleButton;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    Spinner<Integer> numberSpinner;

    Reminder reminder;

    @FXML
    void initialize() {

        // bind enabled state
        endDatePicker.disableProperty().bind(noEndDateToggleButton.selectedProperty());

        noEndDateToggleButton.setSelected(true);
    }

    @Override
    public Reminder getReminder() {
        LocalDate endDate = null;

        if (dateToggleButton.isSelected()) {
            endDate = endDatePicker.getValue();
        }

        reminder.setIncrement(numberSpinner.getValue());
        reminder.setEndDate(endDate);

        return reminder;
    }

    @Override
    public void setReminder(@NotNull final Reminder reminder) {
        this.reminder = reminder;

        numberSpinner.getValueFactory().setValue(reminder.getIncrement());

        if (reminder.getEndDate() != null) {
            endDatePicker.setValue(reminder.getEndDate());
            dateToggleButton.setSelected(true);
        }
    }
}
