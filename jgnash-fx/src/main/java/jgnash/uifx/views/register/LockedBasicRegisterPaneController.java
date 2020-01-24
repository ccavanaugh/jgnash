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
package jgnash.uifx.views.register;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

/**
 * Basic Locked Register pane controller.
 *
 * @author Craig Cavanaugh
 */
public class LockedBasicRegisterPaneController extends RegisterPaneController {

    @FXML
    @Override
    void initialize() {
        super.initialize();

        // Load the register table
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("BasicRegisterTable.fxml"), resources);
            registerTablePane.getChildren().add(fxmlLoader.load());
            registerTableController.set(fxmlLoader.getController());
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
