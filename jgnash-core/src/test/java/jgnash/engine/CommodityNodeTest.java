/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Commodity test.
 *
 * @author Craig Cavanaugh
 */
@ExtendWith(TemporaryFolderExtension.class)
class CommodityNodeTest {

    private TemporaryFolder testFolder;

    @BeforeEach
    void setUp(final TemporaryFolder testFolder) {
        this.testFolder = testFolder;
    }

    @Test
    void ExchangeTest1() {

        try {
            final String database = testFolder.createFile("exchange-test.xml").getAbsolutePath();

            EngineFactory.deleteDatabase(database);

            Engine e = EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                    DataStoreType.XML);

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

            EngineFactory.closeEngine(EngineFactory.DEFAULT);
        } catch (final EngineException | IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void ExchangeTest2() {
        try {
            final String database = testFolder.createFile("exchange-test2.xml").getAbsolutePath();
            EngineFactory.deleteDatabase(database);

            // get an engine, create a commodity and then try to retrieve
            Engine e = EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                    DataStoreType.XML);

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
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    void CommodityNodeStore() {

        try {
            final String database = testFolder.createFile("exchange-test3.xml").getAbsolutePath();
            EngineFactory.deleteDatabase(database);

            // get an engine, create a commodity and then try to retrieve
            Engine e = EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                    DataStoreType.XML);
            Objects.requireNonNull(e);

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

            Objects.requireNonNull(e);
            Object cNode = e.getCurrency("USD");

            System.out.println(cNode);

            //noinspection ConstantConditions
            assertTrue(cNode instanceof CurrencyNode, "Returned object extends CurrencyNode");

            //noinspection ConstantConditions
            assertTrue(cNode instanceof StoredObject, "Returned object extends StoredObject");

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
