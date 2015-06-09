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
package jgnash.uifx.wizard.file;

import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import jgnash.uifx.control.wizard.WizardPaneController;

/**
 * New file wizard panel
 *
 * @author Craig Cavanaugh
 */
public class NewFileOneController implements WizardPaneController<NewFileWizard.Settings> {

    @FXML
    private TextArea textArea;

    @FXML
    private ComboBox storageTypeComboBox;

    @FXML
    private Button fileButton;

    @FXML
    private TextField fileNameField;

    @FXML
    private ResourceBundle resources;

    @Override
    public boolean isPaneValid() {
        return false;
    }

    @Override
    public String toString() {
        return "1. " + resources.getString("Title.DatabaseCfg");
    }
}
