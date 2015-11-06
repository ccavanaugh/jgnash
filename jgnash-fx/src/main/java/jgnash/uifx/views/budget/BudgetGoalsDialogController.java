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

import java.util.ResourceBundle;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.budget.BudgetGoal;

/**
 * @author Craig Cavanaugh
 */
public class BudgetGoalsDialogController {

    @FXML
    private TableView goalTable;

    @FXML
    private Label currencyLabel;

    @FXML
    private ComboBox periodComboBox;

    @FXML
    private ResourceBundle resources;

    private SimpleObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private SimpleObjectProperty<BudgetGoal> budgetGoalProperty = new SimpleObjectProperty<>();

    private IntegerProperty workingYearProperty = new SimpleIntegerProperty();

    @FXML
    private void initialize() {

    }

    public SimpleObjectProperty<Account> accountProperty() {
        return accountProperty;
    }

    public SimpleObjectProperty<BudgetGoal> budgetGoalProperty() {
        return budgetGoalProperty;
    }

    public IntegerProperty workingYearProperty() {
        return workingYearProperty;
    }

    @FXML
    private void handleOkayAction() {
        ((Stage) periodComboBox.getScene().getWindow()).close();
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) periodComboBox.getScene().getWindow()).close();
    }

    @FXML
    private void handleHistoricalFill() {

    }
}
