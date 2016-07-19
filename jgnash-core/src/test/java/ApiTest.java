import java.io.IOException;
import java.math.BigDecimal;

import jgnash.engine.AbstractEngineTest;
import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.SecurityNode;

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
    public void placeHolder() {
        assertTrue(e.getTransactionsWithAttachments().isEmpty());
    }
}
