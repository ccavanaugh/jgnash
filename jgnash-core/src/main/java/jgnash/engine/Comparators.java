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
package jgnash.engine;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Deque;

/**
 * Utility class consisting of {@code Comparators} useful for sorting lists of {@code StoredObject}
 * with the same and mixed inheritance.
 *
 * @author Craig Cavanaugh
 */
public class Comparators {

    private static volatile Comparator<Transaction> transactionByDate = null;
    private static volatile Comparator<Transaction> transactionByAmount = null;
    private static volatile Comparator<Transaction> transactionByMemo = null;
    private static volatile Comparator<Transaction> transactionByNumber = null;
    private static volatile Comparator<Transaction> transactionByPayee = null;
    private static volatile Comparator<Transaction> transactionBySecurity = null;
    private static volatile Comparator<Transaction> transactionByType = null;

    public static Comparator<Account> getAccountByCode() {
        return new AccountByCode();
    }

    public static Comparator<Account> getAccountByName() {
        return new AccountByName();
    }

    public static Comparator<Account> getAccountByPathName() {
        return new AccountByPathName();
    }

    public static Comparator<Account> getAccountByBalance(LocalDate startDate, LocalDate endDate, CurrencyNode currency, boolean ascending) {
        return new AccountByBalance(startDate, endDate, currency, ascending);
    }

    /**
     * Sort {@code Account}s according to their position in the account tree. Parent accounts are
     * sorted before their children.
     *
     * @param subComparator defines the sort order of accounts which have the same parent account
     * @return the {@code Comparator}
     */
    public static Comparator<Account> getAccountByTreePosition(Comparator<Account> subComparator) {
        return new AccountByTreePosition(subComparator);
    }

    public static Comparator<Transaction> getTransactionByDate() {
        if (transactionByDate != null) {
            return transactionByDate;
        }
        return transactionByDate = new TransactionByDate();
    }

    private static class TransactionByDate implements Comparator<Transaction>, Serializable {

        @Override
        public int compare(final Transaction o1, final Transaction o2) {
            return o1.compareTo(o2);
        }
    }

    public static Comparator<Transaction> getTransactionByAmount() {
        if (transactionByAmount != null) {
            return transactionByAmount;
        }
        return transactionByAmount = new TransactionByAmount();
    }

    private static class AccountByCode implements Comparator<Account>, Serializable {

        @Override
        public int compare(final Account a1, final Account a2) {

            // Sort by account code first
            int result = Integer.compare(a1.getAccountCode(), a2.getAccountCode());
            if (result != 0) {
                return result;
            }

            return a1.getName().compareTo(a2.getName());
        }
    }

    private static class AccountByName implements Comparator<Account>, Serializable {

        @Override
        public int compare(final Account a1, final Account a2) {
            return a1.getName().compareTo(a2.getName());
        }
    }

    private static class AccountByPathName implements Comparator<Account>, Serializable {

        @Override
        public int compare(Account a1, Account a2) {
            return a1.getPathName().compareTo(a2.getPathName());
        }
    }

    private static class AccountByBalance implements Comparator<Account>, Serializable {

        private final LocalDate startDate;

        private final LocalDate endDate;

        private final boolean ascending;

        private final CurrencyNode currency;

        AccountByBalance(final LocalDate startDate, final LocalDate endDate, CurrencyNode currency, boolean ascending) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.currency = currency;
            this.ascending = ascending;
        }

        @Override
        public int compare(Account a1, Account a2) {
            int result = a1.getBalance(startDate, endDate, currency)
                    .compareTo(a2.getBalance(startDate, endDate, currency));
            if (!ascending) {
                result *= -1;
            }
            return result;
        }
    }

    private static class AccountByTreePosition implements Comparator<Account>, Serializable {

        private final Comparator<Account> subComparator;

        AccountByTreePosition(final Comparator<Account> subComparator) {
            this.subComparator = subComparator;
        }

        private Deque<Account> accountPath(Account acc) {
            final Deque<Account> path = new LinkedList<>();

            while (acc != null) {
                path.addFirst(acc);
                acc = acc.getParent();
            }
            return path;
        }

        @Override
        public int compare(final Account a1, final Account a2) {
            final Deque<Account> path1 = accountPath(a1);
            final Deque<Account> path2 = accountPath(a2);

            // find the first non-common ancestors
            Account pa1, pa2;

            do {
                pa1 = path1.pollFirst();
                pa2 = path2.pollFirst();
            } while (pa1 != null && pa2 != null && pa1.equals(pa2));

            if (pa1 == null && pa2 == null) {
                // this can only happen if a1 equals a2
                return 0;
            }

            // if one of the paths ended, this is an ancestor of the other one, so sort it first;
            // otherwise, let the subComparator decide on the same-level ancestors
            return (pa1 == null) ? -1 : (pa2 == null) ? 1 : subComparator.compare(pa1, pa2);
        }

    }

    private static class TransactionByAmount implements Comparator<Transaction>, Serializable {

        @Override
        public int compare(final Transaction o1, final Transaction o2) {
            TransactionEntry t1 = o1.getTransactionEntries().get(0);
            TransactionEntry t2 = o2.getTransactionEntries().get(0);

            int result = t1.getAmount(t1.getCreditAccount()).compareTo(t2.getAmount(t2.getCreditAccount()));

            if (result != 0) {
                return result;
            }
            return o1.compareTo(o2);
        }
    }

    /**
     * Comparator for sorting transactions by account.
     */
    public static class TransactionByAccount implements Comparator<Transaction>, Serializable {

        private final Account baseAccount;

        public TransactionByAccount(final Account baseAccount) {
            this.baseAccount = baseAccount;
        }

        @Override
        public int compare(final Transaction o1, final Transaction o2) {
            if (o1 == o2) {
                return 0;
            }

            String s1 = getAccount(o1);
            String s2 = getAccount(o2);

            int result = s1.compareTo(s2);

            if (result != 0) {
                return result;
            }
            return o1.compareTo(o2);
        }

        private String getAccount(final Transaction t) {

            if (t.getTransactionEntries().size() == 1) {
                if (t.getTransactionEntries().get(0).getCreditAccount() != baseAccount) {
                    return t.getTransactionEntries().get(0).getCreditAccount().getPathName();
                }
                return t.getTransactionEntries().get(0).getDebitAccount().getPathName();
            }
            return "!split!";
        }
    }

    public static Comparator<Transaction> getTransactionByPayee() {
        if (transactionByPayee != null) {
            return transactionByPayee;
        }
        return transactionByPayee = new TransactionByPayee();
    }

    private static class TransactionByPayee implements Comparator<Transaction>, Serializable {

        @Override
        public int compare(final Transaction o1, final Transaction o2) {
            int result = o1.getPayee().compareTo(o2.getPayee());

            if (result != 0) {
                return result;
            }
            return o1.compareTo(o2);
        }
    }

    public static Comparator<Transaction> getTransactionByMemo() {
        if (transactionByMemo != null) {
            return transactionByMemo;
        }
        return transactionByMemo = new TransactionByMemo();
    }

    private static class TransactionByMemo implements Comparator<Transaction>, Serializable {

        @Override
        public int compare(final Transaction o1, final Transaction o2) {
            int result = o1.getMemo().compareTo(o2.getMemo());

            if (result != 0) {
                return result;
            }
            return o1.compareTo(o2);
        }
    }

    public static Comparator<Transaction> getTransactionByNumber() {
        if (transactionByNumber != null) {
            return transactionByNumber;
        }
        return transactionByNumber = new TransactionByNumber();
    }

    private static class TransactionByNumber implements Comparator<Transaction>, Serializable {

        @Override
        public int compare(final Transaction o1, final Transaction o2) {
            int result = o1.getNumber().compareTo(o2.getNumber());

            if (result != 0) {
                return result;
            }
            return o1.compareTo(o2);
        }
    }

    public static Comparator<Transaction> getTransactionBySecurity() {
        if (transactionBySecurity != null) {
            return transactionBySecurity;
        }
        return transactionBySecurity = new TransactionBySecurity();
    }

    private static class TransactionBySecurity implements Comparator<Transaction>, Serializable {

        @Override
        public int compare(final Transaction o1, final Transaction o2) {
            if (o1 instanceof InvestmentTransaction && o2 instanceof InvestmentTransaction) {
                int result = ((InvestmentTransaction) o1).getSecurityNode().compareTo(((InvestmentTransaction) o2).getSecurityNode());

                if (result != 0) {
                    return result;
                }
                return o1.compareTo(o2);
            } else if (o1 instanceof InvestmentTransaction) {
                return -1;
            } else if (o2 instanceof InvestmentTransaction) {
                return 1;
            } else {
                return o1.compareTo(o2);
            }
        }
    }

    public static Comparator<Transaction> getTransactionByType() {
        if (transactionByType != null) {
            return transactionByType;
        }
        return transactionByType = new TransactionByType();
    }

    private static class TransactionByType implements Comparator<Transaction>, Serializable {

        @Override
        public int compare(final Transaction o1, final Transaction o2) {
            int result = o1.getTransactionType().compareTo(o2.getTransactionType());

            if (result != 0) {
                return result;
            }
            return o1.compareTo(o2);
        }
    }

    /**
     * Explicit order Comparator.
     *
     * @param <T> object type that is being sorted
     */
    public static class ExplicitComparator<T> implements Comparator<T>, Serializable {

        final List<T> order = new ArrayList<>();

        @SafeVarargs
        public ExplicitComparator(final T... objects) {
            Collections.addAll(order, objects);
        }

        @Override
        public int compare(final T t1, final T t2) {
            return Integer.compare(order.indexOf(t1), order.indexOf(t2));
        }
    }

    private Comparators() {
    }
}
