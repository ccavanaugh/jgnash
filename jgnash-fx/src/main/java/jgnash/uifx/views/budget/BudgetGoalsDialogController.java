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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.CurrencyNode;
import jgnash.engine.MathConstants;
import jgnash.engine.budget.BudgetFactory;
import jgnash.engine.budget.BudgetGoal;
import jgnash.time.Period;
import jgnash.engine.budget.BudgetPeriodDescriptor;
import jgnash.engine.budget.BudgetPeriodDescriptorFactory;
import jgnash.engine.budget.Pattern;
import jgnash.uifx.Options;
import jgnash.uifx.control.BigDecimalTableCell;
import jgnash.uifx.control.DecimalTextField;

/**
 * Controller for budget goals.
 *
 * The startMonth property must be set before account and working year property are assigned.
 *
 * @author Craig Cavanaugh
 */
public class BudgetGoalsDialogController {

    @FXML
    private ButtonBar buttonBar;

    @FXML
    private DecimalTextField fillPatternAmountDecimalTextField;

    @FXML
    private Spinner<Integer> endRowSpinner;

    @FXML
    private Spinner<Integer> startRowSpinner;

    @FXML
    private ComboBox<Pattern> patternComboBox;

    @FXML
    private DecimalTextField fillAllDecimalTextField;

    @FXML
    private TableView<BudgetPeriodDescriptor> goalTable;

    @FXML
    private Label currencyLabel;

    @FXML
    private ComboBox<Period> periodComboBox;

    private BudgetGoal result = null;

    @FXML
    private ResourceBundle resources;

    private final SimpleObjectProperty<Account> account = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<BudgetGoal> budgetGoal = new SimpleObjectProperty<>();

    private final IntegerProperty workingYear = new SimpleIntegerProperty();

    private final IntegerProperty descriptorSize = new SimpleIntegerProperty();

    private final ObjectProperty<NumberFormat> numberFormat = new SimpleObjectProperty<>(NumberFormat.getInstance());

    private final SimpleObjectProperty<Month> startMonth = new SimpleObjectProperty<>();

    @FXML
    private void initialize() {
        buttonBar.buttonOrderProperty().bind(Options.buttonOrderProperty());

        endRowSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1));
        startRowSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1));

        periodComboBox.getItems().addAll(Period.values());

        patternComboBox.getItems().addAll(Pattern.values());
        patternComboBox.setValue(Pattern.EveryRow);

        fillAllDecimalTextField.emptyWhenZeroProperty().set(false);
        fillPatternAmountDecimalTextField.emptyWhenZeroProperty().set(false);

        fillAllDecimalTextField.setDecimal(BigDecimal.ZERO);
        fillPatternAmountDecimalTextField.setDecimal(BigDecimal.ZERO);

        goalTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        goalTable.setEditable(true);

        final TableColumn<BudgetPeriodDescriptor, String> periodColumn
                = new TableColumn<>(resources.getString("Column.Period"));
        periodColumn.setEditable(false);

        periodColumn.setCellValueFactory(param -> {
            if (param != null) {
                return new SimpleStringProperty(param.getValue().getPeriodDescription());
            }
            return new SimpleStringProperty("");
        });
        periodColumn.setSortable(false);

        goalTable.getColumns().add(periodColumn);

        final TableColumn<BudgetPeriodDescriptor, BigDecimal> amountColumn
                = new TableColumn<>(resources.getString("Column.Amount"));
        amountColumn.setEditable(true);
        amountColumn.setSortable(false);

        amountColumn.setCellValueFactory(param -> {
            if (param != null) {
                final BudgetPeriodDescriptor descriptor = param.getValue();
                final BigDecimal goal
                        = budgetGoal.get().getGoal(descriptor.getStartPeriod(), descriptor.getEndPeriod(),
                        descriptor.getStartDate().isLeapYear());

                return new SimpleObjectProperty<>(goal.setScale(accountProperty().get().getCurrencyNode().getScale(),
                        MathConstants.roundingMode));
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
        amountColumn.setCellFactory(cell -> new BigDecimalTableCell<>(numberFormat));

        amountColumn.setOnEditCommit(event -> {
            final BudgetPeriodDescriptor descriptor = event.getTableView().getItems()
                    .get(event.getTablePosition().getRow());

            budgetGoalProperty().get().setGoal(descriptor.getStartPeriod(), descriptor.getEndPeriod(),
                    event.getNewValue(), descriptor.getStartDate().isLeapYear());
        });

        goalTable.getColumns().add(amountColumn);

        periodComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                budgetGoalProperty().get().setBudgetPeriod(newValue);

                final List<BudgetPeriodDescriptor> descriptors = getDescriptors();

                goalTable.getItems().setAll(descriptors);
                descriptorSize.set(descriptors.size());
            }
        });

        budgetGoalProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                periodComboBox.setValue(newValue.getBudgetPeriod());
            }
        });

        // the spinner factory max values do not like being bound; Set value instead
        descriptorSize.addListener((observable, oldValue, newValue) -> {
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) endRowSpinner.getValueFactory())
                    .setMax(newValue.intValue());

            ((SpinnerValueFactory.IntegerSpinnerValueFactory) startRowSpinner.getValueFactory())
                    .setMax(newValue.intValue());

            endRowSpinner.getValueFactory().setValue(newValue.intValue());
        });

        // account has changed; update currency related properties
        accountProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                final CurrencyNode currencyNode = newValue.getCurrencyNode();

                currencyLabel.setText(currencyNode.getSymbol());

                fillAllDecimalTextField.scaleProperty().set(currencyNode.getScale());
                fillAllDecimalTextField.minScaleProperty().set(currencyNode.getScale());

                fillPatternAmountDecimalTextField.scaleProperty().set(currencyNode.getScale());
                fillPatternAmountDecimalTextField.minScaleProperty().set(currencyNode.getScale());

                final NumberFormat decimalFormat = NumberFormat.getInstance();
                if (decimalFormat instanceof DecimalFormat) {
                    decimalFormat.setMinimumFractionDigits(currencyNode.getScale());
                    decimalFormat.setMaximumFractionDigits(currencyNode.getScale());
                }

                numberFormat.set(decimalFormat);
            }
        });
    }

    public SimpleObjectProperty<Account> accountProperty() {
        return account;
    }

    /**
     * A working clone should be set instead of the base.  This property will be modified as needed.
     *
     * @return BudgetGoal property
     */
    SimpleObjectProperty<BudgetGoal> budgetGoalProperty() {
        return budgetGoal;
    }

    IntegerProperty workingYearProperty() {
        return workingYear;
    }

    SimpleObjectProperty<Month> startMonthProperty() {
        return startMonth;
    }

    public Optional<BudgetGoal> getResult() {
        return Optional.ofNullable(result);
    }

    private List<BudgetPeriodDescriptor> getDescriptors() {
        return BudgetPeriodDescriptorFactory.getDescriptors(workingYear.get(), startMonth.get(),
                budgetGoal.get().getBudgetPeriod());
    }

    @FXML
    private void handleHistoricalFill() {
        setBudgetGoal(BudgetFactory.buildAverageBudgetGoal(account.get(), getDescriptors(), true));
    }

    @FXML
    private void handleFillAllAction() {
        final BigDecimal fillAmount = fillAllDecimalTextField.getDecimal();

        for (final BudgetPeriodDescriptor descriptor : getDescriptors()) {
            budgetGoal.get().setGoal(descriptor.getStartPeriod(), descriptor.getEndPeriod(), fillAmount,
                    descriptor.getStartDate().isLeapYear());
        }

        goalTable.refresh();
    }

    @FXML
    private void handlePatternFillAction() {
        final BigDecimal fillAmount = fillPatternAmountDecimalTextField.getDecimal();

        final int startRow = startRowSpinner.getValue() - 1;
        final int endRow = endRowSpinner.getValue() - 1;

        final Pattern pattern = patternComboBox.getValue();

        setBudgetGoal(BudgetFactory.buildBudgetGoal(budgetGoal.get(), getDescriptors(), pattern, startRow,
                endRow, fillAmount));
    }

    private void setBudgetGoal(final BudgetGoal budgetGoal) {
        this.budgetGoal.set(budgetGoal);

        final List<BudgetPeriodDescriptor> descriptors = getDescriptors();

        goalTable.getItems().setAll(descriptors);
        descriptorSize.set(descriptors.size());

        goalTable.refresh();
    }

    @FXML
    private void handleOkayAction() {
        result = budgetGoal.get();
        ((Stage) periodComboBox.getScene().getWindow()).close();
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) periodComboBox.getScene().getWindow()).close();
    }
}
