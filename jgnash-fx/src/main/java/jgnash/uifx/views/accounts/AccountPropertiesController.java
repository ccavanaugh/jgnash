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
package jgnash.uifx.views.accounts;

import java.net.URL;
import java.util.ResourceBundle;

import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.uifx.controllers.CurrencyComboBoxController;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * @author Craig Cavanaugh
 */
public class AccountPropertiesController implements Initializable {

    @FXML
    private ComboBox<AccountType> accountTypeComboBox;

    @FXML
    private Button okButton;

    @FXML
    private Button cancelButton;

    @FXML
    private TextArea notesTextArea;

    @FXML
    private TextField nameTextField;

    @FXML
    private TextField descriptionTextField;

    @FXML
    private TextField accountIdField;

    @FXML
    private TextField bankIdField;

    @FXML
    private ComboBox<CurrencyNode> currencyComboBox;

    @FXML
    private CheckBox lockedCheckBox;

    @FXML
    private CheckBox hideAccountCheckBox;

    @FXML
    private CheckBox placeholderCheckBox;

    @FXML
    private CheckBox excludeBudgetCheckBox;

    private CurrencyComboBoxController currencyComboBoxController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currencyComboBoxController = new CurrencyComboBoxController(currencyComboBox);

        accountTypeComboBox.getItems().addAll(AccountType.values());
        accountTypeComboBox.setValue(AccountType.BANK); // set default
    }
}
