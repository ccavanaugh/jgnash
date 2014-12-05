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
package jgnash.uifx.views.register;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;

/**
 * @author Craig Cavanaugh
 */
public class RegisterActions {

    private RegisterActions() {
        // Utility class
    }

    static void reconcileTransactionAction(final Account account, final Transaction transaction, final ReconciledState reconciled) {
        if (transaction != null) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            if (engine != null) {
                engine.setTransactionReconciled(transaction, account, reconciled);
            }
        }
    }
}
