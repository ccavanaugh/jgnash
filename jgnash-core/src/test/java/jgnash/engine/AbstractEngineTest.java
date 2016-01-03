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
package jgnash.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Craig Cavanaugh
 */
public abstract class AbstractEngineTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    protected String database;

    protected Engine e;

    Account incomeAccount;

    Account expenseAccount;

    Account usdBankAccount;

    Account equityAccount;

    Account investAccount;

    SecurityNode securityNode1;

    protected static final char[] PASSWORD = new char[]{};

    protected abstract Engine createEngine() throws IOException;

    @Before
    public void setUp() throws Exception {
        e = createEngine();
        e.setCreateBackups(false);

        assertNotNull(e);

        // Creating currencies
        CurrencyNode defaultCurrency = DefaultCurrencies.buildCustomNode("USD");

        e.addCurrency(defaultCurrency);
        e.setDefaultCurrency(defaultCurrency);

        CurrencyNode cadCurrency = DefaultCurrencies.buildCustomNode("CAD");
        e.addCurrency(cadCurrency);

        // Creating securities
        securityNode1 = new SecurityNode(defaultCurrency);

        securityNode1.setSymbol("GOOGLE");
        assertTrue(e.addSecurity(securityNode1));

        // Creating accounts
        incomeAccount = new Account(AccountType.INCOME, defaultCurrency);
        incomeAccount.setName("Income Account");
        e.addAccount(e.getRootAccount(), incomeAccount);

        expenseAccount = new Account(AccountType.EXPENSE, defaultCurrency);
        expenseAccount.setName("Expense Account");
        e.addAccount(e.getRootAccount(), expenseAccount);

        usdBankAccount = new Account(AccountType.BANK, defaultCurrency);
        usdBankAccount.setName("USD Bank Account");
        e.addAccount(e.getRootAccount(), usdBankAccount);

        Account cadBankAccount = new Account(AccountType.BANK, cadCurrency);
        cadBankAccount.setName("CAD Bank Account");
        e.addAccount(e.getRootAccount(), cadBankAccount);

        equityAccount = new Account(AccountType.EQUITY, defaultCurrency);
        equityAccount.setName("Equity Account");
        e.addAccount(e.getRootAccount(), equityAccount);

        Account liabilityAccount = new Account(AccountType.LIABILITY, defaultCurrency);
        liabilityAccount.setName("Liability Account");
        e.addAccount(e.getRootAccount(), liabilityAccount);

        investAccount = new Account(AccountType.INVEST, defaultCurrency);
        investAccount.setName("Invest Account");
        e.addAccount(e.getRootAccount(), investAccount);

        // Adding security to the invest account
        List<SecurityNode> securityNodeList = new ArrayList<>();
        securityNodeList.add(securityNode1);
        assertTrue(e.updateAccountSecurities(investAccount, securityNodeList));
    }

    @After
    public void tearDown() throws IOException {
        EngineFactory.closeEngine(EngineFactory.DEFAULT);
        EngineFactory.deleteDatabase(database);

        Files.deleteIfExists(Paths.get(database));
    }
}
