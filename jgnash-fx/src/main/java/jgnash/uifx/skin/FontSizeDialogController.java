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
import javafx.scene.control.Slider;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;

import jgnash.uifx.util.InjectFXML;

/**
 * Font size dialog controller.
 *
 * @author Craig Cavanaugh
 */
public class FontSizeDialogController {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private Slider slider;

    @FXML
    void initialize() {
        // Match the current value so it's not reset
        slider.setValue(ThemeManager.fontScaleProperty().get() * 100);

        slider.labelFormatterProperty().set(new DoubleStringConverter());

        // Bind the font size to the slider
        ThemeManager.fontScaleProperty().bind(slider.valueProperty().divide(100));

        // Unbind the font size when the dialog closes
        parent.addListener((observable, oldValue, scene) -> {
            if (scene != null) {
                scene.windowProperty().addListener((observable1, oldValue1, window)
                        -> window.addEventHandler(WindowEvent.WINDOW_HIDING, event ->
                        ThemeManager.fontScaleProperty().unbind()));
            }
        });
    }

    private static class DoubleStringConverter extends StringConverter<Double> {
        @Override
        public String toString(Double object) {
            return String.format("%.1f%%", object);
        }

        @Override
        public Double fromString(String string) {
            return null;
        }
    }
}
