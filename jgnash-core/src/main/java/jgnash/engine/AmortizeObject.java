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
package jgnash.engine;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

/**
 * This class is used to calculate loan payments.
 * <p>
 * Because BigDecimal is lacking methods of exponents, calculations are performed using StrictMath to maintain
 * portability. Results are returned as doubles. Results will need to be scaled and rounded.
 * 
 * @author Craig Cavanaugh
 * @version $Id: AmortizeObject.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class AmortizeObject {

    private Account interestAccount; // account for interest payment

    private Account bankAccount; // account for principal account

    // (normally the liability account)
    private Account feesAccount; // account id to place non interest fees

    /**
     * the number of payments per year
     */
    private int numPayments;

    /**
     * length of loan in months
     */
    private int length;

    /**
     * the number of compounding periods per year
     */
    private int numCompPeriods;

    /**
     * annual interest rate, APR (ex 6.75)
     */
    private BigDecimal interestRate;

    /**
     * original balance of the loan
     */
    private BigDecimal originalBalance;

    /**
     * PMI, escrow, etc
     */
    private BigDecimal fees = BigDecimal.ZERO;

    /**
     * the payee to use
     */
    private String payee;

    /**
     * the memo to use
     */
    private String memo;

    // private String checkNumber;     // check number to use

    /**
     * origination date
     */
    private Date date = new Date();

    /**
     * calculate interest based on daily periodic rate
     */
    private boolean useDailyRate;

    /**
     * the number of days per year for daily periodic rate
     */
    private BigDecimal daysPerYear;

    /**
     * Empty constructor to keep reflection happy
     */
    public AmortizeObject() {
    }

    public void setDate(Date date) {
        this.date = (Date) date.clone();
    }

    public Date getDate() {
        return date;
    }

    public void setPaymentPeriods(int periods) {
        numPayments = periods;
    }

    public int getPaymentPeriods() {
        return numPayments;
    }

    /**
     * Sets the length of the loan (in months)
     * 
     * @param months length of loan
     */
    public void setLength(int months) {
        this.length = months;
    }

    /**
     * Gets the length of the loan in months
     * 
     * @return length of loan in months
     */
    public int getLength() {
        return length;
    }

    /**
     * Determines if interest will be calculate based on a daily periodic rate, or if it is assumed that the interest is
     * paid exactly on the due date.
     * 
     * @param daily true if interest should be calculated using a daily rate
     */
    public void setUseDailyRate(boolean daily) {
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
     * Sets the number of days per year used to calculate the daily periodic interest rate. The value can be a decimal.
     * 
     * @param days The number of days in a year
     */
    public void setDaysPerYear(BigDecimal days) {
        daysPerYear = days;
    }

    /**
     * Returns the number of days per year used to calculate a daily periodic interest rate.
     * 
     * @return The number of days per year
     */
    public BigDecimal getDaysPerYear() {
        return daysPerYear;
    }

    public void setRate(BigDecimal rate) {
        interestRate = rate;
    }

    public BigDecimal getRate() {
        return interestRate;
    }

    public void setPrincipal(BigDecimal principal) {
        originalBalance = principal;
    }

    public BigDecimal getPrincipal() {
        return originalBalance;
    }

    public void setInterestPeriods(int periods) {
        numCompPeriods = periods;
    }

    public int getInterestPeriods() {
        return numCompPeriods;
    }

    public void setFees(BigDecimal fees) {
        if (fees != null) {
            this.fees = fees;
        } else {
            this.fees = BigDecimal.ZERO;
        }
    }

    public BigDecimal getFees() {
        return fees;
    }

    /**
     * Set the id of the interest account
     * 
     * @param id the id of the interest account
     */
    public void setInterestAccount(Account id) {
        interestAccount = id;
    }

    /**
     * Returns the id of the interest account
     * 
     * @return the id of the interest account
     */
    public Account getInterestAccount() {
        return interestAccount;
    }

    /**
     * Set the id of the principal account
     * 
     * @param id the id of the principal account
     */
    public void setBankAccount(Account id) {
        bankAccount = id;
    }

    /**
     * Returns the id of the principal account
     * 
     * @return the id of the principal account
     */
    public Account getBankAccount() {
        return bankAccount;
    }

    /**
     * Set the id of the fees account
     * 
     * @param id the id of the fees account
     */
    public void setFeesAccount(Account id) {
        feesAccount = id;
    }

    /**
     * Returns the id of the fees account
     * 
     * @return the id of the fees account
     */
    public Account getFeesAccount() {
        return feesAccount;
    }

    public void setPayee(String payee) {
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
    double getEffectiveInterestRate() {
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
    double getDailyPeriodicInterestRate() {
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
     * Calculates the principal and interest payment of an equal payment series M = P * ( Ie / (1 - (1 + Ie) ^ -N)) N =
     * total number of periods the loan is amortized over
     * 
     * @return P and I
     */
    double getPIPayment() {
        if (length > 0 && numPayments > 0 && numCompPeriods > 0 && originalBalance != null && interestRate != null) {
            double i = getEffectiveInterestRate();
            double p = originalBalance.doubleValue();

            return p * (i / (1.0 - StrictMath.pow(1.0 + i, length * -1.0)));
        }
        return 0.0;
    }

    /**
     * Calculates the principal and interest plus finance charges
     * 
     * @return the payment
     */
    public double getPayment() {
        return getPIPayment() + fees.doubleValue();
    }

    /**
     * Calculates the interest portion of the next loan payment given the remaining loan balance
     * 
     * @param balance remaining balance
     * @return interest
     */
    public double getIPayment(BigDecimal balance) {
        if (balance != null) {
            double i = getEffectiveInterestRate();
            return i * balance.doubleValue();
        }
        return 0.0;
    }

    /**
     * Calculates the interest portion of the next loan payment given the remaining loan balance and the dates between
     * payments
     * 
     * @param balance balance
     * @param start start date
     * @param end end date
     * @return interest
     */
    public double getIPayment(BigDecimal balance, Date start, Date end) {
        if (balance != null) {

            Calendar c = Calendar.getInstance();
            c.setTime(end);
            int dayEnd = c.get(Calendar.DAY_OF_YEAR);
            c.setTime(start);
            int dayStart = c.get(Calendar.DAY_OF_YEAR);

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
}
