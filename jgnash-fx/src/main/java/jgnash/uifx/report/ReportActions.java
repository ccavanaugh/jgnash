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

import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.stage.Stage;

import jgnash.uifx.util.FXMLUtils;
import jgnash.util.ResourceUtils;

/**
 * Centralize report actions here
 *
 * @author Craig Cavanaugh
 */
public class ReportActions {

    public static void displayIncomeExpensePieChart() {
        ResourceBundle resources = ResourceUtils.getBundle();

        final Stage stage = FXMLUtils.loadFXML(IncomeExpenseDialogController.class.getResource("IncomeExpenseDialog.fxml"),
                resources);

        stage.setTitle(resources.getString("Title.IncomeExpenseChart"));

        stage.setOnShown(event -> Platform.runLater(() -> {
            stage.setMinHeight(stage.getHeight());
            stage.setMinWidth(stage.getWidth());
        }));

        stage.show();
    }
}
