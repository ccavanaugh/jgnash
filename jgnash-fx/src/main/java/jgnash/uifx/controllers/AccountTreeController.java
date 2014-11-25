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
import jgnash.engine.AccountType;
import jgnash.engine.Comparators;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.RootAccount;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

/**
 * Abstract controller for handling display of the account structure
 *
 * TODO Reference @see for column resize code hints
 *
 * @author Craig Cavanaugh
 *
 * @see com.sun.javafx.scene.control.skin.TreeTableViewSkin#resizeColumnToFitContent(javafx.scene.control.TreeTableColumn, int)
 */
public abstract class AccountTreeController implements Initializable, AccountTypeFilter, MessageListener {

    final private Object lock = new Object();

    protected abstract TreeTableView<Account> getTreeTableView();

    protected ResourceBundle resources;

    public void initialize(final URL location, final ResourceBundle resources) {
        this.resources = resources;

        initializeFilterPreferences();

        addDefaultColumn();

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM, MessageChannel.ACCOUNT);

        // Register invalidation listeners to force a reload
        accountTypesVisible.addListener(observable -> reload());
        expenseTypesVisible.addListener(observable -> reload());
        hiddenTypesVisible.addListener(observable -> reload());
        incomeTypesVisible.addListener(observable -> reload());
    }

    /**
     * Generates and adds the default tree column for the table
     */
    protected void addDefaultColumn() {

        TreeTableView<Account> treeTableView = getTreeTableView();

        // force resize policy for better default appearance
        treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

        TreeTableColumn<Account, String> column = new TreeTableColumn<>(resources.getString("Column.Account"));
        column.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getName()));

        treeTableView.getColumns().add(column);
    }

    protected void loadAccountTree() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            RootAccount r = engine.getRootAccount();

            final TreeItem<Account> root = new TreeItem<>(r);
            root.setExpanded(true);

            getTreeTableView().setRoot(root);
            loadChildren(root);
        } else {
            getTreeTableView().setRoot(null);
        }
    }

    private synchronized void loadChildren(final TreeItem<Account> parentItem) {

        synchronized (lock) {
            Account parent = parentItem.getValue();

            parent.getChildren(Comparators.getAccountByCode()).stream().filter(this::isAccountVisible).forEach(child -> {
                TreeItem<Account> childItem = new TreeItem<>(child);
                childItem.setExpanded(true);

                parentItem.getChildren().add(childItem);

                if (child.getChildCount() > 0) {
                    loadChildren(childItem);
                }
            });
        }
    }

    public synchronized void reload() {
        loadAccountTree();
    }

    /**
     * Determines if an account is visible
     *
     * @param a account to check for visibility
     * @return true is account should be displayed
     */
    private synchronized boolean isAccountVisible(final Account a) {
        AccountType type = a.getAccountType();
        if (type == AccountType.INCOME && getIncomeTypesVisible()) {
            if (!a.isVisible() && getHiddenTypesVisible() || a.isVisible()) {
                return true;
            }
        } else if (type == AccountType.EXPENSE && getExpenseTypesVisible()) {
            if (!a.isVisible() && getHiddenTypesVisible() || a.isVisible()) {
                return true;
            }
        } else if (type != AccountType.INCOME && type != AccountType.EXPENSE && getAccountTypesVisible()) {
            if (!a.isVisible() && getHiddenTypesVisible() || a.isVisible()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void messagePosted(final Message event) {

        Platform.runLater(() -> {
            switch (event.getEvent()) {
                case ACCOUNT_ADD:
                case ACCOUNT_MODIFY:
                case ACCOUNT_REMOVE:
                    reload();
                    break;
                case FILE_CLOSING:
                    MessageBus.getInstance().unregisterListener(this, MessageChannel.SYSTEM, MessageChannel.ACCOUNT);
                    break;
                default:
                    break;
            }
        });
    }

    protected Account getSelectedAccount() {
        final TreeItem<Account> treeItem = getTreeTableView().getSelectionModel().getSelectedItem();

        if (treeItem != null) {
            return treeItem.getValue();
        }

        return null;
    }
}
