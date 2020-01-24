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
package jgnash.convert.importat.qif;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jgnash.convert.importat.DateFormat;
import jgnash.convert.importat.ImportUtils;
import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ReconcileManager;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionFactory;

/**
 * QifImport takes a couple of simple steps to prevent importing a duplicate account. Other than that, duplicate
 * transactions will be imported. At this time, it would require a lot of extra and normally unused data to be stored
 * with each transaction and account to prevent duplication. This import utility is ideal for importing an existing data
 * set, but not for importing monthly bank statements. A more specialized import utility may be useful/required for more
 * advanced in
 *
 * @author Craig Cavanaugh
 */
public class QifImport {

    /**
     * Default for a QIF import
     */
    private static final String FITID = "qif";

    private QifParser parser;

    private final Engine engine;

    private final HashMap<String, Account> expenseMap = new HashMap<>();

    private final HashMap<String, Account> incomeMap = new HashMap<>();

    private final HashMap<String, Account> accountMap = new HashMap<>();

    private boolean partialImport = false;

    private static final Logger logger = Logger.getLogger("qifimport");

    public QifImport() {
        engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
    }

    public QifParser getParser() {
        return parser;
    }

    public void doFullParse(final File file, final DateFormat dateFormat) throws IOException {
        if (file != null) {
            parser = new QifParser(dateFormat);
            parser.parseFullFile(file);
            logger.info("*** Parsing Complete ***");
        }
    }

    public void doFullImport() {
        if (parser != null) {

            importCategories();
            importAccounts();

            logger.info("*** Importing Complete ***");
        }
    }

    public boolean doPartialParse(final File file) {
        if (file != null) {
            partialImport = true;

            parser = new QifParser(DateFormat.US);
            return parser.parsePartialFile(file);
        }

        return false;
    }

    public void dumpStats() {
        if (parser != null) {
            parser.dumpStats();
        }
    }

    private void importCategories() {
        logger.info("*** Importing Categories ***");

        loadCategoryMap(engine.getExpenseAccountList(), expenseMap);
        loadCategoryMap(engine.getIncomeAccountList(), incomeMap);
        reduceCategories();
        addCategories();
    }

    private void importAccounts() {
        loadAccountMap();
        addAccounts();
    }

    private static void loadCategoryMap(final List<Account> list, final Map<String, Account> map) {
        if (list != null) { // protect against a failed load on a new account
            for (Account aList : list) {
                loadCategoryMap(aList, map);
            }
        }
    }

    private static void loadCategoryMap(final Account acc, final Map<String, Account> map) {
        String pathName = acc.getPathName();
        int index = pathName.indexOf(':');
        if (index != -1) {
            map.put(pathName.substring(index + 1), acc);
        }
    }

    /**
     * Returns a list of all accounts excluding the rootAccount and IncomeAccounts and ExpenseAccounts
     *
     * @return List of bank accounts
     */
    private List<Account> getBankAccountList() {

        final List<Account> list = engine.getAccountList();

        return list.stream().filter(a -> a.getAccountType() == AccountType.BANK
                || a.getAccountType() == AccountType.CASH).collect(Collectors.toList());
    }

    private void loadAccountMap() {
        List<Account> list = getBankAccountList();
        list.forEach(this::loadAccountMap);
    }

    private void loadAccountMap(final Account acc) {
        String pathName = acc.getPathName();
        int index = pathName.indexOf(':');
        if (index != -1) {
            accountMap.put(pathName.substring(index + 1), acc);
        }
    }

    /**
     * All of the accounts must be added before the transactions are added because the category (account) must exist
     * first
     */
    private void addAccounts() {

        logger.info("*** Importing Accounts ***");

        List<QifAccount> list = parser.accountList;
        // add all of the accounts first
        for (QifAccount qAcc : list) {
            Account acc;
            if (!accountMap.containsKey(qAcc.name)) { // add the account if it does not exist
                acc = generateAccount(qAcc);
                if (acc != null) {
                    engine.addAccount(engine.getRootAccount(), acc);
                    loadAccountMap(acc);
                }
            }
        }

        logger.info("*** Importing Transactions ***");

        // go back and add the transactions;
        for (QifAccount qAcc : list) {
            Account acc = accountMap.get(qAcc.name);

            // try and match the closest
            if (acc == null) {
                acc = engine.getAccountByName(qAcc.name);
            }

            // TODO Correct import of investment transactions
            if (acc != null && acc.getAccountType() != AccountType.INVEST) {
                addTransactions(qAcc, acc);
            } else {
                if (acc != null) {
                    logger.severe("Investment transactions not fully supported");
                } else {
                    logger.log(Level.SEVERE, "Lost the account: {0}", qAcc.name);
                }
            }
        }
    }

    private void addTransactions(final QifAccount qAcc, final Account acc) {
        if (qAcc.getTransactions().isEmpty()) {
            return;
        }
        List<QifTransaction> list = qAcc.getTransactions();
        for (QifTransaction aList : list) {
            Transaction tran;

            /*if (acc.getAccountType().equals(AccountType.INVEST)) {
            //tran = generateInvestmentTransaction(aList, acc);
            tran = generateTransaction(aList, acc);
            } else {
            tran = generateTransaction(aList, acc);
            }*/

            tran = generateTransaction(aList, acc);

            if (tran != null) {
                if (partialImport) {
                    tran.setFitid(FITID);   // importing a bank statement, flag as imported
                }
                engine.addTransaction(tran);
            } else {
                logger.warning("Null Transaction!");
            }
        }
    }

    private void addCategories() {
        List<QifCategory> list = parser.categories;
        Map<String, Account> map;
        for (QifCategory cat : list) {
            Account acc = generateAccount(cat);
            if (acc.getAccountType() == AccountType.EXPENSE) {
                map = expenseMap;
            } else {
                map = incomeMap;
            }
            Account parent = findBestParent(cat, map);
            engine.addAccount(parent, acc);
            loadCategoryMap(acc, map);
        }
    }

    /**
     * Returns the Account of the best possible parent account for the supplied QifCategory
     *
     * @param cat imported QifCategory to match
     * @param map cached account map
     * @return best Account match
     */
    private Account findBestParent(final QifCategory cat, final Map<String, Account> map) {
        int i = cat.name.lastIndexOf(':');
        if (i != -1) {
            String pathName = cat.name.substring(0, i);
            while (true) {
                if (map.containsKey(pathName)) {
                    return map.get(pathName);
                }
                int j = pathName.lastIndexOf(':');
                if (j != -1) {
                    pathName = pathName.substring(0, j);
                } else {
                    break;
                }
            }
        }
        Account parent;

        if (cat.type.equals("E")) {
            parent = ImportUtils.getRootExpenseAccount();
        } else {
            parent = ImportUtils.getRootIncomeAccount();
        }

        if (parent != null) {
            return parent;
        }

        return engine.getRootAccount();
    }

    /**
     * Returns the best matching account
     *
     * @param category QIF category
     * @return Best matching account
     */
    private Account findBestAccount(final String category) {
        Account acc = null;

        // nulls can happen and don't search on an empty category
        if (category != null && !category.isEmpty()) {
            String name = category;

            if (isAccount(name)) { // account
                name = category.substring(1, category.length() - 1);
                logger.log(Level.FINEST, "Looking for bank account: {0}", name);
                acc = accountMap.get(name);
            }

            if (acc == null) { // income or expense account
                // strip any category tags
                name = QifUtils.stripCategoryTags(name);

                acc = expenseMap.get(name);
                if (acc == null) {
                    acc = incomeMap.get(name);
                }
            }

            if (acc == null) {
                logger.log(Level.WARNING, "No account match for: {0}", name);
            }
        }
        return acc;
    }

    /**
     * Determines if the supplied String represents a QIF account
     *
     * @param category category string to validate
     * @return true if this is suppose to be an account
     */
    private static boolean isAccount(final String category) {
        return category.startsWith("[") && category.endsWith("]");
    }

    /*
     * Removes duplicate categories from a supplied list
     */
    private void reduceCategories() {
        QifCategory cat;
        String path;
        List<QifCategory> list = parser.categories;
        Iterator<QifCategory> i = list.iterator();
        while (i.hasNext()) {
            cat = i.next();
            path = cat.name;
            if (cat.type.equals("E") && expenseMap.containsKey(path)) {
                i.remove();
            } else if (cat.type.equals("I") && incomeMap.containsKey(path)) {
                i.remove();
            }
        }
    }

    /*
     * Creates and returns an Account of the correct type given a QifCategory
     */
    private Account generateAccount(final QifCategory cat) {
        Account account;
        CurrencyNode defaultCurrency = engine.getDefaultCurrency();
        if (cat.type.equals("E")) {
            account = new Account(AccountType.EXPENSE, defaultCurrency);
            // account.setTaxRelated(cat.taxRelated);
            // account.setTaxSchedule(cat.taxSchedule);
        } else {
            account = new Account(AccountType.INCOME, defaultCurrency);
            // account.setTaxRelated(cat.taxRelated);
            // account.setTaxSchedule(cat.taxSchedule);
        }

        // trim off the leading parent account
        int index = cat.name.lastIndexOf(':');

        if (index != -1) {
            account.setName(cat.name.substring(index + 1));
        } else {
            account.setName(cat.name);
        }

        account.setDescription(cat.description);
        return account;
    }

    private Account generateAccount(final QifAccount acc) {
        Account account;
        CurrencyNode defaultCurrency = engine.getDefaultCurrency();

        switch (acc.type) {
            case "Bank":
                account = new Account(AccountType.BANK, defaultCurrency);
                break;
            case "CCard":
                account = new Account(AccountType.CREDIT, defaultCurrency);
                break;
            case "Cash":
                account = new Account(AccountType.CASH, defaultCurrency);
                break;
            case "Invst":
            case "Port":
                account = new Account(AccountType.INVEST, defaultCurrency);
                break;
            case "Oth A":
                account = new Account(AccountType.ASSET, defaultCurrency);
                break;
            case "Oth L":
                account = new Account(AccountType.LIABILITY, defaultCurrency);
                break;
            default:
                logger.log(Level.SEVERE, "Could not generate an account for:\n{0}", acc.toString());
                return null;
        }
        account.setName(acc.name);
        account.setDescription(acc.description);
        return account;
    }

    /**
     * Generates a transaction
     * <p>
     * Notes: If a QifTransaction does not specify an account, then assume it is a single
     * entry transaction for the supplied Account. The transaction most likely came from a online banking source.
     *
     * @param qTran Qif transaction to generate Transaction for
     * @param acc base Account
     * @return new Transaction
     */
    private Transaction generateTransaction(final QifTransaction qTran, final Account acc) {
        Objects.requireNonNull(acc);

        boolean reconciled = "x".equalsIgnoreCase(qTran.status);

        Transaction tran;
        Account cAcc;
        if (qTran.getAccount() != null) {
            cAcc = qTran.getAccount();
        } else {
            cAcc = findBestAccount(qTran.category);
        }

        if (qTran.hasSplits()) {
            tran = new Transaction();
            // create a double entry transaction with splits
            List<QifSplitTransaction> splits = qTran.splits;

            for (QifSplitTransaction splitTransaction : splits) {
                TransactionEntry split = generateSplitTransaction(splitTransaction, acc);

                Objects.requireNonNull(split);  // should not be null, throw an exception
                tran.addTransactionEntry(split);
            }

            ReconcileManager.reconcileTransaction(acc, tran, reconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);
        } else if (acc == cAcc && !qTran.hasSplits() || cAcc == null) {
            // create single entry transaction without splits
            tran = TransactionFactory.generateSingleEntryTransaction(acc, qTran.getAmount(), qTran.getDatePosted(), qTran.getMemo(),
                    qTran.getPayee(), qTran.getCheckNumber());

            ReconcileManager.reconcileTransaction(acc, tran, reconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);
        } else if (!qTran.hasSplits()) { // && cAcc != null
            // create a double entry transaction without splits
            if (qTran.getAmount().signum() == -1) {
                tran = TransactionFactory.generateDoubleEntryTransaction(cAcc, acc, qTran.getAmount(), qTran.getDatePosted(),
                        qTran.getMemo(), qTran.getPayee(), qTran.getCheckNumber());
            } else {
                tran = TransactionFactory.generateDoubleEntryTransaction(acc, cAcc, qTran.getAmount(), qTran.getDatePosted(),
                        qTran.getMemo(), qTran.getPayee(), qTran.getCheckNumber());
            }

            ReconcileManager.reconcileTransaction(cAcc, tran, reconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);
            ReconcileManager.reconcileTransaction(acc, tran, reconciled ? ReconciledState.RECONCILED : ReconciledState.NOT_RECONCILED);

            if (isAccount(qTran.category)) {
                removeMirrorTransaction(qTran, acc); // remove the mirror transaction
            }
        } else {
            // could not find the account this transaction belongs to
            logger.log(Level.WARNING, "Could not create following transaction:" + "\n{0}", qTran.toString());
            return null;
        }
        tran.setDate(qTran.getDatePosted());
        tran.setPayee(qTran.getPayee());
        tran.setNumber(qTran.getCheckNumber());

        return tran;
    }

    private Account unassignedExpense = null;

    private Account unassignedIncome = null;

    /**
     * Generates a Transaction given a QifSplitTransaction
     *
     * @param qTran split qif transaction to convert
     * @param acc base Account
     * @return generated TransactionEntry
     */
    private TransactionEntry generateSplitTransaction(final QifSplitTransaction qTran, final Account acc) {
        TransactionEntry tran = new TransactionEntry();

        Account account = findBestAccount(qTran.category);

        /* Verify that the splits category is not assigned to the parent account.  This is
         * allowed within Quicken, but violates double entry and is not allowed in jGnash.
         * Wipe the resulting account and default to the unassigned accounts to maintain
         * integrity.
         */
        if (account == acc) {
            logger.warning("Detected an invalid split transactions entry, correcting problem");
            account = null;
        }

        /* If a valid account is found at this point, then it should have a duplicate
         * entry in another account that needs to be removed
         */
        if (account != null && isAccount(qTran.category)) {
            removeMirrorSplitTransaction(qTran);
        }

        if (account == null) { // unassigned split transaction.... fix it with a default
            if (qTran.amount.signum() == -1) { // need an expense account
                if (unassignedExpense == null) {
                    unassignedExpense = new Account(AccountType.EXPENSE, engine.getDefaultCurrency());
                    unassignedExpense.setName("** QIF Import - Unassigned Expense Account");
                    unassignedExpense.setDescription("Fix transactions and delete this account");
                    engine.addAccount(engine.getRootAccount(), unassignedExpense);
                    logger.info("Created an account for unassigned expense account");
                }
                account = unassignedExpense;
            } else {
                if (unassignedIncome == null) {
                    unassignedIncome = new Account(AccountType.INCOME, engine.getDefaultCurrency());
                    unassignedIncome.setName("** QIF Import - Unassigned Income Account");
                    unassignedIncome.setDescription("Fix transactions and delete this account");
                    engine.addAccount(engine.getRootAccount(), unassignedIncome);
                    logger.info("Created an account for unassigned income account");
                }
                account = unassignedIncome;
            }
        }

        if (qTran.amount.signum() == -1) {
            tran.setDebitAccount(acc);
            tran.setCreditAccount(account);
        } else {
            tran.setDebitAccount(account);
            tran.setCreditAccount(acc);
        }
        tran.setAmount(qTran.amount.abs());
        tran.setMemo(qTran.memo);

        return tran;
    }

    /* Cannot check against check number and payee because Quicken allows for the
     * different payees at each side of the transaction and does not include the
     * check number on both sides.
     *
     * The date, amount, and account/category is checked to determine if the match is valid
     */
    private void removeMirrorTransaction(final QifTransaction qTran, final Account acc) {
        String name = qTran.category.substring(1, qTran.category.length() - 1);
        List<QifAccount> list = parser.accountList;

        for (QifAccount qAcc : list) {
            if (qAcc.name.equals(name)) {
                List<QifTransaction> items = qAcc.getTransactions();
                Iterator<QifTransaction> i = items.iterator();
                QifTransaction tran;
                while (i.hasNext()) {
                    tran = i.next();
                    if (tran.getAmount().compareTo(qTran.getAmount().negate()) == 0 && tran.getDatePosted().equals(qTran.getDatePosted()) && tran.category.contains(acc.getName())) {
                        i.remove();
                        logger.finest("Removed mirror transaction");

                        return;
                    }
                }
            }
        }
    }

    private void removeMirrorSplitTransaction(final QifSplitTransaction qTran) {
        String name = qTran.category.substring(1, qTran.category.length() - 1);
        logger.log(Level.FINE, "Category name is: {0}", name);
        List<QifAccount> list = parser.accountList;

        for (QifAccount qAcc : list) {
            if (qAcc.name.equals(name)) {
                List<QifTransaction> items = qAcc.getTransactions();
                Iterator<QifTransaction> i = items.iterator();
                QifTransaction tran;
                while (i.hasNext()) {
                    tran = i.next();
                    if (tran != null) {
                        if (tran.getAmount().compareTo(qTran.amount.negate()) == 0 && tran.getMemo().equals(qTran.memo)) {
                            i.remove();
                            logger.finest("Removed mirror split transaction");
                            return;
                        }
                    } else { // should not occur anymore
                        logger.log(Level.SEVERE, "There was a null QifTransaction in QifAccount: \n{0}", qAcc.toString());
                    }
                }
            }
        }

        // could be a split into a bank account... look a level higher
        // TODO add check against the base account... should point at each other..
        // qTran's category same as tran category?
        for (QifAccount qAcc : list) {
            if (qAcc.name.equals(name)) {
                List<QifTransaction> items = qAcc.getTransactions();
                Iterator<QifTransaction> i = items.iterator();
                QifTransaction tran;
                while (i.hasNext()) {
                    tran = i.next();
                    // is the match an account and the opposite value and does not have any splits?
                    // is this a valid method?                                                           
                    if (tran.getAmount().compareTo(qTran.amount.negate()) == 0 && isAccount(tran.category) && !tran.hasSplits()) {
                        logger.log(Level.FINE, "Found a match:\n{0}", tran.toString());
                        i.remove();
                        return;
                    }
                }
            }
        }

        logger.log(Level.WARNING, "Did not find matching mirror:" + "\n{0}", qTran.toString());
    }
}
