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

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import jgnash.plugin.FxPlugin;
import jgnash.plugin.PluginFactory;
import jgnash.uifx.util.InjectFXML;

/**
 * Controller for application options.
 *
 * @author Craig Cavanaugh
 */
public class OptionDialogController {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private TabPane tabPane;

    @FXML
    void initialize() {

        // Load the plugin tabs into the dialog
        Platform.runLater(() -> PluginFactory.getPlugins().stream().filter(plugin -> plugin instanceof FxPlugin)
                .forEachOrdered(plugin -> {
                    final Node tab = ((FxPlugin) plugin).getOptionsNode();
                    if (tab != null) {
                        tabPane.getTabs().add(new Tab(plugin.getName(), tab));
                    }
                }));
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parent.get().getWindow()).close();
    }
}
