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
import jgnash.engine.EngineFactory;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test methods for public API's.  These may duplicate some other tests but ensures intended API's remain publicly
 * accessible for plugins
 *
 * @author Craig Cavanaugh
 */
public class ApiTest extends AbstractEngineTest {

    @Override
    protected Engine createEngine() throws IOException {
        database = testFolder.newFile("api-test.bxds").getAbsolutePath();
        EngineFactory.deleteDatabase(database);

        return EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                DataStoreType.BINARY_XSTREAM);
    }

    private void closeEngine() {
        EngineFactory.closeEngine(EngineFactory.DEFAULT);
    }

    @Test
    public void testMove() {
        Account test = new Account(AccountType.ASSET, e.getDefaultCurrency());
        test.setName("Test");

        assertTrue(e.addAccount(e.getRootAccount(), test));
        assertTrue(e.getRootAccount().contains(test));

        assertTrue(e.moveAccount(test, usdBankAccount));
        assertFalse(e.getRootAccount().contains(test));

        assertTrue(usdBankAccount.contains(test));
    }

    @Test
    public void testPreferences() {
        e.setPreference("myKey", "myValue");
        e.setPreference("myNumber", BigDecimal.TEN.toString());

        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT,
                EngineFactory.EMPTY_PASSWORD);

        assertEquals("myValue", e.getPreference("myKey"));

        final String number = e.getPreference("myNumber");
        assertNotNull(number);

        assertEquals(BigDecimal.TEN, new BigDecimal(number));
    }

    @Test
    public void testSecurities() {
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
        final List<SecurityNode> securitiesList = new ArrayList<>();
        securitiesList.addAll(investAccount.getSecurities());
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

        assertEquals("gobbleDeGook", e.getAccountAttribute(b, "myStuff"));

        // close and reopen to force check for persistence
        closeEngine();

        e = EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);

        b = e.getAccountByUuid(a.getUuid());
        assertEquals("gobbleDeGook", e.getAccountAttribute(b, "myStuff"));
        assertEquals("myValue", e.getAccountAttribute(b, "myKey"));

        String attribute = e.getAccountAttribute(b, "myNumber");

        assertNotNull(attribute);

        assertEquals(BigDecimal.TEN, new BigDecimal(attribute));
    }

    @Test
    public void testTransactionAPI() {
        final String ACCOUNT_NAME = "testAccount";

        final CurrencyNode node = e.getDefaultCurrency();

        final Account a = new Account(AccountType.BANK, node);
        a.setName(ACCOUNT_NAME);

        e.addAccount(e.getRootAccount(), a);

        // Test single entry transaction
        final Transaction transaction = TransactionFactory.generateSingleEntryTransaction(a, BigDecimal.TEN, LocalDate.now(),
                "memo", "payee", "1");

        e.addTransaction(transaction);

        assertEquals(TransactionType.SINGLENTRY, transaction.getTransactionType());

        for (final TransactionEntry transactionEntry : transaction.getTransactionEntries()) {
            assertFalse(transactionEntry.isMultiCurrency());
        }
    }

    @Test
    public void placeHolder() {
        assertTrue(e.getTransactionsWithAttachments().isEmpty());
    }
}
