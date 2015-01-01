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
package jgnash.uifx.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

import jgnash.uifx.util.AccountTypeFilter;

/**
 * @author Craig Cavanaugh
 */
public class AccountTypeFilterFormController implements Initializable {

    @FXML
    CheckBox bankAccountCheckBox;

    @FXML
    CheckBox expenseAccountCheckBox;

    @FXML
    CheckBox incomeAccountCheckBox;

    @FXML
    CheckBox hiddenAccountCheckBox;

    @FXML
    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

    }

    public void setAccountTypeFilter(final AccountTypeFilter filter) {

        // Bind the buttons to the filter
        bankAccountCheckBox.selectedProperty().bindBidirectional(filter.getAccountTypesVisibleProperty());
        incomeAccountCheckBox.selectedProperty().bindBidirectional(filter.getIncomeTypesVisibleProperty());
        expenseAccountCheckBox.selectedProperty().bindBidirectional(filter.getExpenseTypesVisibleProperty());
        hiddenAccountCheckBox.selectedProperty().bindBidirectional(filter.getHiddenTypesVisibleProperty());
    }

    @FXML
    private void closeAction() {
        ((Stage) bankAccountCheckBox.getScene().getWindow()).close();
    }
}
