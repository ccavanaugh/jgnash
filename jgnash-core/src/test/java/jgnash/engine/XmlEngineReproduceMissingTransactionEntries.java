package jgnash.engine;

import org.junit.Assert;
import org.junit.Test;

public class XmlEngineReproduceMissingTransactionEntries {

    @Test
    public void calculateAccountBalanceCorrectly() throws Exception {
        String absolutePath = XmlEngineReproduceMissingTransactionEntries.class.getResource("/identical_transaction_entries.xml").getFile();
        Engine engine = EngineFactory.bootLocalEngine(absolutePath, EngineFactory.DEFAULT, DataStoreType.XML);
        //Engine engine = EngineFactory.bootLocalEngine(absolutePath, EngineFactory.DEFAULT, EngineTest.PASSWORD, DataStoreType.XML);

        Account groceries = engine.getAccountByName("Food");
        Assert.assertEquals(4.0, groceries.getBalance().doubleValue(), 0.0);
    }
}
