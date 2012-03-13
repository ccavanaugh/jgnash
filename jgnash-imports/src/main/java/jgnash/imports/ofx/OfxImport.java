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
package jgnash.imports.ofx;

import java.util.List;

import jgnash.engine.Account;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionFactory;

/**
 * OfxImport utility methods
 * 
 * @author Craig Cavanaugh
 * @version $Id: OfxImport.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class OfxImport {

    public static void importTransactions(List<OfxTransaction> transactions, Account baseAccount) {
        assert transactions != null && baseAccount != null;

        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        for (OfxTransaction tran : transactions) {
            assert tran.account != null;

            if (tran.getState() == OfxTransaction.ImportState.NEW || tran.getState() == OfxTransaction.ImportState.NOTEQUAL) { // do not import matched transactions
                Transaction t;

                if (baseAccount.equals(tran.account)) { // single entry oTran
                    t = TransactionFactory.generateSingleEntryTransaction(baseAccount, tran.amount, tran.datePosted, false, tran.memo, tran.getName(), tran.checkNumber);
                } else { // double entry
                    if (tran.amount.signum() >= 0) {
                        t = TransactionFactory.generateDoubleEntryTransaction(baseAccount, tran.account, tran.amount.abs(), tran.datePosted, tran.memo, tran.getName(), tran.checkNumber);
                    } else {
                        t = TransactionFactory.generateDoubleEntryTransaction(tran.account, baseAccount, tran.amount.abs(), tran.datePosted, tran.memo, tran.getName(), tran.checkNumber);
                    }
                }

                // Set the import ID and add the oTran
                if (t != null) {
                    t.setFitid(tran.transactionID);
                    engine.addTransaction(t);
                }
            }
        }
    }

    public static Account matchAccount(OfxBank bank) {

        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        Account account = null;

        String number = bank.accountId;
        String symbol = bank.currency;

        CurrencyNode node = engine.getCurrency(symbol);

        if (node != null) {
            for (Account a : engine.getAccountList()) {
                if (a.getAccountNumber() != null && a.getAccountNumber().equals(number) && a.getCurrencyNode().equals(node)) {
                    account = a;
                    break;
                }
            }
        } else if (number != null) {
            for (Account a : engine.getAccountList()) {
                if (a.getAccountNumber().equals(number)) {
                    account = a;
                    break;
                }
            }
        }

        return account;
    }

    /*public static void matchTransactions(List<OfxTransaction> list, Account baseAccount) {
        GenericImport.matchTransactions(list, baseAccount);
    }*/

    private OfxImport() {
    }
}
