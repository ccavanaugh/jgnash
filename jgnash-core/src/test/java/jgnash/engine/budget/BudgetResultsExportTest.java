/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.engine.budget;

import java.io.File;
import java.nio.file.Files;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * JUnit test class to export a {@code BudgetResultsModel}
 *
 * @author Craig Cavanaugh
 */
public class BudgetResultsExportTest {

    private static final char[] PASSWORD = new char[]{};

    @Test
    public void testExportBudgetResultsModel() throws Exception {

        File file = Files.createTempFile("budget-", DataStoreType.XML.getDataStore().getFileExt()).toFile();

        file.deleteOnExit();

        Engine e = EngineFactory.bootLocalEngine(file.getName(), EngineFactory.DEFAULT, PASSWORD, DataStoreType.XML);
        CurrencyNode node = e.getDefaultCurrency();

        Account account1 = new Account(AccountType.EXPENSE, node);
        account1.setName("Expense 1");
        e.addAccount(e.getRootAccount(), account1);

        Account account2 = new Account(AccountType.EXPENSE, node);
        account2.setName("Expense 2");
        e.addAccount(e.getRootAccount(), account2);

        Budget budget = new Budget();
        budget.setName("My Budget");
        budget.setDescription("Test");
        budget.setBudgetPeriod(BudgetPeriod.MONTHLY);

        assertTrue(e.addBudget(budget));

        BudgetResultsModel model = new BudgetResultsModel(budget, 2012, node);

        File exportFile = Files.createTempFile("testworkbook", ".xls").toFile();

        BudgetResultsExport.exportBudgetResultsModel(exportFile, model);

        assertTrue(exportFile.exists());

        assertTrue(exportFile.delete());

        assertTrue(file.delete());
    }
}
