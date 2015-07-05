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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.IntegerTextField;
import jgnash.uifx.util.InjectFXML;

/**
 * Controller for creating and modifying a reminder
 *
 * @author Craig Cavanaugh
 */
public class RecurringPropertiesController {

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private TabPane tabs;

    @FXML
    private CheckBox enabledCheckBox;

    @FXML
    private TextField lastOccurrenceTextField;

    @FXML
    private TextField daysPastDueTextField;

    @FXML
    private CheckBox autoEnterCheckBox;

    @FXML
    private IntegerTextField daysBeforeTextField;

    @FXML
    private AccountComboBox accountComboBox;

    @FXML
    private TextField descriptionTextField;

    @FXML
    private TextField payeeTextField;

    @FXML
    private TextArea notesTextArea;

    @FXML
    private void initialize() {

    }

    @FXML
    private void okAction() {
        ((Stage) parentProperty.get().getWindow()).close();
    }

    @FXML
    private void cancelAction() {
        ((Stage) parentProperty.get().getWindow()).close();
    }
}
