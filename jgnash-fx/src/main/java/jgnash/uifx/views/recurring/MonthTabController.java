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

import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SpinnerValueFactory;

import jgnash.engine.recurring.MonthlyReminder;
import jgnash.engine.recurring.Reminder;
import jgnash.util.NotNull;

/**
 * Weekly repeating reminder controller.
 *
 * @author Craig Cavanaugh
 */
public class MonthTabController extends AbstractTabController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    void initialize() {
        super.initialize();

        reminder = new MonthlyReminder();

        numberSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 24, 1, 1));

        typeComboBox.getItems().addAll(resources.getString("Column.Date"), resources.getString("Column.Day"));
        typeComboBox.getSelectionModel().select(0);
    }

    @Override
    public Reminder getReminder() {
        ((MonthlyReminder) reminder).setType(typeComboBox.getSelectionModel().getSelectedIndex());

        return super.getReminder();
    }

    @Override
    public void setReminder(@NotNull final Reminder reminder) {
        if (!(reminder instanceof MonthlyReminder)) {
            throw new RuntimeException("Incorrect Reminder type");
        }

        super.setReminder(reminder);

        typeComboBox.getSelectionModel().select(((MonthlyReminder) reminder).getType());
    }
}
