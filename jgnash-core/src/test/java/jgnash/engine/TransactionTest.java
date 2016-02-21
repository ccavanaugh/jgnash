package jgnash.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TransactionTest {

    @SuppressWarnings("CanBeFinal")
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private static final char[] PASSWORD = new char[]{};

    @Test
    public void testBackEnd() throws IOException {

        final String database = testFolder.newFile("transaction-test.xml").getAbsolutePath();
        EngineFactory.deleteDatabase(database);

        try {
            Engine e = EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, PASSWORD, DataStoreType.XML);

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
            assertTrue("passed test: ", true);
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testEmptyAccount() throws IOException {
        final String database = testFolder.newFile("empty-test.xml").getAbsolutePath();
        EngineFactory.deleteDatabase(database);

        try {
            Engine e = EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, PASSWORD, DataStoreType.XML);

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
