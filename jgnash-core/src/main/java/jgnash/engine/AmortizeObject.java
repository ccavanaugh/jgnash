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
package jgnash.engine;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import jgnash.resource.util.ResourceUtils;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

/**
 * This class is used to calculate loan payments.
 * <p>
 * Because BigDecimal is lacking methods of exponents, calculations are
 * performed using StrictMath to maintain portability. Results are returned as
 * doubles. Results will need to be scaled and rounded.
 *
 * @author Craig Cavanaugh
 */
@Entity
@SequenceGenerator(name = "sequence", allocationSize = 10)
public class AmortizeObject implements Serializable {

    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(generator = "sequence", strategy = GenerationType.SEQUENCE)
    private long id;

    @ManyToOne
    private Account interestAccount; // account for interest payment

    @ManyToOne
    private Account bankAccount; // account for principal account

    // (normally the liability account)
    @ManyToOne
    private Account feesAccount; // account to place non interest fees

    /**
     * Controls the type of transaction automatically generated
     */
    @SuppressWarnings("unused")
    private Integer transactionType;

    /**
     * the number of payments per year.
     */
    private int numPayments;

    /**
     * length of loan in months.
     */
    private int length;

    /**
     * the number of compounding periods per year.
     */
    private int numCompPeriods;

    /**
     * annual interest rate, APR (ex 6.75).
     */
    private BigDecimal interestRate;

    /**
     * original balance of the loan.
     */
    private BigDecimal originalBalance;

    /**
     * PMI, escrow, etc.
     */
    private BigDecimal fees = BigDecimal.ZERO;

    /**
     * the payee to use.
     */
    private String payee;

    /**
     * the memo to use.
     */
    private String memo;

    // private String checkNumber;     // check number to use

    /**
     * origination date.
     */
    private LocalDate date = LocalDate.now();

    /**
     * calculate interest based on daily periodic rate.
     */
    private boolean useDailyRate;

    /**
     * the number of days per year for daily periodic rate.
     */
    private BigDecimal daysPerYear;

    /**
     * Empty constructor to keep reflection happy.
     */
    public AmortizeObject() {
    }

    public void setDate(final LocalDate localDate) {
        date = localDate;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setPaymentPeriods(final int periods) {
        numPayments = periods;
    }

    public int getPaymentPeriods() {
        return numPayments;
    }

    /**
     * Sets the length of the loan (in months).
     *
     * @param months length of loan
     */
    public void setLength(final int months) {
        this.length = months;
    }

    /**
     * Gets the length of the loan in months.
     *
     * @return length of loan in months
     */
    public int getLength() {
        return length;
    }

    /**
     * Determines if interest will be calculate based on a daily periodic rate,
     * or if it is assumed that the interest is paid exactly on the due date.
     *
     * @param daily true if interest should be calculated using a daily rate
     */
    public void setUseDailyRate(final boolean daily) {
        useDailyRate = daily;
    }

    /**
     * Returns how interest will be calculated.
     *
     * @return true if interest is calculated using the daily periodic rate
     */
    public boolean getUseDailyRate() {
        return useDailyRate;
    }

    /**
     * Sets the number of days per year used to calculate the daily periodic
     * interest rate. The value can be a decimal.
     *
     * @param days The number of days in a year
     */
    public void setDaysPerYear(final BigDecimal days) {
        daysPerYear = days;
    }

    /**
     * Returns the number of days per year used to calculate a daily periodic
     * interest rate.
     *
     * @return The number of days per year
     */
    public BigDecimal getDaysPerYear() {
        return daysPerYear;
    }

    public void setRate(final BigDecimal rate) {
        interestRate = rate;
    }

    public BigDecimal getRate() {
        return interestRate;
    }

    public void setPrincipal(final BigDecimal principal) {
        originalBalance = principal;
    }

    public BigDecimal getPrincipal() {
        return originalBalance;
    }

    public void setInterestPeriods(final int periods) {
        numCompPeriods = periods;
    }

    public int getInterestPeriods() {
        return numCompPeriods;
    }

    public void setFees(final BigDecimal fees) {
        this.fees = Objects.requireNonNullElse(fees, BigDecimal.ZERO);
    }

    public BigDecimal getFees() {
        return fees;
    }

    /**
     * Set the id of the interest account.
     *
     * @param id the id of the interest account
     */
    public void setInterestAccount(final Account id) {
        interestAccount = id;
    }

    /**
     * Returns the id of the interest account.
     *
     * @return the id of the interest account
     */
    public Account getInterestAccount() {
        return interestAccount;
    }

    /**
     * Set the id of the principal account.
     *
     * @param id the id of the principal account
     */
    public void setBankAccount(final Account id) {
        bankAccount = id;
    }

    /**
     * Returns the id of the principal account.
     *
     * @return the id of the principal account
     */
    public Account getBankAccount() {
        return bankAccount;
    }

    /**
     * Set the id of the fees account.
     *
     * @param id the id of the fees account
     */
    public void setFeesAccount(final Account id) {
        feesAccount = id;
    }

    /**
     * Returns the id of the fees account.
     *
     * @return the id of the fees account
     */
    public Account getFeesAccount() {
        return feesAccount;
    }

    public void setPayee(final String payee) {
        this.payee = payee;
    }

    public String getPayee() {
        return payee;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getMemo() {
        return memo;
    }

    /**
     * Calculates the effective interest rate.<br>
     * Ie = (1 + i/m)^(m/n) - 1 <br>
     * n = payments per period m = number of times compounded per period
     *
     * @return effective interest rate
     */
    private double getEffectiveInterestRate() {
        if (interestRate != null && numPayments > 0 && numCompPeriods > 0) {
            double i = interestRate.doubleValue() / 100.0;
            return StrictMath.pow(1.0 + i / numCompPeriods, (double) numCompPeriods / (double) numPayments) - 1.0;
        }
        return 0.0;
    }

    /**
     * Calculates the daily interest rate.<br>
     * This works for US, can't find any information on Canada
     *
     * @return periodic interest rate
     */
    private double getDailyPeriodicInterestRate() {
        if (interestRate != null && numPayments > 0 && numCompPeriods > 0 && daysPerYear != null) {
            double rate = getEffectiveInterestRate();
            rate = rate * numPayments;
            return rate / daysPerYear.doubleValue();
        }
        return 0.0;
    }

    //    /**
    //     * Calculates the sum of compounded interest and principal
    //     * S = P(1+Ie)^n*term
    //     *
    //     * @return sum with interest
    //     */
    //    double getSumWithInterest() {
    //        return getPIPayment() * length;
    //    }

    //    public double getTotalInterestPaid() {
    //        return getSumWithInterest() - originalBalance.doubleValue();
    //    }

    /**
     * Calculates the principal and interest payment of an equal payment series
     * M = P * ( Ie / (1 - (1 + Ie) ^ -N)) N = total number of periods the loan
     * is amortized over.
     *
     * @return P and I
     */
    private double getPIPayment() {

        // zero interest loan
        if ((interestRate == null || interestRate.compareTo(BigDecimal.ZERO) == 0) && length > 0 && numPayments > 0
                && originalBalance != null) {

            return originalBalance.doubleValue() / ((length / 12.0) * numPayments);
        }

        if (length > 0 && numPayments > 0 && numCompPeriods > 0 && originalBalance != null) {
            double i = getEffectiveInterestRate();
            double p = originalBalance.doubleValue();

            return p * (i / (1.0 - StrictMath.pow(1.0 + i, length * -1.0)));
        }
        return 0.0;
    }

    /**
     * Calculates the principal and interest plus finance charges.
     *
     * @return the payment
     */
    private double getPayment() {
        return getPIPayment() + fees.doubleValue();
    }

    /**
     * Calculates the interest portion of the next loan payment given the
     * remaining loan balance.
     *
     * @param balance remaining balance
     * @return interest
     */
    private double getIPayment(final BigDecimal balance) {
        if (balance != null) {
            double i = getEffectiveInterestRate();
            return i * balance.doubleValue();
        }
        return 0.0;
    }

    /**
     * Calculates the interest portion of the next loan payment given the
     * remaining loan balance and the dates between payments.
     *
     * @param balance balance
     * @param start   start date
     * @param end     end date
     * @return interest
     */
    private double getIPayment(final BigDecimal balance, final LocalDate start, final LocalDate end) {
        if (balance != null) {

            int dayEnd = end.getDayOfYear();
            int dayStart = start.getDayOfYear();

            int days = Math.abs(dayEnd - dayStart);

            double i = getDailyPeriodicInterestRate();
            return i * days * balance.doubleValue();
        }
        return 0.0;
    }

    //	/**
    //     * Calculates the principal portion of the next loan payment given the
    //     * remaining loan balance
    //     *
    //     * @param balance balance
    //     * @return principal
    //     */
    //	public double getPPayment(BigDecimal balance) {
    //		return getPIPayment() - getIPayment(balance);
    //	}

    @Nullable
    public Transaction generateTransaction(@NotNull final Account account, @NotNull final LocalDate date, final String number) {

        BigDecimal balance = account.getBalance().abs();
        double payment = getPayment();

        double interest;

        if (getUseDailyRate()) {

            LocalDate last;

            if (account.getTransactionCount() > 0) {
                last = account.getTransactionAt(account.getTransactionCount() - 1).getLocalDate();
            } else {
                last = date;
            }

            interest = getIPayment(balance, last, date); // get the interest portion

        } else {
            interest = getIPayment(balance); // get the interest portion
        }

        // get debit account
        final Account bank = getBankAccount();

        if (bank != null) {
            CommodityNode n = bank.getCurrencyNode();

            Transaction transaction = new Transaction();
            transaction.setDate(date);
            transaction.setNumber(number);
            transaction.setPayee(getPayee());

            // transaction is made relative to the debit/checking account

            TransactionEntry entry = new TransactionEntry();

            // this entry is the principal payment
            entry.setCreditAccount(account);
            entry.setDebitAccount(bank);
            entry.setAmount(n.round(payment - interest));
            entry.setMemo(getMemo());

            transaction.addTransactionEntry(entry);

            // handle interest portion of the payment
            Account i = getInterestAccount();
            if (i != null && interest != 0.0) {
                entry = new TransactionEntry();
                entry.setCreditAccount(i);
                entry.setDebitAccount(bank);
                entry.setAmount(n.round(interest));
                entry.setMemo(ResourceUtils.getString("Word.Interest"));
                transaction.addTransactionEntry(entry);
            }

            // a fee has been assigned
            if (getFees().compareTo(BigDecimal.ZERO) != 0) {
                Account f = getFeesAccount();
                if (f != null) {
                    entry = new TransactionEntry();
                    entry.setCreditAccount(f);
                    entry.setDebitAccount(bank);
                    entry.setAmount(getFees());
                    entry.setMemo(ResourceUtils.getString("Word.Fees"));
                    transaction.addTransactionEntry(entry);
                }
            }

            return transaction;
        }

        return null;
    }


     //Creates a payment transaction relative to the liability account
    /*
    private void paymentActionLiability() {

    AmortizeObject ao = ((LiabilityAccount)account).getAmortizeObject();
    Transaction tran = null;

    if (ao != null) {
    DateChkNumberDialog d = new DateChkNumberDialog(null, engine.getAccount(ao.getInterestAccount()));
    d.show();

    if (!d.getResult()) {
    return;
    }

    BigDecimal balance = account.getBalance().abs();
    BigDecimal fees = ao.getFees();
    double payment = ao.getPayment();

    double interest;

    if (ao.getUseDailyRate()) {
    Date today = d.getDate();
    Date last = account.getTransactionAt(account.getTransactionCount() - 1).getDate();
    interest = ao.getIPayment(balance, last, today); // get the interest portion
    } else {
    interest = ao.getIPayment(balance); // get the interest portion
    }

    Account b = engine.getAccount(ao.getBankAccount());
    if (b != null) {
    CommodityNode n = b.getCommodityNode();
    SplitEntryTransaction e;

    SplitTransaction t = new SplitTransaction(b.getCommodityNode());
    t.setAccount(b);
    t.setMemo(ao.getMemo());
    t.setPayee(ao.getPayee());
    t.setNumber(d.getNumber());
    t.setDate(d.getDate());

    // this entry is the complete payment
    e = new SplitEntryTransaction(n);
    e.setCreditAccount(account);
    e.setDebitAccount(b);
    e.setAmount(n.round(payment));
    e.setMemo(ao.getMemo());
    t.addSplit(e);

    try {   // maintain transaction order (stretch time)
    Thread.sleep(2);
    } catch (Exception ie) {}

    // handle interest portion of the payment
    Account i = engine.getAccount(ao.getInterestAccount());
    if (i != null) {
    e = new SplitEntryTransaction(n);
    e.setCreditAccount(i);
    e.setDebitAccount(account);
    e.setAmount(n.round(interest));
    e.setMemo(rb.getString("Word.Interest"));
    t.addSplit(e);
    }

    try {   // maintain transaction order (stretch time)
    Thread.sleep(2);
    } catch (Exception ie) {}

    // a fee has been assigned
    if (ao.getFees().compareTo(new BigDecimal("0")) != 0) {
    Account f = engine.getAccount(ao.getFeesAccount());
    if (f != null) {
    e = new SplitEntryTransaction(n);
    e.setCreditAccount(f);
    e.setDebitAccount(account);
    e.setAmount(ao.getFees());
    e.setMemo(rb.getString("Word.Fees"));
    t.addSplit(e);
    }
    }

    // the total should be the debit to the checking account
    tran = t;
    }
    }

    if (tran != null) {// display the transaction in the register
    newTransaction(tran);
    } else {    // could not generate the transaction
    if (ao == null) {
    Logger.getLogger("jgnashEngine").warning("Please configure amortization");
    } else {
    Logger.getLogger("jgnashEngine").warning("Not enough information");
    }
    }
    }*/
}
