package jgnash.engine;

import io.github.glytching.junit.extension.folder.TemporaryFolder;
import io.github.glytching.junit.extension.folder.TemporaryFolderExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    @ExtendWith(TemporaryFolderExtension.class)
    void testBackEnd(final TemporaryFolder testFolder) throws IOException {

        assertNotNull(testFolder);

        final String database = testFolder.createFile("transaction-test.xml").getAbsolutePath();

        EngineFactory.deleteDatabase(database);

        try {
            Engine e = EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                    DataStoreType.XML);

            e.setCreateBackups(false);

            CurrencyNode defaultCurrency = DefaultCurrencies.buildCustomNode("USD");

            e.addCurrency(defaultCurrency);
            e.setDefaultCurrency(defaultCurrency);

            CurrencyNode cadCurrency = DefaultCurrencies.buildCustomNode("CAD");
            e.addCurrency(cadCurrency);

            Account incomeAccount = new Account(AccountType.INCOME, defaultCurrency);
            incomeAccount.setName("Income Account");
            e.addAccount(e.getRootAccount(), incomeAccount);

            Account expenseAccount = new Account(AccountType.EXPENSE, defaultCurrency);
            expenseAccount.setName("Expense Account");
            e.addAccount(e.getRootAccount(), expenseAccount);

            Account usdBankAccount = new Account(AccountType.BANK, defaultCurrency);
            usdBankAccount.setName("USD Bank Account");
            e.addAccount(e.getRootAccount(), usdBankAccount);

            Account cadBankAccount = new Account(AccountType.BANK, cadCurrency);
            cadBankAccount.setName("CAD Bank Account");
            e.addAccount(e.getRootAccount(), cadBankAccount);

            TransactionEntry entry = new TransactionEntry();

            entry.setDebitAccount(incomeAccount);
            entry.setDebitAmount(new BigDecimal("-500.00"));

            entry.setCreditAmount(new BigDecimal("500.00"));
            entry.setCreditAccount(usdBankAccount);

            entry.setMemo("Income transaction");

            Transaction transaction = new Transaction();
            transaction.addTransactionEntry(entry);
            transaction.setPayee("Employer");

            e.addTransaction(transaction);

            assertEquals(new BigDecimal("500.00"), transaction.getAmount(usdBankAccount));
            assertEquals(new BigDecimal("500.00"), usdBankAccount.getBalance());

            EngineFactory.closeEngine(EngineFactory.DEFAULT);
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    @ExtendWith(TemporaryFolderExtension.class)
    void testEmptyAccount(final TemporaryFolder testFolder) throws IOException {
        final String database = testFolder.createFile("empty-test.xml").getAbsolutePath();

        EngineFactory.deleteDatabase(database);

        try {
            Engine e = EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                    DataStoreType.XML);

            e.setCreateBackups(false);

            CurrencyNode defaultCurrency = DefaultCurrencies.buildCustomNode("USD");

            e.addCurrency(defaultCurrency);
            e.setDefaultCurrency(defaultCurrency);

            Account usdBankAccount = new Account(AccountType.BANK, defaultCurrency);
            usdBankAccount.setName("USD Bank Account");
            e.addAccount(e.getRootAccount(), usdBankAccount);

            // ensure API does not break down

            List<Transaction> transactions = usdBankAccount.getSortedTransactionList();

            assertTrue(transactions.isEmpty());

            transactions = usdBankAccount.getTransactions(LocalDate.now().minusDays(1), LocalDate.now());

            assertTrue(transactions.isEmpty());
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

}
