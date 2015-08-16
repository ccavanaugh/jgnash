/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetGoal;
import jgnash.engine.budget.BudgetPeriod;
import jgnash.engine.recurring.DailyReminder;
import jgnash.engine.recurring.Reminder;
import jgnash.util.FileUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Craig Cavanaugh
 */
public abstract class EngineTest {

    Engine e;

    String testFile;

    private final boolean oldExportState = EngineFactory.exportXMLOnClose();

    protected abstract Engine createEngine() throws Exception;

    static final char[] PASSWORD = new char[]{};

    void closeEngine() {
        EngineFactory.closeEngine(EngineFactory.DEFAULT);
    }

    @Before
    public void setUp() throws Exception {

        EngineFactory.setExportXMLOnClose(false);

        e = createEngine();

        assertNotNull(e);

        CurrencyNode node = e.getDefaultCurrency();

        if (!node.getSymbol().equals("USD")) {
            CurrencyNode defaultCurrency = DefaultCurrencies.buildNode(Locale.US);

            assertNotNull(defaultCurrency);
            assertTrue(e.addCurrency(defaultCurrency));

            e.setDefaultCurrency(defaultCurrency);
        }

        node = e.getCurrency("CAD");

        if (node == null) {
            node = DefaultCurrencies.buildNode(Locale.CANADA);

            assertNotNull(node);

            assertTrue(e.addCurrency(node));
        }
    }

    @After
    public void tearDown() throws IOException {
        EngineFactory.closeEngine(EngineFactory.DEFAULT);
        EngineFactory.setExportXMLOnClose(oldExportState);

        Files.deleteIfExists(Paths.get(testFile));

        final String attachmentDir = System.getProperty("java.io.tmpdir") + File.separator + "attachments";
        final Path directory = Paths.get(attachmentDir);

        FileUtils.deletePathAndContents(directory);
    }

    @Test
    public void testGetName() {
        assertEquals(e.getName(), EngineFactory.DEFAULT);
    }

    @Test
    public void testReminders() throws Exception {

        assertEquals(0, e.getReminders().size());

        assertEquals(0, e.getPendingReminders().size());

        Reminder r = new DailyReminder();
        r.setIncrement(1);
        r.setEndDate(null);

        assertTrue(e.addReminder(r));

        assertEquals(1, e.getReminders().size());
        assertEquals(1, e.getPendingReminders().size());

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);
        assertEquals(e.getReminders().size(), 1);

        // remove a reminder
        e.removeReminder(e.getReminders().get(0));
        assertEquals(0, e.getReminders().size());
        assertEquals(0, e.getPendingReminders().size());

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);
        assertEquals(0, e.getReminders().size());
        assertEquals(0, e.getPendingReminders().size());
    }

    @Test
    public void testGetStoredObjectByUuid() throws Exception {

        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        assertNotNull(e);

        String uuid = e.getDefaultCurrency().getUuid();

        assertSame(e.getDefaultCurrency(), e.getCurrencyNodeByUuid(uuid));
    }

    @Test
    public void testGetStoredObjects() {
        assertTrue(!e.getStoredObjects().isEmpty());
    }

    @Test
    public void testAddCommodity() throws Exception {
        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        assertNotNull(e.getDefaultCurrency());
        assertNotNull(e.getCurrency("USD"));
        assertNotNull(e.getCurrency("CAD"));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testPreferences() throws Exception {
        e.setPreference("myKey", "myValue");
        e.setPreference("myNumber", BigDecimal.TEN.toString());

        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        assertEquals("myValue", e.getPreference("myKey"));
        assertEquals(BigDecimal.TEN, new BigDecimal(e.getPreference("myNumber")));
    }

    @Test
    public void testSecurityNodeStorage() {

        final String SECURITY_SYMBOL =" GOOG";

        SecurityNode securityNode = new SecurityNode(e.getDefaultCurrency());

        securityNode.setSymbol(SECURITY_SYMBOL);
        securityNode.setScale((byte) 2);

        assertTrue(e.addSecurity(securityNode));

        SecurityHistoryNode historyNode = new SecurityHistoryNode(LocalDate.now(), BigDecimal.valueOf(125), 10000,
                BigDecimal.valueOf(126), BigDecimal.valueOf(124));

        e.addSecurityHistory(securityNode, historyNode);


        final SecurityHistoryEvent dividendEvent = new SecurityHistoryEvent(SecurityHistoryEventType.DIVIDEND, LocalDate.now(), BigDecimal.ONE);
        assertTrue(e.addSecurityHistoryEvent(securityNode, dividendEvent));


        final SecurityHistoryEvent splitEvent = new SecurityHistoryEvent(SecurityHistoryEventType.SPLIT, LocalDate.now(), BigDecimal.TEN);
        assertTrue(e.addSecurityHistoryEvent(securityNode, splitEvent));

        assertTrue(securityNode.getHistoryEvents().contains(dividendEvent));
        assertTrue(securityNode.getHistoryEvents().contains(splitEvent));

        assertEquals(2, securityNode.getHistoryEvents().size());

        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        securityNode = e.getSecurity(SECURITY_SYMBOL);

        assertNotNull(securityNode);

        assertEquals(2, securityNode.getHistoryEvents().size());

        List<SecurityHistoryEvent> events = new ArrayList<>(securityNode.getHistoryEvents());

        assertTrue(e.removeSecurityHistoryEvent(securityNode, events.get(0)));
        assertTrue(e.removeSecurityHistoryEvent(securityNode, events.get(1)));

        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        securityNode = e.getSecurity(SECURITY_SYMBOL);

        assertNotNull(securityNode);

        assertEquals(0, securityNode.getHistoryEvents().size());
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
    public void testBudget() throws Exception {

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

        assertTrue(e.addBudget(budget));

        assertEquals(1, e.getBudgetList().size());

        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        assertNotNull(e);

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
        assertTrue(e.removeBudget(e.getBudgetList().get(0)));
        assertEquals(0, e.getBudgetList().size());

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);
        assertEquals(0, e.getBudgetList().size());
    }

    @Test
    public void testGetActiveCurrencies() {
        Set<CurrencyNode> nodes = e.getActiveCurrencies();

        assertNotNull(nodes);

        if (!e.getTransactions().isEmpty()) {
            assertTrue(!nodes.isEmpty());
        } else {
            assertTrue(!nodes.isEmpty());
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
    public void testGetCurrencies() throws Exception {

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

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

    @Test
    public void testGetExchangeRate() throws Exception {

        final LocalDate today = LocalDate.now();
        final LocalDate yesterday = today.minusDays(1);

        CurrencyNode usd = e.getCurrency("USD");
        CurrencyNode cad = e.getCurrency("CAD");

        e.setExchangeRate(usd, cad, new BigDecimal("1.02"), LocalDate.now());
        e.setExchangeRate(usd, cad, new BigDecimal("1.01"), LocalDate.now().minusDays(1));

        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        ExchangeRate rate = e.getExchangeRate(usd, cad);
        assertNotNull(rate);

        assertTrue(new BigDecimal("1.02").compareTo(rate.getRate()) == 0);
        assertTrue(new BigDecimal("1.01").compareTo(rate.getRate(yesterday)) == 0);
    }

    @Ignore
    @Test
    public void testGetSecurities() {
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
        assertTrue(!sep.isEmpty());
    }

    @Ignore
    @Test
    public void testGetAccountList() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetAccountByUuid() {
        String rootUUID = e.getRootAccount().getUuid();

        assertEquals(e.getRootAccount(), e.getAccountByUuid(rootUUID));
    }

    @Test
    public void testGetAccountByName() {
        final String ACCOUNT_NAME = "testAccount";

        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.BANK, node);
        a.setName(ACCOUNT_NAME);

        e.addAccount(e.getRootAccount(), a);

        assertEquals(a, e.getAccountByName(ACCOUNT_NAME));
    }

    @Test
    public void testGetIncomeAccountList() throws Exception {
        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.INCOME, node);
        a.setName("Income");
        e.addAccount(e.getRootAccount(), a);

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        assertTrue(!e.getIncomeAccountList().isEmpty());
    }

    @Test
    public void testGetExpenseAccountList() throws Exception {
        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.EXPENSE, node);
        a.setName("Expense");
        e.addAccount(e.getRootAccount(), a);

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        assertTrue(!e.getExpenseAccountList().isEmpty());
    }

    @Test
    public void testGetBankAccountList() throws Exception {
        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.BANK, node);
        a.setName("Asset");
        e.addAccount(e.getRootAccount(), a);

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        assertTrue(!e.getAccounts(AccountGroup.ASSET).isEmpty());
    }

    @Test
    public void testGetInvestmentAccountList() throws Exception {
        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.INVEST, node);
        a.setName("Invest");
        e.addAccount(e.getRootAccount(), a);

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        assertTrue(!e.getInvestmentAccountList().isEmpty());
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
    public void testAccounts() throws Exception {

        CurrencyNode node = e.getDefaultCurrency();

        assertNotNull(node);

        Account parent = e.getRootAccount();

        assertNotNull(parent);

        for (int i = 0; i < 50; i++) {
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
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        parent = e.getRootAccount();

        // look for all the accounts
        assertEquals(50, e.getAccountList().size());

        for (int i = 0; i < 50; i++) {
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
    public void testAccountAttributes() {
        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.BANK, node);
        a.setName("AccountAttributes");

        e.addAccount(e.getRootAccount(), a);
        e.setAccountAttribute(a, "myStuff", "gobbleDeGook");
        e.setAccountAttribute(a, "myKey", "myValue");
        e.setAccountAttribute(a, "myNumber", BigDecimal.TEN.toString());

        Account b = e.getAccountByUuid(a.getUuid());

        assertEquals("gobbleDeGook", b.getAttribute("myStuff"));

        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        b = e.getAccountByUuid(a.getUuid());
        assertEquals("gobbleDeGook", b.getAttribute("myStuff"));
        assertEquals("myValue", b.getAttribute("myKey"));

        String attribute = b.getAttribute("myNumber");

        assertNotNull(attribute);

        assertEquals(BigDecimal.TEN, new BigDecimal(attribute));
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

    @Test
    public void testRemoveAccount() throws Exception {
        final String ACCOUNT_NAME = "testIsStored";
        CurrencyNode node = e.getDefaultCurrency();

        assertNotNull(e);

        Account a = new Account(AccountType.BANK, node);
        a.setName(ACCOUNT_NAME);

        assertFalse(e.isStored(a));

        e.addAccount(e.getRootAccount(), a);

        assertTrue(e.isStored(a));

        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        a = e.getAccountByName(ACCOUNT_NAME);

        assertTrue(e.removeAccount(a));

        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        assertNull(e.getAccountByName(ACCOUNT_NAME));
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

        final String ACCOUNT_NAME = "testIsStored";
        CurrencyNode node = e.getDefaultCurrency();

        assertNotNull(e);

        Account a = new Account(AccountType.BANK, node);
        a.setName(ACCOUNT_NAME);

        assertFalse(e.isStored(a));

        e.addAccount(e.getRootAccount(), a);

        assertTrue(e.isStored(a));
    }

    @Test
    public void testAddTransaction() throws Exception {
        testGetTransactions();
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

    @Test
    public void testGetTransactions() throws Exception {
        final String ACCOUNT_NAME = "testAccount";

        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.BANK, node);
        a.setName(ACCOUNT_NAME);

        e.addAccount(e.getRootAccount(), a);

        e.addTransaction(TransactionFactory.generateSingleEntryTransaction(a, BigDecimal.TEN, LocalDate.now(), "memo", "payee", "1"));
        e.addTransaction(TransactionFactory.generateSingleEntryTransaction(a, BigDecimal.TEN, LocalDate.now(), "memo", "payee", "2"));
        e.addTransaction(TransactionFactory.generateSingleEntryTransaction(a, BigDecimal.TEN, LocalDate.now(), "memo", "payee", "3"));

        assertEquals(3, a.getTransactionCount());

        assertEquals(3, e.getTransactions().size());

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        a = e.getAccountByName(ACCOUNT_NAME);
        assertEquals(3, a.getTransactionCount());
    }

    @Test
    public void testGetTransactionsWithAttachments() throws Exception {
        final String ACCOUNT_NAME = "testAccount";

        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.BANK, node);
        a.setName(ACCOUNT_NAME);

        e.addAccount(e.getRootAccount(), a);

        e.addTransaction(TransactionFactory.generateSingleEntryTransaction(a, BigDecimal.TEN, LocalDate.now(), "memo",
                "payee", "1"));

        Transaction link = TransactionFactory.generateSingleEntryTransaction(a, BigDecimal.TEN, LocalDate.now(), "memo",
                "payee", "1");
        link.setAttachment("external link");

        e.addTransaction(link);

        assertEquals(2, e.getTransactions().size());
        assertEquals(1, e.getTransactionsWithAttachments().size());

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, PASSWORD);

        assertEquals(2, e.getTransactions().size());
        assertEquals(1, e.getTransactionsWithAttachments().size());
    }

    @Test
    public void testGetUuid() {
        assertTrue(e.getUuid() != null);
        assertTrue(!e.getUuid().isEmpty());
    }

}
