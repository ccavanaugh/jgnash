/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import jgnash.engine.Account;

/**
 * Register pane controller
 *
 * @author Craig Cavanaugh
 */
public abstract class RegisterPaneController implements Initializable {

    @FXML
    protected Button newButton; // TODO Implement handler

    @FXML
    protected Button duplicateButton; // TODO Implement handler

    @FXML
    protected Button deleteButton;

    @FXML
    protected StackPane register;

    /**
     * Active account for the pane
     */
    private ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    protected RegisterTableController registerTableController;

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    @FXML
    private void handleDeleteAction() {
        registerTableController.deleteTransactions();
    }
}
