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
import jgnash.engine.EngineFactory;
import jgnash.engine.RootAccount;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import org.controlsfx.control.ButtonBar;

/**
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

        Button okButton = new Button(resources.getString("Button.Ok"));
        Button cancelButton = new Button(resources.getString("Button.Cancel"));

        buttonBar.addButton(okButton, ButtonBar.ButtonType.OK_DONE);
        buttonBar.addButton(cancelButton, ButtonBar.ButtonType.CANCEL_CLOSE);

        okButton.setOnAction(event -> {
            selectedAccount = treeView.getSelectionModel().getSelectedItem().getValue();
            ((Stage) okButton.getScene().getWindow()).close();
        });

        cancelButton.setOnAction(event -> {
            selectedAccount = null;  // clear selections
            ((Stage) okButton.getScene().getWindow()).close();
        });

        Platform.runLater(this::loadAccountTree);
    }

    public Account getSelectedAccount() {
        return selectedAccount;
    }

    protected void loadAccountTree() {
        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
            RootAccount r = EngineFactory.getEngine(EngineFactory.DEFAULT).getRootAccount();

            final TreeItem<Account> root = new TreeItem<>(r);
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
