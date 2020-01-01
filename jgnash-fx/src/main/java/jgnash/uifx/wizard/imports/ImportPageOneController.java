/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
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
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import jgnash.convert.importat.DateFormat;
import jgnash.convert.importat.ImportBank;
import jgnash.convert.importat.qif.QifAccount;
import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.wizard.AbstractWizardPaneController;
import jgnash.resource.util.ResourceUtils;
import jgnash.resource.util.TextResource;

/**
 * Import Wizard, base account selection.
 *
 * @author Craig Cavanaugh
 */
public class ImportPageOneController extends AbstractWizardPaneController<ImportWizard.Settings> {

    private static final String DATE_FORMAT = "dateFormat";

    @FXML
    private Label dateFormatLabel;

    @FXML
    private ChoiceBox<DateFormat> dateFormatChoiceBox;

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

        accountComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateDescriptor());

        dateFormatChoiceBox.getItems().addAll(DateFormat.values());
        dateFormatChoiceBox.getSelectionModel().select(preferences.getInt(DATE_FORMAT, 0));

        // hide the controls if date format selection is not permitted
        dateFormatChoiceBox.disableProperty().bind(dateFormatSelectionEnabled.not());
        dateFormatChoiceBox.managedProperty().bind(dateFormatSelectionEnabled);
        dateFormatLabel.managedProperty().bind(dateFormatSelectionEnabled);
    }

    @Override
    public void putSettings(final Map<ImportWizard.Settings, Object> map) {
        map.put(ImportWizard.Settings.ACCOUNT, accountComboBox.getValue());

        if (!dateFormatChoiceBox.isDisabled()) {
            map.put(ImportWizard.Settings.DATE_FORMAT, dateFormatChoiceBox.getValue());

            preferences.putInt(DATE_FORMAT, dateFormatChoiceBox.getSelectionModel().getSelectedIndex());

            final ImportBank<?> bank = (ImportBank<?>) map.get(ImportWizard.Settings.BANK);

            if (bank instanceof QifAccount) {
                ((QifAccount) bank).setDateFormat(dateFormatChoiceBox.getValue());
            }
        }
    }

    @Override
    public void getSettings(final Map<ImportWizard.Settings, Object> map) {
        final ImportBank<?> bank = (ImportBank<?>) map.get(ImportWizard.Settings.BANK);

        // Filter the available account types to prevent import errors
        if (bank != null && bank.isInvestmentAccount()) {
            accountComboBox.setPredicate(AccountComboBox.getDefaultPredicate()
                    .and(account -> account.getAccountType().getAccountGroup() == AccountGroup.INVEST));
        } else {
            accountComboBox.setPredicate(AccountComboBox.getDefaultPredicate()
                    .and(account -> account.getAccountType() != AccountType.INCOME
                            && account.getAccountType() != AccountType.EXPENSE));
        }

        if (map.get(ImportWizard.Settings.ACCOUNT) != null) {
            accountComboBox.setAccountValue((Account) map.get(ImportWizard.Settings.ACCOUNT));
        }

        if (!dateFormatChoiceBox.isDisabled()) {
            if (bank instanceof QifAccount) {
                dateFormatChoiceBox.setValue(((QifAccount) bank).getDateFormat());
            }
        }

        updateDescriptor();
    }

    @Override
    public boolean isPaneValid() {
        return valid.getValue();
    }

    @Override
    public String toString() {
        return "1. " + ResourceUtils.getString("Title.SelDestAccount");
    }

    SimpleBooleanProperty dateFormatSelectionEnabled() {
        return dateFormatSelectionEnabled;
    }
}
