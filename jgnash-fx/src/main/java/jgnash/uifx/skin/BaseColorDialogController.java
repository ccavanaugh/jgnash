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
package jgnash.uifx.skin;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;
import javafx.stage.WindowEvent;

import jgnash.uifx.util.InjectFXML;

/**
 * Controller for selecting the base color for the Fx interface.
 *
 * @author Craig Cavanaugh
 */
public class BaseColorDialogController {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private ColorPicker accentColorPicker;

    @FXML
    private ColorPicker focusColorPicker;

    @FXML
    private ColorPicker colorPicker;

    @FXML
    void initialize() {
        accentColorPicker.setValue(ThemeManager.accentColorProperty().getValue());
        colorPicker.setValue(ThemeManager.baseColorProperty().getValue());
        focusColorPicker.setValue(ThemeManager.focusColorProperty().getValue());

        ThemeManager.accentColorProperty().bind(accentColorPicker.valueProperty());
        ThemeManager.baseColorProperty().bind(colorPicker.valueProperty());
        ThemeManager.focusColorProperty().bind(focusColorPicker.valueProperty());

        // Unbind  when the dialog closes
        parent.addListener((observable, oldValue, scene) -> {
            if (scene != null) {
                scene.windowProperty().addListener((observable1, oldValue1, window)
                        -> window.addEventHandler(WindowEvent.WINDOW_HIDING, event -> {
                    ThemeManager.accentColorProperty().unbind();
                    ThemeManager.baseColorProperty().unbind();
                    ThemeManager.focusColorProperty().unbind();
                }));
            }
        });
    }

    @FXML
    private void handleDefaultColorAction() {
        colorPicker.setValue(ThemeManager.getDefaultColor(ThemeManager.getCurrentTheme(), ThemeManager.BASE));
    }

    @FXML
    private void handleDefaultAccentColorAction() {
        accentColorPicker.setValue(ThemeManager.getDefaultColor(ThemeManager.getCurrentTheme(), ThemeManager.ACCENT));
    }

    @FXML
    private void handleDefaultFocusColorAction() {
        focusColorPicker.setValue(ThemeManager.getDefaultColor(ThemeManager.getCurrentTheme(), ThemeManager.FOCUS));
    }
}
