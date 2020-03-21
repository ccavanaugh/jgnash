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
package jgnash.uifx.control.autocomplete;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineException;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionFactory;
import jgnash.engine.xstream.BinaryXStreamDataStore;
import jgnash.uifx.Options;
import jgnash.uifx.control.AutoCompleteTextField;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.testfx.api.FxToolkit.registerPrimaryStage;

/**
 * Unit test for the Autocomplete models
 */
class AutoCompleteModelTest {
    private static final int TRANSACTION_COUNT = 2000;

    @SuppressWarnings({"WeakerAccess", "unused"})
    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setUp() throws TimeoutException {
        assertTrue(Files.isDirectory(tempDir));

        // setup for a headless env
        System.setProperty("java.awt.headless", "true");
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("prism.verbose", "true");


        registerPrimaryStage();
    }

    @SuppressWarnings("SameParameterValue")
    Engine createEngine(final String fileName) {

        final String database = tempDir.toString() + File.separator + fileName + BinaryXStreamDataStore.FILE_EXT;

        System.out.println(database);

        try {
            return EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                    DataStoreType.BINARY_XSTREAM);
        } catch (final EngineException e) {
            fail("Fatal error occurred");
            return null;
        }
    }

    @Test
    @DisabledOnOs(OS.MAC)
    void testPayeeMemoModels() {
        Options.useFuzzyMatchForAutoCompleteProperty().set(true);

        final Engine engine = createEngine("payee-test");

        final Account account = new Account(AccountType.CASH, engine.getDefaultCurrency());

        engine.addAccount(engine.getRootAccount(), account);
        assertEquals(1, engine.getAccountList().size());

        final LocalDate localDate = LocalDate.of(2000, Month.JANUARY, 1);

        // create a bunch of transactions
        for (int i = 1; i <= TRANSACTION_COUNT; i++) {
            final Transaction t = TransactionFactory.generateSingleEntryTransaction(account, BigDecimal.TEN,
                    localDate.plusDays(i), "Memo " + i, "Payee " + i, Integer.toString(i));

            engine.addTransaction(t);
            assertEquals(i, engine.getTransactions().size());
        }
        assertEquals(TRANSACTION_COUNT, engine.getTransactions().size());

        final AutoCompleteTextField<Transaction> autoCompletePayeeTextField = new AutoCompleteTextField<>();
        AutoCompleteFactory.setPayeeModel(autoCompletePayeeTextField, account);

        final AutoCompleteTextField<Transaction> autoCompleteMemoTextField = new AutoCompleteTextField<>();
        AutoCompleteFactory.setMemoModel(autoCompleteMemoTextField);

        final AutoCompleteModel<Transaction> payeeModel = autoCompletePayeeTextField.autoCompleteModelObjectProperty().get();
        final AutoCompleteModel<Transaction> memoModel = autoCompleteMemoTextField.autoCompleteModelObjectProperty().get();

        final AtomicBoolean payeeModelLoaded = payeeModel.isLoadComplete();
        final AtomicBoolean memoModelLoaded = memoModel.isLoadComplete();

        // Block until the atomics have been set, or the test will fail
        Awaitility.await().untilTrue(payeeModelLoaded);
        Awaitility.await().untilTrue(memoModelLoaded);

        //noinspection SpellCheckingInspection
        final String payeeResult = payeeModel.doLookAhead("Paye");
        final String memoResult = memoModel.doLookAhead("Me");

        assertEquals("Payee " + TRANSACTION_COUNT, payeeResult);
        assertEquals("Memo " + TRANSACTION_COUNT, memoResult);

        EngineFactory.closeEngine(EngineFactory.DEFAULT);
    }
}
