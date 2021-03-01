/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2021 Craig Cavanaugh
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
import java.time.LocalDate;
import java.time.Month;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for Investment Performance.
 *
 * @author Craig Cavanaugh
 */
public class InvestmentPerformanceTest extends AbstractEngineTest {

    private final Logger logger = Logger.getLogger(InvestmentPerformanceSummary.class.getName());

    InvestmentPerformanceTest() {
    }

    @Override
    protected Engine createEngine() throws IOException {
        database = testFolder.createFile("invest-perform-test.xml").getAbsolutePath();

        EngineFactory.deleteDatabase(database);

        return EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                DataStoreType.XML);
    }

    @Test
    void basicInvestPerformance() {
        final LocalDate startDate = LocalDate.of(2020, Month.JANUARY, 1);
        final LocalDate endDate = LocalDate.of(2020, Month.DECEMBER, 30);

        e.addTransaction(TransactionFactory.generateDoubleEntryTransaction(usdBankAccount, equityAccount,
                new BigDecimal("500.00"), startDate, "Equity transaction", "", "" ));

        e.addTransaction(TransactionFactory.generateDoubleEntryTransaction(investAccount, usdBankAccount,
                new BigDecimal("500.00"), startDate, "Move cash into investment account", "", "" ));

        final InvestmentPerformanceSummary ips = new InvestmentPerformanceSummary(investAccount, startDate,
                endDate, true);

        assertNotNull(ips);

        ips.runCalculations();

        InvestmentPerformanceSummary.SecurityPerformanceData spd = ips.getPerformanceData(gggSecurityNode);

        logger.info(ips.toString());

        final float delta = 0.01f;

        assertEquals(0f, spd.getSharesHeld().floatValue(), delta);
        assertEquals(0f, spd.getPrice().floatValue(), delta);
    }
}
