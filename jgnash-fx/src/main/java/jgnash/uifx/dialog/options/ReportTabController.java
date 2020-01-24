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

import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

import jgnash.report.pdf.FontRegistry;
import jgnash.report.pdf.ReportFactory;
import jgnash.uifx.Options;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Controller for Report Options.
 *
 * @author Craig Cavanaguh
 */
public class ReportTabController {

    @FXML
    private CheckBox rememberLastReportDateCheckBox;

    @FXML
    private ComboBox<String> headerFontComboBox;

    @FXML
    private ComboBox<String> monoFontComboBox;

    @FXML
    private ComboBox<String> proportionalFontComboBox;

    @FXML
    private void initialize() {
        rememberLastReportDateCheckBox.selectedProperty().bindBidirectional(Options.restoreReportDateProperty());

        new Thread(() -> {
            final List<String> fonts = FontRegistry.getFontList();

            JavaFXUtils.runLater(() -> {

                monoFontComboBox.getItems().setAll(fonts);
                monoFontComboBox.setValue(ReportFactory.getMonoFont());

                proportionalFontComboBox.getItems().setAll(fonts);
                proportionalFontComboBox.setValue(ReportFactory.getProportionalFont());

                headerFontComboBox.getItems().setAll(fonts);
                headerFontComboBox.setValue(ReportFactory.getHeaderFont());

                monoFontComboBox.setOnAction(event -> ReportFactory.setMonoFont(monoFontComboBox.getValue()));

                proportionalFontComboBox.setOnAction(event ->
                        ReportFactory.setProportionalFont(proportionalFontComboBox.getValue()));

                headerFontComboBox.setOnAction(event -> ReportFactory.setHeaderFont(headerFontComboBox.getValue()));
            });
        }).start();
    }
}
