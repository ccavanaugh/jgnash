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

import java.util.List;
import java.util.Set;

import jgnash.bayes.BayesClassifier;
import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionType;

/**
 * Bayes classifier import utility methods
 *
 * @author Craig Cavanaugh
 */
public class BayesImportClassifier {

    /**
     * Utility class, private constructor
     */
    private BayesImportClassifier() {
    }

    public static void classifyTransactions(final List<? extends ImportTransaction> list,
                                            final List<Transaction> transactions, final Account baseAccount) {

        final BayesClassifier<Account> classifier = generateClassifier(transactions, baseAccount);

        for (final ImportTransaction transaction : list) {
            final StringBuilder builder = new StringBuilder();

            builder.append(transaction.getPayee()).append(" ");

            if (transaction.getMemo() != null) {
                builder.append(transaction.getMemo());
            }

            // reinvested dividends do not have a cash account
            if (transaction.getTransactionType() != TransactionType.REINVESTDIV) {
                transaction.setAccount(classifier.classify(builder.toString()));
            }
        }
    }

    private static BayesClassifier<Account> generateClassifier(List<Transaction> transactions,
                                                               final Account baseAccount) {

        final BayesClassifier<Account> classifier = new BayesClassifier<>(baseAccount);

        for (final Transaction t : transactions) {
            final Set<Account> accountSet = t.getAccounts();

            accountSet.remove(baseAccount);

            for (final Account account : accountSet) {
                if (!t.getPayee().isEmpty()) {
                    classifier.train(t.getPayee(), account);
                }

                if (!t.getMemo().isEmpty()) {
                    classifier.train(t.getMemo(), account);
                }
            }
        }

        return classifier;
    }
}
