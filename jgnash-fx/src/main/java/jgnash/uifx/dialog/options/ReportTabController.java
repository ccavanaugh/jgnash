/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
import javafx.scene.text.Font;
import jgnash.ui.report.jasper.ReportFactory;

/**
 * Controller for Report Options.
 *
 * @author Craig Cavanaguh
 */
public class ReportTabController {

    @FXML
    private ComboBox<String> monoFontComboBox;

    @FXML
    private ComboBox<String> proportionalFontComboBox;

    @FXML
    private void initialize() {
        monoFontComboBox.getItems().setAll(Font.getFamilies());
        proportionalFontComboBox.getItems().setAll(Font.getFamilies());

        monoFontComboBox.setValue(ReportFactory.getMonoFont());
        proportionalFontComboBox.setValue(ReportFactory.getProportionalFont());

        monoFontComboBox.setOnAction(event -> ReportFactory.setMonoFont(monoFontComboBox.getValue()));

        proportionalFontComboBox.setOnAction(event ->
                ReportFactory.setProportionalFont(proportionalFontComboBox.getValue()));
    }
}
