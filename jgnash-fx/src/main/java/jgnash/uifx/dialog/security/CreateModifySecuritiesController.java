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
package jgnash.uifx.dialog.security;

import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import jgnash.engine.SecurityNode;
import jgnash.uifx.control.CurrencyComboBox;
import jgnash.uifx.control.IntegerTextField;
import jgnash.uifx.control.QuoteSourceComboBox;
import jgnash.uifx.util.InjectFXML;

/**
 * Controller for creating and modifying securities
 *
 * @author Craig Cavanaugh
 */
public class CreateModifySecuritiesController {

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private ListView<SecurityNode> listView;

    @FXML
    private TextField symbolTextField;

    @FXML
    private TextField cusipTextField;

    @FXML
    private QuoteSourceComboBox quoteSourceComboBox;

    @FXML
    private TextField descriptionTextField;

    @FXML
    private IntegerTextField scaleTextField;

    @FXML
    private CurrencyComboBox reportedCurrencyComboBox;

    @FXML
    private ResourceBundle resources;

    @FXML
    void initialize() {

    }

    @FXML
    private void handleNewAction() {
    }

    @FXML
    private void handleDeleteAction() {
    }

    @FXML
    private void handleCancelAction() {
    }

    @FXML
    private void handleApplyAction() {
    }

    @FXML
    private void handleCloseAction() {
        ((Stage)parentProperty.get().getWindow()).close();
    }
}
