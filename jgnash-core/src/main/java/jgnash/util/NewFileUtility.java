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
package jgnash.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.ResourceBundle;

import jgnash.engine.Account;
import jgnash.engine.AccountTreeXMLFactory;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.RootAccount;
import jgnash.resource.util.ResourceUtils;

/**
 * Utility class for generating new files.
 *
 * @author Craig Cavanaugh
 */
public class NewFileUtility {

    public static void buildNewFile(final String fileName, final DataStoreType dataStoreType, final char[] password,
                                    final CurrencyNode currencyNode, final Collection<CurrencyNode> currencyNodes,
                                    final Collection<RootAccount> rootAccountCollection) throws IOException {
        final ResourceBundle resources = ResourceUtils.getBundle();

        // have to close the engine first
        EngineFactory.closeEngine(EngineFactory.DEFAULT);

        // try to delete any existing database
        if (Files.exists(Paths.get(fileName))) {
            if (!EngineFactory.deleteDatabase(fileName)) {
                throw new IOException(ResourceUtils.getString("Message.Error.DeleteExistingFile", fileName));
            }
        }

        // create the directory if needed
        Files.createDirectories(Paths.get(fileName).getParent());

        final Engine e = EngineFactory.bootLocalEngine(fileName, EngineFactory.DEFAULT, password, dataStoreType);

        CurrencyNode defaultCurrency = currencyNode;

        // match the existing default to the new default.  If they match, replace to prevent
        // creation of a duplicate currency
        if (e.getDefaultCurrency().matches(defaultCurrency)) {
            defaultCurrency = e.getDefaultCurrency();
        }

        // make sure a duplicate default is not added
        for (final CurrencyNode node : currencyNodes) {
            if (!node.matches(defaultCurrency)) {
                e.addCurrency(node);
            }
        }

        if (!defaultCurrency.equals(e.getDefaultCurrency())) {
            e.setDefaultCurrency(defaultCurrency);
        }

        if (!rootAccountCollection.isEmpty()) { // import account sets
            for (final RootAccount root : rootAccountCollection) {
                AccountTreeXMLFactory.importAccountTree(e, root);
            }
        } else { // none selected, create a very basic account set
            final RootAccount root = e.getRootAccount();

            final Account bank = new Account(AccountType.BANK, defaultCurrency);
            bank.setDescription(resources.getString("Name.BankAccounts"));
            bank.setName(resources.getString("Name.BankAccounts"));

            e.addAccount(root, bank);

            final Account income = new Account(AccountType.INCOME, defaultCurrency);
            income.setDescription(resources.getString("Name.IncomeAccounts"));
            income.setName(resources.getString("Name.IncomeAccounts"));

            e.addAccount(root, income);

            final Account expense = new Account(AccountType.EXPENSE, defaultCurrency);
            expense.setDescription(resources.getString("Name.ExpenseAccounts"));
            expense.setName(resources.getString("Name.ExpenseAccounts"));

            e.addAccount(root, expense);
        }
    }

    private NewFileUtility() {
        // Utility class
    }
}
