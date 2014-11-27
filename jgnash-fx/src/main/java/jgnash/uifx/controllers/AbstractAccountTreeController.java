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

import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.uifx.utils.TreeSearch;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/**
 * Abstract Controller handling a {@code TreeView} of {@code Account}s
 *
 * @author Craig Cavanaugh
 */
public abstract class AbstractAccountTreeController implements MessageListener {

    private Account selectedAccount = null;

    protected abstract TreeView<Account> getTreeView();

    public void initialize() {
        getTreeView().setShowRoot(false);

        loadAccountTree();

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM, MessageChannel.ACCOUNT);

        getTreeView().getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<Account>>() {
            @Override
            public void changed(final ObservableValue<? extends TreeItem<Account>> observable, final TreeItem<Account> oldValue, final TreeItem<Account> newValue) {
                if (newValue != null) {
                    selectedAccount = newValue.getValue();
                }
            }
        });
    }

    /**
     * Determines account visibility
     *
     * @param account {@code Account} to determine visibility based on filter state
     * @return {@code true} if the {@code Account} should be visible
     */
    abstract protected boolean isAccountVisible(Account account);

    public Account getSelectedAccount() {
        return selectedAccount;
    }

    public void setSelectedAccount(final Account account) {
        final TreeItem<Account> treeItem = TreeSearch.findTreeItem(getTreeView().getRoot(), account);

        if (treeItem != null) {
            Platform.runLater(() -> getTreeView().getSelectionModel().select(treeItem));
        }
    }

    public void reload() {
        Platform.runLater(this::loadAccountTree);
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
        final Account parent = parentItem.getValue();

        parent.getChildren(Comparators.getAccountByCode()).stream().filter(this::isAccountVisible).forEach(child -> {
            final TreeItem<Account> childItem = new TreeItem<>(child);
            childItem.setExpanded(true);
            parentItem.getChildren().add(childItem);

            if (child.getChildCount() > 0) {
                loadChildren(childItem);
            }
        });
    }

    @Override
    public void messagePosted(final Message event) {
        switch (event.getEvent()) {
            case ACCOUNT_ADD:
            case ACCOUNT_MODIFY:
            case ACCOUNT_REMOVE:
                reload();
                break;
            case FILE_CLOSING:
                Platform.runLater(() -> getTreeView().setRoot(null));   // dump account references immediately
                MessageBus.getInstance().unregisterListener(this, MessageChannel.SYSTEM, MessageChannel.ACCOUNT);
                break;
            default:
                break;
        }
    }
}
