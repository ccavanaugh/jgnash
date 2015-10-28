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
package jgnash.uifx.views.budget;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import jgnash.engine.budget.BudgetPeriod;

/**
 * @author Craig Cavanaugh
 */
public class BudgetPropertiesDialogController {

    @FXML
    private CheckBox incomeCheckBox;

    @FXML
    private CheckBox expenseCheckBox;

    @FXML
    private CheckBox assetCheckBox;

    @FXML
    private CheckBox liabilityCheckBox;

    @FXML
    private ComboBox<BudgetPeriod> periodComboBox;

    @FXML
    private TextField descriptionTextField;

    @FXML
    private void initialize() {
        periodComboBox.getItems().setAll(BudgetPeriod.values());
    }

    @FXML
    public void handleOkayAction() {
        ((Stage) incomeCheckBox.getScene().getWindow()).close();
    }

    @FXML
    public void handleCloseAction() {
        ((Stage) incomeCheckBox.getScene().getWindow()).close();
    }
}
