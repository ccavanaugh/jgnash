/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.attachment.AttachmentManager;
import jgnash.engine.budget.Budget;
import jgnash.engine.budget.BudgetGoal;
import jgnash.engine.concurrent.LockManager;
import jgnash.engine.dao.AccountDAO;
import jgnash.engine.dao.BudgetDAO;
import jgnash.engine.dao.CommodityDAO;
import jgnash.engine.dao.ConfigDAO;
import jgnash.engine.dao.EngineDAO;
import jgnash.engine.dao.RecurringDAO;
import jgnash.engine.dao.TransactionDAO;
import jgnash.engine.dao.TrashDAO;
import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageProperty;
import jgnash.engine.recurring.PendingReminder;
import jgnash.engine.recurring.RecurringIterator;
import jgnash.engine.recurring.Reminder;
import jgnash.net.currency.CurrencyUpdateFactory;
import jgnash.net.security.UpdateFactory;
import jgnash.util.DateUtils;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.NotNull;
import jgnash.util.Nullable;
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
    public static final float CURRENT_VERSION = 2.15f;

    // Lock names
    private static final String ACCOUNT_LOCK = "account";

    private static final String BUDGET_LOCK = "budget";

    private static final String COMMODITY_LOCK = "commodity";

    private static final String CONFIG_LOCK = "config";

    private static final String ENGINE_LOCK = "engine";

    private static final Logger logger = Logger.getLogger(Engine.class.getName());

    private final static long MAXIMUM_TRASH_AGE = 5 * 60 * 1000; // 5 minutes

    private final Resource rb = Resource.get();

    private final ReentrantReadWriteLock accountLock;

    private final ReentrantReadWriteLock budgetLock;

    private final ReentrantReadWriteLock commodityLock;

    private final ReentrantReadWriteLock configLock;

    private final ReentrantReadWriteLock engineLock;

    /**
     * Named identifier for this engine instance
     */
    private final String name;

    /**
     * Unique identifier for this engine instance.
     * Used by this distributed lock manager to keep track of who has a lock
     */
    private final String uuid = UUIDUtil.getUID();

    static {
        logger.setLevel(Level.ALL);
    }

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

    private final EngineDAO eDAO;

    private final AttachmentManager attachmentManager;

    /**
     * Cached for performance
     */
    private String accountSeparator = null;

    /**
     * The maximum number of network errors before scheduled tasks are stopped
     */
    private static final short MAX_ERRORS = 2;

    /**
     * Time in seconds to delay start of background updates
     */
    private static final int SCHEDULED_DELAY = 30;

    private final ScheduledThreadPoolExecutor trashExecutor;

    private final ScheduledThreadPoolExecutor backgroundExecutorService;

    public Engine(final EngineDAO eDAO, final LockManager lockManager, final AttachmentManager attachmentManager, final String name) {
        Objects.requireNonNull(name, "The engine name may not be null");
        Objects.requireNonNull(eDAO, "The engineDAO may not be null");

        this.attachmentManager = attachmentManager;
        this.eDAO = eDAO;
        this.name = name;

        // Generate locks
        accountLock = lockManager.getLock(ACCOUNT_LOCK);
        budgetLock = lockManager.getLock(BUDGET_LOCK);
        commodityLock = lockManager.getLock(COMMODITY_LOCK);
        configLock = lockManager.getLock(CONFIG_LOCK);
        engineLock = lockManager.getLock(ENGINE_LOCK);

        messageBus = MessageBus.getInstance(name);

        initialize();

        checkAndCorrect();

        trashExecutor = new ScheduledThreadPoolExecutor(1, new DefaultDaemonThreadFactory());

        // run trash cleanup every 5 minutes 1 minute after startup
        trashExecutor.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                if (!Thread.currentThread().isInterrupted()) {
                    emptyTrash();
                }
            }
        }, 1, 5, TimeUnit.MINUTES);

        backgroundExecutorService = new ScheduledThreadPoolExecutor(1);
        backgroundExecutorService.setRemoveOnCancelPolicy(true);
        backgroundExecutorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

        // Engine needs to be registered before the update factories can find it.  Push the check to the background executor
        backgroundExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                if (UpdateFactory.getUpdateOnStartup()) {
                    startSecuritiesUpdate(SCHEDULED_DELAY);
                }

                if (CurrencyUpdateFactory.getUpdateOnStartup()) {
                    startExchangeRateUpdate(SCHEDULED_DELAY);
                }
            }
        }, 30, TimeUnit.SECONDS);
    }

    /**
     * Registers a {@code Handler} with the class logger.
     * This also ensures the static logger is initialized.
     * @param handler {@code Handler} to register
     */
    public static void addLogHandler(final Handler handler) {
        logger.addHandler(handler);
    }

    /**
     * Initiates a background exchange rate update with a given start delay
     *
     * @param delay delay in seconds
     * @return {@code Future} for background task
     */
    public Future<Void> startExchangeRateUpdate(final int delay) {
        return backgroundExecutorService.schedule(new BackgroundCallable<>(new CurrencyUpdateFactory.UpdateExchangeRatesCallable()), delay,
                TimeUnit.SECONDS);
    }

    /**
     * Initiates a background securities history update with a given start delay
     *
     * @param delay delay in seconds
     */
    public void startSecuritiesUpdate(final int delay) {
        final List<ScheduledFuture<Boolean>> futures = new ArrayList<>();

        // Load of the scheduler with the tasks and save the futures
        for (final SecurityNode securityNode : getSecurities()) {
            if (securityNode.getQuoteSource() != QuoteSource.NONE) { // failure will occur if source is not defined
                futures.add(backgroundExecutorService.schedule(new BackgroundCallable<>(new UpdateFactory.UpdateSecurityNodeCallable(securityNode)),
                        delay, TimeUnit.SECONDS));
            }
        }

        // Cleanup thread that monitor for excess network connection failures
        new Thread() {
            public void run() {
                short errorCount = 0;

                // Wait for completion of each task and if too many errors occur, cancel all of them
                for (final ScheduledFuture<Boolean> future : futures) {
                    try {
                        if (!future.get(1, TimeUnit.MINUTES)) {
                            errorCount++;
                        }
                    } catch (final InterruptedException | ExecutionException | TimeoutException e) {
                        logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                        errorCount++;
                    } catch (final CancellationException e) {
                        errorCount = Short.MAX_VALUE;   // force a failure
                        break; // futures are being canceled externally, exit the thread
                    }

                    if (errorCount > MAX_ERRORS) {
                        break;
                    }
                }

                if (errorCount > MAX_ERRORS) {
                    for (final ScheduledFuture<Boolean> future : futures) {
                        future.cancel(false);
                    }
                }
            }
        }.start();
    }

    /**
     * Returns the most current known market price for a requested date.  The {@code SecurityNode} history will be
     * searched for an exact match first.  If an exact match is not found, investment transactions will be searched
     * for the closest requested date.  {@code SecurityHistoryNode} history values will take precedent over
     * a transaction with the same closest or matching date.
     *
     * @param transactions Collection of transactions utilizing the requested investment
     * @param node         {@code SecurityNode} we want a price for
     * @param baseCurrency {@code CurrencyNode} reporting currency
     * @param date         {@code Date} we want a market price for
     * @return The best market price or a value of 0 if no history or transactions exist
     */
    public static BigDecimal getMarketPrice(final Collection<Transaction> transactions, final SecurityNode node, final CurrencyNode baseCurrency, final Date date) {

        final Date marketDate = DateUtils.trimDate(date);

        // Search for the exact history node record
        SecurityHistoryNode hNode = node.getHistoryNode(marketDate);

        // not null, must be an exact match, return the value because it has precedence
        if (hNode != null) {
            return node.getMarketPrice(marketDate, baseCurrency);
        }

        // Nothing found yet, continue searching for something better
        Date priceDate = new Date(0);
        BigDecimal price = BigDecimal.ZERO;

        hNode = node.getClosestHistoryNode(marketDate);

        if (hNode != null) {    // Closest option so far
            price = hNode.getPrice();
            priceDate = hNode.getDate();
        }

        // Compare against transactions
        for (final Transaction t : transactions) {
            if (t instanceof InvestmentTransaction && ((InvestmentTransaction) t).getSecurityNode() == node) {

                // The transaction date must be closer than the history node, but not newer than the request date
                if ((t.getDate().after(priceDate) && t.getDate().before(marketDate)) || t.getDate().equals(marketDate)) {

                    // Check for a dividend, etc that may have returned a price of zero
                    final BigDecimal p = ((InvestmentTransaction) t).getPrice();

                    if (p != null && p.compareTo(BigDecimal.ZERO) == 1) {
                        price = p;
                        priceDate = t.getDate();
                    }
                }
            }
        }

        // Get the current exchange rate for the security node
        final BigDecimal rate = node.getReportedCurrencyNode().getExchangeRate(baseCurrency);

        // return the price and factor in the exchange rate
        return price.multiply(rate);
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

    /**
     * Creates a RootAccount and default currency only if necessary
     */
    private void initialize() {

        commodityLock.writeLock().lock();
        accountLock.writeLock().lock();

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
                    node = DefaultCurrencies.getDefault();
                    node.setExchangeRateDAO(exchangeRateDAO);

                    addCurrency(node); // force the node to persisted
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
            accountLock.writeLock().unlock();
            commodityLock.writeLock().unlock();
        }

        logInfo("Engine initialization is complete");
    }

    /**
     * Corrects minor issues with a database that may occur because of prior bugs or file format upgrades
     */
    @SuppressWarnings("ConstantConditions")
    private void checkAndCorrect() {

        commodityLock.writeLock().lock();
        accountLock.writeLock().lock();
        configLock.writeLock().lock();

        try {
            /* Check for more than one config object */
            List<Config> list = eDAO.getStoredObjects(Config.class);
            if (list.size() > 1) {
                // Delete all but the first found config object
                for (int i = 1; i < list.size(); i++) {
                    moveObjectToTrash(list.get(i));
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

            // migrate amortization object to new storage format and remove and orphaned transactions from removal and modifications of reminders
            if (getConfig().getFileVersion() < 2.3f) {
                migrateAmortizeObjects();
                removeOrphanedTransactions();
            }

            // check for improperly set default currency
            if (getDefaultCurrency() == null) {
                setDefaultCurrency(getRootAccount().getCurrencyNode());
                logger.warning("Forcing default currency");
            }

            // purge stale budget goals for place holder accounts
            if (getConfig().getFileVersion() < 2.14f) {
                for (final Account account : getAccountList()) {
                    if (account.isPlaceHolder()) {
                        purgeBudgetGoal(account);
                    }
                }
            }

            // if the file version is not current, then update it
            if (getConfig().getFileVersion() != CURRENT_VERSION) {
                Config config = getConfig();
                config.setFileVersion(CURRENT_VERSION);
                getConfigDAO().update(config);
            }
        } finally {
            configLock.writeLock().unlock();
            accountLock.writeLock().unlock();
            commodityLock.writeLock().unlock();
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

    /**
     * Search and remove orphaned transactions left behind when reminders were removed
     */
    private void removeOrphanedTransactions() {
        for (final Transaction transaction : getTransactions()) {
            boolean orphaned = true;

            for (final Account account : transaction.getAccounts()) {
                if (account.contains(transaction)) {
                    orphaned = false;
                    break;
                }
            }

            if (orphaned) { // still an orphan, check for reminder ownership
                for (final Reminder reminder : getReminders()) {
                    if (reminder.contains(transaction)) {    // reminder transaction may be null
                        orphaned = false;
                        break;
                    }
                }
            }

            if (orphaned) {
                moveObjectToTrash(transaction);
                logInfo("Removed an orphan transaction");
            }
        }
    }

    private void removeExchangeRate(final ExchangeRate rate) {

        commodityLock.writeLock().lock();

        try {
            for (ExchangeRateHistoryNode node : rate.getHistory()) {
                removeExchangeRateHistory(rate, node);
            }
            moveObjectToTrash(rate);
        } finally {
            commodityLock.writeLock().unlock();
        }
    }

    void stopBackgroundServices() {
        logInfo("Controlled engine shutdown initiated");

        shutDownAndWait(backgroundExecutorService);
        shutDownAndWait(trashExecutor);
    }

    void shutdown() {
        eDAO.shutdown();
    }

    private void shutDownAndWait(final ExecutorService executorService) {
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                executorService.shutdownNow();

                if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                    logSevere("Unable to shutdown background service");
                }
            }

        } catch (final InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();

            logger.log(Level.FINEST, e.getLocalizedMessage(), e);
        }
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

    private boolean moveObjectToTrash(final Object object) {
        boolean result = false;

        engineLock.writeLock().lock();

        try {
            if (object instanceof StoredObject) {
                getTrashDAO().add(new TrashObject((StoredObject) object));
                result = true;
            } else {    // simple object with an annotated JPA entity id of type long is assumed
                getTrashDAO().addEntityTrash(object);
                result = true;
            }
        } catch (final Exception ex) {
            logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        } finally {
            engineLock.writeLock().unlock();
        }

        return result;
    }

    /**
     * Empty the trash if any objects are older than the defined time
     */
    private void emptyTrash() {

        messageBus.fireEvent(new Message(MessageChannel.SYSTEM, ChannelEvent.BACKGROUND_PROCESS_STARTED, this));

        engineLock.writeLock().lock();

        try {
            logger.info("Checking for trash");

            final List<TrashObject> trash = getTrashDAO().getTrashObjects();

            /* always sort by the timestamp of the trash object to prevent
             * foreign key removal exceptions when multiple related accounts
             * or objects are removed */
            Collections.sort(trash);

            if (trash.isEmpty()) {
                logger.info("No trash was found");
            }

            final long now = new Date().getTime();

            for (TrashObject o : trash) {
                if (now - o.getDate().getTime() >= MAXIMUM_TRASH_AGE) {
                    getTrashDAO().remove(o);
                }
            }
        } finally {
            engineLock.writeLock().unlock();

            messageBus.fireEvent(new Message(MessageChannel.SYSTEM, ChannelEvent.BACKGROUND_PROCESS_STOPPED, this));
        }
    }

    public boolean addReminder(final Reminder reminder) {
        Objects.requireNonNull(reminder.getUuid());

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

            if (reminder.getTransaction() != null) {
                moveObjectToTrash(reminder.getTransaction());
                reminder.setTransaction(null);
            }

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

    public Reminder getReminderByUuid(final String uuid) {
        return getReminderDAO().getReminderByUuid(uuid);
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

    public <T extends StoredObject> T getStoredObjectByUuid(final Class<T> tClass, final String uuid) {
        return eDAO.getObjectByUuid(tClass, uuid);
    }

    /**
     * Returns a {@code Collection} of all {@code StoredObjects} in a consistent order.
     * {@code StoredObjects} marked for removal and {@code TrashObjects} are filtered from the collection.
     *
     * @return {@code Collection} of {@code StoredObjects}
     * @see Collection
     * @see StoredObjectComparator
     */
    public Collection<StoredObject> getStoredObjects() {

        engineLock.readLock().lock();

        try {

            List<StoredObject> objects = eDAO.getStoredObjects();

            for (Iterator<StoredObject> i = objects.iterator(); i.hasNext(); ) {
                StoredObject o = i.next();

                if (o instanceof TrashObject || o.isMarkedForRemoval()) {
                    i.remove();
                }
            }

            Collections.sort(objects, new StoredObjectComparator());

            return objects;
        } finally {
            engineLock.readLock().unlock();
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

        if (node.getSymbol() == null || node.getSymbol().isEmpty()) {
            result = false;
            logSevere("Commodity symbol was not valid");
        }

        if (node.getScale() < 0) {
            result = false;
            logSevere("Commodity " + node + " had a scale less than zero");
        }

        if (node instanceof SecurityNode && ((SecurityNode) node).getReportedCurrencyNode() == null) {
            result = false;
            logSevere("Commodity " + node + " was not assigned a currency");
        }

        // ensure the UUID being used is unique
        if (eDAO.getObjectByUuid(CommodityNode.class, node.getUuid()) != null) {
            result = false;
            logSevere("Commodity " + node + " was not unique");
        }

        return result;
    }

    /**
     * Adds a new CurrencyNode to the data set.
     * <p/>
     * Checks and prevents the addition of a duplicate Currencies.
     *
     * @param node new CurrencyNode to add
     * @return {@code true} if the add it successful
     */
    public boolean addCurrency(final CurrencyNode node) {

        commodityLock.writeLock().lock();

        try {
            boolean status = isCommodityNodeValid(node);

            if (status) {
                node.setExchangeRateDAO(exchangeRateDAO);

                if (getCurrency(node.getSymbol()) != null) {
                    logger.log(Level.INFO, "Prevented addition of a duplicate CurrencyNode: {0}", node.getSymbol());
                    status = false;
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
            commodityLock.writeLock().unlock();
        }
    }

    /**
     * Adds a new SecurityNode to the data set.
     * <p/>
     * Checks and prevents the addition of a duplicate SecurityNode.
     *
     * @param node new SecurityNode to add
     * @return {@code true} if the add it successful
     */
    public boolean addSecurity(final SecurityNode node) {
        commodityLock.writeLock().lock();

        try {
            boolean status = isCommodityNodeValid(node);

            if (status) {
                if (getSecurity(node.getSymbol()) != null) {
                    logger.log(Level.INFO, "Prevented addition of a duplicate SecurityNode: {0}", node.getSymbol());
                    status = false;
                }
            }

            if (status) {
                status = getCommodityDAO().addCommodity(node);
                logger.log(Level.FINE, "Adding: {0}", node.toString());
            }

            Message message;
            if (status) {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.SECURITY_ADD, this);
            } else {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.SECURITY_ADD_FAILED, this);
            }

            message.setObject(MessageProperty.COMMODITY, node);
            messageBus.fireEvent(message);

            return status;
        } finally {
            commodityLock.writeLock().unlock();
        }
    }

    /**
     * Add a SecurityHistoryNode node to a SecurityNode.  If the SecurityNode already contains
     * an equivalent SecurityHistoryNode, the old SecurityHistoryNode is removed first.
     *
     * @param node  SecurityNode to add to
     * @param hNode SecurityHistoryNode to add
     * @return <tt>true</tt> if successful
     */
    public boolean addSecurityHistory(@NotNull final SecurityNode node, @NotNull final SecurityHistoryNode hNode) {

        // Remove old history of the same date if it exists
        if (node.contains(hNode.getDate())) {
            if (!removeSecurityHistory(node, hNode.getDate())) {
                logSevere(rb.getString("Message.Error.HistRemoval", hNode.getDate(), node.getSymbol()));
                return false;
            }
        }

        commodityLock.writeLock().lock();

        try {
            boolean status = node.addHistoryNode(hNode);

            if (status) {
                status = getCommodityDAO().addSecurityHistory(node, hNode);
            }

            Message message;

            if (status) {
                clearCachedAccountBalance(node);
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.SECURITY_HISTORY_ADD, this);
            } else {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.SECURITY_HISTORY_ADD_FAILED, this);
            }

            message.setObject(MessageProperty.COMMODITY, node);
            messageBus.fireEvent(message);

            return status;
        } finally {
            commodityLock.writeLock().unlock();
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
     * Clears an {@code Accounts} cached balance and recursively works up the tree to the root.
     *
     * @param account {@code Account} to clear
     */
    private void clearCachedAccountBalance(final Account account) {

        accountLock.writeLock().lock();

        try {
            account.clearCachedBalances();
            getAccountDAO().updateAccount(account);
        } finally {
            accountLock.writeLock().unlock();
        }

        if (account.getParent() != null && account.getParent().getAccountType() != AccountType.ROOT) {
            clearCachedAccountBalance(account.getParent());
        }
    }

    CurrencyNode[] getBaseCurrencies(final String exchangeRateId) {

        commodityLock.readLock().lock();

        try {
            List<CurrencyNode> currencies = getCurrencies();

            Collections.sort(currencies);
            Collections.reverse(currencies);

            for (CurrencyNode node1 : currencies) {
                for (CurrencyNode node2 : currencies) {
                    if (node1 != node2 && buildExchangeRateId(node1, node2).equals(exchangeRateId)) {
                        return new CurrencyNode[]{node1, node2};
                    }
                }
            }
            return null;
        } finally {
            commodityLock.readLock().unlock();
        }
    }

    /**
     * Returns an array of currencies being used in accounts
     *
     * @return Set of CurrencyNodes
     */
    public Set<CurrencyNode> getActiveCurrencies() {

        commodityLock.readLock().lock();

        try {
            return getCommodityDAO().getActiveCurrencies();
        } finally {
            commodityLock.readLock().unlock();
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

        commodityLock.readLock().lock();

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
            commodityLock.readLock().unlock();
        }
    }

    public List<CurrencyNode> getCurrencies() {

        commodityLock.readLock().lock();

        try {
            return getCommodityDAO().getCurrencies();
        } finally {
            commodityLock.readLock().unlock();
        }
    }

    public CurrencyNode getCurrencyNodeByUuid(final String uuid) {
        return getCommodityDAO().getCurrencyByUuid(uuid);
    }

    public List<SecurityHistoryNode> getSecurityHistory(final SecurityNode node) {

        commodityLock.readLock().lock();

        try {
            return node.getHistoryNodes();
        } finally {
            commodityLock.readLock().unlock();
        }
    }

    public ExchangeRate getExchangeRate(final CurrencyNode baseCurrency, final CurrencyNode exchangeCurrency) {

        commodityLock.readLock().lock();

        try {
            return exchangeRateDAO.getExchangeRateNode(baseCurrency, exchangeCurrency);
        } finally {
            commodityLock.readLock().unlock();
        }
    }

    public ExchangeRate getExchangeRateByUuid(final String uuid) {
        return getCommodityDAO().getExchangeRateByUuid(uuid);
    }

    public List<SecurityNode> getSecurities() {

        commodityLock.readLock().lock();

        try {
            return getCommodityDAO().getSecurities();
        } finally {
            commodityLock.readLock().unlock();
        }
    }

    /**
     * Find a SecurityNode given it's symbol
     *
     * @param symbol symbol of security to find
     * @return null if not found
     */
    public SecurityNode getSecurity(final String symbol) {

        commodityLock.readLock().lock();

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
            commodityLock.readLock().unlock();
        }
    }

    public SecurityNode getSecurityNodeByUuid(final String uuid) {
        return getCommodityDAO().getSecurityByUuid(uuid);
    }

    private boolean isCommodityNodeUsed(final CommodityNode node) {

        commodityLock.readLock().lock();
        accountLock.readLock().lock();

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
            accountLock.readLock().unlock();
            commodityLock.readLock().unlock();
        }

        return false;
    }

    public boolean removeCommodity(final CurrencyNode node) {
        boolean status = true;

        commodityLock.writeLock().lock();

        try {
            if (isCommodityNodeUsed(node)) {
                status = false;
            } else {
                clearObsoleteExchangeRates();
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
            commodityLock.writeLock().unlock();
        }
    }

    public boolean removeSecurity(final SecurityNode node) {
        boolean status = true;

        commodityLock.writeLock().lock();

        try {
            if (isCommodityNodeUsed(node)) {
                status = false;
            } else {
                // Remove all history nodes first so they are not left behind

                List<SecurityHistoryNode> hNodes = node.getHistoryNodes();

                for (SecurityHistoryNode hNode : hNodes) {
                    if (!removeSecurityHistory(node, hNode.getDate())) {
                        logSevere(rb.getString("Message.Error.HistRemoval", hNode.getDate(), node.getSymbol()));
                    }
                }
                moveObjectToTrash(node);
            }

            Message message;
            if (status) {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.SECURITY_REMOVE, this);
            } else {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.SECURITY_REMOVE_FAILED, this);
            }
            message.setObject(MessageProperty.COMMODITY, node);
            messageBus.fireEvent(message);

            return status;

        } finally {
            commodityLock.writeLock().unlock();
        }
    }

    /**
     * Remove a {@code SecurityHistoryNode} given a {@code Date}
     *
     * @param node {@code SecurityNode} to remove history from
     * @param date the search {@code Date}
     * @return {@code true} if a {@code SecurityHistoryNode} was found and removed
     */
    public boolean removeSecurityHistory(@NotNull final SecurityNode node, @NotNull final Date date) {

        commodityLock.writeLock().lock();

        boolean status = false;

        try {
            final SecurityHistoryNode historyNode = node.getHistoryNode(date);

            if (historyNode != null) {
                status = node.removeHistoryNode(date);

                if (status) {   // removal was a success, make sure we cleanup properly
                    moveObjectToTrash(historyNode);
                    status = getCommodityDAO().removeSecurityHistory(node, historyNode);
                }
            }

            Message message;

            if (status) {
                clearCachedAccountBalance(node);
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.SECURITY_HISTORY_REMOVE, this);
            } else {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.SECURITY_HISTORY_REMOVE_FAILED, this);
            }

            message.setObject(MessageProperty.COMMODITY, node);
            messageBus.fireEvent(message);

            return status;
        } finally {
            commodityLock.writeLock().unlock();
        }
    }

    private Config getConfig() {

        configLock.readLock().lock();

        try {
            if (config == null) {
                config = getConfigDAO().getDefaultConfig();
            }
            return config;

        } finally {
            configLock.readLock().unlock();
        }
    }

    public CurrencyNode getDefaultCurrency() {

        configLock.readLock().lock();
        commodityLock.readLock().lock();

        try {
            CurrencyNode node = getConfig().getDefaultCurrency();

            if (node == null) {
                logger.warning("No default currency assigned");
            }

            return node;
        } finally {
            commodityLock.readLock().unlock();
            configLock.readLock().unlock();
        }
    }

    public void setDefaultCurrency(final CurrencyNode defaultCurrency) {

        // make sure the new default is persisted if it has not been
        if (!isStored(defaultCurrency)) {
            addCurrency(defaultCurrency);
        }

        accountLock.writeLock().lock();
        configLock.writeLock().lock();
        commodityLock.writeLock().lock();

        try {
            Config config = getConfig();
            config.setDefaultCurrency(defaultCurrency);
            getConfigDAO().update(config);

            logInfo("Setting default currency: " + defaultCurrency);

            Message message = new Message(MessageChannel.CONFIG, ChannelEvent.CONFIG_MODIFY, this);
            message.setObject(MessageProperty.CONFIG, config);
            messageBus.fireEvent(message);

            Account root = getRootAccount();

            // The root account holds a reference to the default currency
            root.setCurrencyNode(defaultCurrency);
            getAccountDAO().updateAccount(root);

            message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_MODIFY, this);
            message.setObject(MessageProperty.ACCOUNT, root);
            messageBus.fireEvent(message);
        } finally {
            commodityLock.writeLock().unlock();
            configLock.writeLock().unlock();
            accountLock.writeLock().unlock();
        }
    }

    public void setExchangeRate(final CurrencyNode baseCurrency, final CurrencyNode exchangeCurrency, final BigDecimal rate) {

        setExchangeRate(baseCurrency, exchangeCurrency, rate, new Date());
    }

    public void setExchangeRate(final CurrencyNode baseCurrency, final CurrencyNode exchangeCurrency, final BigDecimal rate, final Date date) {
        Objects.requireNonNull(rate);

        assert rate.compareTo(BigDecimal.ZERO) > 0;

        if (baseCurrency.equals(exchangeCurrency)) {
            return;
        }

        // find the correct ExchangeRate and create if needed
        ExchangeRate exchangeRate = getExchangeRate(baseCurrency, exchangeCurrency);

        if (exchangeRate == null) {
            exchangeRate = new ExchangeRate(buildExchangeRateId(baseCurrency, exchangeCurrency));
            getCommodityDAO().addExchangeRate(exchangeRate);
        }

        // Remove old history of the same date if it exists
        if (exchangeRate.contains(date)) {
            removeExchangeRateHistory(exchangeRate, exchangeRate.getHistory(date));
        }

        commodityLock.writeLock().lock();

        try {
            // create the new history node
            ExchangeRateHistoryNode historyNode;

            if (baseCurrency.getSymbol().compareToIgnoreCase(exchangeCurrency.getSymbol()) > 0) {
                historyNode = new ExchangeRateHistoryNode(date, rate);
            } else {
                historyNode = new ExchangeRateHistoryNode(date, BigDecimal.ONE.divide(rate, MathConstants.mathContext));
            }

            Message message;

            boolean result = false;

            if (exchangeRate.addHistoryNode(historyNode)) {
                result = getCommodityDAO().addExchangeRateHistory(exchangeRate);
            }

            if (result) {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.EXCHANGE_RATE_ADD, this);
            } else {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.EXCHANGE_RATE_ADD_FAILED, this);
            }

            message.setObject(MessageProperty.EXCHANGE_RATE, exchangeRate);

            messageBus.fireEvent(message);
        } finally {
            commodityLock.writeLock().unlock();
        }
    }

    public void removeExchangeRateHistory(final ExchangeRate exchangeRate, final ExchangeRateHistoryNode history) {

        commodityLock.writeLock().lock();

        try {
            Message message;

            boolean result = false;

            if (exchangeRate.contains(history)) {
                if (exchangeRate.removeHistoryNode(history)) {
                    moveObjectToTrash(history);
                    result = getCommodityDAO().removeExchangeRateHistory(exchangeRate);
                }
            }

            if (result) {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.EXCHANGE_RATE_REMOVE, this);
            } else {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.EXCHANGE_RATE_REMOVE_FAILED, this);
            }

            message.setObject(MessageProperty.EXCHANGE_RATE, exchangeRate);
            messageBus.fireEvent(message);
        } finally {
            commodityLock.writeLock().unlock();
        }
    }

    /**
     * Modifies an existing currency node in place. The supplied node should not be a reference to the original
     *
     * @param oldNode      old CommodityNode
     * @param templateNode template CommodityNode
     * @return true if successful
     */
    public boolean updateCommodity(final CommodityNode oldNode, final CommodityNode templateNode) {
        Objects.requireNonNull(oldNode);
        Objects.requireNonNull(templateNode);

        assert oldNode != templateNode;

        commodityLock.writeLock().lock();

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

            if (templateNode instanceof SecurityNode) {
                if (status) {
                    message = new Message(MessageChannel.COMMODITY, ChannelEvent.SECURITY_MODIFY, this);
                    message.setObject(MessageProperty.COMMODITY, oldNode);
                } else {
                    message = new Message(MessageChannel.COMMODITY, ChannelEvent.SECURITY_MODIFY_FAILED, this);
                    message.setObject(MessageProperty.COMMODITY, templateNode);
                }
            } else {
                if (status) {
                    message = new Message(MessageChannel.COMMODITY, ChannelEvent.CURRENCY_MODIFY, this);
                    message.setObject(MessageProperty.COMMODITY, oldNode);
                } else {
                    message = new Message(MessageChannel.COMMODITY, ChannelEvent.CURRENCY_MODIFY_FAILED, this);
                    message.setObject(MessageProperty.COMMODITY, templateNode);
                }
            }

            messageBus.fireEvent(message);
            return status;
        } finally {
            commodityLock.writeLock().unlock();
        }
    }

    public boolean updateReminder(final Reminder reminder) {
        return getReminderDAO().updateReminder(reminder);
    }

    public String getAccountSeparator() {

        configLock.readLock().lock();

        try {
            if (accountSeparator == null) {
                accountSeparator = getConfig().getAccountSeparator();
            }
            return accountSeparator;

        } finally {
            configLock.readLock().unlock();
        }
    }

    public void setAccountSeparator(final String separator) {

        configLock.writeLock().lock();

        try {
            accountSeparator = separator;
            Config config = getConfig();

            config.setAccountSeparator(separator);

            getConfigDAO().update(config);

            Message message = new Message(MessageChannel.CONFIG, ChannelEvent.CONFIG_MODIFY, this);
            message.setObject(MessageProperty.CONFIG, config);

            messageBus.fireEvent(message);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
     * Returns a list of all Accounts excluding the rootAccount
     *
     * @return List of accounts
     */
    public List<Account> getAccountList() {
        final List<Account> accounts = getAccountDAO().getAccountList();
        accounts.remove(getRootAccount());

        return accounts;
    }

    public Account getAccountByUuid(final String id) {
        return getAccountDAO().getAccountByUuid(id);
    }

    /**
     * Search for an account with a matching account name
     *
     * @param accountName Account name to search for. <b>Must not be null</b>
     * @return The matching account. {@code null} if not found.
     */
    public Account getAccountByName(final String accountName) {
        Objects.requireNonNull(accountName);

        final List<Account> list = getAccountList();

        // sort for consistent search order
        Collections.sort(list);

        for (final Account account : list) {
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

        for (final Account account : list) {
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

    public void refresh(final StoredObject object) {
        eDAO.refresh(object);
    }

    /**
     * Adds a new account
     *
     * @param parent The parent account
     * @param child  A new Account object
     * @return true if successful
     */
    public boolean addAccount(final Account parent, final Account child) {
        Objects.requireNonNull(child);
        Objects.requireNonNull(child.getUuid());

        if (child.getAccountType() == AccountType.ROOT) {
            throw new RuntimeException("Invalid Account");
        }

        accountLock.writeLock().lock();

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
            accountLock.writeLock().unlock();
        }
    }

    /**
     * Return the root account
     *
     * @return RootAccount
     */
    public RootAccount getRootAccount() {

        accountLock.readLock().lock();

        try {
            if (rootAccount == null) {
                rootAccount = getAccountDAO().getRootAccount();
            }
            return rootAccount;
        } finally {
            accountLock.readLock().unlock();
        }
    }

    /**
     * Move an account to a new parent account
     *
     * @param account   account to move
     * @param newParent the new parent account
     * @return true if successful
     */
    public boolean moveAccount(final Account account, final Account newParent) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(newParent);

        accountLock.writeLock().lock();

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
            accountLock.writeLock().unlock();
        }
    }

    /**
     * Modifies an existing account given an account as a template. The type of the account cannot be changed.
     *
     * @param template The Account object to use as a template
     * @param account  The existing account
     * @return true if successful
     */
    public boolean modifyAccount(final Account template, final Account account) {

        boolean result;
        Message message;

        accountLock.writeLock().lock();

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
            account.setAccountCode(template.getAccountCode());

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

            // Force clearing of any budget goals if an empty account has been changed to become a place holder
            if (account.isPlaceHolder()) {
                purgeBudgetGoal(account);
            }

            return result;
        } finally {
            accountLock.writeLock().unlock();
        }
    }

    /**
     * Purges any {@code BudgetGoal} associated with an account
     *
     * @param account {@code Account} to remove all associated budget goal history
     */
    private void purgeBudgetGoal(@NotNull final Account account) {
        // clear budget history
        for (final Budget budget : getBudgetList()) {
            budget.removeBudgetGoal(account);
            if (!updateBudget(budget)) {
                logWarning("Unable to remove account goals from the budget");
            }
        }
    }

    /**
     * Sets the account number of an account
     *
     * @param account account to change
     * @param number  new account number
     */
    public void setAccountNumber(final Account account, final String number) {

        accountLock.writeLock().lock();

        try {
            account.setAccountNumber(number);
            getAccountDAO().updateAccount(account);

            Message message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_MODIFY, this);
            message.setObject(MessageProperty.ACCOUNT, account);
            messageBus.fireEvent(message);

            logInfo(rb.getString("Message.AccountModify"));
        } finally {
            accountLock.writeLock().unlock();
        }
    }

    /**
     * Sets an attribute for an {@code Account}.  The key and values are string based
     *
     * @param account {@code Account} to add or update an attribute
     * @param key     the key for the attribute
     * @param value   the value of the attribute
     */
    public void setAccountAttribute(final Account account, @NotNull final String key, @Nullable final String value) {
        // Throw an error if the value exceeds the maximum length
        if (value != null && value.length() > Account.MAX_ATTRIBUTE_LENGTH) {
            Message message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_MODIFY_FAILED, this);
            message.setObject(MessageProperty.ACCOUNT, account);
            messageBus.fireEvent(message);

            logInfo("The maximum length of the attribute was exceeded");

            return;
        }

        accountLock.writeLock().lock();

        try {
            account.setAttribute(key, value);
            getAccountDAO().updateAccount(account);

            Message message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_ATTRIBUTE_MODIFY, this);
            message.setObject(MessageProperty.ACCOUNT, account);
            messageBus.fireEvent(message);

            logInfo(rb.getString("Message.AccountModify"));
        } finally {
            accountLock.writeLock().unlock();
        }
    }

    /**
     * Removes an existing account given it's ID.
     *
     * @param account The account to remove
     * @return true if successful
     */
    public boolean removeAccount(final Account account) {

        accountLock.writeLock().lock();

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
                        purgeBudgetGoal(account);
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
            accountLock.writeLock().unlock();
        }
    }

    public Future<Path> getAttachment(final String attachment) {
        return attachmentManager.getAttachment(attachment);
    }

    public boolean addAttachment(final Path path, final boolean copy) {
        boolean result = false;

        try {
            result = attachmentManager.addAttachment(path, copy);
        } catch (IOException e) {
            logSevere(e.getLocalizedMessage());
        }

        return result;
    }

    public boolean removeAttachment(final String attachment) {
        return attachmentManager.removeAttachment(attachment);
    }

    @SuppressWarnings("deprecation")
    private void migrateAmortizeObjects() {

        accountLock.writeLock().lock();

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
            accountLock.writeLock().unlock();
        }
    }

    /**
     * Sets the amortize object of an account
     *
     * @param account        The Liability account to change
     * @param amortizeObject the new AmortizeObject
     * @return true if successful
     */
    public boolean setAmortizeObject(final Account account, final AmortizeObject amortizeObject) {

        accountLock.writeLock().lock();

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
            accountLock.writeLock().unlock();
        }
    }

    /**
     * Toggles the visibility of an account given its ID.
     *
     * @param account The account to toggle visibility
     */
    public void toggleAccountVisibility(final Account account) {

        accountLock.writeLock().lock();

        try {
            Message message;

            account.setVisible(!account.isVisible());

            if (getAccountDAO().toggleAccountVisibility(account)) {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_VISIBILITY_CHANGE, this);
                message.setObject(MessageProperty.ACCOUNT, account);
                messageBus.fireEvent(message);
            } else {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_VISIBILITY_CHANGE_FAILED, this);
                message.setObject(MessageProperty.ACCOUNT, account);
                messageBus.fireEvent(message);
            }
        } finally {
            accountLock.writeLock().unlock();
        }
    }

    /**
     * Adds a SecurityNode from a InvestmentAccount
     *
     * @param account destination account
     * @param node    SecurityNode to add
     * @return true if add was successful
     */
    private boolean addAccountSecurity(final Account account, final SecurityNode node) {

        accountLock.writeLock().lock();
        commodityLock.writeLock().lock();

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
            commodityLock.writeLock().unlock();
            accountLock.writeLock().unlock();
        }
    }

    /**
     * Removes a SecurityNode from an InvestmentAccount
     *
     * @param account Account to remove SecurityNode from
     * @param node    SecurityNode to remove
     * @return true if successful
     */
    private boolean removeAccountSecurity(final Account account, final SecurityNode node) {
        Objects.requireNonNull(node);

        accountLock.writeLock().lock();
        commodityLock.writeLock().lock();

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
            commodityLock.writeLock().unlock();
            accountLock.writeLock().unlock();
        }
    }

    /**
     * Update an account's securities list. This compares the old list of securities and the supplied list and adds or
     * removes securities to make sure the lists are the same.
     *
     * @param acc  Destination account
     * @param list Collection of SecurityNodes
     * @return true if successful
     */
    public boolean updateAccountSecurities(final Account acc, final Collection<SecurityNode> list) {

        boolean result = true;

        if (acc.memberOf(AccountGroup.INVEST)) {

            accountLock.writeLock().lock();
            commodityLock.writeLock().lock();

            try {
                final Collection<SecurityNode> oldList = acc.getSecurities();

                for (SecurityNode node : oldList) {
                    if (!list.contains(node)) {
                        if (!removeAccountSecurity(acc, node)) {
                            logWarning(rb.getString("Message.Error.SecurityAccountRemove", node.toString(), acc.getName()));
                            result = false;
                        }
                    }
                }

                for (SecurityNode node : list) {
                    if (!oldList.contains(node)) {
                        if (!addAccountSecurity(acc, node)) {
                            logWarning(rb.getString("Message.Error.SecurityAccountRemove", node.toString(), acc.getName()));
                            result = false;
                        }
                    }
                }
            } finally {
                commodityLock.writeLock().unlock();
                accountLock.writeLock().unlock();
            }
        }

        return result;
    }

    public boolean addBudget(final Budget budget) {

        boolean result;

        budgetLock.writeLock().lock();

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
            budgetLock.writeLock().unlock();
        }
    }

    public boolean removeBudget(final Budget budget) {

        boolean result = false;

        budgetLock.writeLock().lock();

        try {
            moveObjectToTrash(budget);

            Message message = new Message(MessageChannel.BUDGET, ChannelEvent.BUDGET_REMOVE, this);

            message.setObject(MessageProperty.BUDGET, budget);
            messageBus.fireEvent(message);

            result = true;
        } catch (final Exception ex) {
            logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        } finally {
            budgetLock.writeLock().unlock();
        }

        return result;
    }

    public void updateBudgetGoals(final Budget budget, final Account account, final BudgetGoal newGoals) {

        budgetLock.writeLock().lock();

        try {
            BudgetGoal oldGoals = budget.getBudgetGoal(account);

            budget.setBudgetGoal(account, newGoals);

            moveObjectToTrash(oldGoals);    // need to keep the old goal around, will be cleaned up later, orphan removal causes refresh issues

            updateBudgetGoals(budget, account);
        } finally {
            budgetLock.writeLock().unlock();
        }
    }

    private void updateBudgetGoals(final Budget budget, final Account account) {

        budgetLock.writeLock().lock();

        try {
            Message message;

            boolean result = getBudgetDAO().update(budget);

            if (result) {
                message = new Message(MessageChannel.BUDGET, ChannelEvent.BUDGET_GOAL_UPDATE, this);
            } else {
                message = new Message(MessageChannel.BUDGET, ChannelEvent.BUDGET_GOAL_UPDATE_FAILED, this);
            }

            message.setObject(MessageProperty.BUDGET, budget);
            message.setObject(MessageProperty.ACCOUNT, account);

            messageBus.fireEvent(message);

            logger.log(Level.FINE, "Budget goal updated for {0}", account.getPathName());
        } finally {
            budgetLock.writeLock().unlock();
        }
    }

    public boolean updateBudget(final Budget budget) {

        boolean result;

        budgetLock.writeLock().lock();

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
            budgetLock.writeLock().unlock();
        }
    }

    public List<Budget> getBudgetList() {

        budgetLock.readLock().lock();

        try {
            return getBudgetDAO().getBudgets();
        } finally {
            budgetLock.readLock().unlock();
        }
    }

    public Budget getBudgetByUuid(final String uuid) {
        return getBudgetDAO().getBudgetByUuid(uuid);
    }

    public boolean isTransactionValid(final Transaction transaction) {

        for (Account a : transaction.getAccounts()) {
            if (a.isLocked()) {
                logWarning(rb.getString("Message.TransactionAccountLocked"));
                return false;
            }
        }

        if (transaction.isMarkedForRemoval()) {
            logger.log(Level.SEVERE, "Transaction already marked for removal", new Throwable());
            return false;
        }

        if (eDAO.getObjectByUuid(Transaction.class, transaction.getUuid()) != null) {
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

        return transaction.getTransactionType() != TransactionType.INVALID;
    }

    /**
     * Determine if a StoredObject is persisted in the database
     *
     * @param object StoredObject to check
     * @return true if persisted
     */
    public boolean isStored(final StoredObject object) {
        return eDAO.getObjectByUuid(StoredObject.class, object.getUuid()) != null;
    }

    public boolean addTransaction(final Transaction transaction) {

        accountLock.writeLock().lock();

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
                    for (TransactionEntry entry : transaction.getTransactionEntries()) {
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
            accountLock.writeLock().unlock();
        }
    }

    public boolean removeTransaction(final Transaction transaction) {

        accountLock.writeLock().lock();

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
                    logSevere("Failed to remove the Transaction");
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
            accountLock.writeLock().unlock();
        }
    }

    /**
     * Changes the reconciled state of a transaction
     *
     * @param transaction transaction to change
     * @param account     account to change state for
     * @param state       new reconciled state
     */
    public void setTransactionReconciled(final Transaction transaction, final Account account, final ReconciledState state) {
        try {
            final Transaction newTransaction = (Transaction) transaction.clone();

            ReconcileManager.reconcileTransaction(account, newTransaction, state);

            if (removeTransaction(transaction)) {
                addTransaction(newTransaction);
            }
        } catch (final CloneNotSupportedException e) {
            logger.log(Level.SEVERE, "Failed to reconcile the Transaction", e);
        }
    }

    public List<String> getTransactionNumberList() {
        configLock.readLock().lock();

        try {
            return getConfig().getTransactionNumberList();
        } finally {
            configLock.readLock().unlock();
        }
    }

    public void setTransactionNumberList(final List<String> list) {
        configLock.writeLock().lock();

        try {
            Config config = getConfig();

            config.setTransactionNumberList(list);
            getConfigDAO().update(config);

            Message message = new Message(MessageChannel.CONFIG, ChannelEvent.CONFIG_MODIFY, this);
            message.setObject(MessageProperty.CONFIG, config);

            messageBus.fireEvent(message);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
     * Get all transactions
     *
     * @return List of transactions that may be altered without concern of side effects
     */
    public List<Transaction> getTransactions() {
        return getTransactionDAO().getTransactions();
    }

    /**
     * Returns a list of transactions with external links.
     *
     * @return List of transactions that may be altered without concern of side effects
     */
    public List<Transaction> getTransactionsWithAttachments() {
        return getTransactionDAO().getTransactionsWithAttachments();
    }

    public Transaction getTransactionByUuid(final String uuid) {
        return getTransactionDAO().getTransactionByUuid(uuid);
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

    public void setPreference(@NotNull final String key, @Nullable final String value) {
        configLock.writeLock().lock();

        try {
            getConfig().setPreference(key, value);
            getConfigDAO().update(getConfig());

            Message message = new Message(MessageChannel.CONFIG, ChannelEvent.CONFIG_MODIFY, this);
            message.setObject(MessageProperty.CONFIG, config);
            messageBus.fireEvent(message);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    @Nullable
    public String getPreference(@NotNull final String key) {
        configLock.readLock().lock();

        try {
            return getConfig().getPreference(key);
        } finally {
            configLock.readLock().unlock();
        }
    }

    public boolean getBoolean(@NotNull final String key, final boolean defaultValue) {
        boolean value = defaultValue;

        final String stringResult = getPreference(key);

        if (stringResult != null && !stringResult.isEmpty()) {
            value = Boolean.valueOf(stringResult);
        }

        return value;
    }

    public void putBoolean(@NotNull final String key, final boolean value) {
        setPreference(key, Boolean.toString(value));
    }

    /**
     * Decorates a Callable to indicate background engine activity is occurring
     *
     * @param <E> return type for the decorated callable
     */
    private class BackgroundCallable<E> implements Callable<E> {

        final private Callable<E> callable;

        BackgroundCallable(@NotNull final Callable<E> callable) {
            this.callable = callable;
        }

        @Override
        public E call() throws Exception {
            messageBus.fireEvent(new Message(MessageChannel.SYSTEM, ChannelEvent.BACKGROUND_PROCESS_STARTED, Engine.this));

            try {
                return callable.call();
            } finally {
                messageBus.fireEvent(new Message(MessageChannel.SYSTEM, ChannelEvent.BACKGROUND_PROCESS_STOPPED, Engine.this));
            }
        }
    }
}
