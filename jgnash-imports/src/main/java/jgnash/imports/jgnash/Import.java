/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
package jgnash.imports.jgnash;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jgnash.engine.AbstractInvestmentTransactionEntry;
import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.AmortizeObject;
import jgnash.engine.CurrencyNode;
import jgnash.engine.DefaultCurrencies;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.QuoteSource;
import jgnash.engine.ReconcileManager;
import jgnash.engine.ReconciledState;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionEntryAddX;
import jgnash.engine.TransactionEntryMergeX;
import jgnash.engine.TransactionEntryReinvestDivX;
import jgnash.engine.TransactionEntryRemoveX;
import jgnash.engine.TransactionEntrySplitX;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionTag;

/**
 * Import jGnash 1.11.x files. Older versions are not supported
 * <p/>
 * Pass 1: Read commodity data and exchange rates and transaction number items Pass 2: Read account data and Pass 3:
 * Read transaction data Pass 4: Read reminders
 * <p/>
 * Accounts will be kept in a lookup table during the parsing operation
 *
 * @author Craig Cavanaugh
 *
 */
public class Import {

    private static final String ID = "_ID_";

    private static final Logger logger = Logger.getLogger(Import.class.getName());

    private static final Pattern CURRENCY_DELIMITER_PATTERN = Pattern.compile("\\x2E");

    /**
     * Id map for accounts
     */
    private Map<String, Account> accountMap = new HashMap<>();

    /**
     * Parent map for accounts
     */
    private Map<Account, String> parentMap = new HashMap<>();

    /**
     * Lock status of the accounts
     */
    private Map<String, Boolean> lockMap = new HashMap<>();

    /**
     * List of splits
     */
    private List<Map<String, String>> splitList = new ArrayList<>();

    /**
     * List of split entries
     */
    private List<Map<String, String>> splitEntryList = new ArrayList<>();

    /**
     * Id map for commodities
     */
    private Map<String, Commodity> commodityMap = new HashMap<>();

    private static List<Runnable> workQueue = new ArrayList<>();

    /**
     * Cache of currency nodes, so that database doesn't need to be queried for each transaction.
     */
    private Map<String, CurrencyNode> currencyCache = new HashMap<>();

    private Import() {
        try {
            Handler fh = new FileHandler("%h/jgnashimport%g.log");
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.FINE);
        } catch (IOException ioe) {
            logger.severe("Could not install file handler");
        }
    }

    public static void doImport(final String filename) {

        Import imp = new Import();

        XMLInputFactory inputFactory = XMLInputFactory.newInstance();

        InputStream input;

        try {
            Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

            CurrencyNode defaultCurrency = DefaultCurrencies.buildNode(Locale.getDefault());

            if (engine.getCurrency(defaultCurrency.getSymbol()) == null) {
                engine.addCommodity(defaultCurrency);
                engine.setDefaultCurrency(defaultCurrency);
            }

            /* read commodity data first */
            input = new BufferedInputStream(new FileInputStream(new File(filename)));
            XMLStreamReader reader = inputFactory.createXMLStreamReader(input, "UTF-8");
            imp.importCommodities(reader);
            reader.close();
            input.close();

            /* Run commodity generation cleanup threads */
            ExecutorService es = Executors.newSingleThreadExecutor();

            for (Iterator<Runnable> i = workQueue.iterator(); i.hasNext(); ) {
                es.execute(i.next());
                i.remove();
            }

            es.shutdown(); // shutdown after threads are complete

            while (!es.isTerminated()) {
                imp.getLogger().info("Waiting for commodity cleanup threads to complete");
                try {
                    es.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    imp.getLogger().log(Level.SEVERE, e.toString(), e);
                }
            }

            imp.getLogger().info("Commodity cleanup threads complete");

            /* read account data first */
            input = new BufferedInputStream(new FileInputStream(new File(filename)));
            reader = inputFactory.createXMLStreamReader(input, "UTF-8");
            imp.importAccounts(reader);
            reader.close();
            input.close();

            imp.getLogger().info("Running account cleanup threads");

            /* Run account generation cleanup threads */
            es = Executors.newSingleThreadExecutor();

            for (Iterator<Runnable> i = workQueue.iterator(); i.hasNext(); ) {
                es.execute(i.next());
                i.remove();
            }

            es.shutdown(); // shutdown after threads are complete

            while (!es.isTerminated()) {
                imp.getLogger().info("Waiting for account cleanup threads to complete");
                try {
                    es.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    imp.getLogger().log(Level.SEVERE, e.toString(), e);
                }
            }

            imp.getLogger().info("Account cleanup threads complete");

            /* import transactions */
            input = new BufferedInputStream(new FileInputStream(new File(filename)));
            reader = inputFactory.createXMLStreamReader(input, "UTF-8");
            imp.importTransactions(reader);
            reader.close();
            input.close();

            /* Lock accounts after transactions have been added */
            imp.lockAccounts();

        } catch (FileNotFoundException e) {
            imp.getLogger().log(Level.SEVERE, e.toString(), e);
        } catch (XMLStreamException e) {
            imp.getLogger().log(Level.SEVERE, e.toString(), e);
        } catch (IOException e) {
            imp.getLogger().log(Level.SEVERE, e.toString(), e);
        }
    }

    Logger getLogger() {
        return logger;
    }

    void importTransactions(final XMLStreamReader reader) {

        logger.info("Begin transaction import");

        try {
            parse:
            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        if (reader.getAttributeCount() > 0 && reader.getAttributeValue(0).contains("Transaction")) {
                            logger.finest("Found the start of a Transaction");
                            parseTransaction(reader);
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (reader.getLocalName().equals("objects")) {
                            logger.fine("Found the end of the object list and transactions");
                            break parse;
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }

        logger.log(Level.INFO, "Generating {0} Split Transactions", splitList.size());

        /* loop through the lists and add split transactions */
        for (Iterator<Map<String, String>> i = splitList.iterator(); i.hasNext(); ) {

            Map<String, String> map = i.next();

            String id = map.get(ID);

            Transaction transaction = new Transaction();

            transaction.setDate(decodeDate(map.get("voucherDate")));
            transaction.setDateEntered(decodeDate(map.get("actTransDate")));
            transaction.setNumber(map.get("number"));
            transaction.setPayee(map.get("payee"));
            transaction.setMemo(map.get("memo"));

            for (Iterator<Map<String, String>> j = this.splitEntryList.iterator(); j.hasNext(); ) {
                Map<String, String> entryMap = j.next();

                if (entryMap.get("parent").equals(id)) {

                    TransactionEntry entry = new TransactionEntry();

                    entry.setMemo(entryMap.get("memo"));

                    Account creditAccount = accountMap.get(entryMap.get("creditAccount"));
                    Account debitAccount = accountMap.get(entryMap.get("debitAccount"));

                    boolean creditReconciled = Boolean.parseBoolean(entryMap.get("creditReconciled"));
                    boolean debitReconciled = Boolean.parseBoolean(entryMap.get("debitReconciled"));

                    entry.setCreditAccount(creditAccount);
                    entry.setDebitAccount(debitAccount);

                    CurrencyNode node = decodeCurrency(entryMap.get("commodity"));
                    BigDecimal amount = new BigDecimal(entryMap.get("amount"));

                    if (creditAccount.getCurrencyNode().equals(node)) {
                        entry.setCreditAmount(amount);
                    } else {
                        BigDecimal exchangeRate = new BigDecimal(entryMap.get("exchangeRate"));
                        entry.setCreditAmount(amount.multiply(exchangeRate));
                    }

                    if (debitAccount.getCurrencyNode().equals(node)) {
                        entry.setDebitAmount(amount.negate());
                    } else {
                        BigDecimal exchangeRate = new BigDecimal(entryMap.get("exchangeRate"));
                        entry.setDebitAmount(amount.multiply(exchangeRate).negate());
                    }

                    transaction.addTransactionEntry(entry);

                    transaction.setReconciled(creditAccount, creditReconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);

                    transaction.setReconciled(debitAccount, debitReconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);

                    j.remove();
                }
            }

            assert transaction.size() > 0;

            Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

            if (!engine.addTransaction(transaction)) {
                logger.log(Level.SEVERE, "Failed to import transaction: {0}", id);
            }

            i.remove();
        }

        logger.info("Transaction import complete");
    }

    void importAccounts(final XMLStreamReader reader) {

        logger.info("Begin Account import");

        try {
            parse:
            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        if (reader.getAttributeCount() > 0) {
                            if (reader.getAttributeValue(0).contains("Account")) {
                                logger.finest("Found the start of an Account");
                                parseAccount(reader);
                            }
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (reader.getLocalName().equals("objects")) {
                            logger.finest("Found the end of the object list and accounts");
                            break parse;
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }

        logger.info("Linking accounts");

        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        for (Account account : parentMap.keySet()) {
            if (account.getAccountType() != AccountType.ROOT) {
                Account parent = accountMap.get(parentMap.get(account));
                if (!account.getParent().equals(parent)) {
                    engine.moveAccount(account, parent);

                    logger.log(Level.FINEST, "Moving {0} to {1}", new Object[]{account.getName(), parent.getName()});
                }
            }
        }

        logger.info("Account import complete");
    }

    private void lockAccounts() {
        for (String id : lockMap.keySet()) {
            accountMap.get(id).setLocked(lockMap.get(id));
        }

        logger.info("Account lock complete");
    }

    /**
     * Import Commodities.
     * <p/>
     * Commodities are ordered first in the file. Exchange rates are last.
     *
     * @param reader XMLStreamReader
     */
    private void importCommodities(final XMLStreamReader reader) {

        logger.info("Begin Commodity import");
        try {
            while (reader.hasNext()) {

                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        if (reader.getAttributeCount() > 0) {
                            if (reader.getAttributeValue(0).equals("SecurityNode")) {
                                logger.finest("Found the start of a SecurityNode");
                                parseSecurityNode(reader);
                            } else if (reader.getAttributeValue(0).equals("CurrencyNode")) {
                                logger.finest("Found the start of a CurrencyNode");
                                parseCurrencyNode(reader);
                            } else if (reader.getAttributeValue(0).equals("CommodityNode")) {
                                logger.finest("Found the start of a CommodityNode");
                                parseCommodityNode(reader);
                            } else if (reader.getAttributeValue(0).equals("ExchangeRate")) {
                                logger.finest("Parse exchange rate");
                                parseExchangeRate(reader);
                            }
                        }
                        break;
                    default:
                }
            }
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }

        logger.info("Commodity import complete");
    }

    private void parseExchangeRate(final XMLStreamReader reader) {
        Map<String, String> elementMap = new HashMap<>();

        /* still at start of the exchange rate.  Need to know when end is reached */
        QName parsingElement = reader.getName();

        try {
            while (reader.hasNext()) {

                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String element = reader.getLocalName();
                        elementMap.put(element.intern(), reader.getElementText());
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (reader.getName().equals(parsingElement)) {
                            logger.finest("Found the end of the exchange rate");

                            try {
                                String key = elementMap.get("key");

                                if (key != null && key.length() == 6) {

                                    Engine e = EngineFactory.getEngine(EngineFactory.DEFAULT);

                                    CurrencyNode cOne = e.getCurrency(key.substring(0, 3));
                                    CurrencyNode cTwo = e.getCurrency(key.substring(3, 6));

                                    // jGnash 1.x would hold onto old exchange rates for deleted commodities
                                    if (cOne != null && cTwo != null) {
                                        BigDecimal rate = new BigDecimal(elementMap.get("rate"));

                                        EngineFactory.getEngine(EngineFactory.DEFAULT).setExchangeRate(cOne, cTwo, rate);

                                        logger.log(Level.FINE, "Set ExchangeRate {0}:{1}", new Object[]{key,
                                                rate.toString()});
                                    }
                                }

                            } catch (Exception e) {
                                logger.log(Level.SEVERE, e.toString(), e);
                            }
                            return;
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (XMLStreamException e) {
            logger.severe("Error importing exchange rate");
            logger.log(Level.SEVERE, e.toString(), e);
        }

    }

    private void parseTransaction(final XMLStreamReader reader) {

        assert reader.getAttributeCount() == 2;

        Map<String, String> elementMap = new HashMap<>();

        /* still at start of the transaction.  Need to know when end is reached */
        QName parsingElement = reader.getName();

        String transactionClass = reader.getAttributeValue(0);
        String transactionId = reader.getAttributeValue(1);

        elementMap.put(ID, transactionId);

        try {
            while (reader.hasNext()) {

                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String element = reader.getLocalName();
                        elementMap.put(element.intern(), reader.getElementText());
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (reader.getName().equals(parsingElement)) {
                            logger.log(Level.FINEST, "Found the end of a Transaction: {0}", transactionId);

                            try {
                                Transaction transaction = generateTransaction(transactionClass, elementMap);

                                if (transaction != null) {
                                    Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                                    engine.addTransaction(transaction);
                                    logger.finest("Transaction add complete");
                                }
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "Error importing transaction id: {0}", transactionId);
                                logger.log(Level.SEVERE, e.toString(), e);
                                throw new RuntimeException(e);
                            }
                            return;
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, "Error importing transaction id: {0}", transactionId);
            logger.log(Level.SEVERE, e.toString(), e);
        }
    }

    private Transaction generateTransaction(final String transactionClass, final Map<String, String> elementMap) {

        //logger.finest("Being generateTransaction");

        if (transactionClass.equals("SplitTransaction")) {
            logger.finest("Found SplitTransaction");
            splitList.add(elementMap);
            return null;
        } else if (transactionClass.equals("SplitEntryTransaction")) {
            logger.finest("Found SplitEntryTransaction");
            splitEntryList.add(elementMap);
            return null;
        }

        //logger.finest("Building base transation");

        Transaction transaction = null;

        Date actDate = decodeDate(elementMap.get("actTransDate"));
        Date date = decodeDate(elementMap.get("voucherDate"));
        String memo = elementMap.get("memo");
        String payee = elementMap.get("payee");
        String number = elementMap.get("number");
        CurrencyNode node = decodeCurrency(elementMap.get("commodity"));

        //logger.finest(transactionClass);

        if (transactionClass.equals("AddXTransaction") || transactionClass.equals("RemoveXTransaction")) {
            BigDecimal price = new BigDecimal(elementMap.get("price"));
            BigDecimal quantity = new BigDecimal(elementMap.get("quantity"));
            Account investmentAccount = accountMap.get(elementMap.get("investmentAccount"));
            boolean reconciled = Boolean.parseBoolean(elementMap.get("reconciled"));
            SecurityNode sNode = EngineFactory.getEngine(EngineFactory.DEFAULT).getSecurity(elementMap.get("security"));

            transaction = new InvestmentTransaction();

            AbstractInvestmentTransactionEntry entry;

            if (transactionClass.equals("AddXTransaction")) {
                entry = new TransactionEntryAddX(investmentAccount, sNode, price, quantity);
            } else {
                entry = new TransactionEntryRemoveX(investmentAccount, sNode, price, quantity);
            }

            entry.setMemo(memo);

            transaction.setDate(date);
            transaction.setDateEntered(actDate);
            transaction.setPayee(payee);
            transaction.addTransactionEntry(entry);

            transaction.setReconciled(investmentAccount, reconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);
        }

        if (transactionClass.equals("SplitXTransaction") || transactionClass.equals("MergeXTransaction")) {
            BigDecimal price = new BigDecimal(elementMap.get("price"));
            BigDecimal quantity = new BigDecimal(elementMap.get("quantity"));
            Account investmentAccount = accountMap.get(elementMap.get("investmentAccount"));
            boolean reconciled = Boolean.parseBoolean(elementMap.get("reconciled"));
            SecurityNode sNode = EngineFactory.getEngine(EngineFactory.DEFAULT).getSecurity(elementMap.get("security"));

            transaction = new InvestmentTransaction();

            AbstractInvestmentTransactionEntry entry;

            if (transactionClass.equals("SplitXTransaction")) {
                entry = new TransactionEntrySplitX(investmentAccount, sNode, price, quantity);
            } else {
                entry = new TransactionEntryMergeX(investmentAccount, sNode, price, quantity);
            }

            entry.setMemo(memo);

            transaction.setDate(date);
            transaction.setDateEntered(actDate);
            transaction.setPayee(payee);
            transaction.addTransactionEntry(entry);

            transaction.setReconciled(investmentAccount, reconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);
        }

        if (transactionClass.equals("BuyXTransaction") || transactionClass.equals("SellXTransaction")) {
            BigDecimal fees = new BigDecimal(elementMap.get("fees"));
            BigDecimal price = new BigDecimal(elementMap.get("price"));
            BigDecimal quantity = new BigDecimal(elementMap.get("quantity"));
            Account investmentAccount = accountMap.get(elementMap.get("investmentAccount"));
            Account account = accountMap.get(elementMap.get("account"));
            boolean accountReconciled = Boolean.parseBoolean(elementMap.get("accountReconciled"));
            boolean investmentAccountReconciled = Boolean.parseBoolean(elementMap.get("investmentAccountReconciled"));
            SecurityNode sNode = EngineFactory.getEngine(EngineFactory.DEFAULT).getSecurity(elementMap.get("security"));

            BigDecimal exchangeRate = BigDecimal.ONE;

            if (sNode != null) {

                if (transactionClass.equals("BuyXTransaction")) {
                    transaction = TransactionFactory.import1xBuyXTransaction(account, investmentAccount, sNode, price, quantity, exchangeRate, fees, date, memo);
                } else {
                    transaction = TransactionFactory.import1xSellXTransaction(account, investmentAccount, sNode, price, quantity, exchangeRate, fees, date, memo);
                }

                transaction.setDateEntered(actDate);
                transaction.setPayee(payee);

                transaction.setReconciled(investmentAccount, investmentAccountReconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);
                transaction.setReconciled(account, accountReconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);
            }
        }

        if (transactionClass.equals("DividendTransaction")) {

            BigDecimal amount = new BigDecimal(elementMap.get("amount"));
            Account investmentAccount = accountMap.get(elementMap.get("investmentAccount"));
            Account account = accountMap.get(elementMap.get("account"));
            boolean accountReconciled = Boolean.parseBoolean(elementMap.get("accountReconciled"));
            boolean investmentAccountReconciled = Boolean.parseBoolean(elementMap.get("investmentAccountReconciled"));
            SecurityNode sNode = EngineFactory.getEngine(EngineFactory.DEFAULT).getSecurity(elementMap.get("security"));

            transaction = TransactionFactory.generateDividendXTransaction(investmentAccount, investmentAccount, account, sNode, amount, amount.negate(), amount, date, memo, accountReconciled);

            ReconcileManager.reconcileTransaction(investmentAccount, transaction, investmentAccountReconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);

            ReconcileManager.reconcileTransaction(account, transaction, accountReconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);
        }

        if (transactionClass.equals("ReinvestDivTransaction")) {
            BigDecimal fees = new BigDecimal(elementMap.get("fees"));
            BigDecimal price = new BigDecimal(elementMap.get("price"));
            BigDecimal quantity = new BigDecimal(elementMap.get("quantity"));
            SecurityNode sNode = EngineFactory.getEngine(EngineFactory.DEFAULT).getSecurity(elementMap.get("security"));
            Account investmentAccount = accountMap.get(elementMap.get("investmentAccount"));
            boolean reconciled = Boolean.parseBoolean(elementMap.get("reconciled"));

            transaction = new InvestmentTransaction();

            AbstractInvestmentTransactionEntry entry = new TransactionEntryReinvestDivX(investmentAccount, sNode, price, quantity);

            entry.setMemo(memo);

            transaction.setDate(date);
            transaction.setDateEntered(actDate);
            transaction.setPayee(payee);

            transaction.addTransactionEntry(entry);

            if (fees.compareTo(BigDecimal.ZERO) > 0) {
                TransactionEntry fTran = new TransactionEntry(investmentAccount, fees.negate());
                fTran.setMemo(memo);
                fTran.setTransactionTag(TransactionTag.INVESTMENT_FEE);

                transaction.addTransactionEntry(fTran);
            }

            transaction.setReconciled(investmentAccount, reconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);
        }

        if (transactionClass.equals("SingleEntryTransaction")) {
            BigDecimal amount = new BigDecimal(elementMap.get("amount"));
            Account account = accountMap.get(elementMap.get("account"));
            boolean reconciled = Boolean.parseBoolean(elementMap.get("reconciled"));

            transaction = TransactionFactory.generateSingleEntryTransaction(account, amount, date, reconciled, memo, payee, number);
            transaction.setDateEntered(actDate);
        } else if (transactionClass.equals("DoubleEntryTransaction")) {
            Account creditAccount = accountMap.get(elementMap.get("creditAccount"));
            Account debitAccount = accountMap.get(elementMap.get("debitAccount"));
            BigDecimal amount = new BigDecimal(elementMap.get("amount"));

            boolean creditReconciled = Boolean.parseBoolean(elementMap.get("creditReconciled"));
            boolean debitReconciled = Boolean.parseBoolean(elementMap.get("debitReconciled"));

            transaction = new Transaction();

            transaction.setDate(date);
            transaction.setDateEntered(actDate);
            transaction.setNumber(number);
            transaction.setPayee(payee);

            TransactionEntry entry = new TransactionEntry();
            entry.setMemo(memo);

            entry.setCreditAccount(creditAccount);
            entry.setDebitAccount(debitAccount);

            if (creditAccount.getCurrencyNode().equals(node)) {
                entry.setCreditAmount(amount);
            } else {
                BigDecimal exchangeRate = new BigDecimal(elementMap.get("exchangeRate"));
                entry.setCreditAmount(amount.multiply(exchangeRate));
            }

            if (debitAccount.getCurrencyNode().equals(node)) {
                entry.setDebitAmount(amount.negate());
            } else {
                BigDecimal exchangeRate = new BigDecimal(elementMap.get("exchangeRate"));
                entry.setDebitAmount(amount.multiply(exchangeRate).negate());
            }

            transaction.addTransactionEntry(entry);

            transaction.setReconciled(creditAccount, creditReconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);

            transaction.setReconciled(debitAccount, debitReconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);
        }
        return transaction;
    }

    @SuppressWarnings("unchecked")
    private void parseAccount(final XMLStreamReader reader) {

        assert reader.getAttributeCount() == 2;

        Map<String, Object> elementMap = new HashMap<>();

        /* still at start of the account.  Need to know when end is reached */
        QName parsingElement = reader.getName();

        String accountClass = reader.getAttributeValue(0);
        String accountId = reader.getAttributeValue(1);

        try {
            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String element = reader.getLocalName();

                        if (element.equals("securities")) {
                            logger.info("Parsing account securities");
                            elementMap.put("securities", parseAccountSecurities(reader));
                        } else if (element.equals("amortize")) {
                            logger.info("Parsing amortize object");
                            elementMap.put("amortize", parseAmortizeObject(reader));
                        } else if (element.equals("locked")) {
                            lockMap.put(accountId, Boolean.valueOf(reader.getElementText()));
                        } else {
                            elementMap.put(element, reader.getElementText());
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (reader.getName().equals(parsingElement)) {
                            logger.finest("Found the end of an Account");

                            Account account = generateAccount(accountClass, elementMap);

                            if (account != null) {
                                accountMap.put(accountId, account);

                                // do not put the root account into the parent map
                                if (account.getAccountType() != AccountType.ROOT) {
                                    parentMap.put(account, (String) elementMap.get("parentAccount"));
                                }

                                if (account.getAccountType() != AccountType.ROOT) {
                                    Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                                    engine.addAccount(engine.getRootAccount(), account);

                                    if (account.getAccountType() == AccountType.LIABILITY) {
                                        Map<String, String> amortizeObject = (Map<String, String>) elementMap.get("amortize");

                                        if (amortizeObject != null) {
                                            AOThread t = new AOThread(account, amortizeObject);
                                            workQueue.add(t);
                                        }
                                    }
                                }
                            }
                            return;
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, "Error importing account id: {0}", accountId);
            logger.log(Level.SEVERE, e.toString(), e);
        }
    }

    private static class SecurityThread extends Thread {

        SecurityNode sNode;

        String cNode;

        public SecurityThread(final SecurityNode sNode, final String cNode) {
            assert sNode != null && cNode != null;

            this.sNode = sNode;
            this.cNode = cNode;
        }

        @Override
        public void run() {
            try {
                SecurityNode clone = (SecurityNode) sNode.clone();

                Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                CurrencyNode currencyNode = engine.getCurrency(cNode);

                if (currencyNode != null) {
                    clone.setReportedCurrencyNode(currencyNode);
                    engine.updateCommodity(sNode, clone);
                }
            } catch (CloneNotSupportedException e) {
                logger.log(Level.SEVERE, e.toString(), e);
            }
        }
    }

    private class AOThread extends Thread {

        Map<String, String> elementMap;

        Account liabilityAccount;

        public AOThread(final Account liabilityAccount, final Map<String, String> elementMap) {
            this.liabilityAccount = liabilityAccount;
            this.elementMap = elementMap;
        }

        @Override
        public void run() {

            assert liabilityAccount != null && elementMap != null;

            AmortizeObject ao = new AmortizeObject();

            ao.setBankAccount(accountMap.get(elementMap.get("bankAccount")));
            ao.setDate(decodeDate(elementMap.get("date")));

            if (elementMap.get("daysPerYear") != null) {
                ao.setDaysPerYear(new BigDecimal(elementMap.get("daysPerYear")));
            }

            ao.setFees(new BigDecimal(elementMap.get("fees")));
            ao.setFeesAccount(accountMap.get(elementMap.get("feesAccount")));
            ao.setInterestAccount(accountMap.get(elementMap.get("interestAccount")));
            ao.setInterestPeriods(Integer.parseInt(elementMap.get("compondingPeriods")));
            ao.setLength(Integer.parseInt(elementMap.get("length")));
            ao.setMemo(elementMap.get("memo"));
            ao.setPayee(elementMap.get("payee"));
            ao.setPaymentPeriods(Integer.parseInt(elementMap.get("paymentPeriods")));
            ao.setPrincipal(new BigDecimal(elementMap.get("principal")));
            ao.setRate(new BigDecimal(elementMap.get("rate")));
            ao.setUseDailyRate(Boolean.parseBoolean(elementMap.get("useDailyRate")));

            Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            engine.setAmortizeObject(liabilityAccount, ao);
        }
    }

    private Map<String, String> parseAmortizeObject(final XMLStreamReader reader) {

        Map<String, String> elementMap = new HashMap<>();

        /* still at start of amortize object.  Need to know when end is reached */
        QName parsingElement = reader.getName();

        try {
            parse:
            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String element = reader.getLocalName();
                        elementMap.put(element, reader.getElementText());
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (reader.getName().equals(parsingElement)) {
                            logger.finest("Found the end of the Amortize Object");
                            break parse;
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }

        return elementMap;
    }

    private String[] parseAccountSecurities(final XMLStreamReader reader) {

        assert reader.getAttributeCount() == 2;

        ArrayList<String> securities = new ArrayList<>();

        /* still at start of security array.  Need to know when end is reached */
        QName parsingElement = reader.getName();

        try {
            parse:
            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        securities.add(reader.getElementText());
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (reader.getName().equals(parsingElement)) {
                            logger.finest("Found the end of the Account Securities");
                            break parse;
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }

        return securities.toArray(new String[securities.size()]);
    }

    private Account generateAccount(final String accountClass, final Map<String, Object> elementMap) {

        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        CurrencyNode node = decodeCurrency((String) elementMap.get("commodity"));

        if (accountClass.equals("RootAccount")) {
            engine.setDefaultCurrency(node);
            return engine.getRootAccount();
        }

        Account account = null;

        if (accountClass.equals("BankAccount")) {
            account = new Account(AccountType.BANK, node);
        } else if (accountClass.equals("ExpenseAccount")) {
            account = new Account(AccountType.EXPENSE, node);
        } else if (accountClass.equals("IncomeAccount")) {
            account = new Account(AccountType.INCOME, node);
        } else if (accountClass.equals("InvestmentAccount")) {
            account = new Account(AccountType.INVEST, node);

            String[] securities = (String[]) elementMap.get("securities");

            if (securities != null) {
                for (String s : securities) {
                    SecurityNode sNode = decodeSecurity(s);
                    account.addSecurity(sNode);
                }
            }
        } else if (accountClass.equals("MutualFundAccount")) {
            account = new Account(AccountType.MUTUAL, node);

            String[] securities = (String[]) elementMap.get("securities");

            if (securities != null) {
                for (String s : securities) {
                    SecurityNode sNode = decodeSecurity(s);
                    account.addSecurity(sNode);
                }
            }
        } else if (accountClass.equals("CreditAccount")) {
            account = new Account(AccountType.CREDIT, node);
        } else if (accountClass.equals("CashAccount")) {
            account = new Account(AccountType.CASH, node);
        } else if (accountClass.equals("EquityAccount")) {
            account = new Account(AccountType.EQUITY, node);
        } else if (accountClass.equals("LiabilityAccount")) {
            account = new Account(AccountType.LIABILITY, node);
        } else if (accountClass.equals("AssetAccount")) {
            account = new Account(AccountType.ASSET, node);
        }

        if (account != null) {
            account.setName((String) elementMap.get("name"));
            account.setDescription((String) elementMap.get("description"));
            account.setNotes((String) elementMap.get("notes"));
            account.setVisible(Boolean.parseBoolean((String) elementMap.get("visible")));
            account.setPlaceHolder(Boolean.parseBoolean((String) elementMap.get("placeHolder")));
            account.setAccountNumber((String) elementMap.get("code"));
        }
        return account;
    }

    private void parseCurrencyNode(final XMLStreamReader reader) {
        Map<String, String> elementMap = new HashMap<>();

        /* still at start of the element.  Need to know when end is reached */
        QName parsingElement = reader.getName();

        try {
            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String element = reader.getLocalName();
                        elementMap.put(element, reader.getElementText());
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (reader.getName().equals(parsingElement)) {
                            logger.finest("Found the end of a CurrencyNode");

                            Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                            if (engine.getCurrency(elementMap.get("symbol")) == null) {

                                CurrencyNode node = new CurrencyNode();

                                node.setSymbol(elementMap.get("symbol"));
                                node.setDescription(elementMap.get("description"));
                                node.setPrefix(elementMap.get("prefix"));
                                node.setSuffix(elementMap.get("suffix"));
                                node.setScale(Byte.parseByte(elementMap.get("scale")));

                                engine.addCommodity(node);
                            }
                            return;
                        }
                        break;

                    default:
                        break;
                }

            }
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }

    }

    private void parseCommodityNode(final XMLStreamReader reader) {
        Map<String, String> elementMap = new HashMap<>();

        /* still at start of the element.  Need to know when end is reached */
        QName parsingElement = reader.getName();

        try {
            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String element = reader.getLocalName();
                        elementMap.put(element, reader.getElementText());
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (reader.getName().equals(parsingElement)) {
                            logger.finest("Found the end of a CommodityNode");

                            Commodity node = new Commodity();

                            node.symbol = elementMap.get("symbol");
                            node.description = elementMap.get("description");
                            node.prefix = elementMap.get("prefix");
                            node.suffix = elementMap.get("suffix");
                            node.scale = Byte.parseByte(elementMap.get("scale"));

                            // place in the map
                            commodityMap.put(node.symbol, node);

                            return;
                        }
                        break;

                    default:
                        break;
                }

            }
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }

    }

    private void parseSecurityNode(final XMLStreamReader reader) {
        Map<String, String> elementMap = new HashMap<>();
        List<SecurityHistoryNode> history = null;

        try {
            /* still at start of the element.  Need to know when end is reached */
            QName parsingElement = reader.getName();

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        if (reader.getLocalName().equals("history")) {
                            logger.finest("parse history");
                            history = parseHistoryNodes(reader);
                        } else {
                            String element = reader.getLocalName();
                            elementMap.put(element, reader.getElementText());
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (reader.getName().equals(parsingElement)) {
                            logger.finest("Found the end of a SecurityNode");

                            Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                            if (engine.getSecurity(elementMap.get("symbol")) == null) {

                                SecurityNode node = new SecurityNode(engine.getDefaultCurrency());

                                node.setSymbol(elementMap.get("symbol"));
                                node.setDescription(elementMap.get("description"));
                                node.setPrefix(elementMap.get("prefix"));
                                node.setSuffix(elementMap.get("suffix"));
                                node.setScale(Byte.parseByte(elementMap.get("scale")));
                                node.setQuoteSource(QuoteSource.YAHOO);

                                if (elementMap.get("reportedCurrency") != null) {
                                    SecurityThread thread = new SecurityThread(node, elementMap.get("reportedCurrency"));
                                    workQueue.add(thread);
                                }

                                engine.addCommodity(node);
                            }

                            if (history != null) {
                                SecurityNode node = engine.getSecurity(elementMap.get("symbol"));
                                if (node != null) {
                                    for (SecurityHistoryNode hNode : history) {
                                        engine.addSecurityHistory(node, hNode);
                                    }
                                }
                            }
                            return;
                        }
                        break;

                    default:
                        break;
                }
            }
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, "Exception at element: {0}", reader.getName().toString());
            logger.log(Level.SEVERE, e.toString(), e);
        }
    }

    private List<SecurityHistoryNode> parseHistoryNodes(final XMLStreamReader reader) {
        List<SecurityHistoryNode> list = new ArrayList<>();

        assert reader.getAttributeCount() == 3;

        // number of history nodes to parse
        int count = Integer.parseInt(reader.getAttributeValue(2));

        logger.log(Level.FINEST, "Parsing {0} SecurityHistoryNodes", count);

        Map<String, String> elementMap = new HashMap<>();

        QName parsingElement = null;

        try {
            while (reader.hasNext() && list.size() < count) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        if (reader.getAttributeCount() > 0 && reader.getAttributeValue(0).equals("SecurityHistoryNode")) { // start of hNode

                            parsingElement = reader.getName();
                        } else {
                            String element = reader.getLocalName();
                            elementMap.put(element, reader.getElementText());
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if (reader.getName().equals(parsingElement)) {
                            // build the security history node;
                            SecurityHistoryNode hNode = new SecurityHistoryNode();

                            hNode.setDate(decodeDate(elementMap.get("date")));
                            hNode.setHigh(new BigDecimal(elementMap.get("high")));
                            hNode.setLow(new BigDecimal(elementMap.get("low")));
                            hNode.setPrice(new BigDecimal(elementMap.get("price")));
                            hNode.setVolume(Long.parseLong(elementMap.get("volume")));

                            elementMap.clear();

                            list.add(hNode);
                        }
                        break;
                    default:
                        break;
                }

            }
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }

        return list;
    }

    private SecurityNode decodeSecurity(final String symbol) {
        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        SecurityNode sNode = engine.getSecurity(symbol);

        if (sNode == null) {

            Commodity cNode = commodityMap.get(symbol);

            if (cNode != null) {

                logger.log(Level.INFO, "Converting a commodity into a security: {0}", symbol);

                sNode = new SecurityNode(engine.getDefaultCurrency());

                sNode.setDescription(cNode.description);
                sNode.setPrefix(cNode.prefix);
                sNode.setSuffix(cNode.suffix);
                sNode.setScale(cNode.scale);
                sNode.setSymbol(cNode.symbol);
                sNode.setQuoteSource(QuoteSource.YAHOO);

                engine.addCommodity(sNode);
            } else { // may be a currency... try to create a security

                CurrencyNode currency = decodeCurrency(symbol);

                if (currency != null) {

                    logger.info("Converting a currency into a security");

                    sNode = new SecurityNode();

                    sNode.setDescription(currency.getDescription());
                    sNode.setPrefix(currency.getPrefix());
                    sNode.setSuffix(currency.getSuffix());
                    sNode.setScale(currency.getScale());
                    sNode.setSymbol(currency.getSymbol());

                    sNode.setReportedCurrencyNode(currency);
                    sNode.setQuoteSource(QuoteSource.YAHOO);

                    engine.addCommodity(sNode);
                }
            }
        }

        if (sNode == null) {
            logger.log(Level.SEVERE, "Bad file, security {0} not mapped", symbol);
        }

        return sNode;
    }

    private CurrencyNode decodeCurrency(final String currency) {

        // Check if the currency is already in the cache.
        if (currencyCache.containsKey(currency)) {
            return currencyCache.get(currency);
        }

        String split[] = CURRENCY_DELIMITER_PATTERN.split(currency);
        String symbol = split[0];

        CurrencyNode node = EngineFactory.getEngine(EngineFactory.DEFAULT).getCurrency(symbol);

        if (node == null) {
            logger.log(Level.INFO, "Converting a commodity into a currency: {0}", symbol);

            Commodity cNode = commodityMap.get(symbol);

            if (cNode != null) {
                node = new CurrencyNode();

                node.setDescription(cNode.description);
                node.setPrefix(cNode.prefix);
                node.setSuffix(cNode.suffix);
                node.setScale(cNode.scale);
                node.setSymbol(cNode.symbol);

                EngineFactory.getEngine(EngineFactory.DEFAULT).addCommodity(node);
            } else {
                // Convert security to currency.  For users who figured out how to push the limits of the jGnash 1.x commodity interface
                SecurityNode sNode = EngineFactory.getEngine(EngineFactory.DEFAULT).getSecurity(symbol);
                if (sNode != null) {
                    node = new CurrencyNode();

                    node.setDescription(sNode.getDescription());
                    node.setPrefix(sNode.getPrefix());
                    node.setSuffix(sNode.getSuffix());
                    node.setScale(sNode.getScale());
                    node.setSymbol(sNode.getSymbol());

                    EngineFactory.getEngine(EngineFactory.DEFAULT).addCommodity(node);
                } else {
                    logger.log(Level.SEVERE, "Bad file, currency " + symbol + " not mapped", new Exception());
                }
            }
        }

        // Put the currency in the cache.
        currencyCache.put(currency, node);

        return node;
    }

    private Calendar calendar = Calendar.getInstance(); // reused for date decode

    private Date decodeDate(final String date) {
        if (date != null) {
            int year = Integer.parseInt(date.substring(0, 4));
            int month = Integer.parseInt(date.substring(5, 7));
            int day = Integer.parseInt(date.substring(8, 10));
            int hour = Integer.parseInt(date.substring(11, 13));
            int minute = Integer.parseInt(date.substring(14, 16));
            int second = Integer.parseInt(date.substring(17, 19));
            int milliSecond = Integer.parseInt(date.substring(20));
            calendar.set(year, month - 1, day, hour, minute, second);
            calendar.set(Calendar.MILLISECOND, milliSecond);
            return calendar.getTime();
        }
        return new Date();
    }

    /* Hold commodity data to be converted to currency or security */
    private static class Commodity {

        String symbol;

        String description;

        String suffix;

        String prefix;

        byte scale;
    }
}
