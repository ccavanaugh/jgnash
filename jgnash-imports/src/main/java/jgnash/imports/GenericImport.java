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
import jgnash.util.DateUtils;

import java.util.Date;
import java.util.List;

/**
 * Generic import utility methods
 *
 * @author Craig Cavanaugh
 * @author Arnout Engelen
 */
public class GenericImport {

    /**
     * Sets the match state of a list of imported transactions
     *
     * @param list list of imported transactions
     * @param baseAccount account to perform match against
     */
    public static void matchTransactions(final List<? extends ImportTransaction> list, final Account baseAccount) {
        for (ImportTransaction oTran : list) {

            // amount must always match
            for (Transaction tran : baseAccount.getReadonlyTransactionList()) {

                if (tran.getAmount(baseAccount).equals(oTran.amount)) { // amounts must always match

                    {   // check for date match
                        Date startDate;
                        Date endDate;

                        // we have a user initiated date, use a smaller window
                        if ((oTran.dateUser != null)) {
                            startDate = DateUtils.addDays(oTran.dateUser, -1);
                            endDate = DateUtils.addDays(oTran.dateUser, 1);
                        } else {    // use the posted date with a larger window
                            startDate = DateUtils.addDays(oTran.datePosted, -3);
                            endDate = DateUtils.addDays(oTran.datePosted, 3);
                        }

                        if (DateUtils.after(tran.getDate(), startDate) && DateUtils.before(tran.getDate(), endDate)) {
                            oTran.setState(OfxTransaction.ImportState.EQUAL);
                            break;
                        }
                    }

                    {   // check for matching check number
                        String checkNumber = oTran.checkNumber;
                        if (checkNumber != null && checkNumber.length() > 0) {
                            if (tran.getNumber() != null && tran.getNumber().equals(checkNumber)) {
                                oTran.setState(OfxTransaction.ImportState.EQUAL);
                                break;
                            }
                        }

                    }

                    { // check for matching fitid number
                        if (oTran instanceof OfxTransaction) {
                            String id = ((OfxTransaction) oTran).transactionID;
                            if (id != null && id.length() > 0) {
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
