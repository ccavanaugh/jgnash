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
package jgnash.uifx.dialog.options;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.net.currency.CurrencyUpdateFactory;
import jgnash.net.security.UpdateFactory;
import jgnash.uifx.Options;

/**
 * Controller for Startup and Shutdown options.
 *
 * @author Craig Cavanaugh
 */
public class StartupShutdownTabController {

    @FXML
    private CheckBox createBackupsCheckBox;

    @FXML
    private CheckBox removeOldBackupsCheckBox;

    @FXML
    private Spinner<Integer> backupCountSpinner;

    @FXML
    private CheckBox updateCurrencies;

    @FXML
    private CheckBox updateSecurities;

    @FXML
    private CheckBox openLastCheckBox;

    @FXML
    private CheckBox checkForUpdatesCheckBox;

    @FXML
    private void initialize() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        backupCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 1, 1));

        if (engine != null) {
            backupCountSpinner.getValueFactory().setValue(engine.getRetainedBackupLimit());
            createBackupsCheckBox.setSelected(engine.createBackups());
            removeOldBackupsCheckBox.setSelected(engine.removeOldBackups());

            backupCountSpinner.valueProperty().addListener((observable, oldValue, newValue)
                    -> engine.setRetainedBackupLimit(newValue));

            createBackupsCheckBox.selectedProperty().addListener((observable, oldValue, newValue)
                    -> engine.setCreateBackups(newValue));

            removeOldBackupsCheckBox.selectedProperty().addListener((observable, oldValue, newValue)
                    -> engine.setRemoveOldBackups(newValue));
        } else {
            backupCountSpinner.setDisable(true);
            createBackupsCheckBox.setDisable(true);
            removeOldBackupsCheckBox.setDisable(true);
            updateCurrencies.setDisable(true);
            updateSecurities.setDisable(true);
        }

        updateSecurities.setSelected(UpdateFactory.getUpdateOnStartup());
        updateCurrencies.setSelected(CurrencyUpdateFactory.getUpdateOnStartup());

        updateSecurities.selectedProperty().addListener((observable, oldValue, newValue)
                -> UpdateFactory.setUpdateOnStartup(newValue));

        updateCurrencies.selectedProperty().addListener((observable, oldValue, newValue)
                -> CurrencyUpdateFactory.setUpdateOnStartup(newValue));


        openLastCheckBox.selectedProperty().bindBidirectional(Options.openLastProperty());

        checkForUpdatesCheckBox.selectedProperty().bindBidirectional(Options.checkForUpdatesProperty());
    }
}
