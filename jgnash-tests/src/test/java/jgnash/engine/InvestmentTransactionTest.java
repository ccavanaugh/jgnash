package jgnash.engine;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import static jgnash.engine.TransactionFactory.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Unit tests for investment account transactions.
 *
 * @author Peti
 * @author Craig Cavanaugh
 */
public class InvestmentTransactionTest extends AbstractEngineTest {

    InvestmentTransactionTest() {
    }

    @Override
    protected Engine createEngine() throws IOException {
        database = testFolder.createFile("invest-transaction-test.xml").getAbsolutePath();

        EngineFactory.deleteDatabase(database);

        return EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                DataStoreType.XML);
    }

    @Test
    void NoErrorIfSecurityHistoryEmpty() {
        // Transferring some money to usdBankAccount
        final LocalDate transactionDate0 = LocalDate.of(2009, Month.DECEMBER, 25);

        TransactionEntry entry = new TransactionEntry();

        entry.setDebitAccount(equityAccount);
        entry.setDebitAmount(new BigDecimal("-500.00"));

        entry.setCreditAmount(new BigDecimal("500.00"));
        entry.setCreditAccount(usdBankAccount);

        entry.setMemo("Equity transaction");

        Transaction transaction = new Transaction();
        transaction.addTransactionEntry(entry);
        transaction.setDate(transactionDate0);

        assertTrue(e.addTransaction(transaction));

        // Adding securityPrice to the security price history
        final LocalDate transactionDate1 = LocalDate.of(2009, Month.DECEMBER, 26);
        BigDecimal securityPrice1;

        securityPrice1 = new BigDecimal("2.00");

        // There is not any History added to the security

        // Creating the list of fees
        List<TransactionEntry> fees = new ArrayList<>();
        TransactionEntry fee1 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Fee1", TransactionTag.INVESTMENT_FEE);
        fees.add(fee1);
        TransactionEntry fee2 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("10.00"), "Fee2", TransactionTag.INVESTMENT_FEE);
        fees.add(fee2);

        // Buying shares
        InvestmentTransaction it;
        it = generateBuyXTransaction(usdBankAccount, investAccount, gggSecurityNode, securityPrice1, new BigDecimal("125"), BigDecimal.ONE, transactionDate1, "Buy shares", fees);

        assertTrue(e.addTransaction(it));
        assertFalse(e.addTransaction(it));
    }

    @Test
    void BuyShares() {
        // Transferring some money to usdBankAccount
        final LocalDate transactionDate0 = LocalDate.of(2009, Month.DECEMBER, 25);

        TransactionEntry entry = new TransactionEntry();

        entry.setDebitAccount(equityAccount);
        entry.setDebitAmount(new BigDecimal("-500.00"));

        entry.setCreditAmount(new BigDecimal("500.00"));
        entry.setCreditAccount(usdBankAccount);

        entry.setMemo("Equity transaction");

        Transaction transaction = new Transaction();
        transaction.addTransactionEntry(entry);
        transaction.setDate(transactionDate0);

        assertTrue(e.addTransaction(transaction));

        // Adding securityPrice to the security price history
        BigDecimal securityPrice1;

        final LocalDate transactionDate1 = LocalDate.of(2009, Month.DECEMBER, 26);

        securityPrice1 = new BigDecimal("2.00");

        SecurityHistoryNode history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(gggSecurityNode, history));

        // Creating the list of fees
        List<TransactionEntry> fees = new ArrayList<>();
        TransactionEntry fee1 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Fee1", TransactionTag.INVESTMENT_FEE);
        fees.add(fee1);
        TransactionEntry fee2 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("10.00"), "Fee2", TransactionTag.INVESTMENT_FEE);
        fees.add(fee2);

        // Buying shares
        InvestmentTransaction it;
        it = generateBuyXTransaction(usdBankAccount, investAccount, gggSecurityNode, securityPrice1, new BigDecimal("125"), BigDecimal.ONE, transactionDate1, "Buy shares", fees);
        assertTrue(e.addTransaction(it));

        // Evaluating the result
        Object[] actual = {usdBankAccount.getBalance(), expenseAccount.getBalance(), investAccount.getBalance(),
                investAccount.getMarketValue(), investAccount.getCashBalance()};
        Object[] expected = {new BigDecimal("220.00"), new BigDecimal("30.00"), new BigDecimal("250.00"),
                new BigDecimal("250.00"), new BigDecimal("0.00")};
        assertArrayEquals(expected, actual, "Account balances are not as expected!");
    }

    @Test
    void SellShares() {
        // Transferring some money to usdBankAccount
        final LocalDate transactionDate0 = LocalDate.of(2009, Month.DECEMBER, 25);
        TransactionEntry entry = new TransactionEntry();

        entry.setDebitAccount(equityAccount);
        entry.setDebitAmount(new BigDecimal("-500.00"));

        entry.setCreditAmount(new BigDecimal("500.00"));
        entry.setCreditAccount(usdBankAccount);

        entry.setMemo("Equity transaction");

        Transaction transaction = new Transaction();
        transaction.addTransactionEntry(entry);
        transaction.setDate(transactionDate0);

        e.addTransaction(transaction);

        // Adding securityPrice to the security price history
        //Date transactionDate1;
        BigDecimal securityPrice1;

        final LocalDate transactionDate1 = LocalDate.of(2009, Month.DECEMBER, 26);

        securityPrice1 = new BigDecimal("2.00");

        SecurityHistoryNode history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(gggSecurityNode, history));

        // Creating the list of buying fees
        List<TransactionEntry> buyingFees = new ArrayList<>();
        TransactionEntry bFee1 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Buying Fee1", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee1);
        TransactionEntry bFee2 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("10.00"), "Buying Fee2", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee2);

        // Buying shares
        InvestmentTransaction it;
        it = generateBuyXTransaction(usdBankAccount, investAccount, gggSecurityNode, securityPrice1, new BigDecimal("125"), BigDecimal.ONE, transactionDate1, "Buy shares", buyingFees);
        e.addTransaction(it);

        final LocalDate transactionDate2 = LocalDate.of(2009, Month.DECEMBER, 27);
        final BigDecimal securityPrice2 = new BigDecimal("3.00");

        history = new SecurityHistoryNode();
        history.setDate(transactionDate2);
        history.setPrice(securityPrice2);

        assertTrue(e.addSecurityHistory(gggSecurityNode, history));

        // Creating the list of selling fees
        List<TransactionEntry> sellingFees = new ArrayList<>();
        TransactionEntry sFee1 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Selling Fee1", TransactionTag.INVESTMENT_FEE);
        sellingFees.add(sFee1);
        TransactionEntry sFee2 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("10.00"), "Selling Fee2", TransactionTag.INVESTMENT_FEE);
        sellingFees.add(sFee2);

        // Creating the list of gains
        List<TransactionEntry> sellingGains = new ArrayList<>();
        TransactionEntry sGain1 = createTransactionEntry(incomeAccount, investAccount, new BigDecimal("20.00"), "Selling Gain1", TransactionTag.GAIN_LOSS);
        sellingGains.add(sGain1);
        TransactionEntry sGain2 = createTransactionEntry(incomeAccount, investAccount, new BigDecimal("10.00"), "Selling Gain2", TransactionTag.GAIN_LOSS);
        sellingGains.add(sGain2);

        it = generateSellXTransaction(usdBankAccount, investAccount, gggSecurityNode, securityPrice2, new BigDecimal("30"), BigDecimal.ONE, transactionDate2, "Selling shares", sellingFees, sellingGains);
        e.addTransaction(it);

        // Checking the result
        Object[] actual = {usdBankAccount.getBalance(), expenseAccount.getBalance(), incomeAccount.getBalance(),
                investAccount.getBalance(), investAccount.getMarketValue(), investAccount.getCashBalance()};
        Object[] expected = {new BigDecimal("280.00"), new BigDecimal("60.00"), new BigDecimal("-30.00"),
                new BigDecimal("285.00"), new BigDecimal("285.00"), new BigDecimal("0.00")};
        assertArrayEquals(expected, actual, "Account balances are not as expected!");
    }

    @Test
    void TransferCashIn() {
        // Transferring some money to usdBankAccount
        final LocalDate transactionDate0 = LocalDate.of(2009, Month.DECEMBER, 25);

        e.addTransaction(TransactionFactory.generateDoubleEntryTransaction(usdBankAccount, equityAccount, new BigDecimal("500.00"), transactionDate0, "Equity transaction", "", "" ));

        // Adding cash in transaction
        final LocalDate transactionDate1 = LocalDate.of(2009, Month.DECEMBER, 26);

        e.addTransaction(TransactionFactory.generateDoubleEntryTransaction(investAccount, usdBankAccount, new BigDecimal("250.00"), transactionDate1, "Cash in transaction", "", ""));

        // Checking the result
        Object[] actual = {usdBankAccount.getBalance(), expenseAccount.getBalance(), incomeAccount.getBalance(),
                investAccount.getBalance(), investAccount.getMarketValue().setScale(2, RoundingMode.DOWN), investAccount.getCashBalance()};

        Object[] expected = {new BigDecimal("250.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("250.00"),
                BigDecimal.ZERO.setScale(2, RoundingMode.DOWN), new BigDecimal("250.00")};

        assertArrayEquals(expected, actual, "Account balances are not as expected!");
    }

    @Test
    void TransferCashOut() {
        // Transferring some money to usdBankAccount
        final LocalDate transactionDate0 = LocalDate.of(2009, Month.DECEMBER, 25);
        TransactionEntry entry = new TransactionEntry();

        entry.setDebitAccount(equityAccount);
        entry.setDebitAmount(new BigDecimal("-500.00"));

        entry.setCreditAmount(new BigDecimal("500.00"));
        entry.setCreditAccount(usdBankAccount);

        entry.setMemo("Equity transaction");

        Transaction transaction = new Transaction();
        transaction.addTransactionEntry(entry);
        transaction.setDate(transactionDate0);

        e.addTransaction(transaction);

        // Adding cash in transaction
        final LocalDate transactionDate1 = LocalDate.of(2009, Month.DECEMBER, 26);

        entry = new TransactionEntry();

        entry.setDebitAccount(usdBankAccount);
        entry.setDebitAmount(new BigDecimal("-250.00"));

        entry.setCreditAmount(new BigDecimal("250.00"));
        entry.setCreditAccount(investAccount);

        entry.setMemo("Cash in transaction");

        transaction = new Transaction();
        transaction.addTransactionEntry(entry);
        transaction.setDate(transactionDate1);

        e.addTransaction(transaction);

        // Adding cash out transaction
        final LocalDate transactionDate2 = LocalDate.of(2009, Month.DECEMBER, 27);

        entry = new TransactionEntry();

        entry.setDebitAccount(investAccount);
        entry.setDebitAmount(new BigDecimal("-125.00"));

        entry.setCreditAmount(new BigDecimal("125.00"));
        entry.setCreditAccount(expenseAccount);

        entry.setMemo("Cash out transaction");

        transaction = new Transaction();
        transaction.addTransactionEntry(entry);
        transaction.setDate(transactionDate2);

        e.addTransaction(transaction);

        // Checking the result
        Object[] actual = {usdBankAccount.getBalance(), expenseAccount.getBalance(), incomeAccount.getBalance(),
                investAccount.getBalance(), investAccount.getMarketValue().setScale(2, RoundingMode.DOWN),
                investAccount.getCashBalance()};

        Object[] expected = {new BigDecimal("250.00"), new BigDecimal("125.00"), BigDecimal.ZERO,
                new BigDecimal("125.00"), BigDecimal.ZERO.setScale(2, RoundingMode.DOWN), new BigDecimal("125.00")};

        assertArrayEquals(expected, actual, "Account balances are not as expected!");
    }

    @Test
    void Dividend() {
        // Transferring some money to usdBankAccount
        final LocalDate transactionDate0 = LocalDate.of(2009, Month.DECEMBER, 25);
        TransactionEntry entry = new TransactionEntry();

        entry.setDebitAccount(equityAccount);
        entry.setDebitAmount(new BigDecimal("-500.00"));

        entry.setCreditAmount(new BigDecimal("500.00"));
        entry.setCreditAccount(usdBankAccount);

        entry.setMemo("Equity transaction");

        Transaction transaction = new Transaction();
        transaction.addTransactionEntry(entry);
        transaction.setDate(transactionDate0);

        e.addTransaction(transaction);

        // Adding securityPrice to the security price history
        final LocalDate transactionDate1 = LocalDate.of(2009, Month.DECEMBER, 26);

        BigDecimal securityPrice1;

        securityPrice1 = new BigDecimal("2.00");

        SecurityHistoryNode history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(gggSecurityNode, history));

        // Creating the list of buying fees
        List<TransactionEntry> buyingFees = new ArrayList<>();
        TransactionEntry bFee1 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Buying Fee1", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee1);
        TransactionEntry bFee2 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("10.00"), "Buying Fee2", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee2);

        // Buying shares
        InvestmentTransaction it;
        it = generateBuyXTransaction(usdBankAccount, investAccount, gggSecurityNode, securityPrice1, new BigDecimal("125"), BigDecimal.ONE, transactionDate1, "Buy shares", buyingFees);
        e.addTransaction(it);

        final LocalDate transactionDate2 = LocalDate.of(2009, Month.DECEMBER, 27);

        it = generateDividendXTransaction(incomeAccount, investAccount, usdBankAccount, gggSecurityNode, new BigDecimal("50.00"), new BigDecimal("-50.00"), new BigDecimal("50.00"), transactionDate2, "Dividend");
        e.addTransaction(it);

        // Checking the result
        Object[] actual = {usdBankAccount.getBalance(), expenseAccount.getBalance(), incomeAccount.getBalance(),
                investAccount.getBalance(), investAccount.getMarketValue(), investAccount.getCashBalance()};
        Object[] expected = {new BigDecimal("270.00"), new BigDecimal("30.00"), new BigDecimal("-50.00"),
                new BigDecimal("250.00"), new BigDecimal("250.00"), new BigDecimal("0.00")};

        assertArrayEquals(expected, actual, "Account balances are not as expected!");
    }

    @Test
    void ReinvestDividend() {
        // Transferring some money to usdBankAccount
        final LocalDate transactionDate0 = LocalDate.of(2009, Month.DECEMBER, 25);
        TransactionEntry entry = new TransactionEntry();

        entry.setDebitAccount(equityAccount);
        entry.setDebitAmount(new BigDecimal("-500.00"));

        entry.setCreditAmount(new BigDecimal("500.00"));
        entry.setCreditAccount(usdBankAccount);

        entry.setMemo("Equity transaction");

        Transaction transaction = new Transaction();
        transaction.addTransactionEntry(entry);
        transaction.setDate(transactionDate0);

        e.addTransaction(transaction);

        // Adding securityPrice to the security price history
        final LocalDate transactionDate1 = LocalDate.of(2009, Month.DECEMBER, 26);
        BigDecimal securityPrice1;

        securityPrice1 = new BigDecimal("2.00");

        SecurityHistoryNode history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(gggSecurityNode, history));

        // Creating the list of buying fees
        List<TransactionEntry> buyingFees = new ArrayList<>();
        TransactionEntry bFee1 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Buying Fee1", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee1);
        TransactionEntry bFee2 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("10.00"), "Buying Fee2", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee2);

        // Buying shares
        InvestmentTransaction it;
        it = generateBuyXTransaction(usdBankAccount, investAccount, gggSecurityNode, securityPrice1, new BigDecimal("125"), BigDecimal.ONE, transactionDate1, "Buy shares", buyingFees);
        e.addTransaction(it);

        final LocalDate transactionDate2 = LocalDate.of(2009, Month.DECEMBER, 27);

        // Creating the list of selling fees
        List<TransactionEntry> reinvestDividendFees = new ArrayList<>();
        TransactionEntry rdFee1 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("5.00"), "Reinvest Dividend Fee1", TransactionTag.INVESTMENT_FEE);
        reinvestDividendFees.add(rdFee1);
        TransactionEntry rdFee2 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("15.00"), "Reinvest Dividend Fee2", TransactionTag.INVESTMENT_FEE);
        reinvestDividendFees.add(rdFee2);

        // Creating the list of gains
        List<TransactionEntry> reinvestDividendGains = new ArrayList<>();
        TransactionEntry rdGain1 = createTransactionEntry(incomeAccount, investAccount, new BigDecimal("20.00"), "Reinvest Dividend Gain1", TransactionTag.GAIN_LOSS);
        reinvestDividendGains.add(rdGain1);
        TransactionEntry rdGain2 = createTransactionEntry(incomeAccount, investAccount, new BigDecimal("30.00"), "Reinvest Dividend Gain2", TransactionTag.GAIN_LOSS);
        reinvestDividendGains.add(rdGain2);

        it = generateReinvestDividendXTransaction(investAccount, gggSecurityNode, securityPrice1, new BigDecimal("15"), transactionDate2, "Reinvest Dividend", reinvestDividendFees, reinvestDividendGains);
        e.addTransaction(it);

        // Checking the result
        Object[] actual = {usdBankAccount.getBalance(), expenseAccount.getBalance(), incomeAccount.getBalance(),
                investAccount.getBalance(), investAccount.getMarketValue(), investAccount.getCashBalance()};

        Object[] expected = {new BigDecimal("220.00"), new BigDecimal("50.00"), new BigDecimal("-50.00"),
                new BigDecimal("280.00"), new BigDecimal("280.00"), new BigDecimal("0.00")};

        assertArrayEquals(expected, actual, "Account balances are not as expected!");
    }

    @Test
    void StockSplit() {
        // Transferring some money to usdBankAccount
        final LocalDate transactionDate0 = LocalDate.of(2009, Month.DECEMBER, 25);
        TransactionEntry entry = new TransactionEntry();

        entry.setDebitAccount(equityAccount);
        entry.setDebitAmount(new BigDecimal("-500.00"));

        entry.setCreditAmount(new BigDecimal("500.00"));
        entry.setCreditAccount(usdBankAccount);

        entry.setMemo("Equity transaction");

        Transaction transaction = new Transaction();
        transaction.addTransactionEntry(entry);
        transaction.setDate(transactionDate0);

        e.addTransaction(transaction);

        // Adding securityPrice to the security price history
        final LocalDate transactionDate1 = LocalDate.of(2009, Month.DECEMBER, 26);
        final BigDecimal securityPrice1 = new BigDecimal("2.00");

        SecurityHistoryNode history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(gggSecurityNode, history));

        // Creating the list of buying fees
        List<TransactionEntry> buyingFees = new ArrayList<>();
        TransactionEntry bFee1 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Buying Fee1", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee1);
        TransactionEntry bFee2 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("10.00"), "Buying Fee2", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee2);

        // Buying shares
        InvestmentTransaction it;
        it = generateBuyXTransaction(usdBankAccount, investAccount, gggSecurityNode, securityPrice1, new BigDecimal("125"), BigDecimal.ONE, transactionDate1, "Buy shares", buyingFees);
        e.addTransaction(it);

        final LocalDate transactionDate2 = LocalDate.of(2009, Month.DECEMBER, 27);

        it = generateSplitXTransaction(investAccount, gggSecurityNode, securityPrice1, new BigDecimal("125"), transactionDate2, "Selling shares");
        e.addTransaction(it);

        // Checking the result
        Object[] actual = {usdBankAccount.getBalance(), expenseAccount.getBalance(), incomeAccount.getBalance(),
                investAccount.getBalance(), investAccount.getMarketValue(), investAccount.getCashBalance()};

        Object[] expected = {new BigDecimal("220.00"), new BigDecimal("30.00"), BigDecimal.ZERO,
                new BigDecimal("500.00"), new BigDecimal("500.00"), new BigDecimal("0.00")};

        assertArrayEquals(expected, actual, "Account balances are not as expected!");
    }

    @Test
    void StockMerge() {
        // Transferring some money to usdBankAccount
        final LocalDate transactionDate0 = LocalDate.of(2009, Month.DECEMBER, 25);
        TransactionEntry entry = new TransactionEntry();

        entry.setDebitAccount(equityAccount);
        entry.setDebitAmount(new BigDecimal("-500.00"));

        entry.setCreditAmount(new BigDecimal("500.00"));
        entry.setCreditAccount(usdBankAccount);

        entry.setMemo("Equity transaction");

        Transaction transaction = new Transaction();
        transaction.addTransactionEntry(entry);
        transaction.setDate(transactionDate0);

        e.addTransaction(transaction);

        // Adding securityPrice to the security price history
        final LocalDate transactionDate1 = LocalDate.of(2009, Month.DECEMBER, 26);
        final BigDecimal securityPrice1 = new BigDecimal("2.00");

        SecurityHistoryNode history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(gggSecurityNode, history));

        // Creating the list of buying fees
        List<TransactionEntry> buyingFees = new ArrayList<>();
        TransactionEntry bFee1 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Buying Fee1", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee1);
        TransactionEntry bFee2 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("10.00"), "Buying Fee2", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee2);

        // Buying shares
        InvestmentTransaction it;
        it = generateBuyXTransaction(usdBankAccount, investAccount, gggSecurityNode, securityPrice1, new BigDecimal("125"), BigDecimal.ONE, transactionDate1, "Buy shares", buyingFees);
        e.addTransaction(it);

        final LocalDate transactionDate2 = LocalDate.of(2009, Month.DECEMBER, 27);

        it = generateMergeXTransaction(investAccount, gggSecurityNode, securityPrice1, new BigDecimal("25"), transactionDate2, "Stock merge");
        e.addTransaction(it);

        // Checking the result
        Object[] actual = {usdBankAccount.getBalance(), expenseAccount.getBalance(), incomeAccount.getBalance(),
                investAccount.getBalance(), investAccount.getMarketValue(), investAccount.getCashBalance()};

        Object[] expected = {new BigDecimal("220.00"), new BigDecimal("30.00"), BigDecimal.ZERO,
                new BigDecimal("200.00"), new BigDecimal("200.00"), new BigDecimal("0.00")};

        assertArrayEquals(expected, actual, "Account balances are not as expected!");
    }
}
