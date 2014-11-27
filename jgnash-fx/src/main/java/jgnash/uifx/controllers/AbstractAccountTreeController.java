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
package jgnash.uifx.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.utils.TreeSearch;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/**
 * Abstract Controller handling a {@code TreeView} of {@code Accounts}s
 *
 * @author Craig Cavanaugh
 */
public abstract class AbstractAccountTreeController implements Initializable {

    private Account selectedAccount = null;

    protected abstract TreeView<Account> getTreeView();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        getTreeView().setShowRoot(false);
        loadAccountTree();

        getTreeView().getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<Account>>() {
            @Override
            public void changed(final ObservableValue<? extends TreeItem<Account>> observable, final TreeItem<Account> oldValue, final TreeItem<Account> newValue) {
                selectedAccount = newValue.getValue();
            }
        });
    }

    public Account getSelectedAccount() {
        return selectedAccount;
    }

    public void setSelectedAccount(final Account account) {
       final TreeItem<Account> treeItem = TreeSearch.findTreeItem(getTreeView().getRoot(), account);

        if (treeItem != null) {
            Platform.runLater(() -> getTreeView().getSelectionModel().select(treeItem));
        }
    }

    protected void loadAccountTree() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            final TreeItem<Account> root = new TreeItem<>(engine.getRootAccount());
            root.setExpanded(true);

            getTreeView().setRoot(root);
            loadChildren(root);
        } else {
            getTreeView().setRoot(null);
        }
    }

    private synchronized void loadChildren(final TreeItem<Account> parentItem) {
        Account parent = parentItem.getValue();

        for (final Account child : parent.getChildren()) {
            final TreeItem<Account> childItem = new TreeItem<>(child);
            childItem.setExpanded(true);
            parentItem.getChildren().add(childItem);

            if (child.getChildCount() > 0) {
                loadChildren(childItem);
            }
        }
    }
}
