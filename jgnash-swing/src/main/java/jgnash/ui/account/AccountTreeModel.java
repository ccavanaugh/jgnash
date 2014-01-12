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
package jgnash.ui.account;

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.EngineFactory;
import jgnash.engine.RootAccount;

/**
 * Account tree model
 * 
 * @author Craig Cavanaugh
 */
class AccountTreeModel extends DefaultTreeModel {

    private boolean incomeVisible = true;

    private boolean expenseVisible = true;

    private boolean accountVisible = true;

    private boolean hiddenVisible = true;

    final private Object lock = new Object();

    public AccountTreeModel() {
        super(null);

        loadAccountTree();
    }

    /**
     * Invoke this method if you've modified the TreeNodes upon which this model depends. The model will notify all of
     * its listeners that the model has changed.
     */
    @Override
    public synchronized void reload() {
        loadAccountTree();
    }

    private synchronized void loadAccountTree() {
        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
            RootAccount r = EngineFactory.getEngine(EngineFactory.DEFAULT).getRootAccount();

            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(r);

            setRoot(rootNode);
            loadChildren(rootNode);
            nodeStructureChanged(rootNode);
        } else {

            setRoot(null);
        }
    }

    private synchronized void loadChildren(DefaultMutableTreeNode parentNode) {

        synchronized (lock) {
            Account parent = (Account) parentNode.getUserObject();

            for (Account child : parent.getChildren()) {
                if (isAccountVisible(child)) {
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
                    insertNodeInto(childNode, parentNode, parentNode.getChildCount());
                    if (child.getChildCount() > 0) {
                        loadChildren(childNode);
                    }
                }
            }
        }
    }

    /**
     * Finds a DefaultMutableTreeNode given the account
     * 
     * @param account Account to find
     * @return DefaultMutableTreeNode if found, null otherwise
     */
    synchronized DefaultMutableTreeNode findAccountNode(Account account) {
        DefaultMutableTreeNode tAccount;

        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) getRoot();

        synchronized (lock) {
            // walk through the enumeration tree and find the account
            for (Enumeration<?> e = rootNode.depthFirstEnumeration(); e.hasMoreElements();) {
                tAccount = (DefaultMutableTreeNode) e.nextElement();
                if (tAccount.getUserObject().equals(account)) {
                    return tAccount;
                }
            }
        }
        return null;
    }

    synchronized void removeAccount(Account account) {
        synchronized (lock) {
            DefaultMutableTreeNode child = findAccountNode(account);
            if (child != null) {
                removeNodeFromParent(child);
            }
        }
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
