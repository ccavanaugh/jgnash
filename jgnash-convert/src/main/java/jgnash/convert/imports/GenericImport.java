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
package jgnash.convert.imports;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionFactory;
import jgnash.convert.imports.ofx.OfxTransaction;
import jgnash.util.DateUtils;

/**
 * Generic import utility methods
 * 
 * @author Craig Cavanaugh
 * @author Arnout Engelen
 */
public class GenericImport {

    public static void importTransactions(final List<? extends ImportTransaction> transactions, final Account baseAccount) {
        Objects.requireNonNull(transactions);
        Objects.requireNonNull(baseAccount);

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        for (final ImportTransaction tran : transactions) {
            Objects.requireNonNull(tran.account);

            if (tran.getState() == ImportTransaction.ImportState.NEW
                    || tran.getState() == ImportTransaction.ImportState.NOT_EQUAL) { // do not import matched transactions
                Transaction t;

                if (baseAccount.equals(tran.account)) { // single entry oTran
                    t = TransactionFactory.generateSingleEntryTransaction(baseAccount, tran.amount, tran.datePosted,
                            tran.memo, tran.payee, tran.checkNumber);
                } else { // double entry
                    if (tran.amount.signum() >= 0) {
                        t = TransactionFactory.generateDoubleEntryTransaction(baseAccount, tran.account,
                                tran.amount.abs(), tran.datePosted, tran.memo, tran.payee, tran.checkNumber);
                    } else {
                        t = TransactionFactory.generateDoubleEntryTransaction(tran.account, baseAccount,
                                tran.amount.abs(), tran.datePosted, tran.memo, tran.payee, tran.checkNumber);
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

    /**
     * Sets the match state of a list of imported transactions
     * 
     * @param list
     *            list of imported transactions
     * @param baseAccount
     *            account to perform match against
     */
    public static void matchTransactions(final List<? extends ImportTransaction> list, final Account baseAccount) {
        for (ImportTransaction oTran : list) {

            // amount must always match
            for (final Transaction tran : baseAccount.getSortedTransactionList()) {

                if (tran.getAmount(baseAccount).equals(oTran.amount)) { // amounts must always match

                    { // check for date match
                        Date startDate;
                        Date endDate;

                        // we have a user initiated date, use a smaller window
                        if ((oTran.dateUser != null)) {
                            startDate = DateUtils.addDays(oTran.dateUser, -1);
                            endDate = DateUtils.addDays(oTran.dateUser, 1);
                        } else { // use the posted date with a larger window
                            startDate = DateUtils.addDays(oTran.datePosted, -3);
                            endDate = DateUtils.addDays(oTran.datePosted, 3);
                        }

                        if (DateUtils.after(tran.getDate(), startDate) && DateUtils.before(tran.getDate(), endDate)) {
                            oTran.setState(OfxTransaction.ImportState.EQUAL);
                            break;
                        }
                    }

                    { // check for matching check number
                        String checkNumber = oTran.checkNumber;
                        if (checkNumber != null && !checkNumber.isEmpty()) {
                            if (tran.getNumber() != null && tran.getNumber().equals(checkNumber)) {
                                oTran.setState(OfxTransaction.ImportState.EQUAL);
                                break;
                            }
                        }

                    }

                    { // check for matching fitid number
                        if (oTran instanceof OfxTransaction) {
                            String id = ((OfxTransaction) oTran).transactionID;
                            if (id != null && !id.isEmpty()) {
                                if (tran.getFitid() != null && tran.getFitid().equals(id)) {
                                    oTran.setState(OfxTransaction.ImportState.EQUAL);
                                    break;
                                }
                            }
                        }
                    }

                    //oTran.setState(OfxTransaction.ImportState.NEW);
                }
            }
        }
    }

    private GenericImport() {
    }
}
