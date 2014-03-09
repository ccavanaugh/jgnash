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
package jgnash.uifx.components;

import java.util.ResourceBundle;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.EngineFactory;
import jgnash.engine.RootAccount;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.util.Callback;

/**
 * Abstract controller for handling display of the account structure
 *
 * @author Craig Cavanaugh
 */
public abstract class AccountTreeController {

    private boolean incomeVisible = true;

    private boolean expenseVisible = true;

    private boolean accountVisible = true;

    private boolean hiddenVisible = true;

    final private Object lock = new Object();

    protected abstract TreeTableView<Account> getTreeTableView();

    protected abstract ResourceBundle getResources();

    protected void initializeTreeTableView() {
        TreeTableView<Account> treeTableView = getTreeTableView();

        TreeTableColumn<Account, String> column = new TreeTableColumn<>(getResources().getString("Column.Account"));
        column.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<Account, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<Account, String> param) {
                return new ReadOnlyStringWrapper(param.getValue().getValue().getName());
            }
        });

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
    private synchronized boolean isAccountVisible(Account a) {
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

    public synchronized void setHiddenVisible(boolean visible) {
        if (hiddenVisible != visible) {
            hiddenVisible = visible;
            reload();
        }
    }

    public synchronized void setIncomeVisible(boolean visible) {
        if (incomeVisible != visible) {
            incomeVisible = visible;
            reload();
        }
    }

    public synchronized void setExpenseVisible(boolean visible) {
        if (expenseVisible != visible) {
            expenseVisible = visible;
            reload();
        }
    }

    public synchronized void setAccountVisible(boolean visible) {
        if (accountVisible != visible) {
            accountVisible = visible;
            reload();
        }
    }

    public synchronized boolean getAccountVisible() {
        return accountVisible;
    }

    public synchronized boolean getExpenseVisible() {
        return expenseVisible;
    }

    public synchronized boolean getHiddenVisible() {
        return hiddenVisible;
    }

    public synchronized boolean getIncomeVisible() {
        return incomeVisible;
    }
}
