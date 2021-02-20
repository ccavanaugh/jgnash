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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetGoal;
import jgnash.engine.recurring.DailyReminder;
import jgnash.engine.recurring.Reminder;
import jgnash.time.Period;
import jgnash.util.FileUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Abstract base class for testing the Engine API.
 *
 * @author Craig Cavanaugh
 */
public abstract class EngineTest {

    private static final float DELTA = .001f;

    private Engine e;

    String testFile;

    protected abstract Engine createEngine() throws Exception;

    private static void closeEngine() {
        EngineFactory.closeEngine(EngineFactory.DEFAULT);
    }

    @BeforeEach
    void setUp() throws Exception {
        Locale.setDefault(Locale.US);

        e = createEngine();

        assertNotNull(e);   // fail if null

        e.setCreateBackups(false);

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

        // close the file/engine
        closeEngine();

        // check for correct file version
        final float version = EngineFactory.getFileVersion(Paths.get(testFile), EngineFactory.EMPTY_PASSWORD);
        final Config config = new Config();
        assertEquals(Float.parseFloat(config.getFileFormat()), version, .0001);

        // reopen the file for more tests
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        assertNotNull(e);
    }

    @AfterEach
    void tearDown() throws IOException {
        EngineFactory.closeEngine(EngineFactory.DEFAULT);

        Files.deleteIfExists(Paths.get(testFile));

        final String attachmentDir = System.getProperty("java.io.tmpdir") + System.getProperty("path.SEPARATOR")
                                             + "attachments";
        final Path directory = Paths.get(attachmentDir);

        FileUtils.deletePathAndContents(directory);
    }

    @Test
    void testGetName() {
        assertEquals(e.getName(), EngineFactory.DEFAULT);
    }

    @Test
    void testReminders() throws CloneNotSupportedException {

        assertEquals(0, e.getReminders().size());
        assertEquals(0, e.getPendingReminders().size());

        Reminder r = new DailyReminder();

        r.setIncrement(1);
        r.setEndDate(null);
        assertFalse(e.addReminder(r));  // should fail because description is not set

        r.setDescription("test");
        assertTrue(e.addReminder(r));   // should pass now

        assertEquals(1, e.getReminders().size());
        assertEquals(1, e.getPendingReminders().size());

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);
        assertEquals(e.getReminders().size(), 1);

        // Clone reminders
        r = e.getReminders().get(0);
        assertNotNull(r);
        Reminder clone = (Reminder) r.clone();
        assertNotNull(clone);

        assertNotEquals(clone, r);
        assertNotEquals(0, clone.compareTo(r));

        // remove a reminder
        e.removeReminder(e.getReminders().get(0));
        assertEquals(0, e.getReminders().size());
        assertEquals(0, e.getPendingReminders().size());

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);
        assertEquals(0, e.getReminders().size());
        assertEquals(0, e.getPendingReminders().size());
    }

    @Test
    void testTags() {
        final Account a = new Account(AccountType.BANK, e.getDefaultCurrency());
        a.setName("Tag Test Account");

        e.addAccount(e.getRootAccount(), a);

        final Tag tag1 = new Tag();
        tag1.setName("tag1");
        assertTrue(e.addTag(tag1));

        final Tag tag2 = new Tag();
        tag2.setName("tag2");
        assertTrue(e.addTag(tag2));

        final Tag tag3 = new Tag();
        tag3.setName("tag3");
        assertTrue(e.addTag(tag3));

        final Transaction singleEntryTransaction = TransactionFactory.generateSingleEntryTransaction(a, BigDecimal.TEN, LocalDate.now(),
                "tagTest", "tag test payee", "1001");

        TransactionEntry entry = singleEntryTransaction.getTransactionEntries().get(0);

        entry.setTags(e.getTags());

        final UUID uuid = singleEntryTransaction.getUuid(); // get uuid for easy lookup

        assertTrue(e.addTransaction(singleEntryTransaction));

        Transaction transaction = e.getTransactionByUuid(uuid);
        assertNotNull(transaction);

        Set<Tag> reTags = transaction.getTransactionEntries().get(0).getTags();
        assertEquals(3, reTags.size());

        for (final Tag tag : e.getTags()) {
            if (tag.getName().equals("tag1")) {
                tag.setDescription("description 1");
                assertTrue(e.updateTag(tag));
            }
        }

        for (final Tag tag : e.getTags()) {
            if (tag.getName().equals("tag1")) {
                assertEquals("description 1", tag.getDescription());
            }
        }

        final Tag tag4 = new Tag();
        tag4.setName("tag4");
        assertTrue(e.addTag(tag4));

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);
        assertNotNull(e);

        assertEquals(4, e.getTags().size());
        assertEquals(3, e.getTagsInUse().size());

        transaction = e.getTransactionByUuid(uuid);
        assertNotNull(transaction);

        reTags = transaction.getTransactionEntries().get(0).getTags();
        assertEquals(3, reTags.size());

        for (final Tag tag : e.getTags()) {
            if (tag.getName().equals("tag1")) {
                assertEquals("description 1", tag.getDescription());
            }
        }

        assertEquals(3, transaction.getTags().size());

        for (final Tag tag : e.getTags()) {
            if (tag.getName().equals("tag4")) {
                assertTrue(e.removeTag(tag));
            } else {
                assertFalse(e.removeTag(tag));
            }
        }
    }

    @Test
    void testGetStoredObjectByUuid() {

        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        assertNotNull(e);

        UUID uuid = e.getDefaultCurrency().getUuid();

        assertSame(e.getDefaultCurrency(), e.getCurrencyNodeByUuid(uuid));
    }

    @Test
    void testGetStoredObjects() {
        assertFalse(e.getStoredObjects().isEmpty());
    }

    @Test
    void testAddCommodity() {
        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        assertNotNull(e.getDefaultCurrency());
        assertNotNull(e.getCurrency("USD"));
        assertNotNull(e.getCurrency("CAD"));
    }

    @Test
    void testPreferences() {
        e.setPreference("myKey", "myValue");
        e.setPreference("myNumber", BigDecimal.TEN.toString());

        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        assertEquals("myValue", e.getPreference("myKey"));
        assertEquals(BigDecimal.TEN, new BigDecimal(e.getPreference("myNumber")));
    }

    @Test
    void testSecurityNodeStorage() {

        final String SECURITY_SYMBOL = "GOOG";

        SecurityNode securityNode = new SecurityNode(e.getDefaultCurrency());

        securityNode.setSymbol(SECURITY_SYMBOL);
        securityNode.setScale((byte) 2);

        assertTrue(e.addSecurity(securityNode));

        SecurityHistoryNode historyNode = new SecurityHistoryNode(LocalDate.now(), BigDecimal.valueOf(125), 10000,
                BigDecimal.valueOf(126), BigDecimal.valueOf(124));

        e.addSecurityHistory(securityNode, historyNode);


        final SecurityHistoryEvent dividendEvent = new SecurityHistoryEvent(SecurityHistoryEventType.DIVIDEND,
                LocalDate.now(), BigDecimal.ONE);
        assertTrue(e.addSecurityHistoryEvent(securityNode, dividendEvent));


        final SecurityHistoryEvent splitEvent = new SecurityHistoryEvent(SecurityHistoryEventType.SPLIT,
                LocalDate.now(), BigDecimal.TEN);
        assertTrue(e.addSecurityHistoryEvent(securityNode, splitEvent));

        assertTrue(securityNode.getHistoryEvents().contains(dividendEvent));
        assertTrue(securityNode.getHistoryEvents().contains(splitEvent));

        assertEquals(2, securityNode.getHistoryEvents().size());

        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        securityNode = e.getSecurity(SECURITY_SYMBOL);

        assertNotNull(securityNode);

        assertEquals(2, securityNode.getHistoryEvents().size());

        List<SecurityHistoryEvent> events = new ArrayList<>(securityNode.getHistoryEvents());

        assertTrue(e.removeSecurityHistoryEvent(securityNode, events.get(0)));
        assertTrue(e.removeSecurityHistoryEvent(securityNode, events.get(1)));

        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        securityNode = e.getSecurity(SECURITY_SYMBOL);

        assertNotNull(securityNode);

        assertEquals(0, securityNode.getHistoryEvents().size());

        Account a = new Account(AccountType.INVEST, e.getDefaultCurrency());
        a.setName("Invest");
        assertTrue(e.addAccount(e.getRootAccount(), a));

        // try again
        assertFalse(e.addAccount(e.getRootAccount(), a));

        assertTrue(e.updateAccountSecurities(a, Collections.singletonList(securityNode)));
        assertEquals(1, a.getSecurities().size());

        assertTrue(e.updateAccountSecurities(a, Collections.emptyList()));
        assertEquals(0, a.getSecurities().size());
    }

    @Test
    void exchangeRateTest1() {

        CurrencyNode usdNode = new CurrencyNode();
        usdNode.setSymbol("USD");
        usdNode.setPrefix("$");
        usdNode.setDescription("US Dollar");
        e.addCurrency(usdNode);

        CurrencyNode cadNode = new CurrencyNode();
        cadNode.setSymbol("CAD");
        cadNode.setPrefix("$");
        cadNode.setDescription("CAD Dollar");
        e.addCurrency(cadNode);

        assertNotNull(usdNode.getSymbol());
        assertNotNull(cadNode.getSymbol());

        e.setExchangeRate(usdNode, cadNode, new BigDecimal("1.100"));

        assertEquals(new BigDecimal("1.100"), usdNode.getExchangeRate(cadNode));
        assertEquals(new BigDecimal("0.909"), cadNode.getExchangeRate(usdNode).setScale(3, RoundingMode.DOWN));

        assertEquals(BigDecimal.ONE, usdNode.getExchangeRate(usdNode));
        assertEquals(BigDecimal.ONE, cadNode.getExchangeRate(cadNode));

        assertTrue(e.removeCommodity(cadNode));
    }

    @Test
    void exchangeRateTest2() {

        CurrencyNode usdNode = new CurrencyNode();
        usdNode.setSymbol("USD");
        usdNode.setPrefix("$");
        usdNode.setDescription("US Dollar");
        e.addCurrency(usdNode);

        CurrencyNode cadNode = new CurrencyNode();
        cadNode.setSymbol("CAD");
        cadNode.setPrefix("$");
        cadNode.setDescription("CAD Dollar");
        e.addCurrency(cadNode);

        assertNotNull(usdNode.getSymbol());
        assertNotNull(cadNode.getSymbol());

        // rate is inverted when added
        e.setExchangeRate(cadNode, usdNode, new BigDecimal("0.909"));

        assertEquals(new BigDecimal("1.100"), usdNode.getExchangeRate(cadNode).setScale(3, RoundingMode.DOWN));
        assertEquals(new BigDecimal("0.909"), cadNode.getExchangeRate(usdNode).setScale(3, RoundingMode.DOWN));


        assertTrue(e.removeCommodity(cadNode));

        EngineFactory.closeEngine(EngineFactory.DEFAULT);
    }

    @Test
    void commodityNodeStoreTest() {
        CurrencyNode node = new CurrencyNode();

        node.setSymbol("USD");
        node.setPrefix("$");
        node.setDescription("US Dollar");

        e.addCurrency(node);

        node = e.getCurrency("USD");

        Account account = new Account(AccountType.BANK, node);
        account.setName("Bank Account");

        e.addAccount(e.getRootAccount(), account);

        Object cNode = e.getCurrency("USD");

        //noinspection ConstantConditions
        assertTrue(cNode instanceof CurrencyNode, "Returned object extends CurrencyNode");

        //noinspection ConstantConditions
        assertTrue(cNode instanceof StoredObject, "Returned object extends StoredObject");

        Set<CurrencyNode> nodes = DefaultCurrencies.generateCurrencies();

        for (final CurrencyNode n : nodes) {
            e.addCurrency(n);
        }

        EngineFactory.closeEngine(EngineFactory.DEFAULT);
    }

    @Test
    void testCurrencyRemove() {
        CurrencyNode currencyNode = new CurrencyNode();
        currencyNode.setDescription("Test");
        currencyNode.setSymbol("BTC");
        currencyNode.setPrefix("$");
        currencyNode.setScale((byte) 8);

        int oldCount = e.getCurrencies().size();
        e.addCurrency(currencyNode);

        assertEquals(oldCount + 1, e.getCurrencies().size());

        currencyNode = e.getCurrency("BTC");

        assertNotNull(currencyNode);

        e.removeCommodity(currencyNode);
        assertEquals(oldCount, e.getCurrencies().size());
    }

    @Test
    void testBudget() {

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
        goal.setBudgetPeriod(Period.WEEKLY);

        budget.setBudgetGoal(a, goal);
        budget.setBudgetPeriod(Period.WEEKLY);

        assertTrue(e.addBudget(budget));

        assertEquals(1, e.getBudgetList().size());

        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        assertNotNull(e);

        assertEquals(1, e.getBudgetList().size());

        a = e.getAccountByName(ACCOUNT_NAME);
        assertNotNull(a);

        Budget recovered = e.getBudgetList().get(0);
        budgetGoals = recovered.getBudgetGoal(a).getGoals();

        // check the goals
        assertEquals(BudgetGoal.PERIODS, budgetGoals.length);

        // check the periods
        assertEquals(Period.WEEKLY, recovered.getBudgetPeriod());
        assertEquals(Period.WEEKLY, recovered.getBudgetGoal(a).getBudgetPeriod());

        for (int i = 0; i < budgetGoals.length; i++) {
            //assertEquals(new BigDecimal(i), budgetGoals[i]);
            //assertThat(new BigDecimal(i), is(closeTo(budgetGoals[i], new BigDecimal(0.0001))));
            assertEquals(i, budgetGoals[i].doubleValue(), 0.0001);
        }

        // remove a budget
        assertTrue(e.removeBudget(e.getBudgetList().get(0)));
        assertEquals(0, e.getBudgetList().size());

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);
        assertEquals(0, e.getBudgetList().size());
    }

    @Test
    void testGetActiveCurrencies() {
        Set<CurrencyNode> nodes = e.getActiveCurrencies();

        assertNotNull(nodes);

        assertFalse(nodes.isEmpty());

        System.out.println("Node count is " + nodes.size());
    }

    @Test
    void testGetCurrencyStringLocale() {
        CurrencyNode currency = e.getCurrency("USD");
        assertNotNull(currency);
    }

    @Test
    void testGetCurrencyString() {
        CurrencyNode currency = e.getCurrency("USD");
        assertNotNull(currency);

        currency = e.getCurrency("CAD");
        assertNotNull(currency);
    }

    @Test
    void testGetCurrencies() {

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        List<CurrencyNode> nodes = e.getCurrencies();
        assertNotNull(nodes);
        System.out.println(nodes.size());
        assertTrue(nodes.size() > 1);
    }


    @Test
    void testSecurityHistory() {
        BigDecimal securityPrice1 = new BigDecimal("2.00");

        final LocalDate transactionDate1 = LocalDate.of(2009, Month.DECEMBER, 26);

        SecurityNode securityNode1 = new SecurityNode(e.getDefaultCurrency());
        securityNode1.setSymbol("GOOGLE");
        assertTrue(e.addSecurity(securityNode1));

        SecurityHistoryNode history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(securityNode1, history));

        assertEquals(1, securityNode1.getHistoryNodes().size());

        // Same date and price, new instance
        history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(securityNode1, history));   // should replace
        assertEquals(1, securityNode1.getHistoryNodes().size());

        // Same date, new instance and updated price
        history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(new BigDecimal("2.01"));

        assertTrue(e.addSecurityHistory(securityNode1, history));  // should replace
        assertEquals(1, securityNode1.getHistoryNodes().size());

        final LocalDate transactionDate2 = LocalDate.of(2009, Month.DECEMBER, 27);

        history = new SecurityHistoryNode();
        history.setDate(transactionDate2);
        history.setPrice(new BigDecimal("2.02"));
        assertTrue(e.addSecurityHistory(securityNode1, history));  // should be okay
        assertEquals(2, securityNode1.getHistoryNodes().size());

        final SecurityHistoryEvent dividendEvent = new SecurityHistoryEvent(SecurityHistoryEventType.DIVIDEND, LocalDate.now(), BigDecimal.ONE);
        securityNode1.addSecurityHistoryEvent(dividendEvent);


        final SecurityHistoryEvent splitEvent = new SecurityHistoryEvent(SecurityHistoryEventType.SPLIT, LocalDate.now(), BigDecimal.TEN);
        securityNode1.addSecurityHistoryEvent(splitEvent);

        assertTrue(securityNode1.getHistoryEvents().contains(dividendEvent));
        assertTrue(securityNode1.getHistoryEvents().contains(splitEvent));

        assertEquals(2, securityNode1.getHistoryEvents().size());

        securityNode1.removeSecurityHistoryEvent(dividendEvent);
        assertEquals(1, securityNode1.getHistoryEvents().size());

        securityNode1.removeSecurityHistoryEvent(splitEvent);
        assertEquals(0, securityNode1.getHistoryEvents().size());
    }

    @Test
    void testGetExchangeRate() {

        final LocalDate today = LocalDate.now();
        final LocalDate yesterday = today.minusDays(1);

        CurrencyNode usd = e.getCurrency("USD");
        CurrencyNode cad = e.getCurrency("CAD");

        e.setExchangeRate(usd, cad, new BigDecimal("1.02"), LocalDate.now());
        e.setExchangeRate(usd, cad, new BigDecimal("1.01"), LocalDate.now().minusDays(1));

        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        ExchangeRate rate = e.getExchangeRate(usd, cad);
        assertNotNull(rate);

        assertEquals(0, new BigDecimal("1.02").compareTo(rate.getRate()));
        assertEquals(0, new BigDecimal("1.01").compareTo(rate.getRate(yesterday)));
    }


    @Test
    void testGetDefaultCurrency() {
        CurrencyNode defaultCurrency = e.getDefaultCurrency();

        assertNotNull(defaultCurrency);
        assertEquals(defaultCurrency.getSymbol(), "USD");
    }

    @Test
    void testRemoveExchangeRateHistory() {

        final LocalDate testDate = LocalDate.now().minusYears(2);

        CurrencyNode usdCurrency = e.getDefaultCurrency();
        assertEquals(usdCurrency.getSymbol(), "USD");

        CurrencyNode cadCurrency = e.getCurrency("CAD");
        assertEquals(cadCurrency.getSymbol(), "CAD");

        e.setExchangeRate(usdCurrency, cadCurrency, new BigDecimal("0.5"), testDate);

        ExchangeRate exchangeRate = e.getExchangeRate(usdCurrency, cadCurrency);
        assertNotNull(exchangeRate);

        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);
        usdCurrency = e.getDefaultCurrency();
        cadCurrency = e.getCurrency("CAD");
        exchangeRate = e.getExchangeRate(usdCurrency, cadCurrency);
        assertNotNull(exchangeRate);

        ExchangeRateHistoryNode historyNode = exchangeRate.getHistory(testDate);
        assertNotNull(historyNode);

        e.removeExchangeRateHistory(exchangeRate, historyNode);
        historyNode = exchangeRate.getHistory(testDate);
        assertNull(historyNode);

        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);
        usdCurrency = e.getDefaultCurrency();
        cadCurrency = e.getCurrency("CAD");
        exchangeRate = e.getExchangeRate(usdCurrency, cadCurrency);
        assertNotNull(exchangeRate);
        historyNode = exchangeRate.getHistory(testDate);
        assertNull(historyNode);
    }

    @Test
    void testUpdateCommodity() throws CloneNotSupportedException {
        CurrencyNode testNode = DefaultCurrencies.buildCustomNode("CAD");
        assertNotNull(testNode);
        e.addCurrency(testNode);

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);
        testNode = e.getCurrency("CAD");
        assertNotNull(testNode);

        final CurrencyNode clone = (CurrencyNode) testNode.clone();
        clone.setDescription("changed");
        e.updateCommodity(testNode, clone);

        testNode = e.getCurrency("CAD");
        assertNotNull(testNode);

        assertEquals("changed", testNode.getDescription());

        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);
        testNode = e.getCurrency("CAD");
        assertNotNull(testNode);
        assertEquals("changed", testNode.getDescription());
    }

    @Test
    void testSetAccountSeparator() {
        String newSep = ".";

        String oldSep = e.getAccountSeparator();
        assertNotNull(oldSep);

        e.setAccountSeparator(newSep);

        assertEquals(e.getAccountSeparator(), newSep);

        e.setAccountSeparator(oldSep);

        assertEquals(e.getAccountSeparator(), oldSep);
    }

    @Test
    void testGetAccountSeparator() {
        String sep = e.getAccountSeparator();

        assertNotNull(sep);
        assertFalse(sep.isEmpty());
    }

    @Test
    void testGetAccountByUuid() {
        UUID rootUUID = e.getRootAccount().getUuid();

        assertEquals(e.getRootAccount(), e.getAccountByUuid(rootUUID));
    }

    @Test
    void testGetAccountByName() {
        final String ACCOUNT_NAME = "testAccount";

        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.BANK, node);
        a.setName(ACCOUNT_NAME);

        e.addAccount(e.getRootAccount(), a);

        assertEquals(a, e.getAccountByName(ACCOUNT_NAME));
    }

    @Test
    void testGetIncomeAccountList() {
        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.INCOME, node);
        a.setName("Income");
        e.addAccount(e.getRootAccount(), a);

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        assertFalse(e.getIncomeAccountList().isEmpty());
    }

    @Test
    void testGetExpenseAccountList() {
        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.EXPENSE, node);
        a.setName("Expense");
        e.addAccount(e.getRootAccount(), a);

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        assertFalse(e.getExpenseAccountList().isEmpty());
    }

    @Test
    void testGetBankAccountList() {
        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.BANK, node);
        a.setName("Asset");
        e.addAccount(e.getRootAccount(), a);

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        assertNotNull(e.getAccountByName("Asset"));
    }

    @Test
    void testGetInvestmentAccountList() {
        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.INVEST, node);
        a.setName("Invest");
        e.addAccount(e.getRootAccount(), a);

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        assertFalse(e.getInvestmentAccountList().isEmpty());
    }

    @Test
    void testAccounts() {

        CurrencyNode node = e.getDefaultCurrency();

        assertNotNull(node);

        Account parent = e.getRootAccount();

        assertNotNull(parent);

        for (int i = 0; i < 50; i++) {
            Account child = new Account(AccountType.BANK, node);
            child.setName("Account" + (i + 1));
            child.setAccountNumber(Integer.toString(i + 1));

            if (i % 2 == 0) {
                child.setPlaceHolder(true);
            }

            e.addAccount(parent, child);

            parent = child;
        }

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        parent = e.getRootAccount();

        // look for all the accounts
        assertEquals(50, e.getAccountList().size());

        for (int i = 0; i < 50; i++) {
            assertEquals(1, parent.getChildCount());

            if (parent.getChildCount() == 1) {
                Account child = parent.getChildren().get(0);

                assertEquals("Account" + (i + 1), child.getName());
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
    void testAccountAttributes() {
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

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        b = e.getAccountByUuid(a.getUuid());
        assertEquals("gobbleDeGook", b.getAttribute("myStuff"));
        assertEquals("myValue", b.getAttribute("myKey"));

        String attribute = b.getAttribute("myNumber");

        assertNotNull(attribute);

        assertEquals(BigDecimal.TEN, new BigDecimal(attribute));
    }

    @Test
    void testAddAccount() {
        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.BANK, node);
        a.setName("testAddAccount");

        assertTrue(e.addAccount(e.getRootAccount(), a));

        assertTrue(e.isStored(a));
    }

    @Test
    void testGetRootAccount() {
        RootAccount root = e.getRootAccount();

        assertNotNull(root);
    }

    @Test
    void testRemoveAccount() {
        final String ACCOUNT_NAME = "testIsStored";
        CurrencyNode node = e.getDefaultCurrency();

        assertNotNull(e);

        Account a = new Account(AccountType.BANK, node);
        a.setName(ACCOUNT_NAME);

        assertFalse(e.isStored(a));

        e.addAccount(e.getRootAccount(), a);

        assertTrue(e.isStored(a));

        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        a = e.getAccountByName(ACCOUNT_NAME);

        assertTrue(e.removeAccount(a));

        closeEngine();

        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        assertNull(e.getAccountByName(ACCOUNT_NAME));
    }

    @Test
    void testIsStored() {

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
    void testAddGetRemoveReconcileSingleEntryTransactions() {
        final String ACCOUNT_NAME = "testAccount";

        CurrencyNode node = e.getDefaultCurrency();

        Account a = new Account(AccountType.BANK, node);
        a.setName(ACCOUNT_NAME);

        e.addAccount(e.getRootAccount(), a);

        e.addTransaction(TransactionFactory.generateSingleEntryTransaction(a, BigDecimal.TEN, LocalDate.now(), "memo1",
                "payee1", "1"));
        e.addTransaction(TransactionFactory.generateSingleEntryTransaction(a, BigDecimal.TEN, LocalDate.now(), "memo2",
                "payee2", "2"));
        e.addTransaction(TransactionFactory.generateSingleEntryTransaction(a, BigDecimal.TEN, LocalDate.now(), "memo3",
                "payee3", "3"));

        assertEquals(3, a.getTransactionCount());

        assertEquals(3, e.getTransactions().size());

        a = e.getAccountByName(ACCOUNT_NAME);
        assertEquals(3, a.getTransactionCount());

        // Check for correct reconciliation
        for (final Transaction transaction : e.getTransactions()) {
            assertEquals(transaction.getReconciled(a), ReconciledState.NOT_RECONCILED);
        }

        for (final Transaction transaction : e.getTransactions()) {
            e.setTransactionReconciled(transaction, a, ReconciledState.CLEARED);
        }

        for (final Transaction transaction : e.getTransactions()) {
            assertEquals(transaction.getReconciled(a), ReconciledState.CLEARED);
        }

        for (final Transaction transaction : e.getTransactions()) {
            e.setTransactionReconciled(transaction, a, ReconciledState.RECONCILED);
        }

        for (final Transaction transaction : e.getTransactions()) {
            assertEquals(transaction.getReconciled(a), ReconciledState.RECONCILED);
        }


        for (int i = 2; i >= 0; i--) {
            List<Transaction> transactions = e.getTransactions();
            assertTrue(e.removeTransaction(transactions.get(0)));
            assertEquals(i, e.getTransactions().size());
        }

        // close and reopen to force check for persistence
        closeEngine();
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        a = e.getAccountByName(ACCOUNT_NAME);
        assertEquals(0, a.getTransactionCount());
    }

    @Test
    void testGetTransactionsWithAttachments() {
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
        e = EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        assertEquals(2, e.getTransactions().size());
        assertEquals(1, e.getTransactionsWithAttachments().size());
    }

    @Test
    void testGetUuid() {
        assertNotNull(e.getUuid());
        assertFalse(e.getUuid().isEmpty());
    }

    @Test
    void testVersion() {
        try {
            RootAccount account = e.getRootAccount();

            Account temp = e.getStoredObjectByUuid(RootAccount.class, account.getUuid());
            assertEquals(account, temp);

            // close and reopen to force check for persistence
            EngineFactory.closeEngine(EngineFactory.DEFAULT);

            final float version = EngineFactory.getFileVersion(Paths.get(testFile), EngineFactory.EMPTY_PASSWORD);
            final float engineVersion = Float.parseFloat(Engine.CURRENT_MAJOR_VERSION + "." + Engine.CURRENT_MINOR_VERSION);

            assertEquals(version, engineVersion, DELTA);
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    void testTransactionNumberList() {
        List<String> numbers = e.getTransactionNumberList();

        int size = numbers.size();

        assertTrue(size > 0);

        numbers.add("test 1");
        numbers.add("test 2");

        e.setTransactionNumberList(numbers);

        List<String> numbers2 = e.getTransactionNumberList();

        assertArrayEquals(numbers.toArray(), numbers2.toArray());

        assertEquals((size + 2), numbers2.size());
    }

    @Test
    void testAccountDepthAndComparator() {
        CurrencyNode defaultCurrency = e.getDefaultCurrency();

        final RootAccount root = e.getRootAccount();

        final Account a = new Account(AccountType.BANK, defaultCurrency);
        a.setName("a");
        e.addAccount(root, a);

        final Account b = new Account(AccountType.BANK, defaultCurrency);
        b.setName("b");
        e.addAccount(a, b);

        final Account c2 = new Account(AccountType.BANK, defaultCurrency);
        c2.setName("c2");
        e.addAccount(b, c2);

        final Account c1 = new Account(AccountType.BANK, defaultCurrency);
        c1.setName("c1");
        e.addAccount(b, c1);

        final Account d = new Account(AccountType.BANK, defaultCurrency);
        d.setName("d");
        e.addAccount(c1, d);

        assertEquals(0, root.getDepth());
        assertEquals(1, a.getDepth());
        assertEquals(2, b.getDepth());
        assertEquals(3, c1.getDepth());
        assertEquals(3, c2.getDepth());
        assertEquals(4, d.getDepth());

        assertNotNull(AccountUtils.searchTree(root, "d", AccountType.BANK, 4));
        assertNull(AccountUtils.searchTree(root, "d", AccountType.BANK, 3));

        // checks the custom Comparator sort order function
        final List<Account> accountList = e.getAccountList();

        accountList.sort(Comparators.getAccountByTreePosition(Comparators.getAccountByCode()));

        for (final Account account : accountList) {
            System.out.println(account.getPathName());
        }

        assertEquals(a, accountList.get(0));
        assertEquals(d, accountList.get(3));
        assertEquals(c2, accountList.get(4));
    }
}
