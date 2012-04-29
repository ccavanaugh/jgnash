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
package jgnash.imports;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.imports.ofx.OfxTransaction;

import java.util.List;

/**
 * Generic import utility methods
 * 
 * @author Craig Cavanaugh
 * @author Arnout Engelen
 */
public class GenericImport {

    public static void matchTransactions(final List<? extends ImportTransaction> list, final Account baseAccount) {
        for (ImportTransaction oTran : list) {
            if (oTran instanceof OfxTransaction) {
                String id = ((OfxTransaction) oTran).transactionID;
                if (id != null && id.length() > 0) {
                    for (Transaction tran : baseAccount.getReadonlyTransactionList()) {
                        if (tran.getFitid() != null && tran.getFitid().equals(id)) {
                            oTran.setState(OfxTransaction.ImportState.EQUAL);
                            break;
                        }
                    }
                }
            }

            String checkNumber = oTran.checkNumber;
            if (checkNumber != null && checkNumber.length() > 0) {
                for (Transaction tran : baseAccount.getReadonlyTransactionList()) {
                    if (tran.getNumber() != null && tran.getNumber().equals(checkNumber)) {
                        oTran.setState(OfxTransaction.ImportState.EQUAL);
                        break;
                    }
                }
            }
        }
    }

    private GenericImport() {
    }
}
