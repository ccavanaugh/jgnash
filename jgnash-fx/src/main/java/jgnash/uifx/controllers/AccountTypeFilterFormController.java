/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

/**
 * @author Craig Cavanaugh
 */
public class AccountTypeFilterFormController implements Initializable {

    @FXML
    ButtonBar buttonBar;

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
        final Button closeButton = new Button(resources.getString("Button.Close"));
        ButtonBar.setButtonData(closeButton, ButtonBar.ButtonData.CANCEL_CLOSE);

        closeButton.setOnAction(event -> ((Stage) closeButton.getScene().getWindow()).close());

        buttonBar.getButtons().add(closeButton);
    }

    public void setAccountTypeFilter(final AccountTypeFilter filter) {

        // Bind the buttons to the filter
        bankAccountCheckBox.selectedProperty().bindBidirectional(filter.getAccountTypesVisibleProperty());
        incomeAccountCheckBox.selectedProperty().bindBidirectional(filter.getIncomeTypesVisibleProperty());
        expenseAccountCheckBox.selectedProperty().bindBidirectional(filter.getExpenseTypesVisibleProperty());
        hiddenAccountCheckBox.selectedProperty().bindBidirectional(filter.getHiddenTypesVisibleProperty());
    }
}
