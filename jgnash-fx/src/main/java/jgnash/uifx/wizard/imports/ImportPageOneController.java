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
package jgnash.uifx.wizard.imports;

import java.util.Map;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import jgnash.engine.Account;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.wizard.AbstractWizardPaneController;
import jgnash.util.TextResource;

/**
 * Import Wizard, base account selection
 *
 * @author Craig Cavanaugh
 */
public class ImportPageOneController extends AbstractWizardPaneController<ImportWizard.Settings> {

    @FXML
    private ResourceBundle resources;

    @FXML
    private TextArea textArea;

    @FXML
    private AccountComboBox accountComboBox;

    private final SimpleBooleanProperty valid = new SimpleBooleanProperty(false);

    @FXML
    private void initialize() {
        textArea.setText(TextResource.getString("ImportOne.txt"));

        valid.bind(accountComboBox.valueProperty().isNotNull());

        updateDescriptor();

        accountComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateDescriptor();
        });
    }

    @Override
    public void putSettings(final Map<ImportWizard.Settings, Object> map) {
        map.put(ImportWizard.Settings.ACCOUNT, accountComboBox.getValue());
    }

    @Override
    public void getSettings(final Map<ImportWizard.Settings, Object> map) {
        if (map.get(ImportWizard.Settings.ACCOUNT) != null) {
            accountComboBox.setValue((Account) map.get(ImportWizard.Settings.ACCOUNT));
        }

        updateDescriptor();
    }

    @Override
    public boolean isPaneValid() {
        return valid.getValue();
    }

    @Override
    public String toString() {
        return "1. " + resources.getString("Title.SelDestAccount");
    }
}
