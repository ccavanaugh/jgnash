package jgnash.engine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static jgnash.engine.TransactionFactory.*;
import static org.junit.jupiter.api.Assertions.*;

/**
  * Unit test for investment history and exchange rate combinations.
  *
  * @author Craig Cavanaugh
  */
class InvestmentHistoryExchangeTest {

     private static final DateTimeFormatter SIMPLE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

     private String database;

     private Engine e;

     private Account expenseAccount;

     private Account usdBankAccount;

     private Account investAccount;

     private SecurityNode securityNode;

     private CurrencyNode usdCurrency;

     private CurrencyNode cadCurrency;

     @Test
     void testExchangeRate() {
         assertEquals(new BigDecimal("0.5"), usdCurrency.getExchangeRate(cadCurrency));
         assertEquals(new BigDecimal("2"), cadCurrency.getExchangeRate(usdCurrency));
     }

     private static LocalDate getLocalDate(final String date) {
         return LocalDate.parse(date, SIMPLE_DATE_FORMAT);
     }

     @Test
     void testHistorySearch() {

         final SecurityHistoryNode old = new SecurityHistoryNode();
         old.setDate(getLocalDate("2014-06-26"));
         old.setPrice(new BigDecimal("500.00"));
         assertTrue(e.addSecurityHistory(securityNode, old));

         final SecurityHistoryNode today = new SecurityHistoryNode();
         today.setDate(getLocalDate("2014-06-27"));
         today.setPrice(new BigDecimal("501.00"));
         assertTrue(e.addSecurityHistory(securityNode, today));

         final SecurityHistoryNode future = new SecurityHistoryNode();
         future.setDate(getLocalDate("2014-06-28"));
         future.setPrice(new BigDecimal("502.00"));
         assertTrue(e.addSecurityHistory(securityNode, future));

         Optional<SecurityHistoryNode> search = securityNode.getClosestHistoryNode(getLocalDate("2014-06-26"));
         assertTrue(search.isPresent());
         search.ifPresent(securityHistoryNode -> assertEquals(old, securityHistoryNode));

         search = securityNode.getClosestHistoryNode(getLocalDate("2014-06-27"));
         assertTrue(search.isPresent());
         search.ifPresent(securityHistoryNode -> assertEquals(today, securityHistoryNode));

         search = securityNode.getClosestHistoryNode(getLocalDate("2014-06-28"));
         assertTrue(search.isPresent());
         search.ifPresent(securityHistoryNode -> assertEquals(future, securityHistoryNode));

         // postdate closest search, should return null
         search = securityNode.getClosestHistoryNode(getLocalDate("2014-06-29"));
         assertTrue(search.isPresent());
         search.ifPresent(securityHistoryNode -> assertEquals(future, securityHistoryNode));

         // predate closest search, should return null
         search = securityNode.getClosestHistoryNode(getLocalDate("2014-06-25"));
         assertFalse(search.isPresent());

         // predate exact match, should turn null;
         search = securityNode.getHistoryNode(getLocalDate("2014-06-25"));
         assertFalse(search.isPresent());

         // postdate exact match, should turn null;
         search = securityNode.getHistoryNode(getLocalDate("2014-06-29"));
         assertFalse(search.isPresent());

         // exact match, should match
         search = securityNode.getHistoryNode(getLocalDate("2014-06-27"));
         assertTrue(search.isPresent());
         search.ifPresent(securityHistoryNode -> assertEquals(today, securityHistoryNode));

         BigDecimal price = Engine.getMarketPrice(Collections.emptyList(), securityNode, usdCurrency,
                 getLocalDate("2014-06-29"));

         assertEquals(new BigDecimal("502.00"), price);

         price = Engine.getMarketPrice(Collections.emptyList(), securityNode, usdCurrency, getLocalDate("2014-06-28"));
         assertEquals(new BigDecimal("502.00"), price);

         price = Engine.getMarketPrice(Collections.emptyList(), securityNode, usdCurrency, getLocalDate("2014-06-27"));
         assertEquals(new BigDecimal("501.00"), price);

         price = Engine.getMarketPrice(Collections.emptyList(), securityNode, usdCurrency, getLocalDate("2014-06-26"));
         assertEquals(new BigDecimal("500.00"), price);

         price = Engine.getMarketPrice(Collections.emptyList(), securityNode, usdCurrency, getLocalDate("2014-06-25"));
         assertEquals(BigDecimal.ZERO, price);

         price = Engine.getMarketPrice(Collections.emptyList(), securityNode, cadCurrency, getLocalDate("2014-06-25"));
         assertEquals(0, price.compareTo(BigDecimal.ZERO));

         price = Engine.getMarketPrice(Collections.emptyList(), securityNode, cadCurrency, getLocalDate("2014-06-26"));
         assertEquals(0, price.compareTo(new BigDecimal("250.00")));

         price = Engine.getMarketPrice(Collections.emptyList(), securityNode, cadCurrency, getLocalDate("2014-06-27"));
         assertEquals(0, price.compareTo(new BigDecimal("250.50")));

         price = Engine.getMarketPrice(Collections.emptyList(), securityNode, cadCurrency, getLocalDate("2014-06-28"));
         assertEquals(0, price.compareTo(new BigDecimal("251.00")));

         price = Engine.getMarketPrice(Collections.emptyList(), securityNode, cadCurrency, getLocalDate("2014-06-29"));
         assertEquals(0, price.compareTo(new BigDecimal("251.00")));

         /// Test with a transaction for history precedence ///

         List<TransactionEntry> fees = new ArrayList<>();
         fees.add(createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Fees",
                 TransactionTag.INVESTMENT_FEE));

         // Buying shares
         Transaction it = generateBuyXTransaction(usdBankAccount, investAccount, securityNode, new BigDecimal("501.34"),
                 new BigDecimal("125"), BigDecimal.ONE, getLocalDate("2014-06-27"), "Buy shares", fees);

         assertTrue(e.addTransaction(it));

         price = Engine.getMarketPrice(investAccount.getSortedTransactionList(), securityNode, usdCurrency,
                 getLocalDate("2014-06-27"));
         assertEquals(new BigDecimal("501.00"), price);

         /// Test a transaction after any known security history ///

         fees.clear();
         fees.add(createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Fees",
                 TransactionTag.INVESTMENT_FEE));

         it = generateBuyXTransaction(usdBankAccount, investAccount, securityNode, new BigDecimal("501.34"),
                 new BigDecimal("125"), BigDecimal.ONE, getLocalDate("2014-06-29"), "Buy shares", fees);

         assertTrue(e.addTransaction(it));


         price = Engine.getMarketPrice(investAccount.getSortedTransactionList(), securityNode, usdCurrency,
                 getLocalDate("2014-06-29"));

         assertEquals(new BigDecimal("501.34"), price);

         price = Engine.getMarketPrice(investAccount.getSortedTransactionList(), securityNode, usdCurrency,
                 getLocalDate("2014-06-30"));

         assertEquals(new BigDecimal("501.34"), price);

         /// Test a transaction after any known security history and between a newer ///

         fees.clear();
         fees.add(createTransactionEntry(investAccount, expenseAccount, new BigDecimal("20.00"), "Fees",
                 TransactionTag.INVESTMENT_FEE));

         it = generateBuyXTransaction(usdBankAccount, investAccount, securityNode, new BigDecimal("502.34"),
                 new BigDecimal("125"), BigDecimal.ONE, getLocalDate("2014-07-01"), "Buy shares", fees);

         assertTrue(e.addTransaction(it));

         price = Engine.getMarketPrice(investAccount.getSortedTransactionList(), securityNode, usdCurrency,
                 getLocalDate("2014-07-01"));

         assertFalse(securityNode.getHistoryNode(getLocalDate("2014-07-01")).isPresent());
         assertEquals(new BigDecimal("502.34"), price);

         price = Engine.getMarketPrice(investAccount.getSortedTransactionList(), securityNode, usdCurrency,
                 getLocalDate("2014-07-02"));

         assertFalse(securityNode.getHistoryNode(getLocalDate("2014-07-02")).isPresent());
         assertEquals(new BigDecimal("502.34"), price);

         assertFalse(securityNode.getHistoryNode(getLocalDate("2014-06-30")).isPresent());
         price = Engine.getMarketPrice(investAccount.getSortedTransactionList(), securityNode, usdCurrency,
                 getLocalDate("2014-06-30"));

         assertEquals(new BigDecimal("501.34"), price);

         final SecurityHistoryNode future2 = new SecurityHistoryNode();
         future2.setDate(getLocalDate("2014-07-02"));
         future2.setPrice(new BigDecimal("503.00"));
         assertTrue(e.addSecurityHistory(securityNode, future2));
         assertTrue(securityNode.getHistoryNode(getLocalDate("2014-07-02")).isPresent());

         price = Engine.getMarketPrice(investAccount.getSortedTransactionList(), securityNode, usdCurrency,
                 getLocalDate("2014-07-02"));

         assertEquals(new BigDecimal("503.00"), price);

     }

     @BeforeEach
     void setUp() {
         try {
             database = Files.createTempFile("jgnash", ".bxds").toString();
             EngineFactory.deleteDatabase(database);

             e = EngineFactory.bootLocalEngine(database, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                     DataStoreType.BINARY_XSTREAM);
             e.setCreateBackups(false);

             // Creating currencies
             usdCurrency = DefaultCurrencies.buildCustomNode("USD");

             e.addCurrency(usdCurrency);
             e.setDefaultCurrency(usdCurrency);

             cadCurrency = DefaultCurrencies.buildCustomNode("CAD");
             e.addCurrency(cadCurrency);

             e.setExchangeRate(usdCurrency, cadCurrency, new BigDecimal("0.5"), LocalDate.now().minusYears(10));

             // Creating securities
             securityNode = new SecurityNode(usdCurrency);

             securityNode.setSymbol("GOOGL");
             securityNode.setDescription("Google");
             securityNode.setScale((byte) 2);
             assertTrue(e.addSecurity(securityNode));

             // Creating accounts
             final Account incomeAccount = new Account(AccountType.INCOME, usdCurrency);
             incomeAccount.setName("Income Account");
             e.addAccount(e.getRootAccount(), incomeAccount);

             expenseAccount = new Account(AccountType.EXPENSE, usdCurrency);
             expenseAccount.setName("Expense Account");
             e.addAccount(e.getRootAccount(), expenseAccount);

             usdBankAccount = new Account(AccountType.BANK, usdCurrency);
             usdBankAccount.setName("USD Bank Account");
             e.addAccount(e.getRootAccount(), usdBankAccount);

             Account cadBankAccount = new Account(AccountType.BANK, cadCurrency);
             cadBankAccount.setName("CAD Bank Account");
             e.addAccount(e.getRootAccount(), cadBankAccount);

             Account equityAccount = new Account(AccountType.EQUITY, usdCurrency);
             equityAccount.setName("Equity Account");
             e.addAccount(e.getRootAccount(), equityAccount);

             Account liabilityAccount = new Account(AccountType.LIABILITY, usdCurrency);
             liabilityAccount.setName("Liability Account");
             e.addAccount(e.getRootAccount(), liabilityAccount);

             investAccount = new Account(AccountType.INVEST, usdCurrency);
             investAccount.setName("Invest Account");
             e.addAccount(e.getRootAccount(), investAccount);

             // Adding security to the invest account
             List<SecurityNode> securityNodeList = new ArrayList<>();
             securityNodeList.add(securityNode);
             assertTrue(e.updateAccountSecurities(investAccount, securityNodeList));
         } catch (final Exception e) {
             fail(e.getMessage());
         }
     }

     @AfterEach
     void tearDown() {
         EngineFactory.closeEngine(EngineFactory.DEFAULT);
         EngineFactory.deleteDatabase(database);
     }
 }
