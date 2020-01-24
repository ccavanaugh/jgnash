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
package jgnash.uifx.views.recurring;

import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;

/**
 * Dialog Controller for Recurring transactions.
 *
 * @author Craig Cavanaugh
 */
public class RecurringDialogController {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private StackPane contentPane;

    @FXML
    private ResourceBundle resources;

    @FXML
    private void initialize() {
        contentPane.getChildren()
                .add(FXMLUtils.load(RecurringViewController.class.getResource("RecurringView.fxml"), resources));
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parent.get().getWindow()).close();
    }
}
