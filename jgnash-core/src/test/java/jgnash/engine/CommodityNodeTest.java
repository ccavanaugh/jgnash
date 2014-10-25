/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

import org.junit.Test;

/**
 * Commodity test
 *
 * @author Craig Cavanaugh
 */
public class CommodityNodeTest {

    //public static final String USER = "";

    private static final char[] PASSWORD = new char[]{};

    @Test
    public void ExchangeTest1() {
        EngineFactory.deleteDatabase(EngineFactory.getDefaultDatabase() + "-exchange-test.xml");

        // get an engine, create a commodity and then try to retrieve

        try {
            Engine e = EngineFactory.bootLocalEngine(EngineFactory.getDefaultDatabase() + "-exchange-test.xml", EngineFactory.DEFAULT, PASSWORD, DataStoreType.XML);

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

            EngineFactory.closeEngine(EngineFactory.DEFAULT);
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void ExchangeTest2() {
        EngineFactory.deleteDatabase(EngineFactory.getDefaultDatabase() + "-exchange-test.xml");

        try {
            // get an engine, create a commodity and then try to retrieve
            Engine e = EngineFactory.bootLocalEngine(EngineFactory.getDefaultDatabase() + "-exchange-test.xml", EngineFactory.DEFAULT, PASSWORD, DataStoreType.XML);

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

            EngineFactory.closeEngine(EngineFactory.DEFAULT);
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void CommodityNodeStore() {

        try {
            EngineFactory.deleteDatabase(EngineFactory.getDefaultDatabase() + "-commodity-test.xml");

            // get an engine, create a commodity and then try to retrieve
            Engine e = EngineFactory.bootLocalEngine(EngineFactory.getDefaultDatabase() + "-commodity-test.xml", EngineFactory.DEFAULT, PASSWORD, DataStoreType.XML);

            CurrencyNode node = new CurrencyNode();

            node.setSymbol("USD");
            node.setPrefix("$");
            node.setDescription("US Dollar");

            e.addCurrency(node);

            node = e.getCurrency("USD");

            Account account = new Account(AccountType.BANK, node);
            account.setName("Bank Account");

            e.addAccount(e.getRootAccount(), account);

            e = EngineFactory.getEngine(EngineFactory.DEFAULT);

            Object cNode = e.getCurrency("USD");

            System.out.println(cNode);

            //noinspection ConstantConditions
            assertTrue("Returned object extends CurrencyNode", cNode instanceof CurrencyNode);

            //noinspection ConstantConditions
            assertTrue("Returned object extends StoredObject", cNode instanceof StoredObject);

            Set<CurrencyNode> nodes = DefaultCurrencies.generateCurrencies();

            for (CurrencyNode n : nodes) {
                e.addCurrency(n);
            }

            EngineFactory.closeEngine(EngineFactory.DEFAULT);
        } catch (Exception e) {
            fail(e.toString());
        }
    }
}
