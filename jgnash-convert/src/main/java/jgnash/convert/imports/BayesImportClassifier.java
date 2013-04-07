/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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

import java.util.List;
import java.util.Set;

import jgnash.bayes.BayesClassifier;
import jgnash.engine.Account;
import jgnash.engine.Transaction;

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

    public static void classifyTransactions(final List<? extends ImportTransaction> list, final Account baseAccount) {
        BayesClassifier<Account> classifier = generateClassifier(baseAccount);

        for (ImportTransaction transaction : list) {
            StringBuilder builder = new StringBuilder();

            if (transaction.payee != null) {
                builder.append(transaction.payee).append(" ");
            }

            if (transaction.memo != null) {
                builder.append(transaction.memo);
            }

            transaction.account = classifier.classify(builder.toString());
        }
    }

    private static BayesClassifier<Account> generateClassifier(final Account baseAccount) {
        BayesClassifier<Account> classifier = new BayesClassifier<>(baseAccount);

        for (Transaction t : baseAccount.getReadOnlyTransactionCollection()) {
            Set<Account> accountSet = t.getAccounts();
            accountSet.remove(baseAccount);

            for (Account account : accountSet) {


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
