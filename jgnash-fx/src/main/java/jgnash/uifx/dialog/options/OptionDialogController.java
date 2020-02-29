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

import java.util.prefs.Preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import jgnash.plugin.FxPlugin;
import jgnash.plugin.PluginFactory;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Controller for application options.
 *
 * @author Craig Cavanaugh
 */
public class OptionDialogController {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    private static final String INDEX = "index";

    @FXML
    private TabPane tabPane;

    @FXML
    void initialize() {

        // Load the plugin tabs into the dialog
        JavaFXUtils.runLater(() -> PluginFactory.getPlugins().stream().filter(plugin -> plugin instanceof FxPlugin)
                .forEachOrdered(plugin -> {
                    final Node tab = ((FxPlugin) plugin).getOptionsNode();
                    if (tab != null) {
                        tabPane.getTabs().add(new Tab(plugin.getName(), tab));
                    }
                }));


        JavaFXUtils.runLater(() -> {
            final Preferences preferences = Preferences.userNodeForPackage(OptionDialogController.class);

            tabPane.getSelectionModel().select(preferences.getInt(INDEX, 0));

            tabPane.getSelectionModel()
                    .selectedIndexProperty().addListener(new WeakChangeListener<>((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    preferences.putInt(INDEX, newValue.intValue());
                }
            }));
        });
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parent.get().getWindow()).close();
    }
}
