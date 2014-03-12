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
import java.util.prefs.Preferences;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.EngineFactory;
import jgnash.engine.RootAccount;

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
public abstract class AccountTreeController implements Initializable, AccountTypeFilter {

    private static final String HIDDEN_VISIBLE = "HiddenVisible";
    private static final String EXPENSE_VISIBLE = "ExpenseVisible";
    private static final String INCOME_VISIBLE = "IncomeVisible";
    private static final String ACCOUNT_VISIBLE = "AccountVisible";

    private boolean incomeVisible = true;

    private boolean expenseVisible = true;

    private boolean accountVisible = true;

    private boolean hiddenVisible = true;

    final private Object lock = new Object();

    protected abstract TreeTableView<Account> getTreeTableView();

    public abstract Preferences getPreferences();

    protected ResourceBundle resources;

    public void initialize(final URL location, final ResourceBundle resources) {
        this.resources = resources;

        setAccountVisible(getPreferences().getBoolean(ACCOUNT_VISIBLE, true));
        setExpenseVisible(getPreferences().getBoolean(EXPENSE_VISIBLE, true));
        setHiddenVisible(getPreferences().getBoolean(HIDDEN_VISIBLE, true));
        setIncomeVisible(getPreferences().getBoolean(INCOME_VISIBLE, true));

        addDefaultColumn();
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
        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
            RootAccount r = EngineFactory.getEngine(EngineFactory.DEFAULT).getRootAccount();

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

            for (Account child : parent.getChildren()) {
                if (isAccountVisible(child)) {
                    TreeItem<Account> childItem = new TreeItem<>(child);
                    childItem.setExpanded(true);

                    parentItem.getChildren().add(childItem);

                    if (child.getChildCount() > 0) {
                        loadChildren(childItem);
                    }
                }
            }
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
        if (type == AccountType.INCOME && incomeVisible) {
            if (!a.isVisible() && hiddenVisible || a.isVisible()) {
                return true;
            }
        } else if (type == AccountType.EXPENSE && expenseVisible) {
            if (!a.isVisible() && hiddenVisible || a.isVisible()) {
                return true;
            }
        } else if (type != AccountType.INCOME && type != AccountType.EXPENSE && accountVisible) {
            if (!a.isVisible() && hiddenVisible || a.isVisible()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void setHiddenVisible(final boolean visible) {
        if (hiddenVisible != visible) {
            hiddenVisible = visible;
            getPreferences().putBoolean(HIDDEN_VISIBLE, visible);
            reload();
        }
    }

    @Override
    public synchronized void setIncomeVisible(final boolean visible) {
        if (incomeVisible != visible) {
            incomeVisible = visible;
            getPreferences().putBoolean(INCOME_VISIBLE, visible);
            reload();
        }
    }

    @Override
    public synchronized void setExpenseVisible(final boolean visible) {
        if (expenseVisible != visible) {
            expenseVisible = visible;
            getPreferences().putBoolean(EXPENSE_VISIBLE, visible);
            reload();
        }
    }

    @Override
    public synchronized void setAccountVisible(boolean visible) {
        if (accountVisible != visible) {
            accountVisible = visible;
            getPreferences().putBoolean(ACCOUNT_VISIBLE, visible);
            reload();
        }
    }

    @Override
    public synchronized boolean isAccountVisible() {
        return accountVisible;
    }

    @Override
    public synchronized boolean isExpenseVisible() {
        return expenseVisible;
    }

    @Override
    public synchronized boolean isHiddenVisible() {
        return hiddenVisible;
    }

    @Override
    public synchronized boolean isIncomeVisible() {
        return incomeVisible;
    }
}
