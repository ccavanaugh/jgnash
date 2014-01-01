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

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.math.BigDecimal;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import jgnash.engine.Account;
import jgnash.ui.actions.ReconcileAccountAction;
import jgnash.ui.components.expandingtable.ExpandingTable;
import jgnash.ui.register.AccountBalanceDisplayManager;

/**
 * A JScrollPane containing a table and will expand and contract on selection
 *
 * @author Craig Cavanaugh
 *
 */
class ExpandingAccountTablePane extends JScrollPane {

    ExpandingAccountTableModel model = new ExpandingAccountTableModel();

    final JTable accountTable;

    public ExpandingAccountTablePane() {
        model = new ExpandingAccountTableModel();

        accountTable = new ExpandingAccountTable(model);

        setViewportView(accountTable);
    }

    public Account getSelectedAccount() {       
        return ((ExpandingAccountTable)accountTable).getSelectedObject();               
    }

    Account getSelectedAccount(final Point p) {        
        return ((ExpandingAccountTable)accountTable).getSelectedObject(p);                        
    }

    void setSelectedAccount(final Account account) {        
        ((ExpandingAccountTable)accountTable).setSelectedObject(account);               
    }   

    void reconcileAccount() {
        ReconcileAccountAction.reconcileAccount(getSelectedAccount());
    }

    private final class ExpandingAccountTable extends ExpandingTable<Account> {            

        public ExpandingAccountTable(ExpandingAccountTableModel model) {
            super(model);
        }

        /**
         * Override prepareRenderer instead of using a custom renderer so the look and feel is preserved
         *
         * @see javax.swing.JTable#prepareRenderer(javax.swing.table.TableCellRenderer, int, int)
         */
        @SuppressWarnings("MagicConstant")
        @Override
        public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {

            Component c = super.prepareRenderer(renderer, row, column);

            if (c instanceof JLabel) {              

                Account account = model.get(row);

                switch (column) {
                    case 0:
                        break;
                    case 2:
                        ((JLabel) c).setHorizontalAlignment(SwingConstants.RIGHT);

                        BigDecimal balance = AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), account.getTreeBalance());

                        if (balance.signum() < 0) {
                            c.setForeground(Color.RED);
                        } else {
                            c.setForeground(defaultForeground);
                        }
                        break;
                    case 3:
                        ((JLabel) c).setHorizontalAlignment(SwingConstants.RIGHT);

                        BigDecimal reconciledBalance = AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), account.getReconciledTreeBalance());

                        if (reconciledBalance.signum() < 0) {
                            c.setForeground(Color.RED);
                        } else {
                            c.setForeground(defaultForeground);
                        }
                        break;
                    default:
                        c.setForeground(defaultForeground);
                        ((JLabel) c).setHorizontalAlignment(defaultAlignment);
                        ((JLabel) c).setIcon(null);
                }

            }

            return c;
        }
    }
}
