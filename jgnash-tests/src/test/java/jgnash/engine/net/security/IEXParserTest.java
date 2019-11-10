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
package jgnash.engine.net.security;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import jgnash.engine.AbstractEngineTest;
import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.net.security.SecurityParser;
import jgnash.net.security.iex.IEXParser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static jgnash.engine.net.security.YahooEventParserTest.GITHUB_ACTION;


public class IEXParserTest  extends AbstractEngineTest {

    private static final String TEST_TOKEN = "TEST_TOKEN";

    @Override
    protected Engine createEngine() throws IOException {

        database = testFolder.createFile("iex-test.bxds").getAbsolutePath();
        EngineFactory.deleteDatabase(database);

        return EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                DataStoreType.BINARY_XSTREAM);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")  // disable on Travis-CI
    void testHistoricalDownload() throws IOException {

        if (System.getenv(GITHUB_ACTION) != null) {   // don't test with Github actions
            return;
        }

        // test env must be configured with a valid token
        if (System.getenv(TEST_TOKEN) == null) {   // don't test with Github actions
            return;
        }

        final SecurityNode ibm = new SecurityNode(e.getDefaultCurrency());
        ibm.setSymbol("IBM");
        ibm.setScale((byte) 2);
        ibm.setQuoteSource(QuoteSource.IEX_CLOUD);

        e.addSecurity(ibm);

        final QuoteSource quoteSource = ibm.getQuoteSource();
        assertNotNull(quoteSource);

        final SecurityParser securityParser = quoteSource.getParser();
        assertNotNull(securityParser);

        assertThat(securityParser, instanceOf(IEXParser.class));

        ((IEXParser)securityParser).setUseSandbox();
        securityParser.setTokenSupplier(() -> System.getenv(TEST_TOKEN));

        //final Set<SecurityHistoryEvent> events = securityParser.retrieveHistoricalEvents(ibm, LocalDate.of(2015, Month.AUGUST, 22));

        final List<SecurityHistoryNode> events =  securityParser.retrieveHistoricalPrice(ibm, LocalDate.of(2019,
                                                                 Month.JANUARY, 2), LocalDate.of(2019,
                                                                 Month.MARCH, 1));

        assertNotNull(events);
        assertEquals(41, events.size());
    }
}
