/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
package jgnash.engine;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetGoal;
import jgnash.engine.budget.BudgetPeriod;
import jgnash.engine.budget.BudgetPeriodDescriptor;
import jgnash.engine.budget.BudgetPeriodDescriptorFactory;
import jgnash.engine.dao.AccountDAO;
import jgnash.engine.dao.BudgetDAO;
import jgnash.engine.dao.CommodityDAO;
import jgnash.engine.dao.ConfigDAO;
import jgnash.engine.dao.EngineDAO;
import jgnash.engine.dao.RecurringDAO;
import jgnash.engine.dao.TransactionDAO;
import jgnash.engine.dao.TrashDAO;
import jgnash.engine.recurring.PendingReminder;
import jgnash.engine.recurring.RecurringIterator;
import jgnash.engine.recurring.Reminder;
import jgnash.message.ChannelEvent;
import jgnash.message.Message;
import jgnash.message.MessageBus;
import jgnash.message.MessageChannel;
import jgnash.message.MessageProperty;
import jgnash.util.DateUtils;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.Resource;

/**
 * Engine class
 * <p/>
 * When objects are removed, they are wrapped in a TrashObject so they may still be referenced for messaging and cleanup
 * operations. After a predefined period of time, they are permanently removed.
 * 
 * @author Craig Cavanaugh
 */
public class Engine {

    /**
     * Current version for the file format
     */
    public static final float CURRENT_VERSION = 2.3f;

    private final Resource rb = Resource.get();

    private static final Logger logger = Logger.getLogger(Engine.class.getName());

    /**
     * All engine instances will share the same message bus
     */
    private MessageBus messageBus = null;

    /**
     * Cached for performance
     */
    private Config config;

    /**
     * Cached for performance
     */
    private RootAccount rootAccount;

    private ExchangeRateDAO exchangeRateDAO;

    private final ReentrantReadWriteLock accountLock = new ReentrantReadWriteLock(true);

    private final ReentrantReadWriteLock budgetLock = new ReentrantReadWriteLock(true);

    private final ReentrantReadWriteLock commodityLock = new ReentrantReadWriteLock(true);

    private final ReentrantReadWriteLock configLock = new ReentrantReadWriteLock(true);

    private final ReentrantReadWriteLock engineLock = new ReentrantReadWriteLock(true);

    private EngineDAO eDAO;

    /**
     * Cached for performance
     */
    private String accountSeparator = null;

    private ScheduledExecutorService trashExecutor;

    private final static long MAXIMUM_TRASH_AGE = 5 * 60 * 1000; // 5 minutes

    /**
     * Named identifier for this engine instance
     */
    private final String name;

    /**
     * Unique identifier for this engine instance
     */
    private final String uuid = UUIDUtil.getUID();

    static {
        logger.setLevel(Level.ALL);
    }

    public Engine(final EngineDAO eDAO, final String name) {

        if (name == null) {
            throw new IllegalArgumentException("The engine name may not be null");
        }

        if (eDAO == null) {
            throw new IllegalArgumentException("The engineDAO may not be null");
        }

        this.eDAO = eDAO;
        this.name = name;

        messageBus = MessageBus.getInstance(name);

        initialize();

        checkAndCorrect();

        trashExecutor = Executors.newSingleThreadScheduledExecutor(new DefaultDaemonThreadFactory());

        // run trash cleanup every 5 minutes 1 minute after startup
        trashExecutor.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {

                emptyTrash();
            }
        }, 1, 5, TimeUnit.MINUTES);
    }

    /**
     * Creates a RootAccount and default currency only if necessary
     */
    private void initialize() {

        WriteLock commodityWriteLock = commodityLock.writeLock();
        WriteLock accountWriteLock = accountLock.writeLock();

        commodityWriteLock.lock();
        accountWriteLock.lock();

        try {

            // ask the Config object to perform and needed configuration
            getConfig().initialize();

            // build the exchange rate storage object
            exchangeRateDAO = new ExchangeRateDAO(getCommodityDAO());

            // assign the exchange rate store to the currencies
            for (CurrencyNode node : getCurrencies()) {
                node.setExchangeRateDAO(exchangeRateDAO);
            }

            // obtain or establish the root account
            RootAccount root = getRootAccount();

            if (root == null) {
                CurrencyNode node = getDefaultCurrency();

                if (node == null) {
                    node = DefaultCurrencies.buildNode(Locale.getDefault());
                    node.setExchangeRateDAO(exchangeRateDAO);

                    addCommodity(node); // force the node to persisted
                }

                root = new RootAccount(node);
                root.setName(rb.getString("Name.Root"));
                root.setDescription(rb.getString("Name.Root"));

                logInfo("Creating RootAccount");

                if (!getAccountDAO().addRootAccount(root)) {
                    logSevere("Was not able to add the root account");
                    throw new RuntimeException("Was not able to add the root account");
                }

                if (getDefaultCurrency() == null) {
                    setDefaultCurrency(node);
                }
            }

        } finally {
            accountWriteLock.unlock();
            commodityWriteLock.unlock();
        }

        logInfo("Engine initialization is complete");
    }

    /**
     * Corrects minor issues with a database that may occur because of prior bugs or file format upgrades
     */
    @SuppressWarnings("ConstantConditions")
    private void checkAndCorrect() {

        WriteLock commodityWriteLock = commodityLock.writeLock();
        WriteLock accountWriteLock = accountLock.writeLock();
        WriteLock configWriteLock = configLock.writeLock();

        commodityWriteLock.lock();
        accountWriteLock.lock();
        configWriteLock.lock();

        try {

            /* Check for detached accounts */
            if (getConfig().getFileVersion() < 2.02f) {
                for (Account account : getAccountDAO().getAccountList()) {
                    if (account.getParent() == null && !account.instanceOf(AccountType.ROOT)) {
                        account.setParent(getRootAccount());

                        getAccountDAO().updateAccount(account);
                        getAccountDAO().updateAccount(getRootAccount());
                        logInfo("Fixing a detached account: " + account.getName());
                    }
                }
            }

            /* Check for null account number strings */
            if (getConfig().getFileVersion() < 2.01f) {
                logInfo("Checking for null account numbers");
                for (Account account : getAccountDAO().getAccountList()) {
                    if (account.getAccountNumber() == null) {
                        account.setAccountNumber("");
                        getAccountDAO().updateAccount(account);
                        logInfo("Fixed null account number");
                    }
                }
            }

            if (getConfig().getFileVersion() < 2.02f) {
                logInfo("Checking for a recursive account structure");
                for (Account account : getAccountDAO().getAccountList()) {
                    if (account.equals(account.getParent())) {
                        logWarning("Correcting recursive account structure:" + account.getName());
                        account.setParent(getRootAccount());

                        getAccountDAO().updateAccount(account);
                        getAccountDAO().updateAccount(getRootAccount());
                    }
                }
            }

            if (getConfig().getFileVersion() < 2.03f) {
                clearObsoleteExchangeRates();
            }

            // check for multiple root accounts
            if (getConfig().getFileVersion() < 2.04f) {
                List<RootAccount> roots = new ArrayList<>();

                for (StoredObject o : getStoredObjects()) {
                    if (o instanceof RootAccount) {
                        roots.add((RootAccount) o);
                    }
                }

                if (roots.size() > 1) {
                    logger.warning("Removing extra root accounts");

                    RootAccount root = roots.get(0);

                    // use the root at 0 as the default
                    for (int i = 1; i < roots.size(); i++) {
                        RootAccount extraRoot = roots.get(i);

                        for (Account child : extraRoot.getChildren()) {
                            if (!moveAccount(child, root)) {
                                logWarning(rb.getString("Message.Error.MoveAccount"));
                            }
                        }

                        moveObjectToTrash(extraRoot);
                    }
                }
            }

            // cleanup currencies
            if (getConfig().getFileVersion() < 2.1f) {
                removeDuplicateCurrencies();
            }

            // force income and expense account to only be display in a budget by default
            if (getConfig().getFileVersion() < 2.2f) {
                for (Account account : getAccountList()) {
                    if (!account.memberOf(AccountGroup.INCOME) && !account.memberOf(AccountGroup.EXPENSE)) {
                        account.setExcludedFromBudget(true);
                        getAccountDAO().updateAccount(account);
                    }
                }
            }

            // migrate amortization object to new storage format
            if (getConfig().getFileVersion() < 2.3f) {
                migrateAmortizeObjects();
            }
            
            // check for improperly set default currency
            if (getDefaultCurrency() == null) {
                setDefaultCurrency(this.getRootAccount().getCurrencyNode());
                logger.warning("Forcing default currency");
            }
            
            // if the file version is not current, then update it
            if (getConfig().getFileVersion() != CURRENT_VERSION) {
                getConfig().setFileVersion(CURRENT_VERSION);
                getConfigDAO().commit(getConfig());
            }
        } finally {
            configWriteLock.unlock();
            accountWriteLock.unlock();
            commodityWriteLock.unlock();
        }
    }

    /**
     * Removes any duplicate currencies by symbol and fixes up references to them
     */
    private void removeDuplicateCurrencies() {

        Map<String, CurrencyNode> keepMap = new HashMap<>();
        List<CurrencyNode> discard = new ArrayList<>();

        CurrencyNode defaultCurrency = getDefaultCurrency();

        // pre-load the default so the root does not have to be changed
        keepMap.put(defaultCurrency.getSymbol(), defaultCurrency);

        for (CurrencyNode node : getCurrencies()) {
            if (!keepMap.containsKey(node.getSymbol())) {
                keepMap.put(node.getSymbol(), node);
            } else if (node != defaultCurrency) {
                discard.add(node);
            }
        }

        for (CurrencyNode node : discard) {
            for (Account account : getAccountList()) {
                if (account.getCurrencyNode() == node) {
                    account.setCurrencyNode(keepMap.get(node.getSymbol()));
                    getAccountDAO().updateAccount(account);
                }
            }

            for (SecurityNode sNode : getSecurities()) {
                if (sNode.getReportedCurrencyNode() == node) {
                    sNode.setReportedCurrencyNode(keepMap.get(node.getSymbol()));
                    getCommodityDAO().updateCommodityNode(sNode);
                }
            }

            removeCommodity(node);
        }
    }

    private void clearObsoleteExchangeRates() {

        for (ExchangeRate rate : getCommodityDAO().getExchangeRates()) {
            if (getBaseCurrencies(rate.getRateId()) == null) {
                removeExchangeRate(rate);
            }
        }
    }

    private void removeExchangeRate(final ExchangeRate rate) {
        WriteLock commodityWriteLock = commodityLock.writeLock();

        commodityWriteLock.lock();

        try {
            for (ExchangeRateHistoryNode node : rate.getHistory()) {
                removeExchangeRateHistory(rate, node);
            }
            moveObjectToTrash(rate);
        } finally {
            commodityWriteLock.unlock();
        }
    }

    void shutdown() {

        trashExecutor.shutdownNow();
        eDAO.shutdown();
    }

    public String getName() {

        return name;
    }

    private AccountDAO getAccountDAO() {

        return eDAO.getAccountDAO();
    }

    private BudgetDAO getBudgetDAO() {

        return eDAO.getBudgetDAO();
    }

    private CommodityDAO getCommodityDAO() {

        return eDAO.getCommodityDAO();
    }

    private ConfigDAO getConfigDAO() {

        return eDAO.getConfigDAO();
    }

    private RecurringDAO getReminderDAO() {

        return eDAO.getRecurringDAO();
    }

    private TransactionDAO getTransactionDAO() {

        return eDAO.getTransactionDAO();
    }

    private TrashDAO getTrashDAO() {

        return eDAO.getTrashDAO();
    }

    private boolean moveObjectToTrash(final StoredObject object) {
        boolean result = false;

        WriteLock engineWriteLock = engineLock.writeLock();
        engineWriteLock.lock();

        try {

            TrashObject trash = new TrashObject(object);
            getTrashDAO().add(trash);

            result = true;
        } finally {
            engineWriteLock.unlock();
        }

        return result;
    }

    /**
     * Empty the trash if any objects are older than the defined time
     */
    private void emptyTrash() {

        WriteLock engineWriteLock = engineLock.writeLock();

        engineWriteLock.lock();

        try {

            logger.info("Checking for trash");

            List<TrashObject> trash = getTrashDAO().getTrashObjects();

            if (trash.isEmpty()) {
                logger.info("No trash was found");
            }

            long now = new Date().getTime();

            for (TrashObject o : trash) {
                if (now - o.getDate().getTime() >= MAXIMUM_TRASH_AGE) {
                    getTrashDAO().remove(o);
                }
            }
        } finally {
            engineWriteLock.unlock();
        }
    }

    public boolean addReminder(final Reminder reminder) {

        assert reminder.getUuid() != null;

        boolean result = getReminderDAO().addReminder(reminder);

        Message message;
        if (result) {
            message = new Message(MessageChannel.REMINDER, ChannelEvent.REMINDER_ADD, this);
        } else {
            message = new Message(MessageChannel.REMINDER, ChannelEvent.REMINDER_ADD_FAILED, this);
        }

        message.setObject(MessageProperty.REMINDER, reminder);
        messageBus.fireEvent(message);

        return result;
    }

    public boolean removeReminder(final Reminder reminder) {
        boolean result = false;

        if (moveObjectToTrash(reminder)) {
            Message message = new Message(MessageChannel.REMINDER, ChannelEvent.REMINDER_REMOVE, this);

            message.setObject(MessageProperty.REMINDER, reminder);
            messageBus.fireEvent(message);

            result = true;
        }

        return result;
    }

    /**
     * Returns a list of reminders
     * 
     * @return List of reminders
     */
    public List<Reminder> getReminders() {

        return getReminderDAO().getReminderList();
    }

    public List<PendingReminder> getPendingReminders() {

        final ArrayList<PendingReminder> pendingList = new ArrayList<>();
        final List<Reminder> list = getReminders();
        final Calendar c = Calendar.getInstance();
        final Date now = new Date(); // today's date

        for (Reminder r : list) {
            if (r.isEnabled()) {
                final RecurringIterator ri = r.getIterator();
                Date next = ri.next();

                while (next != null) {
                    c.setTime(next);
                    c.add(Calendar.DATE, r.getDaysAdvance() * -1); // handle days in advance

                    if (DateUtils.before(c.getTime(), now)) { // need to fire this reminder
                        pendingList.add(new PendingReminder(r, DateUtils.trimDate(next)));
                        next = ri.next();
                    } else {
                        next = null;
                    }
                }
            }
        }

        return pendingList;
    }

    public StoredObject getStoredObjectByUuid(final String id) {

        return eDAO.getObjectByUuid(id);
    }

    /**
     * Returns a <code>Collection</code> of all <code>StoredObjects</code> in a consistent order.
     * <code>StoredObjects</code> marked for removal and <code>TrashObjects</code> are filtered from the collection.
     * 
     * @return <code>Collection</code> of <code>StoredObjects</code>
     * @see Collection
     * @see StoredObjectComparator
     */
    public Collection<StoredObject> getStoredObjects() {

        ReadLock engineReadLock = engineLock.readLock();

        engineReadLock.lock();

        try {

            List<StoredObject> objects = eDAO.getStoredObjects();

            for (Iterator<StoredObject> i = objects.iterator(); i.hasNext();) {
                StoredObject o = i.next();

                if (o instanceof TrashObject || o.isMarkedForRemoval()) {
                    i.remove();
                }
            }

            Collections.sort(objects, new StoredObjectComparator());

            return objects;
        } finally {
            engineReadLock.unlock();
        }
    }

    /**
     * Validate a CommodityNode for correctness
     * 
     * @param node CommodityNode to validate
     * @return true if valid
     */
    private boolean isCommodityNodeValid(final CommodityNode node) {

        boolean result = true;

        if (node.getUuid() == null) {
            result = false;
            logSevere("Commodity uuid was not valid");
        }

        if (node.getSymbol() == null || node.getSymbol().length() == 0) {
            result = false;
            logSevere("Commodity symbol was not valid");
        }

        if (node.getScale() < 0) {
            result = false;
            logSevere("Commodity " + node.toString() + " had a scale less than zero");
        }

        if (node instanceof SecurityNode && ((SecurityNode) node).getReportedCurrencyNode() == null) {
            result = false;
            logSevere("Commodity " + node.toString() + " was not assigned a currency");
        }

        // ensure the UUID being used is unique
        if (eDAO.getObjectByUuid(node.getUuid()) != null) {
            result = false;
            logSevere("Commodity " + node.toString() + " was not unique");
        }

        return result;
    }

    /**
     * Adds a new CommodityNode to the data set.
     * <p/>
     * Checks and prevents the addition of a duplicate CommodityNode. CurrencyNodes with the same symbol but difference
     * locale are allowed.
     * 
     * @param node new CommodityNode to add
     * @return <code>true</code> if the add it successful
     */
    public boolean addCommodity(final CommodityNode node) {
        WriteLock commodityWriteLock = commodityLock.writeLock();

        commodityWriteLock.lock();

        try {

            boolean status = isCommodityNodeValid(node);

            if (status) {
                if (node instanceof CurrencyNode) {

                    ((CurrencyNode) node).setExchangeRateDAO(exchangeRateDAO);

                    if (getCurrency(node.getSymbol()) != null) {
                        logger.log(Level.INFO, "Prevented addition of a duplicate CurrencyNode: {0}", node.getSymbol());
                        status = false;
                    }
                } else {
                    if (getSecurity(node.getSymbol()) != null) {
                        logger.log(Level.INFO, "Prevented addition of a duplicate SecurityNode: {0}", node.getSymbol());
                        status = false;
                    }
                }
            }

            if (status) {
                status = getCommodityDAO().addCommodity(node);
                logger.log(Level.FINE, "Adding: {0}", node.toString());
            }

            Message message;
            if (status) {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.CURRENCY_ADD, this);
            } else {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.CURRENCY_ADD_FAILED, this);
            }

            message.setObject(MessageProperty.COMMODITY, node);
            messageBus.fireEvent(message);

            return status;
        } finally {
            commodityWriteLock.unlock();
        }
    }

    /**
     * Add a SecurityHistoryNode node to a SecurityNode
     * 
     * @param node SecurityNode to add to
     * @param hNode SecurityHistoryNode to add
     * @return true if successful
     */
    public boolean addSecurityHistory(final SecurityNode node, final SecurityHistoryNode hNode) {

        WriteLock commodityWriteLock = commodityLock.writeLock();

        commodityWriteLock.lock();

        try {

            boolean status;

            // must delete any equivalent history nodes by identity
            List<SecurityHistoryNode> list = node.getHistoryNodes();

            for (SecurityHistoryNode h : list) {
                if (h.equals(hNode)) {
                    if (!removeSecurityHistory(node, h)) {
                        logSevere(MessageFormat.format(rb.getString("Message.Error.HistRemoval"), hNode.getDate(), node.getSymbol()));
                    }
                    break;
                }
            }

            node.addHistoryNode(hNode);

            status = getCommodityDAO().addSecurityHistory(node, hNode);

            Message message;
            if (status) {
                clearCachedAccountBalance(node);
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.COMMODITY_HISTORY_ADD, this);
            } else {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.COMMODITY_HISTORY_ADD_FAILED, this);
            }

            message.setObject(MessageProperty.COMMODITY, node);
            messageBus.fireEvent(message);

            return status;
        } finally {
            commodityWriteLock.unlock();
        }
    }

    /**
     * Returns a list of investment accounts that use the given security node
     * 
     * @param node security node
     * @return list of investment accounts
     */
    Set<Account> getInvestmentAccountList(final SecurityNode node) {

        Set<Account> accounts = new HashSet<>();

        for (Account account : getInvestmentAccountList()) {
            if (account.containsSecurity(node)) {
                accounts.add(account);
            }
        }
        return accounts;
    }

    public static BigDecimal getMarketPrice(final Collection<Transaction> transactions, final SecurityNode node, final CurrencyNode baseCurrency, final Date date) {

        Date testDate = DateUtils.trimDate(date);

        // the best history node record
        SecurityHistoryNode hNode = node.getHistoryNode(testDate);

        // Get the current exchange rate for the security node
        BigDecimal rate = node.getReportedCurrencyNode().getExchangeRate(baseCurrency);

        // return if dates are exact match
        if (hNode != null && testDate.equals(DateUtils.trimDate(hNode.getDate()))) {
            // return the price and factor in the exchange rate
            return hNode.getPrice().multiply(rate);
        }

        Date priceDate = null;
        BigDecimal price = BigDecimal.ZERO;

        // no exact match yet, search the transactions

        search: for (Transaction t : transactions) {
            if (t instanceof InvestmentTransaction) {

                BigDecimal p = ((InvestmentTransaction) t).getPrice();

                // Dividend transactions, etc return BigDecimal.Zero; Protect against setting a price equal to zero
                if (((InvestmentTransaction) t).getSecurityNode() == node && p != null && p.compareTo(BigDecimal.ZERO) == 1) {
                    switch (t.getDate().compareTo(testDate)) {
                        case -1:
                            price = p;
                            priceDate = t.getDate();
                            break;
                        case 0: // found an exact match, return it
                            return p;
                        case 1:
                        default:
                            break search; // stop the loop
                    }
                }
            }
        }

        // determine the closest date
        if (hNode == null && priceDate == null) {
            return BigDecimal.ZERO; // nothing found
        } else if (priceDate != null && hNode != null) {
            if (priceDate.compareTo(hNode.getDate()) >= 0) {
                return price;
            }
        } else if (hNode == null) {
            return price;
        }

        // return the price and factor in the exchange rate
        return hNode.getPrice().multiply(rate);
    }

    /**
     * Forces all investment accounts containing the security to clear the cached account balance and reconciled account
     * balance and recalculate when queried.
     * 
     * @param node SecurityNode that was changed
     */
    private void clearCachedAccountBalance(final SecurityNode node) {

        for (Account account : getInvestmentAccountList(node)) {
            clearCachedAccountBalance(account);
        }
    }

    /**
     * Clears an <code>Accounts</code> cached balance and recursively works up the tree to the root.
     * 
     * @param account <code>Account</code> to clear
     */
    private void clearCachedAccountBalance(final Account account) {

        account.clearCachedBalances();
        getAccountDAO().updateAccount(account);

        if (account.getParent() != null && account.getParent().getAccountType() != AccountType.ROOT) {
            clearCachedAccountBalance(account.getParent());
        }
    }

    static String buildExchangeRateId(final CurrencyNode baseCurrency, final CurrencyNode exchangeCurrency) {

        String rateId;
        if (baseCurrency.getSymbol().compareToIgnoreCase(exchangeCurrency.getSymbol()) > 0) {
            rateId = baseCurrency.getSymbol() + exchangeCurrency.getSymbol();
        } else {
            rateId = exchangeCurrency.getSymbol() + baseCurrency.getSymbol();
        }
        return rateId;
    }

    CurrencyNode[] getBaseCurrencies(final String exchangeRateId) {

        ReadLock commodityReadLock = commodityLock.readLock();
        commodityReadLock.lock();

        try {
            List<CurrencyNode> currencies = getCurrencies();

            Collections.sort(currencies);
            Collections.reverse(currencies);

            for (CurrencyNode node1 : currencies) {
                for (CurrencyNode node2 : currencies) {
                    if (node1 != node2 && buildExchangeRateId(node1, node2).equals(exchangeRateId)) {
                        return new CurrencyNode[] { node1, node2 };
                    }
                }
            }
            return null;
        } finally {
            commodityReadLock.unlock();
        }
    }

    /**
     * Returns an array of currencies being used in accounts
     * 
     * @return Set of CurrencyNodes
     */
    public Set<CurrencyNode> getActiveCurrencies() {

        ReadLock commodityReadLock = commodityLock.readLock();
        commodityReadLock.lock();

        try {
            return getCommodityDAO().getActiveCurrencies();
        } finally {
            commodityReadLock.unlock();
        }
    }

    /**
     * Returns a CurrencyNode given the symbol. This will not generate a new CurrencyNode. It must be explicitly created
     * and added.
     * 
     * @param symbol Currency symbol
     * @return null if the CurrencyNode as not been defined
     */
    public CurrencyNode getCurrency(final String symbol) {

        ReadLock commodityReadLock = commodityLock.readLock();
        commodityReadLock.lock();

        try {
            CurrencyNode rNode = null;

            for (CurrencyNode node : getCurrencies()) {
                if (node.getSymbol().equals(symbol)) {
                    rNode = node;
                    break;
                }
            }
            return rNode;
        } finally {
            commodityReadLock.unlock();
        }
    }

    public List<CurrencyNode> getCurrencies() {

        ReadLock commodityReadLock = commodityLock.readLock();
        commodityReadLock.lock();

        try {
            return getCommodityDAO().getCurrencies();
        } finally {
            commodityReadLock.unlock();
        }
    }

    public List<SecurityHistoryNode> getSecurityHistory(final SecurityNode node) {

        ReadLock commodityReadLock = commodityLock.readLock();
        commodityReadLock.lock();

        try {
            return node.getHistoryNodes();
        } finally {
            commodityReadLock.unlock();
        }
    }

    public ExchangeRate getExchangeRate(final CurrencyNode baseCurrency, final CurrencyNode exchangeCurrency) {

        ReadLock commodityReadLock = commodityLock.readLock();
        commodityReadLock.lock();

        try {
            return exchangeRateDAO.getExchangeRateNode(baseCurrency, exchangeCurrency);
        } finally {
            commodityReadLock.unlock();
        }
    }

    public List<SecurityNode> getSecurities() {

        ReadLock commodityReadLock = commodityLock.readLock();
        commodityReadLock.lock();

        try {
            return getCommodityDAO().getSecurities();
        } finally {
            commodityReadLock.unlock();
        }
    }

    /**
     * Find a SecurityNode given it's symbol
     * 
     * @param symbol symbol of security to find
     * @return null if not found
     */
    public SecurityNode getSecurity(final String symbol) {

        ReadLock commodityReadLock = commodityLock.readLock();
        commodityReadLock.lock();

        try {
            List<SecurityNode> list = getSecurities();

            SecurityNode sNode = null;

            for (SecurityNode node : list) {
                if (node.getSymbol().equals(symbol)) {
                    sNode = node;
                    break;
                }
            }
            return sNode;
        } finally {
            commodityReadLock.unlock();
        }
    }

    private boolean isCommodityNodeUsed(final CommodityNode node) {

        ReadLock commodityReadLock = commodityLock.readLock();
        ReadLock accountReadLock = accountLock.readLock();

        commodityReadLock.lock();
        accountReadLock.lock();

        try {
            List<Account> list = getAccountList();

            for (Account a : list) {
                if (a.getCurrencyNode().equals(node)) {
                    return true;
                }

                if (a.getAccountType() == AccountType.INVEST || a.getAccountType() == AccountType.MUTUAL) {
                    for (SecurityNode j : a.getSecurities()) {
                        if (j.equals(node) || j.getReportedCurrencyNode().equals(node)) {
                            return true;
                        }
                    }
                }
            }

            List<SecurityNode> sList = getSecurities();

            for (SecurityNode sNode : sList) {
                if (sNode.getReportedCurrencyNode().equals(node)) {
                    return true;
                }
            }

        } finally {
            accountReadLock.unlock();
            commodityReadLock.unlock();
        }

        return false;
    }

    public boolean removeCommodity(final CommodityNode node) {

        boolean status = true;

        WriteLock commodityWriteLock = commodityLock.writeLock();
        commodityWriteLock.lock();

        try {
            if (isCommodityNodeUsed(node)) {
                status = false;
            } else {
                // Remove all history nodes first so they are not left behind
                if (node instanceof SecurityNode) {
                    List<SecurityHistoryNode> hNodes = ((SecurityNode) node).getHistoryNodes();

                    for (SecurityHistoryNode hNode : hNodes) {
                        if (!removeSecurityHistory((SecurityNode) node, hNode)) {
                            logSevere(MessageFormat.format(rb.getString("Message.Error.HistRemoval"), hNode.getDate(), node.getSymbol()));
                        }
                    }
                } else {
                    clearObsoleteExchangeRates();
                }
                moveObjectToTrash(node);
            }

            Message message;
            if (status) {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.CURRENCY_REMOVE, this);
            } else {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.CURRENCY_REMOVE_FAILED, this);
            }
            message.setObject(MessageProperty.COMMODITY, node);
            messageBus.fireEvent(message);

            return status;

        } finally {
            commodityWriteLock.unlock();
        }
    }

    public boolean removeSecurityHistory(final SecurityNode node, final SecurityHistoryNode hNode) {

        WriteLock commodityWriteLock = commodityLock.writeLock();
        commodityWriteLock.lock();

        try {
            node.removeHistoryNode(hNode);

            boolean status = getCommodityDAO().removeSecurityHistory(node, hNode);

            Message message;
            if (status) {
                clearCachedAccountBalance(node);
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.COMMODITY_HISTORY_REMOVE, this);
            } else {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.COMMODITY_HISTORY_REMOVE_FAILED, this);
            }

            message.setObject(MessageProperty.COMMODITY, node);
            messageBus.fireEvent(message);

            return status;
        } finally {
            commodityWriteLock.unlock();
        }
    }

    public void setDefaultCurrency(final CurrencyNode defaultCurrency) {

        // make sure the new default is persisted if it has not been
        if (!isStored(defaultCurrency)) {
            addCommodity(defaultCurrency);
        }

        WriteLock accountWriteLock = accountLock.writeLock();
        WriteLock configWriteLock = configLock.writeLock();
        WriteLock commodityWriteLock = commodityLock.writeLock();

        accountWriteLock.lock();
        configWriteLock.lock();
        commodityWriteLock.lock();

        try {
            Config c = getConfig();
            c.setDefaultCurrency(defaultCurrency);
            getConfigDAO().commit(c);

            logInfo("Setting default currency: " + defaultCurrency.toString());

            Account root = getRootAccount();

            // The root account holds a reference to the default currency
            root.setCurrencyNode(defaultCurrency);
            getAccountDAO().updateAccount(root);

        } finally {
            commodityWriteLock.unlock();
            configWriteLock.unlock();
            accountWriteLock.unlock();
        }
    }

    private Config getConfig() {

        ReadLock configReadLock = configLock.readLock();
        configReadLock.lock();

        try {
            if (config == null) {
                config = getConfigDAO().getDefaultConfig();
            }
            return config;

        } finally {
            configReadLock.unlock();
        }
    }

    public CurrencyNode getDefaultCurrency() {

        ReadLock configReadLock = configLock.readLock();
        ReadLock commodityReadLock = commodityLock.readLock();

        configReadLock.lock();
        commodityReadLock.lock();

        try {
            CurrencyNode node = getConfig().getDefaultCurrency();

            if (node == null) {
                logger.warning("No default currency assigned");
            }

            return node;
        } finally {
            commodityReadLock.unlock();
            configReadLock.unlock();
        }
    }

    public void setExchangeRate(final CurrencyNode baseCurrency, final CurrencyNode exchangeCurrency, final BigDecimal rate) {

        setExchangeRate(baseCurrency, exchangeCurrency, rate, new Date());
    }

    public void setExchangeRate(final CurrencyNode baseCurrency, final CurrencyNode exchangeCurrency, final BigDecimal rate, final Date date) {

        assert rate != null && rate.compareTo(BigDecimal.ZERO) > 0;

        if (baseCurrency.equals(exchangeCurrency)) {
            return;
        }

        WriteLock commodityWriteLock = commodityLock.writeLock();
        commodityWriteLock.lock();

        try {
            // find the correct ExchangeRate and create if needed
            ExchangeRate exchangeRate = getExchangeRate(baseCurrency, exchangeCurrency);

            if (exchangeRate == null) {
                exchangeRate = new ExchangeRate(buildExchangeRateId(baseCurrency, exchangeCurrency));
            }

            // create the history node
            ExchangeRateHistoryNode historyNode;

            if (baseCurrency.getSymbol().compareToIgnoreCase(exchangeCurrency.getSymbol()) > 0) {
                historyNode = new ExchangeRateHistoryNode(DateUtils.trimDate(date), rate);
            } else {
                historyNode = new ExchangeRateHistoryNode(DateUtils.trimDate(date), BigDecimal.ONE.divide(rate, MathConstants.mathContext));
            }

            // remove the old history node first if it exists
            List<ExchangeRateHistoryNode> history = exchangeRate.getHistory();

            for (ExchangeRateHistoryNode node : history) {
                if (node.equals(historyNode)) {
                    removeExchangeRateHistory(exchangeRate, node);
                }
            }

            exchangeRate.addHistoryNode(historyNode);

            getCommodityDAO().addExchangeRateHistory(exchangeRate, historyNode);

            Message message = new Message(MessageChannel.COMMODITY, ChannelEvent.EXCHANGE_RATE_ADD, this);
            message.setObject(MessageProperty.EXCHANGERATE, exchangeRate);

            messageBus.fireEvent(message);
        } finally {
            commodityWriteLock.unlock();
        }
    }

    public void removeExchangeRateHistory(final ExchangeRate exchangeRate, final ExchangeRateHistoryNode history) {

        WriteLock commodityWriteLock = commodityLock.writeLock();
        commodityWriteLock.lock();

        try {
            Message message;

            if (exchangeRate.contains(history)) {
                exchangeRate.removeHistoryNode(history);
                getCommodityDAO().removeExchangeRateHistory(exchangeRate, history);

                message = new Message(MessageChannel.COMMODITY, ChannelEvent.EXCHANGE_RATE_REMOVE, this);
            } else {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.EXCHANGE_RATE_REMOVE_FAILED, this);
            }

            message.setObject(MessageProperty.EXCHANGERATE, exchangeRate);
            messageBus.fireEvent(message);
        } finally {
            commodityWriteLock.unlock();
        }
    }

    /**
     * Modifies an existing currency node in place. The supplied node should not be a reference to the original
     * 
     * @param oldNode old CommodityNode
     * @param templateNode template CommodityNode
     * @return true if successful
     */
    public boolean updateCommodity(final CommodityNode oldNode, final CommodityNode templateNode) {

        assert oldNode != null && templateNode != null;
        assert oldNode != templateNode;

        WriteLock commodityWriteLock = commodityLock.writeLock();
        commodityWriteLock.lock();

        try {
            boolean status = true;

            if (oldNode.getClass().equals(templateNode.getClass())) {

                oldNode.setDescription(templateNode.getDescription());
                oldNode.setPrefix(templateNode.getPrefix());
                oldNode.setScale(templateNode.getScale());
                oldNode.setSuffix(templateNode.getSuffix());

                if (templateNode instanceof SecurityNode) {
                    oldNode.setSymbol(templateNode.getSymbol()); // allow symbol to change

                    ((SecurityNode) oldNode).setReportedCurrencyNode(((SecurityNode) templateNode).getReportedCurrencyNode());

                    ((SecurityNode) oldNode).setQuoteSource(((SecurityNode) templateNode).getQuoteSource());

                    ((SecurityNode) oldNode).setISIN(((SecurityNode) templateNode).getISIN());
                }

                getCommodityDAO().updateCommodityNode(oldNode);
            } else {
                status = false;
                logger.warning("Template object class did not match old object class");
            }

            Message message;
            if (status) {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.CURRENCY_MODIFY, this);
                message.setObject(MessageProperty.COMMODITY, oldNode);
            } else {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.CURRENCY_MODIFY_FAILED, this);
                message.setObject(MessageProperty.COMMODITY, templateNode);
            }

            messageBus.fireEvent(message);
            return status;
        } finally {
            commodityWriteLock.unlock();
        }
    }

    public void updateReminder(final Reminder reminder) {
        getReminderDAO().updateReminder(reminder);
    }
    
    /**
     * Returns the engine logger
     * 
     * @return the engine logger
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * Log a informational message
     * 
     * @param message message to display
     */
    private static void logInfo(final String message) {
        logger.log(Level.INFO, message);
    }

    /**
     * Log a warning message
     * 
     * @param message message to display
     */
    private static void logWarning(final String message) {

        logger.warning(message);
    }

    /**
     * Log a severe message
     * 
     * @param message message to display
     */
    private static void logSevere(final String message) {

        logger.severe(message);
    }

    public void setAccountSeparator(final String separator) {

        WriteLock configWriteLock = configLock.writeLock();
        configWriteLock.lock();

        try {
            accountSeparator = separator;
            Config c = getConfig();

            c.setAccountSeparator(separator);

            getConfigDAO().commit(c);

        } finally {
            configWriteLock.unlock();
        }
    }

    public String getAccountSeparator() {

        ReadLock configReadLock = configLock.readLock();
        configReadLock.lock();

        try {
            if (accountSeparator == null) {
                accountSeparator = getConfig().getAccountSeparator();
            }
            return accountSeparator;

        } finally {
            configReadLock.unlock();
        }
    }

    /**
     * Returns a list of all Accounts excluding the rootAccount
     * 
     * @return List of accounts
     */
    public List<Account> getAccountList() {
        List<Account> accounts = getAccountDAO().getAccountList();
        accounts.remove(getRootAccount());

        return accounts;
    }

    public Account getAccountByUuid(final String id) {

        return getAccountDAO().getAccountByUuid(id);
    }

    /**
     * Search for an account with a matching account number
     * 
     * @param accountName Account name to search for. <b>Must not be null</b>
     * @return The matching account. <code>null</code> if not found.
     */
    public Account getAccountByName(final String accountName) {

        if (accountName == null) {
            throw new IllegalArgumentException("Specified name may not be null");
        }

        final List<Account> list = getAccountList();

        // sort for consistent search order
        Collections.sort(list);

        for (Account account : list) {
            if (accountName.equals(account.getName())) {
                return account;
            }
        }
        return null;
    }

    /**
     * Returns a list of IncomeAccounts excluding the rootIncomeAccount
     * 
     * @return List of income accounts
     */
    public List<Account> getIncomeAccountList() {

        return getAccountDAO().getIncomeAccountList();
    }

    /**
     * Returns a list of ExpenseAccounts excluding the rootExpenseAccount
     * 
     * @return List if expense accounts
     */
    public List<Account> getExpenseAccountList() {

        return getAccountDAO().getExpenseAccountList();
    }

    /**
     * Returns a list of Accounts that are members of the group
     * 
     * @param group the requested AccountGroup
     * @return member accounts
     */
    public List<Account> getAccounts(final AccountGroup group) {
        final List<Account> accountList = new ArrayList<>();
        final List<Account> list = getAccountList();

        for (Account account : list) {
            if (account.memberOf(group)) {
                accountList.add(account);
            }
        }

        return accountList;
    }

    /**
     * Returns a list of all accounts excluding the rootAccount and IncomeAccounts and ExpenseAccounts
     * 
     * @return List of investment accounts
     */
    public List<Account> getInvestmentAccountList() {

        return getAccountDAO().getInvestmentAccountList();
    }

    public void refreshAccount(final Account account) {

        getAccountDAO().refreshAccount(account);
    }

    public void refreshBudget(final Budget budget) {

        getBudgetDAO().refreshBudget(budget);
    }

    public void refreshCommodity(final CommodityNode node) {

        getCommodityDAO().refreshCommodityNode(node);
    }

    public void refreshExchangeRate(final ExchangeRate rate) {

        getCommodityDAO().refreshExchangeRate(rate);
    }

    public void refreshReminder(final Reminder reminder) {

        getReminderDAO().refreshReminder(reminder);
    }

    public void refreshTransaction(final Transaction transaction) {

        getTransactionDAO().refreshTransaction(transaction);
    }

    /**
     * Adds a new account
     * 
     * @param parent The parent account
     * @param child A new Account object
     * @return true if successful
     */
    public boolean addAccount(final Account parent, final Account child) {

        assert child != null;
        assert child.getUuid() != null;

        if (child.getAccountType() == AccountType.ROOT) {
            throw new RuntimeException("Invalid Account");
        }

        WriteLock accountWriteLock = accountLock.writeLock();
        accountWriteLock.lock();

        try {
            Message message;
            boolean result;

            result = parent.addChild(child);

            if (result) {
                result = getAccountDAO().addAccount(parent, child);
            }

            if (result) {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_ADD, this);
                message.setObject(MessageProperty.ACCOUNT, child);
                messageBus.fireEvent(message);

                logInfo(rb.getString("Message.AccountAdd"));
                result = true;
            } else {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_ADD_FAILED, this);
                message.setObject(MessageProperty.ACCOUNT, child);
                messageBus.fireEvent(message);
                result = false;
            }
            return result;
        } finally {
            accountWriteLock.unlock();
        }
    }

    /**
     * Return the root account
     * 
     * @return RootAccount
     */
    public RootAccount getRootAccount() {

        ReadLock accountReadLock = accountLock.readLock();
        accountReadLock.lock();

        try {
            if (rootAccount == null) {
                rootAccount = getAccountDAO().getRootAccount();
            }
            return rootAccount;
        } finally {
            accountReadLock.unlock();
        }
    }

    /**
     * Move an account to a new parent account
     * 
     * @param account account to move
     * @param newParent the new parent account
     * @return true if successful
     */
    public boolean moveAccount(final Account account, final Account newParent) {

        assert account != null && newParent != null;

        WriteLock accountWriteLock = accountLock.writeLock();
        accountWriteLock.lock();

        try {
            // cannot invert the child/parent relationship of an account
            if (account.contains(newParent)) {
                Message message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_MODIFY_FAILED, this);
                message.setObject(MessageProperty.ACCOUNT, account);
                messageBus.fireEvent(message);

                logInfo(rb.getString("Message.AccountMoveFailed"));

                return false;
            }

            Account oldParent = account.getParent();

            if (oldParent != null) { // check for detached account

                oldParent.removeChild(account);

                getAccountDAO().updateAccount(account);
                getAccountDAO().updateAccount(oldParent);

                Message message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_MODIFY, this);
                message.setObject(MessageProperty.ACCOUNT, oldParent);

                messageBus.fireEvent(message);
            }

            newParent.addChild(account);

            getAccountDAO().updateAccount(account);
            getAccountDAO().updateAccount(newParent);

            Message message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_MODIFY, this);
            message.setObject(MessageProperty.ACCOUNT, newParent);

            messageBus.fireEvent(message);

            logInfo(rb.getString("Message.AccountModify"));

            return true;
        } finally {
            accountWriteLock.unlock();
        }
    }

    /**
     * Modifies an existing account given an account as a template. The type of the account cannot be changed.
     * 
     * @param template The Account object to use as a template
     * @param account The existing account
     * @return true if successful
     */
    public boolean modifyAccount(final Account template, final Account account) {

        boolean result;
        Message message;

        WriteLock accountWriteLock = accountLock.writeLock();
        accountWriteLock.lock();

        try {
            account.setName(template.getName());
            account.setDescription(template.getDescription());
            account.setNotes(template.getNotes());

            account.setLocked(template.isLocked());
            account.setPlaceHolder(template.isPlaceHolder());
            account.setVisible(template.isVisible());
            account.setExcludedFromBudget(template.isExcludedFromBudget());
            account.setAccountNumber(template.getAccountNumber());
            account.setBankId(template.getBankId());

            if (account.getAccountType().isMutable()) {
                account.setAccountType(template.getAccountType());
            }

            // allow allow a change if the account does not contain transactions
            if (account.getTransactionCount() == 0) {
                account.setCurrencyNode(template.getCurrencyNode());
            }

            result = getAccountDAO().updateAccount(account);

            if (result) {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_MODIFY, this);
                message.setObject(MessageProperty.ACCOUNT, account);
                messageBus.fireEvent(message);

                logInfo(rb.getString("Message.AccountModify"));
            } else {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_MODIFY_FAILED, this);
                message.setObject(MessageProperty.ACCOUNT, account);
                messageBus.fireEvent(message);
            }

            /* Check to see if the account needs to be moved */
            if (account.parentAccount != template.parentAccount && template.parentAccount != null && result) {
                if (!moveAccount(account, template.parentAccount)) {
                    logWarning(rb.getString("Message.Error.MoveAccount"));
                    result = false;
                }
            }

            return result;
        } finally {
            accountWriteLock.unlock();
        }
    }

    /**
     * Sets the account number of an account
     * 
     * @param account account to change
     * @param number new account number
     */
    public void setAccountNumber(final Account account, final String number) {

        WriteLock accountWriteLock = accountLock.writeLock();
        accountWriteLock.lock();

        try {
            account.setAccountNumber(number);
            getAccountDAO().updateAccount(account);

            Message message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_MODIFY, this);
            message.setObject(MessageProperty.ACCOUNT, account);
            messageBus.fireEvent(message);

            logInfo(rb.getString("Message.AccountModify"));
        } finally {
            accountWriteLock.unlock();
        }
    }

    /**
     * Removes an existing account given it's ID.
     * 
     * @param account The account to remove
     * @return true if successful
     */
    public boolean removeAccount(final Account account) {

        WriteLock accountWriteLock = accountLock.writeLock();
        accountWriteLock.lock();

        try {
            boolean result = false;

            if (account.getTransactionCount() > 0 || account.getChildCount() > 0) {
                result = false;
            } else {
                Account parent = account.getParent();

                if (parent != null) {
                    result = parent.removeChild(account);

                    if (result) {
                        getAccountDAO().updateAccount(parent);

                        // clear budget history
                        for (Budget budget : getBudgetList()) {
                            budget.removeBudgetGoal(account);
                            updateBudget(budget);
                        }
                    }
                }

                moveObjectToTrash(account);
            }

            Message message;

            if (result) {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_REMOVE, this);
                message.setObject(MessageProperty.ACCOUNT, account);
                messageBus.fireEvent(message);

                logInfo(rb.getString("Message.AccountRemove"));
            } else {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_REMOVE_FAILED, this);
                message.setObject(MessageProperty.ACCOUNT, account);
                messageBus.fireEvent(message);
            }

            return result;
        } finally {
            accountWriteLock.unlock();
        }
    }

    @SuppressWarnings("deprecation")
    private void migrateAmortizeObjects() {

        WriteLock accountWriteLock = accountLock.writeLock();
        accountWriteLock.lock();

        try {

            for (Account account : getAccounts(AccountGroup.LIABILITY)) {
                if (account.getProperty(AccountProperty.AMORTIZEOBJECT) != null) {

                    AmortizeObject oldAmortizeObject = (AmortizeObject) account.getProperty(AccountProperty.AMORTIZEOBJECT);

                    account.setAmortizeObject(oldAmortizeObject);

                    account.removeProperty(AccountProperty.AMORTIZEOBJECT);
                    getAccountDAO().removeAccountProperty(account, oldAmortizeObject);
                }
            }
        } finally {
            accountWriteLock.unlock();
        }
    }

    /**
     * Sets the amortize object of an account
     * 
     * @param account The Liability account to change
     * @param amortizeObject the new AmortizeObject
     * @return true if successful
     */
    public boolean setAmortizeObject(final Account account, final AmortizeObject amortizeObject) {

        WriteLock accountWriteLock = accountLock.writeLock();
        accountWriteLock.lock();

        try {
            if (account != null && amortizeObject != null && account.getAccountType() == AccountType.LIABILITY) {

                account.setAmortizeObject(amortizeObject);

                if (!getAccountDAO().updateAccount(account)) {
                    logSevere("Was not able to save the amortize object");
                }

                return true;
            }
            return false;
        } finally {
            accountWriteLock.unlock();
        }
    }

    /**
     * Toggles the visibility of an account given its ID.
     * 
     * @param account The account to toggle visibility
     * @return <tt>true</tt> if the supplied account ID was found <tt>false</tt> if the supplied account ID was not
     *         found
     */
    public boolean toggleAccountVisibility(final Account account) {

        WriteLock accountWriteLock = accountLock.writeLock();
        accountWriteLock.lock();

        try {
            boolean result = false;

            Message message;

            account.setVisible(!account.isVisible());

            if (getAccountDAO().toggleAccountVisibility(account)) {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_VISIBILITY_CHANGE, this);
                message.setObject(MessageProperty.ACCOUNT, account);
                messageBus.fireEvent(message);

                result = true;
            } else {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_VISIBILITY_CHANGE_FAILED, this);
                message.setObject(MessageProperty.ACCOUNT, account);
                messageBus.fireEvent(message);
            }

            return result;
        } finally {
            accountWriteLock.unlock();
        }
    }

    /**
     * Adds a SecurityNode from a InvestmentAccount
     * 
     * @param account destination account
     * @param node SecurityNode to add
     * @return true if add was successful
     */
    private boolean addAccountSecurity(final Account account, final SecurityNode node) {

        WriteLock accountWriteLock = accountLock.writeLock();
        WriteLock commodityWriteLock = commodityLock.writeLock();

        accountWriteLock.lock();
        commodityWriteLock.lock();

        try {
            Message message;

            boolean result = account.addSecurity(node);

            if (result) {
                result = getAccountDAO().addAccountSecurity(account, node);
            }

            if (result) {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_SECURITY_ADD, this);
            } else {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_SECURITY_ADD_FAILED, this);
            }

            message.setObject(MessageProperty.ACCOUNT, account);
            message.setObject(MessageProperty.COMMODITY, node);
            messageBus.fireEvent(message);

            return result;

        } finally {
            commodityWriteLock.unlock();
            accountWriteLock.unlock();
        }
    }

    /**
     * Removes a SecurityNode from an InvestmentAccount
     * 
     * @param account Account to remove SecurityNode from
     * @param node SecurityNode to remove
     * @return true if successful
     */
    private boolean removeAccountSecurity(final Account account, final SecurityNode node) {

        assert node != null;

        WriteLock accountWriteLock = accountLock.writeLock();
        WriteLock commodityWriteLock = commodityLock.writeLock();

        accountWriteLock.lock();
        commodityWriteLock.lock();

        try {
            Message message;
            boolean result = account.removeSecurity(node);

            if (result) {
                getAccountDAO().updateAccount(account);
            }

            if (result) {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_SECURITY_REMOVE, this);
            } else {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_SECURITY_REMOVE_FAILED, this);
            }

            message.setObject(MessageProperty.ACCOUNT, account);
            message.setObject(MessageProperty.COMMODITY, node);
            messageBus.fireEvent(message);

            return result;

        } finally {
            commodityWriteLock.unlock();
            accountWriteLock.unlock();
        }
    }

    /**
     * Update an account's securities list. This compares the old list of securities and the supplied list and adds or
     * removes securities to make sure the lists are the same.
     * 
     * @param acc Destination account
     * @param list Collection of SecurityNodes
     * @return true if successful
     */
    public boolean updateAccountSecurities(final Account acc, final Collection<SecurityNode> list) {

        boolean result = false;

        if (acc.memberOf(AccountGroup.INVEST)) {

            WriteLock accountWriteLock = accountLock.writeLock();
            WriteLock commodityWriteLock = commodityLock.writeLock();

            accountWriteLock.lock();
            commodityWriteLock.lock();

            try {
                final Collection<SecurityNode> oldList = acc.getSecurities();

                for (SecurityNode node : oldList) {
                    if (!list.contains(node)) {
                        removeAccountSecurity(acc, node);
                    }
                }

                for (SecurityNode node : list) {
                    if (!oldList.contains(node)) {
                        addAccountSecurity(acc, node);
                    }
                }

                result = true;
            } finally {
                commodityWriteLock.unlock();
                accountWriteLock.unlock();
            }
        }

        return result;
    }

    public boolean addBudget(final Budget budget) {

        boolean result;

        WriteLock budgetWriteLock = budgetLock.writeLock();

        budgetWriteLock.lock();

        try {
            Message message;

            result = getBudgetDAO().add(budget);

            if (result) {
                message = new Message(MessageChannel.BUDGET, ChannelEvent.BUDGET_ADD, this);
            } else {
                message = new Message(MessageChannel.BUDGET, ChannelEvent.BUDGET_ADD_FAILED, this);
            }

            message.setObject(MessageProperty.BUDGET, budget);
            messageBus.fireEvent(message);

            return result;

        } finally {
            budgetWriteLock.unlock();
        }
    }

    public boolean removeBudget(final Budget budget) {

        boolean result = false;

        WriteLock writeLock = budgetLock.writeLock();

        writeLock.lock();

        try {
            Message message;

            moveObjectToTrash(budget);

            message = new Message(MessageChannel.BUDGET, ChannelEvent.BUDGET_REMOVE, this);

            message.setObject(MessageProperty.BUDGET, budget);
            messageBus.fireEvent(message);

            result = true;
        } finally {
            writeLock.unlock();
        }

        return result;
    }

    public void updateBudgetGoals(final Budget budget, final Account account, final BudgetGoal newGoals) {
        WriteLock writeLock = budgetLock.writeLock();

        writeLock.lock();

        try {
            BudgetPeriod budgetPeriod = budget.getBudgetPeriod();

            BudgetGoal oldGoals = budget.getBudgetGoal(account);

            List<BudgetPeriodDescriptor> descriptorList = BudgetPeriodDescriptorFactory.getDescriptors(DateUtils.getCurrentYear(), budgetPeriod);
            List<BudgetPeriodDescriptor> changedDescriptors = new ArrayList<>();

            for (BudgetPeriodDescriptor descriptor : descriptorList) {
                BigDecimal oldAmount = oldGoals.getGoal(descriptor.getStartPeriod(), descriptor.getEndPeriod());
                BigDecimal newAmount = newGoals.getGoal(descriptor.getStartPeriod(), descriptor.getEndPeriod());

                if (oldAmount.compareTo(newAmount) != 0) {
                    changedDescriptors.add(descriptor);
                }
            }

            budget.setBudgetGoal(account, newGoals);

            updateBudgetGoals(budget, account, changedDescriptors);

        } finally {
            writeLock.unlock();
        }
    }

    private void updateBudgetGoals(final Budget budget, final Account account, final List<BudgetPeriodDescriptor> changedPeriods) {
        WriteLock writeLock = budgetLock.writeLock();

        writeLock.lock();

        try {
            Message baseMessage;

            boolean result = getBudgetDAO().update(budget);

            if (result) {
                baseMessage = new Message(MessageChannel.BUDGET, ChannelEvent.BUDGET_GOAL_UPDATE, this);
            } else {
                baseMessage = new Message(MessageChannel.BUDGET, ChannelEvent.BUDGET_GOAL_UPDATE_FAILED, this);
            }

            baseMessage.setObject(MessageProperty.BUDGET, budget);
            baseMessage.setObject(MessageProperty.ACCOUNT, account);

            for (BudgetPeriodDescriptor period : changedPeriods) {
                Message message;
                try {
                    message = baseMessage.clone();
                    message.setMessage(BudgetPeriodDescriptor.encodeToString(period));

                    messageBus.fireEvent(message);
                } catch (CloneNotSupportedException e) {
                    logger.log(Level.SEVERE, e.toString(), e);
                }
            }

            logger.log(Level.FINE, "Budget goal updated for {0}", account.getPathName());
        } finally {
            writeLock.unlock();
        }
    }

    public boolean updateBudget(final Budget budget) {

        boolean result;

        WriteLock writeLock = budgetLock.writeLock();

        writeLock.lock();

        try {
            Message message;

            result = getBudgetDAO().update(budget);

            if (result) {
                message = new Message(MessageChannel.BUDGET, ChannelEvent.BUDGET_UPDATE, this);
            } else {
                message = new Message(MessageChannel.BUDGET, ChannelEvent.BUDGET_UPDATE_FAILED, this);
            }

            message.setObject(MessageProperty.BUDGET, budget);
            messageBus.fireEvent(message);

            logger.log(Level.FINE, "Budget updated");

            return result;

        } finally {
            writeLock.unlock();
        }
    }

    public List<Budget> getBudgetList() {

        ReadLock readLock = budgetLock.readLock();
        readLock.lock();

        try {
            return getBudgetDAO().getBudgets();
        } finally {
            readLock.unlock();
        }
    }

    public boolean isTransactionValid(final Transaction transaction) {

        for (Account a : transaction.getAccounts()) {
            if (a.isLocked()) {
                logWarning(rb.getString("Message.TransactionAccountLocked"));
                return false;
            }
        }

        if (transaction.isMarkedForRemoval()) {
            logger.log(Level.WARNING, "Transaction already marked for removal");
            return false;
        }

        if (eDAO.getObjectByUuid(transaction.getUuid()) != null) {
            logger.log(Level.WARNING, "Transaction UUID was not unique");
            return false;
        }

        if (transaction.size() < 1) {
            logger.log(Level.WARNING, "Invalid Transaction");
            return false;
        }

        for (TransactionEntry e : transaction.getTransactionEntries()) {
            if (e == null) {
                logger.log(Level.WARNING, "Null TransactionEntry");
                return false;
            }
        }

        for (TransactionEntry e : transaction.getTransactionEntries()) {
            if (e.getTransactionTag() == null) {
                logger.log(Level.WARNING, "Null TransactionTag");
                return false;
            }
        }

        for (TransactionEntry e : transaction.getTransactionEntries()) {
            if (e.getCreditAccount() == null) {
                logger.log(Level.WARNING, "Null Credit Account");
                return false;
            }

            if (e.getDebitAccount() == null) {
                logger.log(Level.WARNING, "Null Debit Account");
                return false;
            }

            if (e.getCreditAmount() == null) {
                logger.log(Level.WARNING, "Null Credit Amount");
                return false;
            }

            if (e.getDebitAmount() == null) {
                logger.log(Level.WARNING, "Null Debit Amount");
                return false;
            }
        }

        if (transaction.getTransactionType() == TransactionType.SPLITENTRY && transaction.getCommonAccount() == null) {
            logger.log(Level.WARNING, "Entries do not share a common account");
            return false;
        }

        return !(transaction instanceof InvestmentTransaction) || transaction.getTransactionType() != null;

    }

    /**
     * Determine if a StoredObject is persisted in the database
     * 
     * @param object StoredObject to check
     * @return true if persisted
     */
    public boolean isStored(final StoredObject object) {

        return eDAO.getObjectByUuid(object.getUuid()) != null;
    }

    public boolean addTransaction(final Transaction transaction) {

        WriteLock accountWriteLock = accountLock.writeLock();
        accountWriteLock.lock();

        try {
            boolean result = isTransactionValid(transaction);

            if (result) {
                /* Add the transaction to each account */
                for (Account account : transaction.getAccounts()) {
                    if (!account.addTransaction(transaction)) {
                        logSevere("Failed to add the Transaction");
                    }
                }
                result = getTransactionDAO().addTransaction(transaction);

                logInfo(rb.getString("Message.TransactionAdd"));

                /* If successful, extract and enter a default exchange rate for the transaction date if a rate has not been set */
                if (result) {
                    for (TransactionEntry entry: transaction.getTransactionEntries()) {
                        if (entry.isMultiCurrency()) {
                            final ExchangeRate rate = getExchangeRate(entry.getDebitAccount().getCurrencyNode(), entry.getCreditAccount().getCurrencyNode());

                            if (rate.getRate(transaction.getDate()).equals(BigDecimal.ZERO)) { // no rate for the date has been set
                                final BigDecimal exchangeRate = entry.getDebitAmount().abs().divide(entry.getCreditAmount().abs(), MathConstants.mathContext);

                                setExchangeRate(entry.getCreditAccount().getCurrencyNode(), entry.getDebitAccount().getCurrencyNode(), exchangeRate, transaction.getDate());
                            }
                        }
                    }
                }
            }

            postTransactionAdd(transaction, result);

            return result;
        } finally {
            accountWriteLock.unlock();
        }
    }

    public boolean removeTransaction(final Transaction transaction) {

        WriteLock accountWriteLock = accountLock.writeLock();
        accountWriteLock.lock();

        try {
            for (Account account : transaction.getAccounts()) {
                if (account.isLocked()) {
                    logWarning(rb.getString("Message.TransactionRemoveLocked"));
                    return false;
                }
            }

            /* Remove the transaction from each account */
            for (Account account : transaction.getAccounts()) {
                if (!account.removeTransaction(transaction)) {
                    logSevere("Failed to add the Transaction");
                }
            }

            logInfo(rb.getString("Message.TransactionRemove"));

            boolean result = getTransactionDAO().removeTransaction(transaction);

            // move transactions into the trash
            if (result) {
                moveObjectToTrash(transaction);
            }

            postTransactionRemove(transaction, result);

            return result;
        } finally {
            accountWriteLock.unlock();
        }
    }

    /**
     * Changes the reconciled state of a transaction
     * 
     * @param transaction transaction to change
     * @param account account to change state for
     * @param state new reconciled state
     */
    public void setTransactionReconciled(final Transaction transaction, final Account account, final ReconciledState state) {
        try {
            final Transaction clone = (Transaction) transaction.clone();

            clone.setReconciled(account, state);

            if (removeTransaction(transaction)) {
                addTransaction(clone);
            }
        } catch (CloneNotSupportedException e) {           
            logger.log(Level.SEVERE, "Failed to reconcile the Transaction", e);
        }
    }

    public List<String> getTransactionNumberList() {

        return getConfig().getTransactionNumberList();
    }

    public void setTransactionNumberList(final List<String> list) {

        Config c = getConfig();

        c.setTransactionNumberList(list);

        getConfigDAO().commit(c);
    }

    /**
     * Get all transactions
     * <p>
     * The returned list may be altered by the caller without causing side effects
     * 
     * @return all transactions
     */
    public List<Transaction> getTransactions() {

        return getTransactionDAO().getTransactions();
    }

    private void postTransactionAdd(final Transaction transaction, final boolean result) {

        for (Account a : transaction.getAccounts()) {
            Message message;

            if (result) {
                message = new Message(MessageChannel.TRANSACTION, ChannelEvent.TRANSACTION_ADD, this);
            } else {
                message = new Message(MessageChannel.TRANSACTION, ChannelEvent.TRANSACTION_ADD_FAILED, this);
            }
            message.setObject(MessageProperty.ACCOUNT, a);
            message.setObject(MessageProperty.TRANSACTION, transaction);

            messageBus.fireEvent(message);
        }
    }

    private void postTransactionRemove(final Transaction transaction, final boolean result) {

        for (Account a : transaction.getAccounts()) {
            Message message;

            if (result) {
                message = new Message(MessageChannel.TRANSACTION, ChannelEvent.TRANSACTION_REMOVE, this);
            } else {
                message = new Message(MessageChannel.TRANSACTION, ChannelEvent.TRANSACTION_REMOVE_FAILED, this);
            }
            message.setObject(MessageProperty.ACCOUNT, a);
            message.setObject(MessageProperty.TRANSACTION, transaction);

            messageBus.fireEvent(message);
        }
    }

    /**
     * Returns the unique identifier for this engine instance
     * 
     * @return uuid
     */
    public String getUuid() {

        return uuid;
    }
}
