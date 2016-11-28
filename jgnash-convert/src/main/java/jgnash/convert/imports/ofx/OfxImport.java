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
package jgnash.convert.imports.ofx;

import java.util.List;
import java.util.Objects;

import jgnash.convert.imports.GenericImport;
import jgnash.convert.imports.ImportTransaction;
import jgnash.engine.Account;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;

/**
 * OfxImport utility methods
 *
 * @author Craig Cavanaugh
 */
public class OfxImport {

    public static void importTransactions(final List<ImportTransaction> transactions, final Account baseAccount) {
        GenericImport.importTransactions(transactions, baseAccount);
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

    /**
     * Private constructor, utility class
     */
    private OfxImport() {
    }
}
