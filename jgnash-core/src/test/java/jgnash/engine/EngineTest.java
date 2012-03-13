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
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetGoal;
import jgnash.engine.budget.BudgetPeriod;
import jgnash.engine.recurring.DailyReminder;
import jgnash.engine.recurring.Reminder;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Craig Cavanaugh
 * @version $Id: EngineTest.java 3173 2012-02-10 17:31:43Z ccavanaugh $
 */
public abstract class EngineTest {

    protected Engine e;

    protected String testFile;

    protected boolean oldExportState = EngineFactory.exportXMLOnClose();

    public abstract Engine createEngine();

    @Before
    public void setUp() throws Exception {

        EngineFactory.setExportXMLOnClose(false);

        e = createEngine();

        assertNotNull(e);

        CurrencyNode node = e.getDefaultCurrency();

        if (!node.getSymbol().equals("USD")) {
            CurrencyNode defaultCurrency = DefaultCurrencies.buildNode(Locale.US);

            assertNotNull(defaultCurrency);
            assertTrue(e.addCommodity(defaultCurrency));

            e.setDefaultCurrency(defaultCurrency);
        }

        node = e.getCurrency("CAD");

        if (node == null) {
            node = DefaultCurrencies.buildNode(Locale.CANADA);

            assertNotNull(node);

            assertTrue(e.addCommodity(node));
        }
    }

    @After
    public void tearDown() {
        EngineFactory.closeEngine(EngineFactory.DEFAULT);
        EngineFactory.setExportXMLOnClose(oldExportState);
    }

    @Test
    public void testGetName() {
        assertEquals(e.getName(), EngineFactory.DEFAULT);
    }

    @Test
    public void testReminders() {

        assertEquals(0, e.getReminders().size());

        assertEquals(0, e.getPendingReminders().size());

        Reminder r = new DailyReminder();
        r.setIncrement(1);
        r.setEndDate(null);

        e.addReminder(r);

        assertEquals(1, e.getReminders().size());
        assertEquals(1, e.getPendingReminders().size());

        // close and reopen to force check for persistence
        EngineFactory.closeEngine(EngineFactory.DEFAULT);
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT);
        assertEquals(e.getReminders().size(), 1);

        // remove a reminder
        e.removeReminder(e.getReminders().get(0));
        assertEquals(0, e.getReminders().size());
        assertEquals(0, e.getPendingReminders().size());

        // close and reopen to force check for persistence
        EngineFactory.closeEngine(EngineFactory.DEFAULT);
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT);
        assertEquals(0, e.getReminders().size());
        assertEquals(0, e.getPendingReminders().size());
    }

    @Test
    public void testGetStoredObjectByUuid() {

        // close and reopen to force check for persistence
        EngineFactory.closeEngine(EngineFactory.DEFAULT);
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT);

        String uuid = e.getDefaultCurrency().getUuid();

        assertSame(e.getDefaultCurrency(), e.getStoredObjectByUuid(uuid));
    }

    @Test
    public void testGetStoredObjects() {
        assertTrue(e.getStoredObjects().size() > 0);
    }

    @Test
    public void testAddCommodity() {
        // close and reopen to force check for persistence
        EngineFactory.closeEngine(EngineFactory.DEFAULT);


        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT);
        
        assertNotNull(e.getDefaultCurrency());
        assertNotNull(e.getCurrency("USD"));
        assertNotNull(e.getCurrency("CAD"));
    }

    @Ignore
    @Test
    public void testAddSecurityHistory() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testGetInvestmentAccountListSecurityNode() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testGetMarketPrice() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testBuildExchangeRateId() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testGetBaseCurrencies() {
        fail("Not yet implemented");
    }

    @Test
    public void testBudget() {

        final String ACCOUNT_NAME = "testAccount";

        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.BANK, node);
        a.setName(ACCOUNT_NAME);

        e.addAccount(e.getRootAccount(), a);

        assertEquals(0, e.getBudgetList().size());

        Budget budget = new Budget();
        budget.setName("Default");
        budget.setDescription("Default Budget");

        BigDecimal[] budgetGoals = new BigDecimal[BudgetGoal.PERIODS];

        BudgetGoal goal = new BudgetGoal();

        // load the goals
        for (int i = 0; i < budgetGoals.length; i++) {
            budgetGoals[i] = new BigDecimal(i);
        }
        goal.setGoals(budgetGoals);
        goal.setBudgetPeriod(BudgetPeriod.WEEKLY);

        budget.setBudgetGoal(a, goal);
        budget.setBudgetPeriod(BudgetPeriod.WEEKLY);

        e.addBudget(budget);

        assertEquals(1, e.getBudgetList().size());

        // close and reopen to force check for persistence
        EngineFactory.closeEngine(EngineFactory.DEFAULT);
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT);
        assertEquals(1, e.getBudgetList().size());

        a = e.getAccountByName(ACCOUNT_NAME);
        assertNotNull(a);

        Budget recovered = e.getBudgetList().get(0);
        budgetGoals = recovered.getBudgetGoal(a).getGoals();

        // check the goals
        assertEquals(BudgetGoal.PERIODS, budgetGoals.length);

        // check the periods
        assertEquals(BudgetPeriod.WEEKLY, recovered.getBudgetPeriod());
        assertEquals(BudgetPeriod.WEEKLY, recovered.getBudgetGoal(a).getBudgetPeriod());

        for (int i = 0; i < budgetGoals.length; i++) {
            assertEquals(new BigDecimal(i), budgetGoals[i]);
        }

        // remove a budget
        e.removeBudget(e.getBudgetList().get(0));
        assertEquals(0, e.getBudgetList().size());

        // close and reopen to force check for persistence
        EngineFactory.closeEngine(EngineFactory.DEFAULT);
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT);
        assertEquals(0, e.getBudgetList().size());
    }

    @Test
    public void testGetActiveCurrencies() {
        Set<CurrencyNode> nodes = e.getActiveCurrencies();

        assertNotNull(nodes);

        if (e.getTransactions().size() > 0) {
            assertTrue(nodes.size() > 0);
        } else {
            assertTrue(nodes.size() > 0);
        }

        System.out.println("Node count is " + nodes.size());
    }

    @Test
    public void testGetCurrencyStringLocale() {
        CurrencyNode currency = e.getCurrency("USD");
        assertNotNull(currency);
    }

    @Test
    public void testGetCurrencyString() {
        CurrencyNode currency = e.getCurrency("USD");
        assertNotNull(currency);

        currency = e.getCurrency("CAD");
        assertNotNull(currency);
    }

    @Test
    public void testGetCurrencies() {

        // close and reopen to force check for persistence
        EngineFactory.closeEngine(EngineFactory.DEFAULT);
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT);

        List<CurrencyNode> nodes = e.getCurrencies();
        assertNotNull(nodes);
        System.out.println(nodes.size());
        assertTrue(nodes.size() > 1);
    }

    @Ignore
    @Test
    public void testGetSecurityHistory() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testGetExchangeRate() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testGetSecurities() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testGetSecurity() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testRemoveCommodity() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testRemoveSecurityHistory() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testSetDefaultCurrency() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetDefaultCurrency() {
        CurrencyNode defaultCurrency = e.getDefaultCurrency();

        assertNotNull(defaultCurrency);
        assertEquals(defaultCurrency.getSymbol(), "USD");
    }

    @Ignore
    @Test
    public void testSetExchangeRateCurrencyNodeCurrencyNodeBigDecimal() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testSetExchangeRateCurrencyNodeCurrencyNodeBigDecimalDate() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testRemoveExchangeRateHistory() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testUpdateCommodity() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testUpdateReminder() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetAccountSeparator() {
        String newSep = ".";

        String oldSep = e.getAccountSeparator();
        assertNotNull(oldSep);

        e.setAccountSeparator(newSep);

        assertEquals(e.getAccountSeparator(), newSep);

        e.setAccountSeparator(oldSep);

        assertEquals(e.getAccountSeparator(), oldSep);
    }

    @Test
    public void testGetAccountSeparator() {
        String sep = e.getAccountSeparator();
        assertNotNull(sep);

        if (sep != null) {
            assertTrue(sep.length() > 0);
        }
    }

    @Ignore
    @Test
    public void testGetAccountList() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testGetAccountByUuid() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testGetAccountByName() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testGetIncomeAccountList() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testGetExpenseAccountList() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testGetBankAccountList() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testGetInvestmentAccountList() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testRefreshAccount() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testRefreshCommodity() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testRefreshExchangeRate() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testRefreshReminder() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testRefreshTransaction() {
        fail("Not yet implemented");
    }

    @Test
    public void testAccounts() {

        CurrencyNode node = e.getDefaultCurrency();

        Account parent = e.getRootAccount();

        for (int i = 0; i < 500; i++) {
            Account child = new Account(AccountType.BANK, node);
            child.setName("Account" + Integer.toString(i + 1));
            child.setAccountNumber(Integer.toString(i + 1));

            if (i % 2 == 0) {
                child.setPlaceHolder(true);
            }

            e.addAccount(parent, child);

            parent = child;
        }

        // close and reopen to force check for persistence
        EngineFactory.closeEngine(EngineFactory.DEFAULT);
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT);

        parent = e.getRootAccount();

        // look for all the accounts
        assertEquals(500, e.getAccountList().size());

        for (int i = 0; i < 500; i++) {
            assertEquals(1, parent.getChildCount());

            if (parent.getChildCount() == 1) {
                Account child = parent.getChildren().get(0);

                assertEquals("Account" + Integer.toString(i + 1), child.getName());
                assertEquals(Integer.toString(i + 1), child.getAccountNumber());

                if (i % 2 == 0) {
                    assertTrue(child.isPlaceHolder());
                } else {
                    assertFalse(child.isPlaceHolder());
                }

                parent = child;
            }
        }

    }

    @Test
    public void testAddAccount() {
        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.BANK, node);
        a.setName("testAddAccount");

        assertTrue(e.addAccount(e.getRootAccount(), a));

        assertTrue(e.isStored(a));
    }

    @Test
    public void testGetRootAccount() {
        RootAccount root = e.getRootAccount();

        assertNotNull(root);
    }

    @Ignore
    @Test
    public void testMoveAccount() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testModifyAccount() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testSetAccountNumber() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testRemoveAccount() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testSetAmortizeObject() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testToggleAccountVisibility() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testUpdateAccountSecurities() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testIsTransactionValid() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsStored() {
        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.BANK, node);
        a.setName("testIsStored");

        assertFalse(e.isStored(a));

        e.addAccount(e.getRootAccount(), a);

        assertTrue(e.isStored(a));
    }

    @Ignore
    @Test
    public void testAddTransaction() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testRemoveTransaction() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testSetTransactionReconciled() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testGetTransactionNumberList() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testSetTransactionNumberList() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testGetTransactions() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetUuid() {
        assertTrue(e.getUuid() != null);
        assertTrue(e.getUuid().length() > 0);
    }

}
