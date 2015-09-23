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
import java.util.prefs.Preferences;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

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

    private static final String DATE_FORMAT = "dateFormat";

    @FXML
    private ChoiceBox<String> dateFormatChoiceBox;

    @FXML
    private TextFlow textFlow;

    @FXML
    private ResourceBundle resources;

    @FXML
    private AccountComboBox accountComboBox;

    private final SimpleBooleanProperty valid = new SimpleBooleanProperty(false);

    private final SimpleBooleanProperty dateFormatSelectionEnabled = new SimpleBooleanProperty(false);

    private final Preferences preferences = Preferences.userNodeForPackage(ImportPageOneController.class);

    @FXML
    private void initialize() {
        textFlow.getChildren().addAll(new Text(TextResource.getString("ImportOne.txt")));

        valid.bind(accountComboBox.valueProperty().isNotNull());

        updateDescriptor();

        accountComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateDescriptor();
        });

        dateFormatChoiceBox.getItems().addAll("mm/dd/yyyy", "dd/mm/yyyy");
        dateFormatChoiceBox.getSelectionModel().select(preferences.get(DATE_FORMAT, "mm/dd/yyyy"));

        dateFormatChoiceBox.disableProperty().bind(dateFormatSelectionEnabled.not());
    }

    @Override
    public void putSettings(final Map<ImportWizard.Settings, Object> map) {
        map.put(ImportWizard.Settings.ACCOUNT, accountComboBox.getValue());
        map.put(ImportWizard.Settings.DATE_FORMAT, dateFormatChoiceBox.getValue());
        preferences.put(DATE_FORMAT, dateFormatChoiceBox.getValue());
    }

    @Override
    public void getSettings(final Map<ImportWizard.Settings, Object> map) {
        if (map.get(ImportWizard.Settings.ACCOUNT) != null) {
            accountComboBox.setValue((Account) map.get(ImportWizard.Settings.ACCOUNT));
        }

        if (map.get(ImportWizard.Settings.DATE_FORMAT) != null) {
            dateFormatChoiceBox.setValue( (String)map.get(ImportWizard.Settings.DATE_FORMAT));
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

    SimpleBooleanProperty dateFormatSelectionEnabled() {
        return dateFormatSelectionEnabled;
    }
}
