package jgnash.uifx.report;

import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

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
    private CheckBox percentValuesCheckBox;

    @FXML
    private CheckBox emptyAccountCheckBox;

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
        accountComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateChart();
            }
        });

        pieChart.setLegendVisible(false);

        updateChart();
    }

    private void updateChart() {
        ObservableList<PieChart.Data> pieChartData =
                FXCollections.observableArrayList(
                        new PieChart.Data("Grapefruit", 13),
                        new PieChart.Data("Oranges", 25),
                        new PieChart.Data("Plums", 10),
                        new PieChart.Data("Pears", 22),
                        new PieChart.Data("Apples", 30));

        pieChart.setData(pieChartData);
        pieChart.setTitle(accountComboBox.getValue().getName());
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parentProperty.get().getWindow()).close();
    }
}
