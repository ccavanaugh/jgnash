package jgnash.uifx.report;

import java.text.NumberFormat;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.text.CommodityFormat;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.util.InjectFXML;

/**
 * Income and Expense Pie Chart
 *
 * @author Craig Cavanaugh
 */
public class IncomeExpenseDialogController {

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private PieChart pieChart;

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private AccountComboBox accountComboBox;

    @FXML
    private ResourceBundle resources;

    @FXML
    public void initialize() {

        accountComboBox.showPlaceHoldersProperty().set(true);

        startDatePicker.setValue(endDatePicker.getValue().minusYears(1));

        final ChangeListener<Object> listener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateChart();
            }
        };

        accountComboBox.valueProperty().addListener(listener);
        startDatePicker.valueProperty().addListener(listener);
        endDatePicker.valueProperty().addListener(listener);

        pieChart.setLegendSide(Side.RIGHT);

        updateChart();
    }

    private void updateChart() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        Account a = accountComboBox.getValue();

        if (a != null) {
            final CurrencyNode defaultCurrency = a.getCurrencyNode();

            final NumberFormat numberFormat = CommodityFormat.getFullNumberFormat(defaultCurrency);

            final ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

            double total = a.getTreeBalance(startDatePicker.getValue(), endDatePicker.getValue(), defaultCurrency).doubleValue();

            for (final Account child : a.getChildren()) {
                double balance = child.getTreeBalance(startDatePicker.getValue(), endDatePicker.getValue(), defaultCurrency).doubleValue();

                if (balance > 0 || balance < 0) {
                    final String label = child.getName() + " - " + numberFormat.format(balance);
                    pieChartData.add(new PieChart.Data(label, balance / total * 100));
                }
            }

            pieChart.setData(pieChartData);

            final NumberFormat percentFormat = NumberFormat.getPercentInstance();
            percentFormat.setMaximumFractionDigits(1);
            percentFormat.setMinimumFractionDigits(1);

            // Install tooltips on the data after it has been added to the chart
            pieChart.getData().stream().forEach(data ->
                    Tooltip.install(data.getNode(), new Tooltip(percentFormat.format(data.getPieValue() / 100d))));

            final String title;

            // pick an appropriate title
            if (a.getAccountType() == AccountType.EXPENSE) {
                title = resources.getString("Title.PercentExpense");
            } else if (a.getAccountType() == AccountType.INCOME) {
                title = resources.getString("Title.PercentIncome");
            } else {
                title = resources.getString("Title.PercentDist");
            }

            pieChart.setTitle(title + " - " + accountComboBox.getValue().getName() + " - " + numberFormat.format(total));

            // abs() on all values won't work if children aren't of uniform sign,
            // then again, this chart is not right to display those trees
            //boolean negate = total != null && total.signum() < 0;
        } else {
            pieChart.setData(FXCollections.emptyObservableList());
            pieChart.setTitle("No Data");
        }
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parentProperty.get().getWindow()).close();
    }
}
