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
import javafx.scene.control.RadioButton;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ReconcileManager;
import jgnash.uifx.Options;

/**
 * Controller for Register Options.
 *
 * @author Craig Cavanaguh
 */
public class RegisterTabController {

    @FXML
    private CheckBox restoreLastRegisterTab;

    @FXML
    private CheckBox rememberLastTranDateCheckBox;

    @FXML
    private CheckBox confirmDeleteCheckBox;

    @FXML
    private RadioButton disableReconcileCheckBox;

    @FXML
    private RadioButton reconcileBothCheckBox;

    @FXML
    private RadioButton reconcileIncomeExpenseCheckBox;

    @FXML
    private CheckBox enableAutoCompleteCheckBox;

    @FXML
    private CheckBox caseSensitiveCheckBox;

    @FXML
    private CheckBox fuzzyMatchCheckBox;

    @FXML
    private void initialize() {

        confirmDeleteCheckBox.selectedProperty().bindBidirectional(Options.confirmOnTransactionDeleteProperty());
        rememberLastTranDateCheckBox.selectedProperty().bindBidirectional(Options.rememberLastDateProperty());

        fuzzyMatchCheckBox.disableProperty().bind(enableAutoCompleteCheckBox.selectedProperty().not());
        caseSensitiveCheckBox.disableProperty().bind(enableAutoCompleteCheckBox.selectedProperty().not());

        enableAutoCompleteCheckBox.selectedProperty().bindBidirectional(Options.useAutoCompleteProperty());
        fuzzyMatchCheckBox.selectedProperty().bindBidirectional(Options.useFuzzyMatchForAutoCompleteProperty());
        caseSensitiveCheckBox.selectedProperty().bindBidirectional(Options.autoCompleteIsCaseSensitiveProperty());

        restoreLastRegisterTab.selectedProperty().bindBidirectional(Options.restoreLastTabProperty());

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        if (engine != null) {
            if (ReconcileManager.isAutoReconcileDisabled()) {
                disableReconcileCheckBox.setSelected(true);
            } else if (ReconcileManager.getAutoReconcileBothSides()) {
                reconcileBothCheckBox.setSelected(true);
            } else {
                reconcileIncomeExpenseCheckBox.setSelected(true);
            }

            disableReconcileCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    ReconcileManager.setDoNotAutoReconcile();
                }
            });

            reconcileBothCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    ReconcileManager.setAutoReconcileBothSides(true);
                }
            });

            reconcileIncomeExpenseCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    ReconcileManager.setAutoReconcileIncomeExpense(true);
                }
            });

        } else {
            reconcileBothCheckBox.setDisable(true);
            reconcileIncomeExpenseCheckBox.setDisable(true);
            disableReconcileCheckBox.setDisable(true);
        }
    }
}
