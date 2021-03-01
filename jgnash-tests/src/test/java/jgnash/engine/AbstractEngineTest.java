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

import io.github.glytching.junit.extension.folder.TemporaryFolder;
import io.github.glytching.junit.extension.folder.TemporaryFolderExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abstract base for testing the core engine API's.
 *
 * @author Craig Cavanaugh
 */
@ExtendWith(TemporaryFolderExtension.class)
public abstract class AbstractEngineTest {

    protected TemporaryFolder testFolder;

    protected String database;

    protected Engine e;

    Account incomeAccount;

    Account expenseAccount;

    protected Account usdBankAccount;

    protected Account checkingAccount;

    Account equityAccount;

    protected Account investAccount;

    SecurityNode gggSecurityNode;

    protected abstract Engine createEngine() throws IOException;

    @BeforeEach
    public void setUp(final TemporaryFolder testFolder) throws Exception {
        Locale.setDefault(Locale.US);

        this.testFolder = testFolder;

        e = createEngine();

        assertNotNull(e);

        e.setCreateBackups(false);

        // Creating currencies
        CurrencyNode defaultCurrency = DefaultCurrencies.buildCustomNode("USD");

        e.addCurrency(defaultCurrency);
        e.setDefaultCurrency(defaultCurrency);

        CurrencyNode cadCurrency = DefaultCurrencies.buildCustomNode("CAD");
        e.addCurrency(cadCurrency);

        // Creating accounts
        incomeAccount = new Account(AccountType.INCOME, defaultCurrency);
        incomeAccount.setName("Income Account");
        e.addAccount(e.getRootAccount(), incomeAccount);

        expenseAccount = new Account(AccountType.EXPENSE, defaultCurrency);
        expenseAccount.setName("Expense Account");
        e.addAccount(e.getRootAccount(), expenseAccount);

        usdBankAccount = new Account(AccountType.BANK, defaultCurrency);
        usdBankAccount.setName("USD Bank Account");
        usdBankAccount.setBankId("xyzabc");
        usdBankAccount.setAccountNumber("10001-A01");
        e.addAccount(e.getRootAccount(), usdBankAccount);

        checkingAccount = new Account(AccountType.CHECKING, defaultCurrency);
        checkingAccount.setName("Checking Account");
        checkingAccount.setBankId("xyzabc");
        checkingAccount.setAccountNumber("10001-C01");
        e.addAccount(e.getRootAccount(), checkingAccount);

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

        // Creating securities
        gggSecurityNode = new SecurityNode(defaultCurrency);
        gggSecurityNode.setSymbol("GOOGLE");
        assertTrue(e.addSecurity(gggSecurityNode));

        // Adding security to the invest account
        List<SecurityNode> securityNodeList = new ArrayList<>();
        securityNodeList.add(gggSecurityNode);
        assertTrue(e.updateAccountSecurities(investAccount, securityNodeList));
    }

    @AfterEach
    public void tearDown() throws IOException {
        EngineFactory.closeEngine(EngineFactory.DEFAULT);
        EngineFactory.deleteDatabase(database);

        Files.deleteIfExists(Paths.get(database));
    }
}
