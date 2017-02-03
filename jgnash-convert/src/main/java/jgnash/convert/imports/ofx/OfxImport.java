/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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
package jgnash.convert.imports.ofx;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jgnash.convert.common.OfxTags;
import jgnash.convert.imports.ImportSecurity;
import jgnash.convert.imports.ImportState;
import jgnash.convert.imports.ImportTransaction;
import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.SecurityNode;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionTag;

/**
 * OfxImport utility methods
 *
 * @author Craig Cavanaugh
 */
public class OfxImport {

    /**
     * Private constructor, utility class
     */
    private OfxImport() {
    }

    public static void importTransactions(final OfxBank ofxBank, final Account baseAccount) {
        Objects.requireNonNull(ofxBank.getTransactions());
        Objects.requireNonNull(baseAccount);

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        for (final ImportTransaction tran : ofxBank.getTransactions()) {
            Objects.requireNonNull(tran.getAccount());

            if (tran.getState() == ImportState.NEW
                    || tran.getState() == ImportState.NOT_EQUAL) { // do not import matched transactions

                Transaction transaction = null;

                if (tran.isInvestmentTransaction()) {
                    if (baseAccount.getAccountType().getAccountGroup() == AccountGroup.INVEST) {

                        transaction = importInvestmentTransaction(ofxBank, tran, baseAccount);

                        if (transaction != null) {

                            // check and add the security node to the account if not present
                            if (!baseAccount.containsSecurity(((InvestmentTransaction) transaction).getSecurityNode())) {
                                engine.addAccountSecurity(((InvestmentTransaction) transaction).getInvestmentAccount(),
                                        ((InvestmentTransaction) transaction).getSecurityNode());
                            }
                        }

                    } else { // Signal an error
                        System.out.println("Base account was not an investment account type");
                    }
                } else {
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
                }

                // add the new transaction
                if (transaction != null) {
                    transaction.setFitid(tran.getFITID());
                    engine.addTransaction(transaction);
                }
            }
        }
    }

    private static InvestmentTransaction importInvestmentTransaction(final OfxBank ofxBank, final ImportTransaction ofxTransaction,
                                                                     final Account investmentAccount) {

        // OFX reinvested dividends can be merged into one or created as a zero commission purchase

        // TODO match up security
        // Specify an income account!
        // Specify a fees account

        final SecurityNode securityNode = matchSecurity(ofxBank, ofxTransaction.getSecurityId());
        final String memo = ofxTransaction.getMemo();
        final LocalDate datePosted = ofxTransaction.getDatePosted();

        final BigDecimal dividend = ofxTransaction.getAmount();
        final BigDecimal units = ofxTransaction.getUnits();
        final BigDecimal unitPrice = ofxTransaction.getUnitPrice();

        Account cashAccount = investmentAccount;

        // force use of cash balance
        if (OfxTags.CASH.equals(ofxTransaction.getSubAccount())) {
            cashAccount = investmentAccount;
        }

        List<TransactionEntry> fees = Collections.emptyList();

        if (!ofxTransaction.getCommission().equals(BigDecimal.ZERO)) {
            // TODO: This is a single entry fee

            final TransactionEntry transactionEntry = new TransactionEntry(investmentAccount, ofxTransaction.getCommission().negate());
            transactionEntry.setTransactionTag(TransactionTag.INVESTMENT_FEE);
            fees.add(transactionEntry);
        }

        if (!ofxTransaction.getFees().equals(BigDecimal.ZERO)) {
            // TODO: This is a single entry fee

            final TransactionEntry transactionEntry = new TransactionEntry(investmentAccount, ofxTransaction.getFees().negate());
            transactionEntry.setTransactionTag(TransactionTag.INVESTMENT_FEE);
            fees.add(transactionEntry);
        }

        InvestmentTransaction transaction = null;

        if (securityNode != null) {

            // TODO: Create option to use a jGnash version of reinvest dividend transaction for an OFX dividend and reinvest

            switch (ofxTransaction.getTransactionType()) {
                case DIVIDEND:

                    // TODO: This is a single entry dividend
                    transaction = TransactionFactory.generateDividendXTransaction(investmentAccount, investmentAccount, cashAccount,
                            securityNode, dividend, dividend, dividend, datePosted, memo);
                    break;
                case REINVESTDIV:   // cash with zero commission

                    // TODO: This is a single entry buy
                    transaction = TransactionFactory.generateBuyXTransaction(cashAccount, investmentAccount, securityNode,
                            unitPrice, units, BigDecimal.ONE, datePosted, memo, fees);

                    break;
                case BUYSHARE:
                    // TODO: This is a single entry buy
                    transaction = TransactionFactory.generateBuyXTransaction(cashAccount, investmentAccount, securityNode,
                            unitPrice, units, BigDecimal.ONE, datePosted, memo, fees);

                    break;
                case SELLSHARE:
                    // TODO: This is a single entry buy

                    List<TransactionEntry> gains = Collections.emptyList();

                    transaction = TransactionFactory.generateSellXTransaction(cashAccount, investmentAccount, securityNode,
                            unitPrice, units, BigDecimal.ONE, datePosted, memo, fees, gains);

                    break;
                default:
            }
        }

        return transaction;
    }

    private static SecurityNode matchSecurity(final OfxBank ofxBank, final String securityId) {

        SecurityNode securityNode = null;

        for (final ImportSecurity importSecurity : ofxBank.getSecurityList()) {
            if (importSecurity.getId().isPresent()) {
                if (importSecurity.getId().get().equals(securityId)) {
                    securityNode = importSecurity.getSecurityNode();
                    break;
                }
            }
        }

        return securityNode;
    }

    public static Account matchAccount(final OfxBank bank) {

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

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
}
