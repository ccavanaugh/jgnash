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
package jgnash.uifx.report;

import java.util.Objects;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.Options;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.util.InjectFXML;

/**
 * Income and Expense Bar Chart
 *
 * @author Craig Cavanaugh
 */
public class IncomeExpenseBarChartDialogController {

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private StackPane chartPane;

    @FXML
    private BarChart<String, Number> barChart;

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private DatePickerEx endDatePicker;

    @FXML
    private ResourceBundle resources;

    @FXML
    public void initialize() {

        // Respect animation preference
        barChart.animatedProperty().setValue(Options.animationsEnabledProperty().get());

        //final Preferences preferences = Preferences.userNodeForPackage(IncomeExpenseBarChartDialogController.class);

        startDatePicker.setValue(endDatePicker.getValue().minusYears(1));

        final ChangeListener<Object> listener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                Platform.runLater(this::updateChart);
            }
        };

        startDatePicker.valueProperty().addListener(listener);
        endDatePicker.valueProperty().addListener(listener);

        barChart.setLegendSide(Side.RIGHT);

        // Push the initial load to the end of the platform thread for better startup and nicer visual effect
        Platform.runLater(this::updateChart);
    }

    private void updateChart() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

    }

    @FXML
    private void handleSaveAction() {
        ChartUtilities.saveChart(barChart);
    }

    @FXML
    private void handleCopyToClipboard() {
        ChartUtilities.copyToClipboard(barChart);
    }

    @FXML
    private void handlePrintAction() {
        ChartUtilities.printChart(barChart);
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parentProperty.get().getWindow()).close();
    }
}
