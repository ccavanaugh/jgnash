import java.io.IOException;

import jgnash.engine.AbstractEngineTest;
import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test methods for public API's
 *
 * @author Craig Cavanaugh
 */
public class ApiTest extends AbstractEngineTest {

    @Override
    protected Engine createEngine() throws IOException {
        database = testFolder.newFile("api-test.bxds").getAbsolutePath();
        EngineFactory.deleteDatabase(database);

        return EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, PASSWORD, DataStoreType.BINARY_XSTREAM);
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
}
