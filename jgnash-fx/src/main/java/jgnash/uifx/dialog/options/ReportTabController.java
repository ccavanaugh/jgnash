/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
import javafx.scene.control.ComboBox;

import jgnash.report.pdf.FontRegistry;
import jgnash.report.pdf.ReportFactory;

/**
 * Controller for Report Options.
 *
 * @author Craig Cavanaguh
 */
public class ReportTabController {

    @FXML
    private ComboBox<String> headerFontComboBox;

    @FXML
    private ComboBox<String> monoFontComboBox;

    @FXML
    private ComboBox<String> proportionalFontComboBox;

    @FXML
    private void initialize() {
        monoFontComboBox.getItems().setAll(FontRegistry.getFontList());
        proportionalFontComboBox.getItems().setAll(FontRegistry.getFontList());
        headerFontComboBox.getItems().setAll(FontRegistry.getFontList());

        monoFontComboBox.setValue(ReportFactory.getMonoFont());
        proportionalFontComboBox.setValue(ReportFactory.getProportionalFont());
        headerFontComboBox.setValue(ReportFactory.getHeaderFont());

        monoFontComboBox.setOnAction(event -> ReportFactory.setMonoFont(monoFontComboBox.getValue()));

        proportionalFontComboBox.setOnAction(event ->
                ReportFactory.setProportionalFont(proportionalFontComboBox.getValue()));

        headerFontComboBox.setOnAction(event -> ReportFactory.setHeaderFont(headerFontComboBox.getValue()));
    }
}
