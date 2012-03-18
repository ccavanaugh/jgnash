/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import jgnash.engine.Account;

/**
 * This extends the DefaultTreeCellRenderer to make place holder accounts look disabled
 * 
 * @author Craig Cavanaugh
 *
 */
abstract class AbstractAccountEnabledTreeCellRenderer implements TreeCellRenderer {

    private final TreeCellRenderer delegate;

    AbstractAccountEnabledTreeCellRenderer(final TreeCellRenderer delegate) {
        this.delegate = delegate;
    }

    protected abstract boolean isAccountEnabled(Account a);

    @Override
    public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel, final boolean expanded, final boolean leaf, final int row, final boolean focus) {

        Component c = delegate.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focus);

        Account a = (Account) ((DefaultMutableTreeNode) value).getUserObject();

        if (isAccountEnabled(a)) {
            c.setEnabled(true);
        } else {
            c.setEnabled(false);
        }

        return c;
    }

    /**
     * Returns a selection model based on the returned value of isAccountEnabled
     * 
     * @return selection model
     */
    public DefaultTreeSelectionModel getSelectionModel() {
        return new SelectionModel();
    }

    /**
     * Selection model based on the returned value of isAccountEnabled. Depending on the result, the path may be
     * disabled from selection
     */
    private class SelectionModel extends DefaultTreeSelectionModel {

        private static final long serialVersionUID = -1484099294329591524L;

        public SelectionModel() {
            super();
            setSelectionMode(SINGLE_TREE_SELECTION);
        }

        /**
         * Overrides the super to prevent selection of place holder accounts
         * 
         * @param path new path to select
         */
        @Override
        public void setSelectionPath(TreePath path) {
            if (path != null) {
                Object o = path.getLastPathComponent();
                if (o != null) {
                    Account a = (Account) ((DefaultMutableTreeNode) o).getUserObject();
                    if (a != null && isAccountEnabled(a)) {
                        super.setSelectionPath(path);
                    }
                }
            } else {
                super.setSelectionPath(null);
            }
        }
    }
}
