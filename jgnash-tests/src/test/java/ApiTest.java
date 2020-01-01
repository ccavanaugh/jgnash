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
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jgnash.engine.AbstractEngineTest;
import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineException;
import jgnash.engine.EngineFactory;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;
import jgnash.util.LogUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test methods for public API's.  These may duplicate some other tests but ensures intended API's remain publicly
 * accessible for plugins and UIs
 *
 * @author Craig Cavanaugh
 */
public class ApiTest extends AbstractEngineTest {

    @BeforeAll
    static void test() {
        LogUtil.configureLogging();
    }

    @Override
    protected Engine createEngine() throws IOException {
        database = testFolder.createFile("api-test.bxds").getAbsolutePath();

        EngineFactory.deleteDatabase(database);

        try {
            final Engine engine =  EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                    DataStoreType.BINARY_XSTREAM);

            engine.setCreateBackups(false); // disable for test

            return engine;
        } catch (final EngineException e) {
            fail("Fatal error occurred");
            return  null;
        }
    }

    private static void closeEngine() {
        EngineFactory.closeEngine(EngineFactory.DEFAULT);
    }

    @Test
    void testMove() {
        Account test = new Account(AccountType.ASSET, e.getDefaultCurrency());
        test.setName("Test");

        assertTrue(e.addAccount(e.getRootAccount(), test));
        assertTrue(e.getRootAccount().contains(test));

        assertTrue(e.moveAccount(test, usdBankAccount));
        assertFalse(e.getRootAccount().contains(test));

        assertTrue(usdBankAccount.contains(test));
    }

    @Test
    void testPreferences() {
        e.setPreference("myKey", "myValue");
        e.setPreference("myNumber", BigDecimal.TEN.toString());

        e.putBoolean("myBoolean", true);
        assertTrue(e.getBoolean("myBoolean", false));

        e.setRetainedBackupLimit(5);
        assertEquals(5, e.getRetainedBackupLimit());

        e.setRemoveOldBackups(false);
        assertFalse(e.removeOldBackups());

        e.setRemoveOldBackups(true);
        assertTrue(e.removeOldBackups());

        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        assertEquals("myValue", e.getPreference("myKey"));
        assertTrue(e.getBoolean("myBoolean", false));
        assertEquals(5, e.getRetainedBackupLimit());

        final String number = e.getPreference("myNumber");
        assertNotNull(number);

        assertEquals(BigDecimal.TEN, new BigDecimal(number));
    }

    @Test
    void testSecurities() {
        SecurityNode securityNode = new SecurityNode(e.getDefaultCurrency());
        securityNode.setSymbol("GGG");
        securityNode.setScale((byte) 2);

        e.addSecurity(securityNode);
        assertEquals(securityNode, e.getSecurity("GGG"));

        assertEquals(0, securityNode.getHistoryNodes().size());
        assertFalse(securityNode.getClosestHistoryNode(LocalDate.now()).isPresent());
        assertFalse(securityNode.getHistoryNode(LocalDate.now()).isPresent());
        assertEquals(BigDecimal.ZERO, securityNode.getMarketPrice(LocalDate.now(), e.getDefaultCurrency()));

        // Add the security node to the account
        final List<SecurityNode> securitiesList = new ArrayList<>(investAccount.getSecurities());
        final int securityCount = securitiesList.size();
        securitiesList.add(securityNode);
        e.updateAccountSecurities(investAccount, securitiesList);
        assertEquals(securityCount + 1, investAccount.getSecurities().size());

        // Returned market price should be zero
        assertEquals(BigDecimal.ZERO, Engine.getMarketPrice(new ArrayList<>(), securityNode, e.getDefaultCurrency(),
                LocalDate.now()));

        // Create and add some security history
        final SecurityHistoryNode historyNode = new SecurityHistoryNode(LocalDate.now(), BigDecimal.TEN, 10000,
                BigDecimal.TEN, BigDecimal.TEN);

        e.addSecurityHistory(securityNode, historyNode);
        assertEquals(1, securityNode.getHistoryNodes().size());

        // Returned market price should be 10
        assertEquals(BigDecimal.TEN, Engine.getMarketPrice(new ArrayList<>(), securityNode, e.getDefaultCurrency(),
                LocalDate.now()));


        // Ensure the security updates correctly with a replacement value
        final SecurityHistoryNode historyNodeReplacement = new SecurityHistoryNode(LocalDate.now(), BigDecimal.ONE, 10000,
                BigDecimal.ONE, BigDecimal.ONE);

        e.addSecurityHistory(securityNode, historyNodeReplacement);
        assertEquals(1, securityNode.getHistoryNodes().size());

        // Returned market price should be 1
        assertEquals(BigDecimal.ONE, Engine.getMarketPrice(new ArrayList<>(), securityNode, e.getDefaultCurrency(),
                LocalDate.now()));
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

        assertEquals("gobbleDeGook", Engine.getAccountAttribute(b, "myStuff"));

        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        b = e.getAccountByUuid(a.getUuid());
        assertEquals("gobbleDeGook", Engine.getAccountAttribute(b, "myStuff"));
        assertEquals("myValue", Engine.getAccountAttribute(b, "myKey"));

        String attribute = Engine.getAccountAttribute(b, "myNumber");

        assertNotNull(attribute);

        assertEquals(BigDecimal.TEN, new BigDecimal(attribute));
    }

    @Test
    void testTransactionAPI() {
        final String ACCOUNT_NAME = "testAccount";

        final CurrencyNode node = e.getDefaultCurrency();

        final Account a = new Account(AccountType.BANK, node);
        a.setName(ACCOUNT_NAME);

        e.addAccount(e.getRootAccount(), a);

        // Test single entry transaction
        final Transaction transaction = TransactionFactory.generateSingleEntryTransaction(a, BigDecimal.TEN, LocalDate.now(),
                "memo", "payee", "1");

        e.addTransaction(transaction);

        assertTrue(a.contains(transaction));

        assertEquals(0, a.indexOf(transaction));

        assertEquals(TransactionType.SINGLENTRY, transaction.getTransactionType());

        for (final TransactionEntry transactionEntry : transaction.getTransactionEntries()) {
            assertFalse(transactionEntry.isMultiCurrency());
        }
    }

    @Test
    void placeHolder() {
        assertTrue(e.getTransactionsWithAttachments().isEmpty());
    }
}
