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
package jgnash.uifx.views.recurring;

import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import jgnash.engine.recurring.DailyReminder;
import jgnash.engine.recurring.Reminder;
import jgnash.engine.recurring.WeeklyReminder;
import jgnash.uifx.control.DatePickerEx;
import jgnash.util.NotNull;

import java.util.Date;

/**
 * Weekly repeating reminder controller
 *
 * @author Craig Cavanaugh
 */
public class WeekTabController implements RecurringTabController {

    @FXML
    private RadioButton noEndDateToggleButton;

    @FXML
    private RadioButton dateToggleButton;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private Spinner numberSpinner;

    private Reminder reminder = new DailyReminder();

    @FXML
    @SuppressWarnings("unchecked")
    private void initialize() {
        numberSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, 1, 1));

        // bind enabled state
        endDatePicker.disableProperty().bind(noEndDateToggleButton.selectedProperty());

        noEndDateToggleButton.setSelected(true);
    }

    @Override
    public Reminder getReminder() {
        final WeeklyReminder r = (WeeklyReminder) reminder;

        Date endDate = null;

        if (noEndDateToggleButton.isSelected()) {
            endDate = endDatePicker.getDate();
        }

        r.setIncrement(((Number) numberSpinner.getValue()).intValue());
        r.setEndDate(endDate);

        return reminder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setReminder(@NotNull final Reminder reminder) {
        if (!(reminder instanceof WeeklyReminder)) {
            throw new RuntimeException("Incorrect Reminder type");
        }

        this.reminder = reminder;

        final WeeklyReminder r = (WeeklyReminder) reminder;

        numberSpinner.getValueFactory().setValue(r.getIncrement());

        if (r.getEndDate() != null) {
            endDatePicker.setDate(r.getEndDate());
            dateToggleButton.setSelected(true);
        }
    }
}
