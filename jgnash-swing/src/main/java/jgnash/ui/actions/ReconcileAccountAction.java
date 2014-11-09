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
package jgnash.ui.actions;

import java.awt.EventQueue;

import jgnash.engine.Account;
import jgnash.ui.reconcile.ReconcileDialog;
import jgnash.ui.reconcile.ReconcileSettingsDialog;

/**
 * Reconcile action class
 *
 * @author Craig Cavanaugh
 */
public class ReconcileAccountAction {

    public static void reconcileAccount(final Account account) {
        if (account != null) {

            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ReconcileSettingsDialog dlg = new ReconcileSettingsDialog(account);

                    if (dlg.showDialog()) {
                        new ReconcileDialog(account, dlg.getStatementDate(), dlg.getOpeningBalance(), dlg.getClosingBalance()).setVisible(true);
                    }
                }
            });
        }
    }

    private ReconcileAccountAction() {
    }
}
