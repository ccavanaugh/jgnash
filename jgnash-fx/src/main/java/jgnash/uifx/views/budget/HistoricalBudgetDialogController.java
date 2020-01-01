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

import java.util.Objects;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetFactory;
import jgnash.time.Period;
import jgnash.uifx.Options;
import jgnash.uifx.StaticUIMethods;
import jgnash.resource.util.TextResource;

/**
 * A mini wizard for creating a new budget based on historical data.
 *
 * @author Craig Cavanaugh
 */
public class HistoricalBudgetDialogController {

    @FXML
    private ButtonBar buttonBar;

    @FXML
    private TextFlow textFlow;

    @FXML
    private TextField nameTextField;

    @FXML
    private ComboBox<Period> periodComboBox;

    @FXML
    private CheckBox roundupCheckBox;

    @FXML
    private Button okButton;

    @FXML
    private ResourceBundle resources;

    @FXML
    private void initialize() {
        buttonBar.buttonOrderProperty().bind(Options.buttonOrderProperty());

        periodComboBox.getItems().addAll(Period.values());
        periodComboBox.setValue(Period.MONTHLY);

        textFlow.getChildren().addAll(new Text(TextResource.getString("NewBudgetOne.txt")));

        okButton.disableProperty().bind(nameTextField.textProperty().isEmpty());
    }

    @FXML
    private void handleOkAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final Budget budget = BudgetFactory.buildAverageBudget(periodComboBox.getValue(), nameTextField.getText(),
                roundupCheckBox.isSelected());

        new Thread(() -> {
            if (!engine.addBudget(budget)) {
                StaticUIMethods.displayError(resources.getString("Message.Error.NewBudget"));
            }
        }).start();

        ((Stage) okButton.getScene().getWindow()).close();
    }

    @FXML
    private void handleCancelAction() {
        ((Stage) okButton.getScene().getWindow()).close();
    }
}
