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

import java.util.List;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionFactory;
import jgnash.imports.ofx.OfxTransaction;

/**
 * Generic import utility methods
 * 
 * @author Craig Cavanaugh
 * @author Arnout Engelen
 * @version $Id: GenericImport.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class GenericImport {

    public static void importTransactions(List<? extends ImportTransaction> transactions, Account baseAccount) {
        assert transactions != null && baseAccount != null;

        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        for (ImportTransaction tran : transactions) {
            assert tran.account != null;

            if (tran.getState() == ImportTransaction.ImportState.NEW || tran.getState() == ImportTransaction.ImportState.NOTEQUAL) { // do not import matched transactions
                Transaction t;

                if (baseAccount.equals(tran.account)) { // single entry oTran
                    t = TransactionFactory.generateSingleEntryTransaction(baseAccount, tran.amount, tran.datePosted, false, tran.memo, tran.payee, tran.checkNumber);
                } else { // double entry
                    if (tran.amount.signum() >= 0) {
                        t = TransactionFactory.generateDoubleEntryTransaction(baseAccount, tran.account, tran.amount.abs(), tran.datePosted, tran.memo, tran.payee, tran.checkNumber);
                    } else {
                        t = TransactionFactory.generateDoubleEntryTransaction(tran.account, baseAccount, tran.amount.abs(), tran.datePosted, tran.memo, tran.payee, tran.checkNumber);
                    }
                }

                // add the oTran
                if (t != null) {
                    // for now we don't have transaction id's
                    //t.setFitid(tran.transactionID);
                    engine.addTransaction(t);
                }
            }
        }
    }

    public static void matchTransactions(List<? extends ImportTransaction> list, Account baseAccount) {
        for (ImportTransaction oTran : list) {
            if (oTran instanceof OfxTransaction) {
                String fitid = ((OfxTransaction) oTran).transactionID;
                if (fitid != null && fitid.length() > 0) {
                    for (Transaction tran : baseAccount.getReadonlyTransactionList()) {
                        if (tran.getFitid() != null && tran.getFitid().equals(fitid)) {
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
