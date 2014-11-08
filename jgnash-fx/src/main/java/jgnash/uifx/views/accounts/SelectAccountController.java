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
package jgnash.uifx.views.accounts;

import java.net.URL;
import java.util.ResourceBundle;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.utils.TreeSearch;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import javafx.scene.control.ButtonBar;

/**
 * Controller for selecting an account from a tree
 *
 * @author Craig Cavanaugh
 */
public class SelectAccountController implements Initializable {

    @FXML
    private TreeView<Account> treeView;

    @FXML
    private ButtonBar buttonBar;

    private Account selectedAccount = null;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        // Create and add the ok and cancel buttons to the button bar
        final Button okButton = new Button(resources.getString("Button.Ok"));
        final Button cancelButton = new Button(resources.getString("Button.Cancel"));

        ButtonBar.setButtonData(okButton, ButtonBar.ButtonData.OK_DONE);
        ButtonBar.setButtonData(cancelButton, ButtonBar.ButtonData.CANCEL_CLOSE);

        buttonBar.getButtons().addAll(okButton, cancelButton);

        okButton.setOnAction(event -> {
            if (treeView.getSelectionModel().getSelectedItem() != null) {
                selectedAccount = treeView.getSelectionModel().getSelectedItem().getValue();
            }
            ((Stage) okButton.getScene().getWindow()).close();
        });

        cancelButton.setOnAction(event -> {
            selectedAccount = null;  // clear selections
            ((Stage) cancelButton.getScene().getWindow()).close();
        });

        loadAccountTree();
    }

    public Account getSelectedAccount() {
        return selectedAccount;
    }

    public void setSelectedAccount(final Account account) {
       final TreeItem<Account> treeItem = TreeSearch.findTreeItem(treeView.getRoot(), account);

        if (treeItem != null) {
            Platform.runLater(() -> treeView.getSelectionModel().select(treeItem));
        }
    }

    protected void loadAccountTree() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            final TreeItem<Account> root = new TreeItem<>(engine.getRootAccount());
            root.setExpanded(true);

            treeView.setRoot(root);
            loadChildren(root);
        } else {
            treeView.setRoot(null);
        }
    }

    private synchronized void loadChildren(final TreeItem<Account> parentItem) {
        Account parent = parentItem.getValue();

        for (Account child : parent.getChildren()) {
            TreeItem<Account> childItem = new TreeItem<>(child);
            childItem.setExpanded(true);
            parentItem.getChildren().add(childItem);

            if (child.getChildCount() > 0) {
                loadChildren(childItem);
            }
        }
    }
}
