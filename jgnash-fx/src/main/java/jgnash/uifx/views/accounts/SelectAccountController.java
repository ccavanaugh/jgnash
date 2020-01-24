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
package jgnash.uifx.views.accounts;

import java.util.Optional;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.uifx.control.AbstractAccountTreeController;
import jgnash.util.NotNull;

/**
 * Controller for selecting an account from a tree.
 *
 * @author Craig Cavanaugh
 */
public class SelectAccountController extends AbstractAccountTreeController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private TreeView<Account> treeView;

    @FXML
    private ButtonBar buttonBar;

    private Account selectedAccount = null;

    @Override
    protected TreeView<Account> getTreeView() {
        return treeView;
    }

    @Override
    protected boolean isAccountVisible(final Account account) {
        return true;
    }

    @Override
    protected boolean isAccountSelectable(final Account account) {
        return true;
    }

    @FXML
    public void initialize() {
        super.initialize();

        getTreeView().setShowRoot(true);

        // Create and add the ok and cancel buttons to the button bar
        final Button okButton = new Button(resources.getString("Button.Ok"));
        final Button cancelButton = new Button(resources.getString("Button.Cancel"));

        ButtonBar.setButtonData(okButton, ButtonBar.ButtonData.OK_DONE);
        ButtonBar.setButtonData(cancelButton, ButtonBar.ButtonData.CANCEL_CLOSE);

        buttonBar.getButtons().addAll(okButton, cancelButton);

        okButton.setOnAction(event -> ((Stage) okButton.getScene().getWindow()).close());

        cancelButton.setOnAction(event -> {
            selectedAccount = null;  // clear selections
            ((Stage) cancelButton.getScene().getWindow()).close();
        });

        getSelectedAccountProperty().addListener((observable, oldValue, newValue) -> selectedAccount = newValue);
    }

    @NotNull
    public Optional<Account> getSelectedAccount() {
        return Optional.ofNullable(selectedAccount);
    }
}
