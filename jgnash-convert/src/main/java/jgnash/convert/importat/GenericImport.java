/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
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
package jgnash.convert.importat;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionFactory;
import jgnash.time.DateUtils;
import jgnash.util.NotNull;

/**
 * Generic import utility methods
 *
 * @author Craig Cavanaugh
 * @author Arnout Engelen
 */
public class GenericImport {

    private GenericImport() {
    }

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

                Transaction transaction;

                if (tran.isInvestmentTransaction()) {
                    if (baseAccount.getAccountType().getAccountGroup() == AccountGroup.INVEST) {
                        System.out.println("Should be creating an investment transaction");
                    } else { // Signal an error
                        System.out.println("The base account was not an investment account type");
                    }
                }

                if (baseAccount.equals(tran.getAccount())) { // single entry oTran
                    transaction = TransactionFactory.generateSingleEntryTransaction(baseAccount, tran.getAmount(),
                            tran.getDatePosted(), tran.getMemo(), tran.getPayee(), tran.getCheckNumber());
                } else { // double entry
                    if (tran.getAmount().signum() >= 0) {
                        transaction = TransactionFactory.generateDoubleEntryTransaction(baseAccount, tran.getAccount(),
                                tran.getAmount().abs(), tran.getDatePosted(), tran.getMemo(), tran.getPayee(),
                                tran.getCheckNumber());
                    } else {
                        transaction = TransactionFactory.generateDoubleEntryTransaction(tran.getAccount(), baseAccount,
                                tran.getAmount().abs(), tran.getDatePosted(), tran.getMemo(), tran.getPayee(),
                                tran.getCheckNumber());
                    }
                }

                transaction.setFitid(tran.getFITID());
                engine.addTransaction(transaction);
            }
        }
    }

    /**
     * Sets the match state of a list of imported transactions
     *
     * @param list        list of imported transactions
     * @param baseAccount account to perform match against
     */
    public static void matchTransactions(final List<? extends ImportTransaction> list, @NotNull final Account baseAccount) {
        Objects.requireNonNull(baseAccount);

        for (final ImportTransaction importTransaction : list) {

            // amount must always match
            for (final Transaction tran : baseAccount.getSortedTransactionList()) {

                // amounts must be comparably the same, do not use an equality check
                if (tran.getAmount(baseAccount).compareTo(importTransaction.getAmount()) == 0) {

                    // check for date match
                    final LocalDate startDate;
                    final LocalDate endDate;

                    // we have a user initiated date, use a smaller window
                    if ((importTransaction.getDateUser() != null)) {
                        startDate = importTransaction.getDateUser().minusDays(1);
                        endDate = importTransaction.getDateUser().plusDays(1);
                    } else { // use the posted date with a larger window
                        startDate = importTransaction.getDatePosted().minusDays(3);
                        endDate = importTransaction.getDatePosted().plusDays(3);
                    }

                    if (DateUtils.after(tran.getLocalDate(), startDate) && DateUtils.before(tran.getLocalDate(), endDate)) {
                        importTransaction.setState(ImportState.EQUAL);
                        break;
                    }


                    // check for matching check number
                    final String checkNumber = importTransaction.getCheckNumber();
                    if (checkNumber != null && !checkNumber.isEmpty()) {
                        if (tran.getNumber().equals(checkNumber)) {
                            importTransaction.setState(ImportState.EQUAL);
                            break;
                        }
                    }

                    // check for matching fitid number
                    final String id = importTransaction.getFITID();
                    if (id != null && !id.isEmpty()) {
                        if (tran.getFitid() != null && tran.getFitid().equals(id)) {
                            importTransaction.setState(ImportState.EQUAL);
                            break;
                        }
                    }

                }
            }
        }
    }

    public static Account findFirstAvailableAccount() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        for (final Account account : engine.getAccountList()) {
            if (!account.isPlaceHolder() && !account.isLocked()) {
                return account;
            }
        }

        return null;
    }

    public static void importSecurities(final List<ImportSecurity> importSecurities, final CurrencyNode currencyNode) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        for (final ImportSecurity importSecurity : importSecurities) {
            if (ImportUtils.matchSecurity(importSecurity).isEmpty()) {   // Import only if a match is not found
                final SecurityNode securityNode = ImportUtils.createSecurityNode(importSecurity, currencyNode);

                // link the security node
                importSecurity.setSecurityNode(securityNode);

                engine.addSecurity(securityNode);

                // if the ImportSecurity has pricing information, import it as well
                importSecurity.getLocalDate().ifPresent(localDate -> importSecurity.getUnitPrice().ifPresent(price -> {
                    SecurityHistoryNode securityHistoryNode = new SecurityHistoryNode(localDate, price, 0, price, price);

                    engine.addSecurityHistory(securityNode, securityHistoryNode);
                }));
            } else {    // check to see if the cuspid needs to be updated

                // link the security node
                ImportUtils.matchSecurity(importSecurity).ifPresent(importSecurity::setSecurityNode);

                ImportUtils.matchSecurity(importSecurity)
                        .ifPresent(securityNode -> importSecurity.getId().ifPresent(securityId -> {
                            if (securityNode.getISIN() == null || securityNode.getISIN().isEmpty()) {
                                try {
                                    final SecurityNode clone = (SecurityNode) securityNode.clone();
                                    clone.setISIN(securityId);

                                    engine.updateCommodity(securityNode, clone);

                                    Logger.getLogger(GenericImport.class.getName()).info("Assigning CUSPID");
                                } catch (final CloneNotSupportedException e) {
                                    Logger.getLogger(GenericImport.class.getName()).log(Level.SEVERE,
                                            e.getLocalizedMessage(), e);
                                }
                            }
                        }));
            }

        }
    }
}
