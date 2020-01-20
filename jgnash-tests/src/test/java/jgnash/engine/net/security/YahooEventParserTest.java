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
package jgnash.engine.net.security;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jgnash.engine.AbstractEngineTest;
import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityHistoryEvent;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.net.YahooCrumbManager;
import jgnash.net.security.SecurityParser;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * JUnit test for the Yahoo security downloader.
 *
 * @author Craig Cavanaugh
 */
public class YahooEventParserTest extends AbstractEngineTest {

    private static final int ATTEMPTS = 3;
    public static final String GITHUB_ACTION = "GITHUB_ACTION";

    @Override
    protected Engine createEngine() throws IOException {

        database = testFolder.createFile("yahoo-test.bxds").getAbsolutePath();
        EngineFactory.deleteDatabase(database);

        return EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                DataStoreType.BINARY_XSTREAM);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")  // disable on Travis-CI
    @Disabled
    void testParser() throws IOException {

        if (System.getenv(GITHUB_ACTION) != null) {   // don't test with Github actions
           return;
        }

        for (int i = 0; i < ATTEMPTS; i++) {     // try multiple times to pass
            final SecurityNode ibm = new SecurityNode(e.getDefaultCurrency());
            ibm.setSymbol("IBM");
            ibm.setScale((byte) 2);
            ibm.setQuoteSource(QuoteSource.YAHOO);

            final SecurityHistoryNode historyNode = new SecurityHistoryNode(LocalDate.of(1962, Month.JANUARY, 1),
                    BigDecimal.TEN, 1000, BigDecimal.TEN, BigDecimal.TEN);

            e.addSecurity(ibm);
            e.addSecurityHistory(ibm, historyNode);

            QuoteSource quoteSource = ibm.getQuoteSource();
            Objects.requireNonNull(quoteSource);

            SecurityParser securityParser = quoteSource.getParser();
            Objects.requireNonNull(securityParser);

            final Set<SecurityHistoryEvent> events = securityParser.retrieveHistoricalEvents(ibm, LocalDate.of(2015, Month.AUGUST, 22));

            assertNotNull(events);

            // size fluctuates
            if (events.size() <= 221 && events.size() >= 220) {
                assertTrue(events.size() <= 221 && events.size() >= 220);
                return;
            }
        }

        fail("Failed to pass test");
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")  // disable on Travis-CI
    @Disabled
    void testHistoricalDownload() throws IOException {

        if (System.getenv(GITHUB_ACTION) != null) {   // don't test with Github actions
            return;
        }

        for (int i = 0; i < ATTEMPTS; i++) {      // try multiple times to pass
            final SecurityNode ibm = new SecurityNode(e.getDefaultCurrency());
            ibm.setSymbol("IBM");
            ibm.setScale((byte) 2);

            e.addSecurity(ibm);

            YahooCrumbManager.clearAuthorization();     // force re-authorization to prevent failed unit test

            final List<SecurityHistoryNode> events = Objects.requireNonNull(QuoteSource.YAHOO.getParser())
                                                             .retrieveHistoricalPrice(ibm, LocalDate.of(2016,
                    Month.JANUARY, 1), LocalDate.of(2016, Month.DECEMBER, 30));

            if (events.size() == 252) {
                assertEquals(252, events.size());
                return;
            }
        }

        fail("Failed to pass test");
    }
}
