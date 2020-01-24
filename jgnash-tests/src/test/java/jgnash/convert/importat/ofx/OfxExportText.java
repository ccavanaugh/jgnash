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
package jgnash.convert.importat.ofx;

import jgnash.convert.exportantur.ofx.OfxExport;
import jgnash.convert.importat.ImportTransaction;
import jgnash.engine.AbstractEngineTest;
import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OfxExportText extends AbstractEngineTest {

    @Override
    protected Engine createEngine() throws IOException {
        database = testFolder.createFile("ofxExportTest.xml").getAbsolutePath();

        EngineFactory.deleteDatabase(database);

        return EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                DataStoreType.XML);
    }

    @Test
    void testExport() throws Exception {

        Transaction transaction = TransactionFactory.generateDoubleEntryTransaction(checkingAccount, usdBankAccount,
                BigDecimal.TEN, LocalDate.now(), "Transfer Test", "Transfer", "");

        assertTrue(e.addTransaction(transaction));

        Path path = Files.createTempFile("j", ".ofx");

        OfxExport ofxExport = new OfxExport(usdBankAccount, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1),
                path.toFile());

        ofxExport.exportAccount();

        assertTrue(Files.exists(path));

        OfxBank ofxBank = OfxV2Parser.parse(path);

        assertNotNull(ofxBank);

        assertEquals(0, ofxBank.statusCode);
        assertEquals("INFO", ofxBank.statusSeverity);

        assertEquals(1, ofxBank.getTransactions().size());

        ImportTransaction importTransaction = ofxBank.getTransactions().get(0);

        assertEquals("10001-C01", importTransaction.getAccountTo());

        Files.delete(path);
    }
}
