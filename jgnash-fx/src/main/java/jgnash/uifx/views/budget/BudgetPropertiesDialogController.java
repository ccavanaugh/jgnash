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
package jgnash.uifx.views.budget;

import java.math.RoundingMode;
import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.budget.Budget;
import jgnash.resource.util.MonthName;
import jgnash.resource.util.ResourceUtils;
import jgnash.time.Period;
import jgnash.uifx.Options;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.util.NotNull;

/**
 * Controller for budget properties.
 *
 * @author Craig Cavanaugh
 */
public class BudgetPropertiesDialogController {

    @FXML
    private ButtonBar buttonBar;

    @FXML
    private CheckBox incomeCheckBox;

    @FXML
    private CheckBox expenseCheckBox;

    @FXML
    private CheckBox assetCheckBox;

    @FXML
    private CheckBox liabilityCheckBox;

    @FXML
    private ComboBox<Period> periodComboBox;

    @FXML
    private TextField descriptionTextField;

    @FXML
    private ComboBox<RoundMode> roundingMethodComboBox;

    @FXML
    private Spinner<Integer> scaleSpinner;

    @FXML
    private ComboBox<MonthName> startMonthComboBox;

    private Budget budget;

    @FXML
    private void initialize() {
        periodComboBox.getItems().setAll(Period.values());
        startMonthComboBox.getItems().setAll(MonthName.values());

        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        int maxScale = 0;   // max scale is a function of available currencies

        for (final CurrencyNode currencyNode : engine.getCurrencies()) {
            maxScale = Math.max(maxScale, currencyNode.getScale());
        }

        roundingMethodComboBox.setCellFactory(param -> new RoundModeListCell());

        scaleSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-10, maxScale, maxScale, 1));

        roundingMethodComboBox.getItems().setAll(RoundMode.values());
        roundingMethodComboBox.setValue(RoundMode.FLOOR);

        startMonthComboBox.setValue(MonthName.JANUARY);
    }

    @FXML
    public void handleOkayAction() {

        buttonBar.buttonOrderProperty().bind(Options.buttonOrderProperty());

        boolean modified = false;

        if (budget.getBudgetPeriod() != periodComboBox.getValue()) {
            modified = true;
            budget.setBudgetPeriod(periodComboBox.getValue());
        }

        if (!descriptionTextField.getText().isEmpty() && !budget.getDescription().equals(descriptionTextField.getText())) {
            modified = true;
            budget.setDescription(descriptionTextField.getText());
        }

        if (assetCheckBox.isSelected() != budget.areAssetAccountsIncluded()) {
            modified = true;
            budget.setAssetAccountsIncluded(assetCheckBox.isSelected());
        }

        if (incomeCheckBox.isSelected() != budget.areIncomeAccountsIncluded()) {
            modified = true;
            budget.setIncomeAccountsIncluded(incomeCheckBox.isSelected());
        }

        if (expenseCheckBox.isSelected() != budget.areExpenseAccountsIncluded()) {
            modified = true;
            budget.setExpenseAccountsIncluded(expenseCheckBox.isSelected());
        }

        if (liabilityCheckBox.isSelected() != budget.areLiabilityAccountsIncluded()) {
            modified = true;
            budget.setLiabilityAccountsIncluded(liabilityCheckBox.isSelected());
        }

        if (scaleSpinner.getValue().byteValue() != budget.getRoundingScale()) {
            modified = true;
            budget.setRoundingScale(scaleSpinner.getValue().byteValue());
        }

        if (roundingMethodComboBox.getValue().roundingMode != budget.getRoundingMode()) {
            modified = true;
            budget.setRoundingMode(roundingMethodComboBox.getValue().roundingMode);
        }

        if (startMonthComboBox.getValue().getMonth() != budget.getStartMonth()) {
            modified = true;
            budget.setStartMonth(startMonthComboBox.getValue().getMonth());
        }

        if (modified) {
            final Thread thread = new Thread(() -> {
                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);

                engine.updateBudget(budget);
            });

            thread.start();
        }

        ((Stage) incomeCheckBox.getScene().getWindow()).close();
    }

    @FXML
    public void handleCloseAction() {
        ((Stage) incomeCheckBox.getScene().getWindow()).close();
    }

    void setBudget(@NotNull final Budget budget) {

        Objects.requireNonNull(budget);

        this.budget = budget;

        JavaFXUtils.runLater(() -> {
            descriptionTextField.setText(budget.getDescription());
            periodComboBox.setValue(budget.getBudgetPeriod());

            assetCheckBox.setSelected(budget.areAssetAccountsIncluded());
            incomeCheckBox.setSelected(budget.areIncomeAccountsIncluded());
            expenseCheckBox.setSelected(budget.areExpenseAccountsIncluded());
            liabilityCheckBox.setSelected(budget.areLiabilityAccountsIncluded());
            scaleSpinner.getValueFactory().setValue((int) budget.getRoundingScale());
            roundingMethodComboBox.setValue(RoundMode.valueOf(budget.getRoundingMode()));
            startMonthComboBox.setValue(MonthName.valueOf(budget.getStartMonth()));
        });
    }

    private enum RoundMode {

        CEILING(RoundingMode.CEILING, ResourceUtils.getString("RoundingMode.Ceiling.Name"), ResourceUtils.getString("RoundingMode.Ceiling.Description")),
        DOWN(RoundingMode.DOWN, ResourceUtils.getString("RoundingMode.Down.Name"), ResourceUtils.getString("RoundingMode.Down.Description")),
        HALF_DOWN(RoundingMode.HALF_DOWN, ResourceUtils.getString("RoundingMode.HalfDown.Name"), ResourceUtils.getString("RoundingMode.HalfDown.Description")),
        HALF_EVEN(RoundingMode.HALF_EVEN, ResourceUtils.getString("RoundingMode.HalfEven.Name"), ResourceUtils.getString("RoundingMode.HalfEven.Description")),
        HALF_UP(RoundingMode.HALF_UP, ResourceUtils.getString("RoundingMode.HalfUp.Name"), ResourceUtils.getString("RoundingMode.HalfUp.Description")),
        FLOOR(RoundingMode.FLOOR, ResourceUtils.getString("RoundingMode.Floor.Name"), ResourceUtils.getString("RoundingMode.Floor.Description")),
        UP(RoundingMode.UP, ResourceUtils.getString("RoundingMode.Up.Name"), ResourceUtils.getString("RoundingMode.Up.Description"));

        private final RoundingMode roundingMode;
        private final String name;
        private final String description;

        RoundMode(final RoundingMode roundingMode, final String name, final String description) {
            this.roundingMode = roundingMode;
            this.description = description;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static RoundMode valueOf(final RoundingMode roundingMode) {
            switch (roundingMode) {
                case CEILING:
                    return RoundMode.CEILING;
                case DOWN:
                    return RoundMode.DOWN;
                case HALF_DOWN:
                    return RoundMode.HALF_DOWN;
                case HALF_EVEN:
                    return RoundMode.HALF_EVEN;
                case HALF_UP:
                    return RoundMode.HALF_UP;
                case FLOOR:
                    return RoundMode.FLOOR;
                case UP:
                    return RoundMode.UP;
                default:
                    throw new IllegalArgumentException("argument out of range");
            }
        }
    }

    private static class RoundModeListCell extends ListCell<RoundMode> {

        @Override
        public void updateItem(final RoundMode item, final boolean empty) {

            super.updateItem(item, empty);  // required

            if (!empty && item != null) {
                setText(item.name);
                setTooltip(new Tooltip(item.description));
            } else {
                setText("");
                setTooltip(null);
            }
        }
    }
}
