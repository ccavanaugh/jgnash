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

import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

import jgnash.uifx.Options;

/**
 * Controller for General Options.
 *
 * @author Craig Cavanaugh
 */
public class GeneralTabController {

    @FXML
    private CheckBox filterRegexEnabledCheckBox;

    @FXML
    private RadioButton windowsStyleRadioButton;

    @FXML
    private RadioButton macOSStyleRadioButton;

    @FXML
    private RadioButton linuxStyleRadioButton;

    @FXML
    private CheckBox animationsEnabledCheckBox;

    @FXML
    private CheckBox selectOnFocusCheckBox;

    @FXML
    private CheckBox autoPackRegisterTableCheckBox;

    @FXML
    private ToggleGroup toggleGroup;

    @FXML
    private void initialize() {
        selectOnFocusCheckBox.selectedProperty().bindBidirectional(Options.selectOnFocusProperty());
        animationsEnabledCheckBox.selectedProperty().bindBidirectional(Options.animationsEnabledProperty());
        filterRegexEnabledCheckBox.selectedProperty().bindBidirectional(Options.regexForFiltersProperty());

        autoPackRegisterTableCheckBox.selectedProperty().bindBidirectional(Options.autoPackTablesProperty());

        switch (Options.buttonOrderProperty().get()) {
            case ButtonBar.BUTTON_ORDER_LINUX:
                linuxStyleRadioButton.setSelected(true);
                break;
            case ButtonBar.BUTTON_ORDER_WINDOWS:
            default:
                windowsStyleRadioButton.setSelected(true);
                break;
            case ButtonBar.BUTTON_ORDER_MAC_OS:
                macOSStyleRadioButton.setSelected(true);
                break;
        }

        toggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == linuxStyleRadioButton) {
                Options.buttonOrderProperty().set(ButtonBar.BUTTON_ORDER_LINUX);
            } else if (newValue == macOSStyleRadioButton) {
                Options.buttonOrderProperty().set(ButtonBar.BUTTON_ORDER_MAC_OS);
            } else {
                Options.buttonOrderProperty().set(ButtonBar.BUTTON_ORDER_WINDOWS);
            }
        });
    }
}
