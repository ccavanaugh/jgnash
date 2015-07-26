package jgnash.engine;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import static jgnash.engine.TransactionFactory.*;
import static org.junit.Assert.*;

/**
 * Unit tests for investment account transactions
 *
 * @author Peti
 * @author Craig Cavanaugh
 */
public class InvestmentTransactionTest extends AbstractEngineTest {

    public InvestmentTransactionTest() {
    }

    @Override
    protected Engine createEngine() {
        database = EngineFactory.getDefaultDatabase() + "-investTransaction-test.xml";
        EngineFactory.deleteDatabase(database);

        return EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, PASSWORD, DataStoreType.XML);
    }

    @Test
    public void NoErrorIfSecurityHistoryEmpty() {
        // Transferring some money to usdBankAccount
        Date transactionDate0;
        transactionDate0 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-25", new ParsePosition(0));

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
        Date transactionDate1;
        BigDecimal securityPrice1;

        transactionDate1 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-26", new ParsePosition(0));
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
        it = generateBuyXTransaction(usdBankAccount, investAccount, securityNode1, securityPrice1, new BigDecimal("125"), BigDecimal.ONE, transactionDate1, "Buy shares", fees);

        assertTrue(e.addTransaction(it));
        assertFalse(e.addTransaction(it));
    }

    @Test
    public void BuyShares() {
        // Transferring some money to usdBankAccount
        Date transactionDate0;
        transactionDate0 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-25", new ParsePosition(0));
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
        Date transactionDate1;
        BigDecimal securityPrice1;

        transactionDate1 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-26", new ParsePosition(0));
        securityPrice1 = new BigDecimal("2.00");

        SecurityHistoryNode history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(securityNode1, history));

        // Creating the list of fees
        List<TransactionEntry> fees = new ArrayList<>();
        TransactionEntry fee1 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Fee1", TransactionTag.INVESTMENT_FEE);
        fees.add(fee1);
        TransactionEntry fee2 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("10.00"), "Fee2", TransactionTag.INVESTMENT_FEE);
        fees.add(fee2);

        // Buying shares
        InvestmentTransaction it;
        it = generateBuyXTransaction(usdBankAccount, investAccount, securityNode1, securityPrice1, new BigDecimal("125"), BigDecimal.ONE, transactionDate1, "Buy shares", fees);
        assertTrue(e.addTransaction(it));

        // Evaluating the result
        Object actual[] = {usdBankAccount.getBalance(), expenseAccount.getBalance(), investAccount.getBalance(),
                investAccount.getMarketValue(), investAccount.getCashBalance()};
        Object expected[] = {new BigDecimal("220.00"), new BigDecimal("30.00"), new BigDecimal("250.00"),
                new BigDecimal("250.00"), new BigDecimal("0.00")};
        assertArrayEquals("Account balances are not as expected!", expected, actual);
    }

    @Test
    public void SellShares() {
        // Transferring some money to usdBankAccount
        Date transactionDate0;
        transactionDate0 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-25", new ParsePosition(0));
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
        Date transactionDate1;
        BigDecimal securityPrice1;

        transactionDate1 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-26", new ParsePosition(0));
        securityPrice1 = new BigDecimal("2.00");

        SecurityHistoryNode history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(securityNode1, history));

        // Creating the list of buying fees
        List<TransactionEntry> buyingFees = new ArrayList<>();
        TransactionEntry bFee1 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Buying Fee1", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee1);
        TransactionEntry bFee2 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("10.00"), "Buying Fee2", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee2);

        // Buying shares
        InvestmentTransaction it;
        it = generateBuyXTransaction(usdBankAccount, investAccount, securityNode1, securityPrice1, new BigDecimal("125"), BigDecimal.ONE, transactionDate1, "Buy shares", buyingFees);
        e.addTransaction(it);

        Date transactionDate2;
        BigDecimal securityPrice2;
        transactionDate2 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-27", new ParsePosition(0));
        securityPrice2 = new BigDecimal("3.00");

        history = new SecurityHistoryNode();
        history.setDate(transactionDate2);
        history.setPrice(securityPrice2);

        assertTrue(e.addSecurityHistory(securityNode1, history));

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

        it = generateSellXTransaction(usdBankAccount, investAccount, securityNode1, securityPrice2, new BigDecimal("30"), BigDecimal.ONE, transactionDate2, "Selling shares", sellingFees, sellingGains);
        e.addTransaction(it);

        // Checking the result
        Object actual[] = {usdBankAccount.getBalance(), expenseAccount.getBalance(), incomeAccount.getBalance(),
                investAccount.getBalance(), investAccount.getMarketValue(), investAccount.getCashBalance()};
        Object expected[] = {new BigDecimal("280.00"), new BigDecimal("60.00"), new BigDecimal("-30.00"),
                new BigDecimal("285.00"), new BigDecimal("285.00"), new BigDecimal("0.00")};
        assertArrayEquals("Account balances are not as expected!", expected, actual);
    }

    @Test
    public void TransferCashIn() throws ParseException {
        // Transferring some money to usdBankAccount
        Date transactionDate0;
        transactionDate0 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-25");

        e.addTransaction(TransactionFactory.generateDoubleEntryTransaction(usdBankAccount, equityAccount, new BigDecimal("500.00"), transactionDate0, "Equity transaction", "", "" ));

        // Adding cash in transaction
        Date transactionDate1;
        transactionDate1 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-26");

        e.addTransaction(TransactionFactory.generateDoubleEntryTransaction(investAccount, usdBankAccount, new BigDecimal("250.00"), transactionDate1, "Cash in transaction", "", ""));

        // Checking the result
        Object actual[] = {usdBankAccount.getBalance(), expenseAccount.getBalance(), incomeAccount.getBalance(),
                investAccount.getBalance(), investAccount.getMarketValue(), investAccount.getCashBalance()};

        Object expected[] = {new BigDecimal("250.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("250.00"),
                BigDecimal.ZERO, new BigDecimal("250.00")};

        assertArrayEquals("Account balances are not as expected!", expected, actual);
    }

    @Test
    public void TransferCashOut() {
        // Transferring some money to usdBankAccount
        Date transactionDate0;
        transactionDate0 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-25", new ParsePosition(0));
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
        Date transactionDate1;
        transactionDate1 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-26", new ParsePosition(0));

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
        Date transactionDate2;
        transactionDate2 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-27", new ParsePosition(0));

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
        Object actual[] = {usdBankAccount.getBalance(), expenseAccount.getBalance(), incomeAccount.getBalance(),
                investAccount.getBalance(), investAccount.getMarketValue(), investAccount.getCashBalance()};
        Object expected[] = {new BigDecimal("250.00"), new BigDecimal("125.00"), BigDecimal.ZERO,
                new BigDecimal("125.00"), BigDecimal.ZERO, new BigDecimal("125.00")};
        assertArrayEquals("Account balances are not as expected!", expected, actual);
    }

    @Test
    public void Dividend() {
        // Transferring some money to usdBankAccount
        Date transactionDate0;
        transactionDate0 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-25", new ParsePosition(0));
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
        Date transactionDate1;
        BigDecimal securityPrice1;

        transactionDate1 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-26", new ParsePosition(0));
        securityPrice1 = new BigDecimal("2.00");

        SecurityHistoryNode history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(securityNode1, history));

        // Creating the list of buying fees
        List<TransactionEntry> buyingFees = new ArrayList<>();
        TransactionEntry bFee1 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Buying Fee1", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee1);
        TransactionEntry bFee2 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("10.00"), "Buying Fee2", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee2);

        // Buying shares
        InvestmentTransaction it;
        it = generateBuyXTransaction(usdBankAccount, investAccount, securityNode1, securityPrice1, new BigDecimal("125"), BigDecimal.ONE, transactionDate1, "Buy shares", buyingFees);
        e.addTransaction(it);

        Date transactionDate2;
        transactionDate2 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-27", new ParsePosition(0));

        it = generateDividendXTransaction(incomeAccount, investAccount, usdBankAccount, securityNode1, new BigDecimal("50.00"), new BigDecimal("-50.00"), new BigDecimal("50.00"), transactionDate2, "Dividend");
        e.addTransaction(it);

        // Checking the result
        Object actual[] = {usdBankAccount.getBalance(), expenseAccount.getBalance(), incomeAccount.getBalance(),
                investAccount.getBalance(), investAccount.getMarketValue(), investAccount.getCashBalance()};
        Object expected[] = {new BigDecimal("270.00"), new BigDecimal("30.00"), new BigDecimal("-50.00"),
                new BigDecimal("250.00"), new BigDecimal("250.00"), new BigDecimal("0.00")};
        assertArrayEquals("Account balances are not as expected!", expected, actual);
    }

    @Test
    public void ReinvestDividend() {
        // Transferring some money to usdBankAccount
        Date transactionDate0;
        transactionDate0 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-25", new ParsePosition(0));
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
        Date transactionDate1;
        BigDecimal securityPrice1;

        transactionDate1 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-26", new ParsePosition(0));
        securityPrice1 = new BigDecimal("2.00");

        SecurityHistoryNode history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(securityNode1, history));

        // Creating the list of buying fees
        List<TransactionEntry> buyingFees = new ArrayList<>();
        TransactionEntry bFee1 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Buying Fee1", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee1);
        TransactionEntry bFee2 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("10.00"), "Buying Fee2", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee2);

        // Buying shares
        InvestmentTransaction it;
        it = generateBuyXTransaction(usdBankAccount, investAccount, securityNode1, securityPrice1, new BigDecimal("125"), BigDecimal.ONE, transactionDate1, "Buy shares", buyingFees);
        e.addTransaction(it);

        Date transactionDate2;
        transactionDate2 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-27", new ParsePosition(0));

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

        it = generateReinvestDividendXTransaction(investAccount, securityNode1, securityPrice1, new BigDecimal("15"), transactionDate2, "Reinvest Dividend", reinvestDividendFees, reinvestDividendGains);
        e.addTransaction(it);

        // Checking the result
        Object actual[] = {usdBankAccount.getBalance(), expenseAccount.getBalance(), incomeAccount.getBalance(),
                investAccount.getBalance(), investAccount.getMarketValue(), investAccount.getCashBalance()};
        Object expected[] = {new BigDecimal("220.00"), new BigDecimal("50.00"), new BigDecimal("-50.00"),
                new BigDecimal("280.00"), new BigDecimal("280.00"), new BigDecimal("0.00")};
        assertArrayEquals("Account balances are not as expected!", expected, actual);
    }

    @Test
    public void StockSplit() {
        // Transferring some money to usdBankAccount
        Date transactionDate0;
        transactionDate0 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-25", new ParsePosition(0));
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
        Date transactionDate1;
        BigDecimal securityPrice1;

        transactionDate1 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-26", new ParsePosition(0));
        securityPrice1 = new BigDecimal("2.00");

        SecurityHistoryNode history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(securityNode1, history));

        // Creating the list of buying fees
        List<TransactionEntry> buyingFees = new ArrayList<>();
        TransactionEntry bFee1 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Buying Fee1", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee1);
        TransactionEntry bFee2 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("10.00"), "Buying Fee2", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee2);

        // Buying shares
        InvestmentTransaction it;
        it = generateBuyXTransaction(usdBankAccount, investAccount, securityNode1, securityPrice1, new BigDecimal("125"), BigDecimal.ONE, transactionDate1, "Buy shares", buyingFees);
        e.addTransaction(it);

        Date transactionDate2;
        transactionDate2 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-27", new ParsePosition(0));

        it = generateSplitXTransaction(investAccount, securityNode1, securityPrice1, new BigDecimal("125"), transactionDate2, "Selling shares");
        e.addTransaction(it);

        // Checking the result
        Object actual[] = {usdBankAccount.getBalance(), expenseAccount.getBalance(), incomeAccount.getBalance(),
                investAccount.getBalance(), investAccount.getMarketValue(), investAccount.getCashBalance()};

        Object expected[] = {new BigDecimal("220.00"), new BigDecimal("30.00"), BigDecimal.ZERO,
                new BigDecimal("500.00"), new BigDecimal("500.00"), new BigDecimal("0.00")};

        assertArrayEquals("Account balances are not as expected!", expected, actual);
    }

    @Test
    public void StockMerge() {
        // Transferring some money to usdBankAccount
        Date transactionDate0;
        transactionDate0 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-25", new ParsePosition(0));
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
        Date transactionDate1;
        BigDecimal securityPrice1;

        transactionDate1 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-26", new ParsePosition(0));
        securityPrice1 = new BigDecimal("2.00");

        SecurityHistoryNode history = new SecurityHistoryNode();
        history.setDate(transactionDate1);
        history.setPrice(securityPrice1);

        assertTrue(e.addSecurityHistory(securityNode1, history));

        // Creating the list of buying fees
        List<TransactionEntry> buyingFees = new ArrayList<>();
        TransactionEntry bFee1 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Buying Fee1", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee1);
        TransactionEntry bFee2 = createTransactionEntry(investAccount, expenseAccount, new BigDecimal("10.00"), "Buying Fee2", TransactionTag.INVESTMENT_FEE);
        buyingFees.add(bFee2);

        // Buying shares
        InvestmentTransaction it;
        it = generateBuyXTransaction(usdBankAccount, investAccount, securityNode1, securityPrice1, new BigDecimal("125"), BigDecimal.ONE, transactionDate1, "Buy shares", buyingFees);
        e.addTransaction(it);

        Date transactionDate2;
        transactionDate2 = new SimpleDateFormat("yyyy-MM-dd").parse("2009-12-27", new ParsePosition(0));

        it = generateMergeXTransaction(investAccount, securityNode1, securityPrice1, new BigDecimal("25"), transactionDate2, "Stock merge");
        e.addTransaction(it);

        // Checking the result
        Object actual[] = {usdBankAccount.getBalance(), expenseAccount.getBalance(), incomeAccount.getBalance(),
                investAccount.getBalance(), investAccount.getMarketValue(), investAccount.getCashBalance()};

        Object expected[] = {new BigDecimal("220.00"), new BigDecimal("30.00"), BigDecimal.ZERO,
                new BigDecimal("200.00"), new BigDecimal("200.00"), new BigDecimal("0.00")};

        assertArrayEquals("Account balances are not as expected!", expected, actual);
    }

    public static TransactionEntry createTransactionEntry(final Account debitAccount, final Account creditAccount, final BigDecimal amount, final String memo, final TransactionTag transactionTag) {
        TransactionEntry entry = new TransactionEntry();

        entry.setMemo(memo);

        entry.setDebitAccount(debitAccount);
        entry.setCreditAccount(creditAccount);

        entry.setDebitAmount(amount.abs().negate());
        entry.setCreditAmount(amount.abs());

        entry.setTransactionTag(transactionTag);
        return entry;
    }
}
