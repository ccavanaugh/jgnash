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

import java.util.Objects;
import java.util.Optional;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.SecurityNode;

/**
 * Various utility methods used when importing transactions
 *
 * @author Craig Cavanaugh
 */
public class ImportUtils {

    private ImportUtils() {
    }

    public static Account getRootExpenseAccount() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        return searchForRootType(engine.getRootAccount(), AccountType.EXPENSE);
    }

    public static Account getRootIncomeAccount() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        return searchForRootType(engine.getRootAccount(), AccountType.INCOME);
    }

    /**
     * Matches an ImportTransaction to an Account based on an AccountTo tag if it exists
     * @param importTransaction Import transaction to test
     * @return Account if found, null otherwise
     */
    public static Account matchAccount(final ImportTransaction importTransaction) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final String number = importTransaction.getAccountTo();

        Account account = null;

        if (number != null) {
            for (final Account a : engine.getAccountList()) {
                if (a.getAccountNumber().equals(number)) {
                    account = a;
                    break;
                }
            }
        }

        return account;
    }

    private static Account searchForRootType(final Account account, final AccountType accountType) {
        Account result = null;

        // search immediate top level accounts
        for (Account a : account.getChildren()) {
            if (a.getAccountType().equals(accountType)) {
                return a;
            }
        }

        // recursive search
        for (Account a : account.getChildren()) {
            result = searchForRootType(a, accountType);
            if (result != null) {
                break;
            }
        }

        return result;
    }

    static Optional<SecurityNode> matchSecurity(final ImportSecurity security) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        for (final SecurityNode securityNode : engine.getSecurities()) {
            if (securityNode.getSymbol().equals(security.getTicker())) {
                return Optional.of(securityNode);
            }
        }
        return Optional.empty();
    }

    static SecurityNode createSecurityNode(final ImportSecurity security, final CurrencyNode currencyNode) {
        final SecurityNode securityNode = new SecurityNode(currencyNode);

        securityNode.setSymbol(security.getTicker());
        securityNode.setScale(currencyNode.getScale());

        security.getSecurityName().ifPresent(securityNode::setDescription);
        security.getId().ifPresent(securityNode::setISIN);

        return securityNode;
    }
}
