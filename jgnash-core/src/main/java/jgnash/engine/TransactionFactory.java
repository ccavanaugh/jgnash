/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.engine;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import jgnash.text.CommodityFormat;
import jgnash.util.Resource;

/**
 * Transaction Factory
 * 
 * @author Craig Cavanaugh
 */
public class TransactionFactory {

    /**
     * Create an AddX investment transaction
     * 
     * @param investmentAccount Investment account
     * @param node Security to add
     * @param price Price of each share
     * @param quantity Number of shares
     * @param date Transaction date
     * @param memo Transaction memo
     * @return new Investment Transaction
     */
    public static InvestmentTransaction generateAddXTransaction(final Account investmentAccount, final SecurityNode node, final BigDecimal price, final BigDecimal quantity, final Date date, final String memo) {
        Objects.requireNonNull(investmentAccount);
        Objects.requireNonNull(node);
        Objects.requireNonNull(price);
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(date);
        Objects.requireNonNull(memo);

        InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setDate(date);
        transaction.setMemo(memo);

        TransactionEntryAddX entry = new TransactionEntryAddX(investmentAccount, node, price, quantity);
        entry.setMemo(memo);

        transaction.setPayee(buildPayee("Word.Add", node, price, quantity));

        transaction.addTransactionEntry(entry);

        return transaction;
    }

    /**
     * Import a jGnash 1.x buy security transaction. <b>Should not be used for new transactions.</b>
     * 
     * @param account Account to buy against (can be the same investment account)
     * @param investmentAccount Investment account
     * @param node Security to buy
     * @param price Price of each share
     * @param quantity Number of shares
     * @param exchangeRate Exchange rate (Can be BigDecimal.ONE, cannot be null)
     * @param fee Purchase fee
     * @param date Transaction date
     * @param memo Transaction memo
     * @return new Transaction
     */
    public static InvestmentTransaction import1xBuyXTransaction(final Account account, final Account investmentAccount, final SecurityNode node, final BigDecimal price, final BigDecimal quantity, final BigDecimal exchangeRate, final BigDecimal fee, final Date date, final String memo) {

        assert account != null && investmentAccount != null && node != null && price != null && quantity != null;
        assert exchangeRate != null && fee != null && date != null && memo != null;

        List<TransactionEntry> fees = new ArrayList<>();

        // fees are charged against cash balance of the investment account
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            TransactionEntry e = new TransactionEntry(investmentAccount, fee.negate());
            e.setMemo(memo);
            e.setTransactionTag(TransactionTag.INVESTMENT_FEE);

            fees.add(e);
        }

        return generateBuyXTransaction(account, investmentAccount, node, price, quantity, exchangeRate, date, memo, fees);
    }

    /**
     * Create a buy security transaction
     * 
     * @param account Account to buy against (can be the same investment account)
     * @param investmentAccount Investment account
     * @param node Security to buy
     * @param price Price of each share
     * @param quantity Number of shares
     * @param exchangeRate Exchange rate (Can be BigDecimal.ONE, cannot be null)
     * @param date Transaction date
     * @param memo Transaction memo
     * @param fees List of transaction fees
     * @return new Transaction
     */
    public static InvestmentTransaction generateBuyXTransaction(final Account account, final Account investmentAccount, final SecurityNode node, final BigDecimal price, final BigDecimal quantity, final BigDecimal exchangeRate, final Date date, final String memo, final Collection<TransactionEntry> fees) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(investmentAccount);
        Objects.requireNonNull(node);
        Objects.requireNonNull(price);
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(date);
        Objects.requireNonNull(memo);
        Objects.requireNonNull(exchangeRate);
        Objects.requireNonNull(fees);

        // verify fees are tagged correctly
        for (TransactionEntry fee : fees) {
            if (fee.getTransactionTag() != TransactionTag.INVESTMENT_FEE) {
                throw new RuntimeException(Resource.get().getString("Message.Error.InvalidTransactionTag"));
            }
        }

        InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setDate(date);
        transaction.setMemo(memo);

        TransactionEntryBuyX entry = new TransactionEntryBuyX(account, investmentAccount, node, price, quantity, exchangeRate);
        entry.setMemo(memo);

        transaction.setPayee(buildPayee("Word.Buy", node, price, quantity));

        transaction.addTransactionEntry(entry);

        // process transaction fees
        if (!fees.isEmpty()) {

            // total of the fees charged against the investment account
            BigDecimal nonCashBalanceFees = BigDecimal.ZERO;

            // loop through and add investment fees to the transaction
            for (TransactionEntry fee : fees) {
                transaction.addTransactionEntry(fee);

                nonCashBalanceFees = nonCashBalanceFees.add(fee.getAmount(investmentAccount).abs());
            }

            // transfer cash from the account to the cash balance of the investment account to cover fees
            if (!account.equals(investmentAccount)) {

                byte scale = account.getCurrencyNode().getScale();
                BigDecimal exchangedAmount = nonCashBalanceFees.abs().multiply(exchangeRate).setScale(scale, MathConstants.roundingMode);

                TransactionEntry tran = new TransactionEntry(investmentAccount, account, nonCashBalanceFees, exchangedAmount.negate());
                tran.setMemo(memo);
                tran.setTransactionTag(TransactionTag.INVESTMENT_CASH_TRANSFER);
                transaction.addTransactionEntry(tran);
            }
        }

        //ReconcileManager.reconcileTransaction(account, transaction, reconciled);
        //ReconcileManager.reconcileTransaction(investmentAccount, transaction, reconciled);

        Logger.getLogger(TransactionFactory.class.getName()).info(transaction.toString());

        return transaction;
    }

    /**
     * Create a Dividend transaction
     * 
     * @param incomeAccount Income source account for the cash dividend
     * @param investmentAccount Investment account
     * @param cashAccount The account receiving the cash dividend. May be the same investment account.
     * @param node Security for dividend
     * @param dividend Cash dividend
     * @param incomeExchangedAmount Income account exchanged amount (Can be the same as dividend, cannot be null)
     * @param cashExchangedAmount The exchanged amount for the cash account (Can be the same as dividend, cannot be null)
     * @param date Transaction date
     * @param memo Transaction memo
     * @return new InvestmentTransaction
     */
    public static InvestmentTransaction generateDividendXTransaction(final Account incomeAccount, final Account investmentAccount, final Account cashAccount, final SecurityNode node, final BigDecimal dividend, final BigDecimal incomeExchangedAmount, final BigDecimal cashExchangedAmount, final Date date, final String memo) {
        Objects.requireNonNull(incomeAccount);
        Objects.requireNonNull(cashAccount);
        Objects.requireNonNull(investmentAccount);
        Objects.requireNonNull(node);
        Objects.requireNonNull(dividend);
        Objects.requireNonNull(incomeExchangedAmount);
        Objects.requireNonNull(date);
        Objects.requireNonNull(memo);

        assert incomeExchangedAmount.signum() <= 0;

        final InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setDate(date);
        transaction.setMemo(memo);

        final TransactionEntryDividendX entry = new TransactionEntryDividendX(incomeAccount, investmentAccount, node, dividend, incomeExchangedAmount);
        entry.setMemo(memo);

        final Resource rb = Resource.get();
        final NumberFormat format = CommodityFormat.getFullNumberFormat(incomeAccount.getCurrencyNode());

        transaction.setPayee(rb.getString("Word.Dividend") + " : " + node.getSymbol() + " @ " + format.format(dividend));

        transaction.addTransactionEntry(entry);

        if (!cashAccount.equals(investmentAccount)) {
            TransactionEntry tran = new TransactionEntry(cashAccount, investmentAccount, cashExchangedAmount, dividend.negate());
            tran.setMemo(memo);
            tran.setTransactionTag(TransactionTag.INVESTMENT_CASH_TRANSFER);
            transaction.addTransactionEntry(tran);
        }

        //ReconcileManager.reconcileTransaction(incomeAccount, transaction, reconciled);
        //ReconcileManager.reconcileTransaction(investmentAccount, transaction, reconciled);
        //ReconcileManager.reconcileTransaction(cashAccount, transaction, reconciled);

        return transaction;
    }

    /**
     * Create a Return of Capital transaction
     * 
     * @param incomeAccount Income source account for the cash dividend
     * @param investmentAccount Investment account
     * @param cashAccount The account receiving the cash dividend. May be the same investment account.
     * @param node Security for dividend
     * @param dividend Cash dividend
     * @param incomeExchangedAmount Income account exchanged amount (Can be the same as dividend, cannot be null)
     * @param cashExchangedAmount The exchanged amount for the cash account (Can be the same as dividend, cannot be
     *        null)
     * @param date Transaction date
     * @param memo Transaction memo
     * @return new InvestmentTransaction
     */
    public static InvestmentTransaction generateRocXTransaction(final Account incomeAccount, final Account investmentAccount, final Account cashAccount, final SecurityNode node, final BigDecimal dividend, final BigDecimal incomeExchangedAmount, final BigDecimal cashExchangedAmount, final Date date, final String memo) {
        Objects.requireNonNull(incomeAccount);
        Objects.requireNonNull(cashAccount);
        Objects.requireNonNull(investmentAccount);
        Objects.requireNonNull(node);
        Objects.requireNonNull(dividend);
        Objects.requireNonNull(incomeExchangedAmount);
        Objects.requireNonNull(date);
        Objects.requireNonNull(memo);

        assert incomeExchangedAmount.signum() <= 0;

        final InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setDate(date);
        transaction.setMemo(memo);

        final TransactionEntryRocX entry = new TransactionEntryRocX(incomeAccount, investmentAccount, node, dividend, incomeExchangedAmount);
        entry.setMemo(memo);

        final Resource rb = Resource.get();
        final NumberFormat format = CommodityFormat.getFullNumberFormat(incomeAccount.getCurrencyNode());

        transaction.setPayee(rb.getString("Word.ReturnOfCapital") + " : " + node.getSymbol() + " @ " + format.format(dividend));

        transaction.addTransactionEntry(entry);

        if (!cashAccount.equals(investmentAccount)) {
            TransactionEntry tran = new TransactionEntry(cashAccount, investmentAccount, cashExchangedAmount, dividend.negate());
            tran.setMemo(memo);
            tran.setTransactionTag(TransactionTag.INVESTMENT_CASH_TRANSFER);
            transaction.addTransactionEntry(tran);
        }

        // ReconcileManager.reconcileTransaction(incomeAccount, transaction, reconciled);
        // ReconcileManager.reconcileTransaction(investmentAccount, transaction, reconciled);
        // ReconcileManager.reconcileTransaction(cashAccount, transaction, reconciled);

        return transaction;
    }

    /**
     * Generate a double entry transaction with exchange rate
     * 
     * @param creditAccount Credit account
     * @param debitAccount Debit account
     * @param creditAmount Transaction credit amount
     * @param debitAmount Transaction credit amount
     * @param date Transaction date
     * @param memo Transaction memo
     * @param payee Transaction payee
     * @param number Transaction number
     * @return new Transaction
     */
    public static Transaction generateDoubleEntryTransaction(final Account creditAccount, final Account debitAccount, final BigDecimal creditAmount, final BigDecimal debitAmount, final Date date, final String memo, final String payee, final String number) {
        Objects.requireNonNull(creditAccount);
        Objects.requireNonNull(debitAccount);

        assert creditAccount != debitAccount;

        Transaction transaction = new Transaction();

        transaction.setDate(date);
        transaction.setNumber(number);
        transaction.setPayee(payee);
        transaction.setMemo(memo);

        TransactionEntry entry = new TransactionEntry(creditAccount, debitAccount, creditAmount, debitAmount);
        entry.setMemo(memo);

        transaction.addTransactionEntry(entry);

        return transaction;
    }

    /**
     * Generate a double entry transaction
     * 
     * @param creditAccount Credit account
     * @param debitAccount Debit account
     * @param amount Transaction amount
     * @param date Transaction date
     * @param memo Transaction memo
     * @param payee Transaction payee
     * @param number Transaction number
     * @return new Transaction
     */
    public static Transaction generateDoubleEntryTransaction(final Account creditAccount, final Account debitAccount, final BigDecimal amount, final Date date, final String memo, final String payee, final String number) {
        Objects.requireNonNull(creditAccount);
        Objects.requireNonNull(debitAccount);

        assert creditAccount != debitAccount;

        Transaction transaction = new Transaction();

        transaction.setDate(date);
        transaction.setNumber(number);
        transaction.setPayee(payee);
        transaction.setMemo(memo);

        TransactionEntry entry = new TransactionEntry(creditAccount, debitAccount, amount);
        entry.setMemo(memo);

        transaction.addTransactionEntry(entry);

        return transaction;
    }

    /**
     * Create a Split investment transaction
     * 
     * @param investmentAccount Investment account
     * @param node Security that merged
     * @param price Price of each share
     * @param quantity Number of shares
     * @param date Transaction date
     * @param memo Transaction memo
     * @return new Investment Transaction
     */
    public static InvestmentTransaction generateMergeXTransaction(final Account investmentAccount, final SecurityNode node, final BigDecimal price, final BigDecimal quantity, final Date date, final String memo) {
        Objects.requireNonNull(investmentAccount);
        Objects.requireNonNull(node);
        Objects.requireNonNull(price);
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(date);
        Objects.requireNonNull(memo);

        InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setDate(date);
        transaction.setMemo(memo);

        TransactionEntryMergeX entry = new TransactionEntryMergeX(investmentAccount, node, price, quantity);
        entry.setMemo(memo);

        transaction.setPayee(buildPayee("Word.Merge", node, price, quantity));

        transaction.addTransactionEntry(entry);

        return transaction;
    }

    /**
     * Create a Reinvested Dividend transaction
     * 
     * @param investmentAccount Investment account
     * @param node Security for dividend
     * @param price Share price
     * @param quantity Quantity of shares reinvested
     * @param date Date of transaction
     * @param memo Transaction memo
     * @param fees Fee entry(s)
     * @param gains Gain/Loss entry(s)
     * @return new InvestmentTransaction
     */
    public static InvestmentTransaction generateReinvestDividendXTransaction(final Account investmentAccount, final SecurityNode node, final BigDecimal price, final BigDecimal quantity, final Date date, final String memo, final Collection<TransactionEntry> fees, final Collection<TransactionEntry> gains) {
        Objects.requireNonNull(investmentAccount);
        Objects.requireNonNull(node);
        Objects.requireNonNull(price);
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(date);
        Objects.requireNonNull(memo);
        Objects.requireNonNull(fees);
        Objects.requireNonNull(gains);

        for (TransactionEntry fee : fees) {
            if (fee.getTransactionTag() != TransactionTag.INVESTMENT_FEE) {
                throw new RuntimeException(Resource.get().getString("Message.Error.InvalidTransactionTag"));
            }
        }

        for (TransactionEntry gain : gains) {
            if (gain.getTransactionTag() != TransactionTag.GAIN_LOSS) {
                throw new RuntimeException(Resource.get().getString("Message.Error.InvalidTransactionTag"));
            }
        }

        InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setDate(date);
        transaction.setMemo(memo);

        TransactionEntryReinvestDivX entry = new TransactionEntryReinvestDivX(investmentAccount, node, price, quantity);

        entry.setMemo(memo);

        final Resource rb = Resource.get();

        final NumberFormat format = CommodityFormat.getFullNumberFormat(node);

        transaction.setPayee(rb.getString("Word.ReInvDiv") + " : " + node.getSymbol() + ' ' + quantity.toString()
                + " @ " + format.format(quantity));

        transaction.addTransactionEntry(entry);

        if (!fees.isEmpty()) {
            BigDecimal totalFees = BigDecimal.ZERO;

            // loop through and add investment fees
            for (final TransactionEntry fee : fees) {
                transaction.addTransactionEntry(fee);
                totalFees = totalFees.add(fee.getAmount(investmentAccount));
            }

            // create a single entry transaction that offsets any resulting fees
            TransactionEntry feesOffsetEntry = new TransactionEntry(investmentAccount, totalFees.negate());
            feesOffsetEntry.setMemo(memo);
            feesOffsetEntry.setTransactionTag(TransactionTag.FEES_OFFSET);

            assert feesOffsetEntry.isSingleEntry(); // check

            transaction.addTransactionEntry(feesOffsetEntry);
        }

        if (!gains.isEmpty()) {

            BigDecimal totalGains = BigDecimal.ZERO;

            // loop through and add gain/loss entries
            for (final TransactionEntry gain : gains) {
                transaction.addTransactionEntry(gain);
                totalGains = totalGains.add(gain.getAmount(investmentAccount));
            }

            // create a single entry transaction that offsets investment gains or loss
            final TransactionEntry gainsOffsetEntry = new TransactionEntry(investmentAccount, totalGains.negate());
            gainsOffsetEntry.setMemo(memo);
            gainsOffsetEntry.setTransactionTag(TransactionTag.GAINS_OFFSET);

            assert gainsOffsetEntry.isSingleEntry(); // check

            transaction.addTransactionEntry(gainsOffsetEntry);
        }

        return transaction;
    }

    /**
     * Create a RemoveX investment transaction
     * 
     * @param investmentAccount Investment account
     * @param node Security to remove
     * @param price Price of each share
     * @param quantity Number of shares
     * @param date Transaction date
     * @param memo Transaction memo
     * @return new Investment Transaction
     */
    public static InvestmentTransaction generateRemoveXTransaction(final Account investmentAccount, final SecurityNode node, final BigDecimal price, final BigDecimal quantity, final Date date, final String memo) {
        Objects.requireNonNull(investmentAccount);
        Objects.requireNonNull(node);
        Objects.requireNonNull(price);
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(date);
        Objects.requireNonNull(memo);

        InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setDate(date);
        transaction.setMemo(memo);

        TransactionEntryRemoveX entry = new TransactionEntryRemoveX(investmentAccount, node, price, quantity);
        entry.setMemo(memo);

        transaction.setPayee(buildPayee("Word.Remove", node, price, quantity));

        transaction.addTransactionEntry(entry);

        return transaction;
    }

    /**
     * Import a jGnash 1.x a sell security transaction <b>Should not be used for new transactions.</b>
     * 
     * @param account Account to buy against (can be the same investment account)
     * @param investmentAccount Investment account
     * @param node Security to sell
     * @param price Price of each share
     * @param quantity Number of shares
     * @param exchange Exchange rate (Can be BigDecimal.ONE, cannot be null)
     * @param fee Purchase fee
     * @param date Transaction date
     * @param memo Transaction memo
     * @return new Transaction
     */
    public static InvestmentTransaction import1xSellXTransaction(final Account account, final Account investmentAccount, final SecurityNode node, final BigDecimal price, final BigDecimal quantity, final BigDecimal exchange, final BigDecimal fee, final Date date, final String memo) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(investmentAccount);
        Objects.requireNonNull(node);
        Objects.requireNonNull(price);
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(exchange);
        Objects.requireNonNull(fee);
        Objects.requireNonNull(date);
        Objects.requireNonNull(memo);

        List<TransactionEntry> fees = new ArrayList<>();

        // fees are charged against cash balance of the investment account
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            TransactionEntry e = new TransactionEntry(investmentAccount, fee.negate());
            e.setMemo(memo);
            e.setTransactionTag(TransactionTag.INVESTMENT_FEE);

            fees.add(e);
        }

        return generateSellXTransaction(account, investmentAccount, node, price, quantity, exchange, date, memo, fees, new ArrayList<TransactionEntry>());
    }

    /**
     * Create a sell security transaction
     * 
     * @param account Account receive sale profits or loss. May be the same as the investment account (Cash balance)
     * @param investmentAccount Investment account
     * @param node Security to sell
     * @param price Price of each share
     * @param quantity Number of shares
     * @param exchangeRate Exchanged amount (cannot be null)
     * @param date Transaction date
     * @param memo Transaction memo
     * @param fees Purchase fee
     * @param gains Gains/Loss entries
     * @return new Transaction
     */
    public static InvestmentTransaction generateSellXTransaction(final Account account, final Account investmentAccount, final SecurityNode node, final BigDecimal price, final BigDecimal quantity, final BigDecimal exchangeRate, final Date date, final String memo, final Collection<TransactionEntry> fees, final Collection<TransactionEntry> gains) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(investmentAccount);
        Objects.requireNonNull(node);
        Objects.requireNonNull(price);
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(exchangeRate);
        Objects.requireNonNull(gains);
        Objects.requireNonNull(fees);
        Objects.requireNonNull(date);
        Objects.requireNonNull(memo);

        // verify fees are tagged correctly
        for (final TransactionEntry fee : fees) {
            if (fee.getTransactionTag() != TransactionTag.INVESTMENT_FEE) {
                throw new RuntimeException(Resource.get().getString("Message.Error.InvalidTransactionTag"));
            }
        }

        // verify gains are tagged correctly
        for (final TransactionEntry gain : gains) {
            if (gain.getTransactionTag() != TransactionTag.GAIN_LOSS) {
                throw new RuntimeException(Resource.get().getString("Message.Error.InvalidTransactionTag"));
            }
        }

        final InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setDate(date);
        transaction.setMemo(memo);

        final TransactionEntrySellX entry = new TransactionEntrySellX(account, investmentAccount, node, price, quantity, exchangeRate);
        entry.setMemo(memo);

        transaction.setPayee(buildPayee("Word.Sell", node, price, quantity));

        transaction.addTransactionEntry(entry);

        // process transaction fees
        if (!fees.isEmpty()) {
            // total of the fees charged against the investment account
            BigDecimal nonCashBalanceFees = BigDecimal.ZERO;

            // loop through and add investment fees to the transaction
            for (final TransactionEntry fee : fees) {
                transaction.addTransactionEntry(fee);

                nonCashBalanceFees = nonCashBalanceFees.add(fee.getAmount(investmentAccount).abs());
            }

            // transfer cash from the account to the cash balance of the investment account to cover fees
            if (!account.equals(investmentAccount)) {

                byte scale = account.getCurrencyNode().getScale();
                BigDecimal exchangedAmount = nonCashBalanceFees.abs().multiply(exchangeRate).setScale(scale, MathConstants.roundingMode);

                TransactionEntry tran = new TransactionEntry(investmentAccount, account, nonCashBalanceFees, exchangedAmount.negate());
                tran.setMemo(memo);
                tran.setTransactionTag(TransactionTag.INVESTMENT_CASH_TRANSFER);
                transaction.addTransactionEntry(tran);
            }
        }

        if (!gains.isEmpty()) { // capital gains entered
            BigDecimal totalGains = BigDecimal.ZERO;

            // loop through and add gains/loss entries
            for (final TransactionEntry gain : gains) {
                transaction.addTransactionEntry(gain);
                totalGains = totalGains.add(gain.getAmount(investmentAccount));
            }

            // create a single entry transaction that offsets investment gains or loss
            final TransactionEntry gainsOffsetEntry = new TransactionEntry(investmentAccount, totalGains.negate());
            gainsOffsetEntry.setMemo(memo);
            gainsOffsetEntry.setTransactionTag(TransactionTag.GAINS_OFFSET);

            assert gainsOffsetEntry.isSingleEntry(); // check

            transaction.addTransactionEntry(gainsOffsetEntry);
        }

        //ReconcileManager.reconcileTransaction(account, transaction, reconciled);
        //ReconcileManager.reconcileTransaction(investmentAccount, transaction, reconciled);

        Logger.getLogger(TransactionFactory.class.getName()).info(transaction.toString());

        return transaction;
    }

    /**
     * Create a single entry transaction
     * 
     * @param account Destination account
     * @param amount Transaction amount
     * @param date Transaction date
     * @param memo Transaction memo
     * @param payee Transaction payee
     * @param number Transaction number
     * @return new Transaction
     */
    public static Transaction generateSingleEntryTransaction(final Account account, final BigDecimal amount, final Date date, final String memo, final String payee, final String number) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(date);

        Transaction transaction = new Transaction();

        transaction.setDate(date);
        transaction.setNumber(number);
        transaction.setPayee(payee);
        transaction.setMemo(memo);

        TransactionEntry entry = new TransactionEntry(account, amount);
        entry.setMemo(memo);

        assert entry.isSingleEntry(); // check

        transaction.addTransactionEntry(entry);

        return transaction;
    }

    /**
     * Create a Split investment transaction
     * 
     * @param investmentAccount Investment account
     * @param node Security that split
     * @param price Price of each share
     * @param quantity Number of shares
     * @param date Transaction date
     * @param memo Transaction memo
     * @return new Investment Transaction
     */
    public static InvestmentTransaction generateSplitXTransaction(final Account investmentAccount, final SecurityNode node, final BigDecimal price, final BigDecimal quantity, final Date date, final String memo) {

        assert investmentAccount != null && node != null && price != null && quantity != null;
        assert date != null && memo != null;

        InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setDate(date);
        transaction.setMemo(memo);

        TransactionEntrySplitX entry = new TransactionEntrySplitX(investmentAccount, node, price, quantity);
        entry.setMemo(memo);

        transaction.setPayee(buildPayee("Word.Split", node, price, quantity));

        transaction.addTransactionEntry(entry);

        return transaction;
    }

    private static String buildPayee(final String wordProperty, final SecurityNode node, final BigDecimal price, final BigDecimal quantity) {
        final Resource rb = Resource.get();

        final NumberFormat format = CommodityFormat.getFullNumberFormat(node);

        return rb.getString(wordProperty) + " : " + node.getSymbol() + ' ' + quantity.toString() + " @ "
                + format.format(price);
    }

    private TransactionFactory() {
    }
}
