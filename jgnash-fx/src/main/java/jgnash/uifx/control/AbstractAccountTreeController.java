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
package jgnash.uifx.control;

import java.util.Objects;
import java.util.TreeSet;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.util.TreeSearch;
import jgnash.util.Nullable;

/**
 * Abstract Controller handling a {@code TreeView} of {@code Account}s.
 *
 * @author Craig Cavanaugh
 */
public abstract class AbstractAccountTreeController implements MessageListener {

    private final ReadOnlyObjectWrapper<Account> selectedAccount = new ReadOnlyObjectWrapper<>();

    protected abstract TreeView<Account> getTreeView();

    /**
     * Determines account visibility.
     *
     * @param account {@code Account} to determine visibility based on filter state
     * @return {@code true} if the {@code Account} should be visible
     */
    protected abstract boolean isAccountVisible(Account account);

    protected abstract boolean isAccountSelectable(Account account);

    private final ObservableSet<Account> filteredAccounts = FXCollections.observableSet(new TreeSet<>());

    /**
     * Adds accounts to be excluded from the list of selectable accounts.
     *
     * @param filteredAccountsList collection of {@code Account} that should be excluded regardless
     *                             of {@code isAccountVisible()}
     */
    public void addExcludeAccounts(@Nullable final Account... filteredAccountsList) {
        Objects.requireNonNull(filteredAccounts);

        for (final Account account: filteredAccountsList != null ? filteredAccountsList : new Account[0]) {
            if (account != null) {
                filteredAccounts.add(account);
            }
        }
    }

    public void initialize() {
        getTreeView().setShowRoot(false);

        loadAccountTree();

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM, MessageChannel.ACCOUNT);

        getTreeView().getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        if (isAccountSelectable(newValue.getValue())) {
                            selectedAccount.set(newValue.getValue());
                        }
                    }
                });

        filteredAccounts.addListener((SetChangeListener<Account>) change -> reload());
    }

    public ReadOnlyObjectProperty<Account> getSelectedAccountProperty() {
        return selectedAccount.getReadOnlyProperty();
    }

    public void setSelectedAccount(final Account account) {
        final TreeItem<Account> treeItem = TreeSearch.findTreeItem(getTreeView().getRoot(), account);

        if (treeItem != null) {
            JavaFXUtils.runLater(() -> getTreeView().getSelectionModel().select(treeItem));
        }
    }

    public void reload() {
        JavaFXUtils.runLater(this::loadAccountTree);
    }

    private void loadAccountTree() {
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

        parent.getChildren(Comparators.getAccountByCode()).stream().filter(child ->
                !filteredAccounts.contains(child) && isAccountVisible(child)).forEach(child ->
        {
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
                JavaFXUtils.runLater(() -> getTreeView().setRoot(null));   // dump account references immediately
                MessageBus.getInstance().unregisterListener(this, MessageChannel.SYSTEM, MessageChannel.ACCOUNT);
                break;
            default:
                break;
        }
    }
}
