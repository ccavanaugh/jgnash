/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionFactory;
import jgnash.convert.imports.ofx.OfxTransaction;
import jgnash.util.DateUtils;
import jgnash.util.NotNull;

/**
 * Generic import utility methods
 * 
 * @author Craig Cavanaugh
 * @author Arnout Engelen
 */
public class GenericImport {

    public static void importTransactions(@NotNull final List<? extends ImportTransaction> transactions,
                                          @NotNull final Account baseAccount) {
        Objects.requireNonNull(transactions);
        Objects.requireNonNull(baseAccount);

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        for (final ImportTransaction tran : transactions) {
            Objects.requireNonNull(tran.getAccount());

            if (tran.getState() == ImportState.NEW
                    || tran.getState() == ImportState.NOT_EQUAL) { // do not import matched transactions
                Transaction t;

                if (baseAccount.equals(tran.getAccount())) { // single entry oTran
                    t = TransactionFactory.generateSingleEntryTransaction(baseAccount, tran.getAmount(),
                            tran.getDatePosted(), tran.getMemo(), tran.getPayee(), tran.getCheckNumber());
                } else { // double entry
                    if (tran.getAmount().signum() >= 0) {
                        t = TransactionFactory.generateDoubleEntryTransaction(baseAccount, tran.getAccount(),
                                tran.getAmount().abs(), tran.getDatePosted(), tran.getMemo(), tran.getPayee(),
                                tran.getCheckNumber());
                    } else {
                        t = TransactionFactory.generateDoubleEntryTransaction(tran.getAccount(), baseAccount,
                                tran.getAmount().abs(), tran.getDatePosted(), tran.getMemo(), tran.getPayee(),
                                tran.getCheckNumber());
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
    public static void matchTransactions(final List<? extends ImportTransaction> list, @NotNull final Account baseAccount) {
        Objects.requireNonNull(baseAccount);

        for (final ImportTransaction oTran : list) {

            // amount must always match
            for (final Transaction tran : baseAccount.getSortedTransactionList()) {

                // amounts must be comparably the same, do not use an equality check
                if (tran.getAmount(baseAccount).compareTo(oTran.getAmount()) == 0) {

                    { // check for date match
                        LocalDate startDate;
                        LocalDate endDate;

                        // we have a user initiated date, use a smaller window
                        if ((oTran.getDateUser() != null)) {
                            startDate = oTran.getDateUser().minusDays(1);
                            endDate = oTran.getDateUser().plusDays(1);
                        } else { // use the posted date with a larger window
                            startDate = oTran.getDatePosted().minusDays(3);
                            endDate = oTran.getDatePosted().plusDays(3);
                        }

                        if (DateUtils.after(tran.getLocalDate(), startDate) && DateUtils.before(tran.getLocalDate(), endDate)) {
                            oTran.setState(ImportState.EQUAL);
                            break;
                        }
                    }

                    { // check for matching check number
                        String checkNumber = oTran.getCheckNumber();
                        if (checkNumber != null && !checkNumber.isEmpty()) {
                            if (tran.getNumber().equals(checkNumber)) {
                                oTran.setState(ImportState.EQUAL);
                                break;
                            }
                        }

                    }

                    { // check for matching fitid number
                        if (oTran instanceof OfxTransaction) {
                            final String id = oTran.getTransactionID();
                            if (id != null && !id.isEmpty()) {
                                if (tran.getFitid() != null && tran.getFitid().equals(id)) {
                                    oTran.setState(ImportState.EQUAL);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static Account matchAccount(final String id) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        List<Account> accountList = engine.getAccountList();

        if (id != null) {
            for (final Account account : accountList) {
                if (account.getUuid().equals(id)) {
                    return account;
                }

                if (account.getBankId() != null && account.getBankId().equals(id)) {
                    return account;
                }

                if (account.getAccountNumber() != null && account.getAccountNumber().equals(id)) {
                    return account;
                }
            }
        } else {
            for (final Account account : accountList) {
                if (!account.isPlaceHolder() && !account.isLocked()) {
                    return account;
                }
            }
        }

        return null;
    }

    private GenericImport() {
    }
}
