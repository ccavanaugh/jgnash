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
package jgnash.engine;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import jgnash.engine.recurring.MonthlyReminder;
import jgnash.engine.recurring.PendingReminder;
import jgnash.engine.recurring.RecurringIterator;
import jgnash.engine.recurring.Reminder;
import jgnash.net.currency.CurrencyUpdateFactory;
import jgnash.net.security.UpdateFactory;
import jgnash.resource.util.ResourceUtils;
import jgnash.time.DateUtils;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

import org.apache.commons.collections4.ListUtils;

/**
 * Engine class
 * <p>
 * When objects are removed, they are wrapped in a TrashObject so they may still be referenced for messaging and cleanup
 * operations. After a predefined period of time, they are permanently removed.
 *
 * @author Craig Cavanaugh
 */
public class Engine {

    /**
     * Current version for the file format.
     */
    public static final int CURRENT_MAJOR_VERSION = 3;

    public static final int CURRENT_MINOR_VERSION = 6;

    // Lock name
    private static final String BIG_LOCK = "bigLock";

    private static final Logger logger = Logger.getLogger(Engine.class.getName());

    private static final long MAXIMUM_TRASH_AGE = 2L * 60L * 1000L; // 2 minutes

    /**
     * The maximum number of network errors before scheduled tasks are stopped.
     */
    private static final short MAX_ERRORS = 2;

    /**
     * Time in seconds to delay start of background updates.
     */
    private static final int SCHEDULED_DELAY = 30;

    /**
     * Time is seconds for a forced shutdown of background services
     */
    private static final int FORCED_SHUTDOWN_TIMEOUT = 15;

    private static final String MESSAGE_ACCOUNT_MODIFY = "Message.AccountModify";

    private static final String COMMODITY = "Commodity ";

    static {
        logger.setLevel(Level.ALL);
    }

    private final ResourceBundle rb = ResourceUtils.getBundle();

    /**
     * Primary lock for any operation that alters or reads data
     */
    private final ReentrantReadWriteLock dataLock;

    private final AtomicInteger backGroundCounter = new AtomicInteger();
    /**
     * Named identifier for this engine instance.
     */
    private final String name;
    /**
     * Unique identifier for this engine instance.
     * Used by this distributed lock manager to keep track of who has a lock
     */
    private final String uuid = UUID.randomUUID().toString();

    private final EngineDAO eDAO;

    private final AttachmentManager attachmentManager;

    /**
     * Background executor service for trash management and currency / security updates
     */
    private final ScheduledThreadPoolExecutor backgroundExecutorService;

    /**
     * All engine instances will share the same message bus.
     */
    private final MessageBus messageBus;

    /**
     * Cached for performance.
     */
    private Config config;

    /**
     * Cached for performance.
     */
    private RootAccount rootAccount;

    private ExchangeRateDAO exchangeRateDAO;

    /**
     * Cached for performance.
     */
    private String accountSeparator = null;

    public Engine(final EngineDAO eDAO, final LockManager lockManager, final AttachmentManager attachmentManager, final String name) {
        Objects.requireNonNull(name, "The engine name may not be null");
        Objects.requireNonNull(eDAO, "The engineDAO may not be null");

        logger.log(Level.INFO, "Release {0}.{1}", new Object[]{CURRENT_MAJOR_VERSION, CURRENT_MINOR_VERSION});

        this.attachmentManager = attachmentManager;
        this.eDAO = eDAO;
        this.name = name;

        // Generate lock
        dataLock = lockManager.getLock(BIG_LOCK);

        messageBus = MessageBus.getInstance(name);

        initialize();

        checkAndCorrect();

        backgroundExecutorService = new ScheduledThreadPoolExecutor(1,
                new DefaultDaemonThreadFactory("Engine Background Executor"));
        backgroundExecutorService.setRemoveOnCancelPolicy(true);
        backgroundExecutorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

        // run trash cleanup every 5 minutes 45 seconds after startup
        backgroundExecutorService.scheduleWithFixedDelay(() -> {
            if (!Thread.currentThread().isInterrupted()) {
                emptyTrash();
            }
        }, 45, 5L * 60L, TimeUnit.SECONDS);

        backgroundExecutorService.schedule(() -> {
            if (UpdateFactory.getUpdateOnStartup()) {
                // don't update on weekends unless needed
                if (UpdateFactory.shouldAutomaticUpdateOccur(getConfig().getLastSecuritiesUpdateTimestamp())) {
                    startSecuritiesUpdate(SCHEDULED_DELAY);
                }
            }
        }, 30, TimeUnit.SECONDS);

        backgroundExecutorService.schedule(() -> {
            if (CurrencyUpdateFactory.getUpdateOnStartup()) {
                startExchangeRateUpdate(SCHEDULED_DELAY);
            }
        }, 30, TimeUnit.SECONDS);
    }

    boolean isFileDirty() {
        return eDAO.isDirty() || getAccountDAO().isDirty() || getBudgetDAO().isDirty() || getCommodityDAO().isDirty()
                       || getConfigDAO().isDirty() || getReminderDAO().isDirty() || getTransactionDAO().isDirty()
                       || getTrashDAO().isDirty();
    }

    /**
     * Registers a {@code Handler} with the class logger.
     * This also ensures the static logger is initialized.
     *
     * @param handler {@code Handler} to register
     */
    public static void addLogHandler(final Handler handler) {
        logger.addHandler(handler);
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
     * @param localDate    {@code LocalDate} we want a market price for
     * @return The best market price or a value of 0 if no history or transactions exist
     */
    public static BigDecimal getMarketPrice(final Collection<Transaction> transactions, final SecurityNode node,
                                            final CurrencyNode baseCurrency, final LocalDate localDate) {

        // Search for the exact history node record
        Optional<SecurityHistoryNode> optional = node.getHistoryNode(localDate);

        // not null, must be an exact match, return the value because it has precedence
        if (optional.isPresent()) {
            return node.getMarketPrice(localDate, baseCurrency);
        }

        // Nothing found yet, continue searching for something better
        LocalDate priceDate = LocalDate.ofEpochDay(0);
        BigDecimal price = BigDecimal.ZERO;

        optional = node.getClosestHistoryNode(localDate);

        if (optional.isPresent()) {    // Closest option so far
            price = optional.get().getPrice();
            priceDate = optional.get().getLocalDate();
        }

        // Compare against transactions
        for (final Transaction t : transactions) {
            if (t instanceof InvestmentTransaction && ((InvestmentTransaction) t).getSecurityNode() == node) {

                // The transaction date must be closer than the history node, but not newer than the request date
                if ((t.getLocalDate().isAfter(priceDate) && t.getLocalDate().isBefore(localDate)) || t.getLocalDate().equals(localDate)) {

                    // Check for a dividend, etc that may have returned a price of zero
                    final BigDecimal p = ((InvestmentTransaction) t).getPrice();

                    if (p != null && p.compareTo(BigDecimal.ZERO) > 0) {
                        price = p;
                        priceDate = t.getLocalDate();
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
     * Returns the engine logger.
     *
     * @return the engine logger
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * Log a informational message.
     *
     * @param message message to display
     */
    private static void logInfo(final String message) {
        logger.log(Level.INFO, message);
    }

    /**
     * Log a warning message.
     *
     * @param message message to display
     */
    private static void logWarning(final String message) {
        logger.warning(message);
    }

    /**
     * Log a severe message.
     *
     * @param message message to display
     */
    private static void logSevere(final String message) {
        logger.severe(message);
    }

    private static void shutDownAndWait(final ExecutorService executorService) {
        executorService.shutdownNow();

        try {
            if (!executorService.awaitTermination(FORCED_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)) {

                if (!executorService.awaitTermination(FORCED_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)) {
                    logSevere("Unable to shutdown background service");
                }
            }

        } catch (final InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();

            logger.log(Level.FINEST, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Initiates a background exchange rate update with a given start delay.
     *
     * @param delay delay in seconds
     */
    public void startExchangeRateUpdate(final int delay) {
        backgroundExecutorService.schedule(new BackgroundCallable(new CurrencyUpdateFactory.UpdateExchangeRatesCallable()), delay,
                TimeUnit.SECONDS);
    }

    /**
     * Initiates a background securities history update with a given start delay.
     *
     * @param delay delay in seconds
     */
    public void startSecuritiesUpdate(final int delay) {
        final List<BackgroundCallable> callables = new ArrayList<>();

        getSecurities().stream().filter(securityNode ->
                                                securityNode.getQuoteSource() != QuoteSource.NONE).forEach(securityNode -> { // failure will occur if source is not defined

            callables.add(new BackgroundCallable(new UpdateFactory.UpdateSecurityNodeCallable(securityNode)));
            callables.add(new BackgroundCallable(new UpdateFactory.UpdateSecurityNodeEventsCallable(securityNode)));
        });

        // Cleanup thread that monitors for excess network connection failures
        new SecuritiesUpdateRunnable(callables, delay).start();

        // Save the last update
        config.setLastSecuritiesUpdateTimestamp(LocalDateTime.now());
    }

    /**
     * Creates a RootAccount and default currency only if necessary.
     */
    private void initialize() {

        dataLock.writeLock().lock();

        try {

            // ask the Config object to perform any needed configuration
            getConfig().initialize();

            // build the exchange rate storage object
            exchangeRateDAO = new ExchangeRateDAO(getCommodityDAO());

            // assign the exchange rate store to the currencies
            for (final CurrencyNode node : getCurrencies()) {
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
                    throw new EngineException("Was not able to add the root account");
                }

                if (getDefaultCurrency() == null) {
                    setDefaultCurrency(node);
                }
            }

        } finally {
            dataLock.writeLock().unlock();
        }

        logInfo("Engine initialization is complete");
    }

    /**
     * Corrects minor issues with a database that may occur because of prior bugs or file format upgrades.
     */
    private void checkAndCorrect() {
        dataLock.writeLock().lock();

        try {
            // check and correct multiple root accounts from old files... there are still a few.
            List<Account> accountList = getAccountList().stream().filter(account -> account.getAccountType()
                                                                                            .equals(AccountType.ROOT)).collect(Collectors.toList());

            if (accountList.size() > 1) {
                for (Account account : accountList) {
                    if (account.getChildCount() == 0) {
                        removeAccount(account);
                        logWarning("Removed an extra / empty root account");
                    }
                }
            }

            final List<Config> list = eDAO.getStoredObjects(Config.class);
            if (list.size() > 1) {
                // Delete all but the first found config object
                for (int i = 1; i < list.size(); i++) {
                    logWarning("Removed an extra Config object");
                    moveObjectToTrash(list.get(i));
                }
            }

            // Transaction timestamps were updated for release 2.25
            if (getConfig().getMinorFileFormatVersion() < 25 && getConfig().getMajorFileFormatVersion() < 3) {
                // Update transactions in chunks of 200
                ListUtils.partition(getTransactions(), 200).forEach(eDAO::bulkUpdate);
            }

            // update the file version if it is not current
            if (getConfig().getMajorFileFormatVersion() != CURRENT_MAJOR_VERSION
                        || getConfig().getMinorFileFormatVersion() != CURRENT_MINOR_VERSION) {

                final Config localConfig = getConfig();
                localConfig.updateFileVersion();
                getConfigDAO().update(localConfig);
            }
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    private void clearObsoleteExchangeRates() {
        getCommodityDAO().getExchangeRates().stream()
                .filter(rate -> getBaseCurrencies(rate.getRateId()).length == 0)
                .forEach(this::removeExchangeRate);
    }

    private void removeExchangeRate(final ExchangeRate rate) {
        dataLock.writeLock().lock();

        try {
            for (final ExchangeRateHistoryNode node : rate.getHistory()) {
                removeExchangeRateHistory(rate, node);
            }
            moveObjectToTrash(rate);
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    void stopBackgroundServices() {
        logInfo("Controlled engine shutdown initiated");

        shutDownAndWait(backgroundExecutorService);

        logInfo("Background services have been stopped");
    }

    void shutdown() {
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

    private boolean moveObjectToTrash(final Object object) {
        boolean result = false;

        dataLock.writeLock().lock();

        try {
            if (object instanceof StoredObject) {
                getTrashDAO().add(new TrashObject((StoredObject) object));
            } else {    // simple object with an annotated JPA entity id of type long is assumed
                getTrashDAO().addEntityTrash(object);
            }
            result = true;
        } catch (final Exception ex) {
            logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        } finally {
            dataLock.writeLock().unlock();
        }

        return result;
    }

    /**
     * Empty the trash if any objects are older than the defined time.
     */
    private void emptyTrash() {
        if (backGroundCounter.incrementAndGet() == 1) {
            messageBus.fireEvent(new Message(MessageChannel.SYSTEM, ChannelEvent.BACKGROUND_PROCESS_STARTED,
                    Engine.this));
        }

        dataLock.writeLock().lock();

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

            trash.stream().filter(o -> ChronoUnit.MILLIS.between(o.getDate(), LocalDateTime.now()) >= MAXIMUM_TRASH_AGE)
                    .forEach(o -> getTrashDAO().remove(o));
        } finally {
            dataLock.writeLock().unlock();

            if (backGroundCounter.decrementAndGet() == 0) {
                messageBus.fireEvent(new Message(MessageChannel.SYSTEM, ChannelEvent.BACKGROUND_PROCESS_STOPPED,
                        Engine.this));
            }
        }
    }

    /**
     * Creates a default reminder given a transaction and the primary account.  The Reminder will need to persisted.
     *
     * @param transaction Transaction for the reminder.  The transaction will be cloned
     * @param account     primary account
     * @return new default {@code MonthlyReminder}
     */
    public static Reminder createDefaultReminder(final Transaction transaction, final Account account) {
        final Reminder reminder = new MonthlyReminder();

        try {
            reminder.setAccount(account);
            reminder.setStartDate(transaction.getLocalDate().plusMonths(1));
            reminder.setTransaction((Transaction) transaction.clone());
            reminder.setDescription(transaction.getPayee());
            reminder.setNotes(transaction.getMemo());
        } catch (final CloneNotSupportedException e) {
            logSevere(e.getLocalizedMessage());
        }
        return reminder;
    }

    public boolean addReminder(final Reminder reminder) {
        Objects.requireNonNull(reminder.getUuid());

        boolean result = false;

        // make sure the description has been set
        if (reminder.getDescription() != null && !reminder.getDescription().isBlank()) {
            result = getReminderDAO().addReminder(reminder);
        }

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
     * Returns a list of reminders.
     *
     * @return List of reminders
     */
    public List<Reminder> getReminders() {
        return getReminderDAO().getReminderList();
    }

    public Reminder getReminderByUuid(final UUID uuid) {
        return getReminderDAO().getReminderByUuid(uuid);
    }

    public List<PendingReminder> getPendingReminders() {
        final ArrayList<PendingReminder> pendingList = new ArrayList<>();
        final List<Reminder> list = getReminders();
        final LocalDate now = LocalDate.now(); // today's date

        for (final Reminder r : list) {
            if (r.isEnabled()) {
                final RecurringIterator ri = r.getIterator();
                LocalDate next = ri.next();

                while (next != null) {
                    LocalDate date = next;

                    if (r.isAutoCreate()) {
                        date = date.minusDays(r.getDaysAdvance());
                    }

                    if (DateUtils.before(date, now)) { // need to fire this reminder
                        pendingList.add(new PendingReminder(r, next));
                        next = ri.next();
                    } else {
                        next = null;
                    }
                }
            }
        }

        return pendingList;
    }

    public static PendingReminder getPendingReminder(@NotNull Reminder reminder) {
        final RecurringIterator ri = reminder.getIterator();
        LocalDate next = ri.next();

        if (next != null) {
            return new PendingReminder(reminder, next);
        }

        return null;
    }

    public void processPendingReminders(final Collection<PendingReminder> pendingReminders) {
        pendingReminders.stream().filter(PendingReminder::isApproved).forEach(pending -> {
            final Reminder reminder = pending.getReminder();

            if (reminder.getTransaction() != null) { // add the transaction
                final Transaction t = reminder.getTransaction();

                // Update to the commit date (commit date can be modified)
                t.setDate(pending.getCommitDate());
                addTransaction(t);
            }
            // update the last fired date... date returned from the iterator
            reminder.setLastDate(); // mark as complete
            if (!updateReminder(reminder)) {
                logSevere(rb.getString("Message.Error.ReminderUpdate"));
            }
        });
    }

    public <T extends StoredObject> T getStoredObjectByUuid(final Class<T> tClass, final UUID uuid) {
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
        dataLock.readLock().lock();

        try {

            List<StoredObject> objects = eDAO.getStoredObjects();

            // Filter out objects to be removed
            objects.removeIf(TrashObject.class::isInstance);

            objects.sort(new StoredObjectComparator());

            return objects;
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * Validate a CommodityNode for correctness.
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
            logSevere(COMMODITY + node + " had a scale less than zero");
        }

        if (node instanceof SecurityNode && ((SecurityNode) node).getReportedCurrencyNode() == null) {
            result = false;
            logSevere(COMMODITY + node + " was not assigned a currency");
        }

        // ensure the UUID being used is unique
        if (eDAO.getObjectByUuid(CommodityNode.class, node.getUuid()) != null) {
            result = false;
            logSevere(COMMODITY + node + " was not unique");
        }

        return result;
    }

    /**
     * Adds a new CurrencyNode to the data set.
     * <p>
     * Checks and prevents the addition of a duplicate Currencies.
     *
     * @param node new CurrencyNode to add
     * @return {@code true} if the add it successful
     */
    public boolean addCurrency(final CurrencyNode node) {
        dataLock.writeLock().lock();

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
                logger.log(Level.FINE, "Adding: {0}", node);
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
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Links the {@code CurrencyNode} to the exchange DAO.
     * Support method to be used during import operations
     *
     * @param currencyNode {@code CurrencyNode} to link
     */
    void attachCurrencyNode(final CurrencyNode currencyNode) {
        currencyNode.setExchangeRateDAO(exchangeRateDAO);
    }

    /**
     * Adds a new SecurityNode to the data set.
     * <p>
     * Checks and prevents the addition of a duplicate SecurityNode.
     *
     * @param node new SecurityNode to add
     * @return {@code true} if the add it successful
     */
    public boolean addSecurity(final SecurityNode node) {
        dataLock.writeLock().lock();

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
                logger.log(Level.FINE, "Adding: {0}", node);
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
            dataLock.writeLock().unlock();
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
        dataLock.writeLock().lock();

        try {
            // Remove old history of the same date if it exists
            if (node.contains(hNode.getLocalDate())) {
                if (!removeSecurityHistory(node, hNode.getLocalDate())) {
                    logSevere(ResourceUtils.getString("Message.Error.HistRemoval", hNode.getLocalDate(), node.getSymbol()));
                    return false;
                }
            }

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
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Add a SecurityHistoryNode node to a SecurityNode.  If the SecurityNode already contains
     * an equivalent SecurityHistoryNode, the old SecurityHistoryNode is removed first.
     *
     * @param node         SecurityNode to add to
     * @param historyEvent SecurityHistoryNode to add
     * @return <tt>true</tt> if successful
     */
    public boolean addSecurityHistoryEvent(@NotNull final SecurityNode node, @NotNull final SecurityHistoryEvent historyEvent) {
        dataLock.writeLock().lock();

        try {

            // Remove old history event if it exists, equality is used to work around hibernate optimizations
            // A defensive copy of the old events is used to prevent concurrent modification errors
            new HashSet<>(node.getHistoryEvents()).stream().filter(event -> event.equals(historyEvent))
                    .forEach(event -> removeSecurityHistoryEvent(node, historyEvent));

            boolean status = node.addSecurityHistoryEvent(historyEvent);

            if (status) {
                status = getCommodityDAO().addSecurityHistoryEvent(node, historyEvent);
            }

            Message message;

            if (status) {
                clearCachedAccountBalance(node);
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.SECURITY_HISTORY_EVENT_ADD, this);
            } else {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.SECURITY_HISTORY_EVENT_ADD_FAILED, this);
            }

            message.setObject(MessageProperty.COMMODITY, node);
            messageBus.fireEvent(message);

            return status;
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Returns a list of investment accounts that use the given security node.
     *
     * @param node security node
     * @return list of investment accounts
     */
    private Set<Account> getInvestmentAccountList(final SecurityNode node) {
        return getInvestmentAccountList().parallelStream()
                       .filter(account -> account.containsSecurity(node)).collect(Collectors.toSet());
    }

    /**
     * Forces all investment accounts containing the security to clear the cached account balance and reconciled account
     * balance and recalculate when queried.
     *
     * @param node SecurityNode that was changed
     */
    private void clearCachedAccountBalance(final SecurityNode node) {
        getInvestmentAccountList(node).forEach(this::clearCachedAccountBalance);
    }

    /**
     * Clears an {@code Accounts} cached balance and recursively works up the tree to the root.
     *
     * @param account {@code Account} to clear
     */
    private void clearCachedAccountBalance(final Account account) {

        dataLock.writeLock().lock();

        try {
            account.clearCachedBalances();

            // force a persistence update if working as a client / server
            if (eDAO.isRemote()) {
                getAccountDAO().updateAccount(account);
            }
        } finally {
            dataLock.writeLock().unlock();
        }

        if (account.getParent() != null && account.getParent().getAccountType() != AccountType.ROOT) {
            clearCachedAccountBalance(account.getParent());
        }
    }

    private CurrencyNode[] getBaseCurrencies(final String exchangeRateId) {
        dataLock.readLock().lock();

        try {
            final List<CurrencyNode> currencies = getCurrencies();

            Collections.sort(currencies);
            Collections.reverse(currencies);

            for (final CurrencyNode node1 : currencies) {
                for (final CurrencyNode node2 : currencies) {
                    if (node1 != node2 && buildExchangeRateId(node1, node2).equals(exchangeRateId)) {
                        return new CurrencyNode[]{node1, node2};
                    }
                }
            }
            return new CurrencyNode[0];
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * Returns an array of currencies being used in accounts.
     *
     * @return Set of CurrencyNodes
     */
    public Set<CurrencyNode> getActiveCurrencies() {
        dataLock.readLock().lock();

        try {
            return getCommodityDAO().getActiveCurrencies();
        } finally {
            dataLock.readLock().unlock();
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
        dataLock.readLock().lock();

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
            dataLock.readLock().unlock();
        }
    }

    public List<CurrencyNode> getCurrencies() {
        dataLock.readLock().lock();

        try {
            return getCommodityDAO().getCurrencies();
        } finally {
            dataLock.readLock().unlock();
        }
    }

    public CurrencyNode getCurrencyNodeByUuid(final UUID uuid) {
        return getCommodityDAO().getCurrencyByUuid(uuid);
    }

    public ExchangeRate getExchangeRate(final CurrencyNode baseCurrency, final CurrencyNode exchangeCurrency) {
        dataLock.readLock().lock();

        try {
            return exchangeRateDAO.getExchangeRateNode(baseCurrency, exchangeCurrency);
        } finally {
            dataLock.readLock().unlock();
        }
    }

    public ExchangeRate getExchangeRateByUuid(final UUID uuid) {
        return getCommodityDAO().getExchangeRateByUuid(uuid);
    }

    @NotNull
    public List<SecurityNode> getSecurities() {
        dataLock.readLock().lock();

        try {
            return getCommodityDAO().getSecurities();
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * Find a SecurityNode given it's symbol.
     *
     * @param symbol symbol of security to find
     * @return null if not found
     */
    public SecurityNode getSecurity(final String symbol) {
        dataLock.readLock().lock();

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
            dataLock.readLock().unlock();
        }
    }

    public SecurityNode getSecurityNodeByUuid(final UUID uuid) {
        return getCommodityDAO().getSecurityByUuid(uuid);
    }

    private boolean isCommodityNodeUsed(final CommodityNode node) {
        dataLock.readLock().lock();

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
            dataLock.readLock().unlock();
        }

        return false;
    }

    public boolean removeCommodity(final CurrencyNode node) {
        boolean status = true;

        dataLock.writeLock().lock();

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
            dataLock.writeLock().unlock();
        }
    }

    public boolean removeSecurity(final SecurityNode node) {
        boolean status = true;

        dataLock.writeLock().lock();

        try {
            if (isCommodityNodeUsed(node)) {
                status = false;
            } else {
                // Remove all history nodes first so they are not left behind

                // A copy is made to prevent a concurrent modification error to the underlying list, Bug #208
                final List<SecurityHistoryNode> hNodes = new ArrayList<>(node.getHistoryNodes());

                hNodes.stream()
                        .filter(hNode -> !removeSecurityHistory(node, hNode.getLocalDate()))
                        .forEach(hNode -> logSevere(ResourceUtils.getString("Message.Error.HistRemoval",
                                hNode.getLocalDate(), node.getSymbol())));
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
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Remove a {@code SecurityHistoryNode} given a {@code Date}.
     *
     * @param node {@code SecurityNode} to remove history from
     * @param date the search {@code Date}
     * @return {@code true} if a {@code SecurityHistoryNode} was found and removed
     */
    public boolean removeSecurityHistory(@NotNull final SecurityNode node, @NotNull final LocalDate date) {
        dataLock.writeLock().lock();

        boolean status = false;

        try {
            final Optional<SecurityHistoryNode> optional = node.getHistoryNode(date);

            if (optional.isPresent()) {
                status = node.removeHistoryNode(date);

                if (status) {   // removal was a success, make sure we cleanup properly
                    moveObjectToTrash(optional.get());
                    status = getCommodityDAO().removeSecurityHistory(node, optional.get());

                    logInfo(ResourceUtils.getString("Message.RemovingSecurityHistory", date, node.getSymbol()));
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
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Remove a {@code SecurityHistoryEvent} from a {@code SecurityNode}.
     *
     * @param node         {@code SecurityNode} to remove history from
     * @param historyEvent the {@code SecurityHistoryEvent} to remove
     * @return {@code true} if the {@code SecurityHistoryEvent} was found and removed
     */
    public boolean removeSecurityHistoryEvent(@NotNull final SecurityNode node, @NotNull final SecurityHistoryEvent historyEvent) {
        dataLock.writeLock().lock();

        boolean status;

        try {
            status = node.removeSecurityHistoryEvent(historyEvent);

            if (status) {   // removal was a success, make sure we cleanup properly
                moveObjectToTrash(historyEvent);
                status = getCommodityDAO().removeSecurityHistoryEvent(node, historyEvent);
            }

            Message message;

            if (status) {
                clearCachedAccountBalance(node);
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.SECURITY_HISTORY_EVENT_REMOVE, this);
            } else {
                message = new Message(MessageChannel.COMMODITY, ChannelEvent.SECURITY_HISTORY_EVENT_REMOVE_FAILED, this);
            }

            message.setObject(MessageProperty.COMMODITY, node);
            messageBus.fireEvent(message);

            return status;
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    private Config getConfig() {

        dataLock.readLock().lock();

        try {
            if (config == null) {
                config = getConfigDAO().getDefaultConfig();
            }
            return config;

        } finally {
            dataLock.readLock().unlock();
        }
    }

    public CurrencyNode getDefaultCurrency() {

        dataLock.readLock().lock();

        try {
            CurrencyNode node = getConfig().getDefaultCurrency();

            if (node == null) {
                logger.warning("No default currency assigned");
            }

            return node;
        } finally {
            dataLock.readLock().unlock();
        }
    }

    public void setDefaultCurrency(final CurrencyNode defaultCurrency) {

        // make sure the new default is persisted if it has not been
        if (!isStored(defaultCurrency)) {
            addCurrency(defaultCurrency);
        }

        dataLock.writeLock().lock();

        try {
            final Config currencyConfig = getConfig();
            currencyConfig.setDefaultCurrency(defaultCurrency);
            getConfigDAO().update(currencyConfig);

            logInfo("Setting default currency: " + defaultCurrency);

            Message message = new Message(MessageChannel.CONFIG, ChannelEvent.CONFIG_MODIFY, this);
            message.setObject(MessageProperty.CONFIG, currencyConfig);
            messageBus.fireEvent(message);

            Account root = getRootAccount();

            // The root account holds a reference to the default currency
            root.setCurrencyNode(defaultCurrency);
            getAccountDAO().updateAccount(root);

            message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_MODIFY, this);
            message.setObject(MessageProperty.ACCOUNT, root);
            messageBus.fireEvent(message);
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    public void setExchangeRate(final CurrencyNode baseCurrency, final CurrencyNode exchangeCurrency,
                                final BigDecimal rate) {
        setExchangeRate(baseCurrency, exchangeCurrency, rate, LocalDate.now());
    }

    public void setExchangeRate(final CurrencyNode baseCurrency, final CurrencyNode exchangeCurrency,
                                final BigDecimal rate, final LocalDate localDate) {
        Objects.requireNonNull(rate);

        if (rate.compareTo(BigDecimal.ZERO) < 1) {
            throw new EngineException("Rate must be greater than zero");
        }

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
        if (exchangeRate.contains(localDate)) {
            removeExchangeRateHistory(exchangeRate, exchangeRate.getHistory(localDate));
        }

        dataLock.writeLock().lock();

        try {
            // create the new history node
            ExchangeRateHistoryNode historyNode;

            if (baseCurrency.getSymbol().compareToIgnoreCase(exchangeCurrency.getSymbol()) > 0) {
                historyNode = new ExchangeRateHistoryNode(localDate, rate);
            } else {
                historyNode = new ExchangeRateHistoryNode(localDate, BigDecimal.ONE.divide(rate, MathConstants.mathContext));
            }

            final Message message;

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
            dataLock.writeLock().unlock();
        }
    }

    public void removeExchangeRateHistory(final ExchangeRate exchangeRate, final ExchangeRateHistoryNode history) {

        dataLock.writeLock().lock();

        try {
            final Message message;

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
            dataLock.writeLock().unlock();
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

        if (oldNode == templateNode) {
            throw new EngineException("node were the same");
        }

        dataLock.writeLock().lock();

        try {
            boolean status;

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

                status = getCommodityDAO().updateCommodityNode(oldNode);
            } else {
                status = false;
                logger.warning("Template object class did not match old object class");
            }

            final Message message;

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
            dataLock.writeLock().unlock();
        }
    }

    private boolean updateReminder(final Reminder reminder) {
        final boolean result = getReminderDAO().updateReminder(reminder);

        final Message message;

        if (result) {
            message = new Message(MessageChannel.REMINDER, ChannelEvent.REMINDER_UPDATE, this);
        } else {
            message = new Message(MessageChannel.REMINDER, ChannelEvent.REMINDER_UPDATE_FAILED, this);
        }

        message.setObject(MessageProperty.REMINDER, reminder);

        messageBus.fireEvent(message);

        return result;
    }

    public String getAccountSeparator() {

        dataLock.readLock().lock();

        try {
            if (accountSeparator == null) {
                accountSeparator = getConfig().getAccountSeparator();
            }
            return accountSeparator;

        } finally {
            dataLock.readLock().unlock();
        }
    }

    public void setAccountSeparator(final String separator) {

        dataLock.writeLock().lock();

        try {
            accountSeparator = separator;
            Config localConfig = getConfig();

            localConfig.setAccountSeparator(separator);

            getConfigDAO().update(localConfig);

            Message message = new Message(MessageChannel.CONFIG, ChannelEvent.CONFIG_MODIFY, this);
            message.setObject(MessageProperty.CONFIG, localConfig);

            messageBus.fireEvent(message);
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Returns a list of all Accounts excluding the rootAccount.
     *
     * @return List of accounts
     */
    public List<Account> getAccountList() {
        final List<Account> accounts = getAccountDAO().getAccountList();
        accounts.remove(getRootAccount());

        return accounts;
    }

    public Account getAccountByUuid(final UUID id) {
        return getAccountDAO().getAccountByUuid(id);
    }

    /**
     * Search for an account with a matching account name.
     *
     * @param accountName Account name to search for. <b>Must not be null</b>
     * @return The matching account. {@code null} if not found.
     */
    public Account getAccountByName(@NotNull final String accountName) {
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
     * Returns a list of IncomeAccounts excluding the rootIncomeAccount.
     *
     * @return List of income accounts
     */
    @NotNull
    public List<Account> getIncomeAccountList() {
        return getAccountDAO().getIncomeAccountList();
    }

    /**
     * Returns a list of ExpenseAccounts excluding the rootExpenseAccount.
     *
     * @return List if expense accounts
     */
    @NotNull
    public List<Account> getExpenseAccountList() {
        return getAccountDAO().getExpenseAccountList();
    }

    /**
     * Returns a list of all accounts excluding the rootAccount and IncomeAccounts and ExpenseAccounts.
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
     * Adds a new account.
     *
     * @param parent The parent account
     * @param child  A new Account object
     * @return true if successful
     */
    public boolean addAccount(final Account parent, final Account child) {
        Objects.requireNonNull(child);
        Objects.requireNonNull(child.getUuid());

        if (child.getAccountType() == AccountType.ROOT) {
            throw new IllegalArgumentException("Invalid Account");
        }

        dataLock.writeLock().lock();

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
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Return the root account.
     *
     * @return RootAccount
     */
    public RootAccount getRootAccount() {

        dataLock.readLock().lock();

        try {
            if (rootAccount == null) {
                rootAccount = getAccountDAO().getRootAccount();
            }
            return rootAccount;
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * Move an account to a new parent account..
     *
     * @param account   account to move
     * @param newParent the new parent account
     * @return true if successful
     */
    public boolean moveAccount(final Account account, final Account newParent) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(newParent);

        dataLock.writeLock().lock();

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

            logInfo(rb.getString(MESSAGE_ACCOUNT_MODIFY));

            return true;
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Changes an Account's code.
     *
     * @param account Account to change
     * @param code    new code
     * @return true is successful
     */
    public boolean setAccountCode(final Account account, final int code) {
        account.setAccountCode(code);

        boolean result = getAccountDAO().updateAccount(account);

        if (result) {
            final Message message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_MODIFY, this);
            message.setObject(MessageProperty.ACCOUNT, account);
            messageBus.fireEvent(message);

            logInfo(rb.getString(MESSAGE_ACCOUNT_MODIFY));
        } else {
            final Message message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_MODIFY_FAILED, this);
            message.setObject(MessageProperty.ACCOUNT, account);
            messageBus.fireEvent(message);
        }

        return result;
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

        dataLock.writeLock().lock();

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

                logInfo(rb.getString(MESSAGE_ACCOUNT_MODIFY));
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
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Purges any {@code BudgetGoal} associated with an account.
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
     * Sets the account number of an account.
     *
     * @param account account to change
     * @param number  new account number
     */
    public void setAccountNumber(final Account account, final String number) {

        dataLock.writeLock().lock();

        try {
            account.setAccountNumber(number);
            getAccountDAO().updateAccount(account);

            Message message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_MODIFY, this);
            message.setObject(MessageProperty.ACCOUNT, account);
            messageBus.fireEvent(message);

            logInfo(rb.getString(MESSAGE_ACCOUNT_MODIFY));
        } finally {
            dataLock.writeLock().unlock();
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

        dataLock.writeLock().lock();

        try {
            account.setAttribute(key, value);
            getAccountDAO().updateAccount(account);

            Message message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_ATTRIBUTE_MODIFY, this);
            message.setObject(MessageProperty.ACCOUNT, account);
            messageBus.fireEvent(message);

            logInfo(rb.getString(MESSAGE_ACCOUNT_MODIFY));
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Returns an {@code Account} attribute.
     *
     * @param account account to extract attribute from
     * @param key     the attribute key
     * @return the attribute if found
     * @see #setAccountAttribute
     */
    public static String getAccountAttribute(@NotNull final Account account, @NotNull final String key) {
        return account.getAttribute(key);
    }

    /**
     * Removes an existing account given it's ID.
     *
     * @param account The account to remove
     * @return true if successful
     */
    public boolean removeAccount(final Account account) {

        dataLock.writeLock().lock();

        try {
            boolean result = false;

            if (account.getTransactionCount() == 0 && account.getChildCount() == 0) {
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
            dataLock.writeLock().unlock();
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

    /**
     * Sets the amortize object of an account.
     *
     * @param account        The Liability account to change
     * @param amortizeObject the new AmortizeObject
     * @return true if successful
     */
    public boolean setAmortizeObject(final Account account, final AmortizeObject amortizeObject) {

        dataLock.writeLock().lock();

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
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Toggles the visibility of an account given its ID.
     *
     * @param account The account to toggle visibility
     */
    public void toggleAccountVisibility(final Account account) {

        dataLock.writeLock().lock();

        try {
            Message message;

            account.setVisible(!account.isVisible());

            if (getAccountDAO().toggleAccountVisibility(account)) {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_VISIBILITY_CHANGE, this);
            } else {
                message = new Message(MessageChannel.ACCOUNT, ChannelEvent.ACCOUNT_VISIBILITY_CHANGE_FAILED, this);
            }

            message.setObject(MessageProperty.ACCOUNT, account);
            messageBus.fireEvent(message);
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Adds a SecurityNode from a InvestmentAccount.
     *
     * @param account destination account
     * @param node    SecurityNode to add
     * @return true if add was successful
     */
    public boolean addAccountSecurity(final Account account, final SecurityNode node) {

        dataLock.writeLock().lock();

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
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Removes a SecurityNode from an InvestmentAccount.
     *
     * @param account Account to remove SecurityNode from
     * @param node    SecurityNode to remove
     * @return true if successful
     */
    private boolean removeAccountSecurity(final Account account, final SecurityNode node) {
        Objects.requireNonNull(node);

        dataLock.writeLock().lock();

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
            dataLock.writeLock().unlock();
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
            dataLock.writeLock().lock();

            try {
                final Collection<SecurityNode> oldList = acc.getSecurities();

                for (SecurityNode node : oldList) {
                    if (!list.contains(node)) {
                        if (!removeAccountSecurity(acc, node)) {
                            logWarning(ResourceUtils.getString("Message.Error.SecurityAccountRemove", node.toString(),
                                    acc.getName()));
                            result = false;
                        }
                    }
                }

                for (SecurityNode node : list) {
                    if (!oldList.contains(node)) {
                        if (!addAccountSecurity(acc, node)) {
                            logWarning(ResourceUtils.getString("Message.Error.SecurityAccountRemove", node.toString(),
                                    acc.getName()));
                            result = false;
                        }
                    }
                }
            } finally {
                dataLock.writeLock().unlock();
            }
        }

        return result;
    }

    public boolean addBudget(final Budget budget) {

        boolean result;

        dataLock.writeLock().lock();

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
            dataLock.writeLock().unlock();
        }
    }

    public boolean removeBudget(final Budget budget) {

        boolean result = false;

        dataLock.writeLock().lock();

        try {
            moveObjectToTrash(budget);

            Message message = new Message(MessageChannel.BUDGET, ChannelEvent.BUDGET_REMOVE, this);

            message.setObject(MessageProperty.BUDGET, budget);
            messageBus.fireEvent(message);

            result = true;
        } catch (final Exception ex) {
            logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        } finally {
            dataLock.writeLock().unlock();
        }

        return result;
    }

    public void updateBudgetGoals(final Budget budget, final Account account, final BudgetGoal newGoals) {
        dataLock.writeLock().lock();

        try {
            BudgetGoal oldGoals = budget.getBudgetGoal(account);

            budget.setBudgetGoal(account, newGoals);

            moveObjectToTrash(oldGoals);    // need to keep the old goal around, will be cleaned up later, orphan removal causes refresh issues

            updateBudgetGoals(budget, account);
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    private void updateBudgetGoals(final Budget budget, final Account account) {
        dataLock.writeLock().lock();

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
            dataLock.writeLock().unlock();
        }
    }

    public boolean updateBudget(final Budget budget) {

        boolean result;

        dataLock.writeLock().lock();

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
            dataLock.writeLock().unlock();
        }
    }

    public List<Budget> getBudgetList() {

        dataLock.readLock().lock();

        try {
            return getBudgetDAO().getBudgets();
        } finally {
            dataLock.readLock().unlock();
        }
    }

    public Budget getBudgetByUuid(final UUID uuid) {
        return getBudgetDAO().getBudgetByUuid(uuid);
    }

    public boolean isTransactionValid(final Transaction transaction) {

        for (final Account a : transaction.getAccounts()) {
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

        for (final TransactionEntry e : transaction.getTransactionEntries()) {
            if (e == null) {
                logger.log(Level.WARNING, "Null TransactionEntry");
                return false;
            }
        }

        for (final TransactionEntry e : transaction.getTransactionEntries()) {
            if (e.getTransactionTag() == null) {
                logger.log(Level.WARNING, "Null TransactionTag");
                return false;
            }
        }

        for (final TransactionEntry e : transaction.getTransactionEntries()) {
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

        if (transaction instanceof InvestmentTransaction) {
            final InvestmentTransaction investmentTransaction = (InvestmentTransaction) transaction;

            if (!investmentTransaction.getInvestmentAccount().containsSecurity(investmentTransaction.getSecurityNode())) {
                logger.log(Level.WARNING, "Investment Account is missing the security");
                return false;
            }
        }

        return transaction.getTransactionType() != TransactionType.INVALID;
    }

    /**
     * Determine if a StoredObject is persisted in the database.
     *
     * @param object StoredObject to check
     * @return true if persisted
     */
    public boolean isStored(final StoredObject object) {
        return eDAO.getObjectByUuid(StoredObject.class, object.getUuid()) != null;
    }

    public boolean addTransaction(final Transaction transaction) {

        dataLock.writeLock().lock();

        try {
            boolean result = isTransactionValid(transaction);

            if (result) {
                /* Add the transaction to each account */
                transaction.getAccounts().stream()
                        .filter(account -> !account.addTransaction(transaction))
                        .forEach(account -> logSevere("Failed to add the Transaction"));
                result = getTransactionDAO().addTransaction(transaction);

                logInfo(rb.getString("Message.TransactionAdd"));

                /* If successful, extract and enter a default exchange rate for the transaction date if a rate has not been set */
                if (result) {
                    // no rate for the date has been set
                    transaction.getTransactionEntries().stream()
                            .filter(TransactionEntry::isMultiCurrency)
                            .forEach(entry -> {
                                final ExchangeRate rate = getExchangeRate(entry.getDebitAccount().getCurrencyNode(),
                                        entry.getCreditAccount().getCurrencyNode());

                                if (rate.getRate(transaction.getLocalDate()).compareTo(BigDecimal.ZERO) == 0) { // no rate for the date has been set
                                    final BigDecimal exchangeRate = entry.getDebitAmount().abs()
                                                                            .divide(entry.getCreditAmount().abs(),
                                                                                    MathConstants.mathContext);

                                    setExchangeRate(entry.getCreditAccount().getCurrencyNode(),
                                            entry.getDebitAccount().getCurrencyNode(), exchangeRate, transaction.getLocalDate());
                                }
                            });
                }
            }

            postTransactionAdd(transaction, result);

            return result;
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    public boolean removeTransaction(final Transaction transaction) {

        dataLock.writeLock().lock();

        try {
            for (final Account account : transaction.getAccounts()) {
                if (account.isLocked()) {
                    logWarning(rb.getString("Message.TransactionRemoveLocked"));
                    return false;
                }
            }

            /* Remove the transaction from each account */
            transaction.getAccounts().stream()
                    .filter(account -> !account.removeTransaction(transaction))
                    .forEach(account -> logSevere("Failed to remove the Transaction"));

            logInfo(rb.getString("Message.TransactionRemove"));

            boolean result = getTransactionDAO().removeTransaction(transaction);

            // move transactions into the trash
            if (result) {
                moveObjectToTrash(transaction);
            }

            postTransactionRemove(transaction, result);

            return result;
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Changes the reconciled state of a transaction.
     *
     * @param transaction transaction to change
     * @param account     account to change state for
     * @param state       new reconciled state
     */
    public void setTransactionReconciled(final Transaction transaction, final Account account, final ReconciledState state) {
        dataLock.writeLock().lock(); // hold a write lock to ensure nothing slips in between the remove and add

        try {
            final Transaction newTransaction = (Transaction) transaction.clone();

            ReconcileManager.reconcileTransaction(account, newTransaction, state);

            if (removeTransaction(transaction)) {
                addTransaction(newTransaction);
            }
        } catch (final CloneNotSupportedException e) {
            logger.log(Level.SEVERE, "Failed to reconcile the Transaction", e);
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    public List<String> getTransactionNumberList() {
        dataLock.readLock().lock();

        try {
            return getConfig().getTransactionNumberList();
        } finally {
            dataLock.readLock().unlock();
        }
    }

    public void setTransactionNumberList(final List<String> list) {
        dataLock.writeLock().lock();

        try {
            final Config transactionConfig = getConfig();

            transactionConfig.setTransactionNumberList(list);
            getConfigDAO().update(transactionConfig);

            Message message = new Message(MessageChannel.CONFIG, ChannelEvent.CONFIG_MODIFY, this);
            message.setObject(MessageProperty.CONFIG, transactionConfig);

            messageBus.fireEvent(message);
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Get all transactions.
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

    public Transaction getTransactionByUuid(final UUID uuid) {
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
     * Adds a new Tag
     *
     * @param tag Tag to add
     * @return true is successful
     */
    public boolean addTag(@NotNull Tag tag) {
        Objects.requireNonNull(tag);

        dataLock.writeLock().lock();

        try {
            boolean result = eDAO.getTagDAO().add(tag);

            final Message message = new Message(MessageChannel.TAG, result ? ChannelEvent.TAG_ADD
                                                                            : ChannelEvent.TAG_ADD_FAILED, this);
            message.setObject(MessageProperty.TAG, tag);
            messageBus.fireEvent(message);

            return result;
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Updates an existing Tag
     *
     * @param tag Tag to update
     * @return true is successful
     */
    public boolean updateTag(@NotNull Tag tag) {
        Objects.requireNonNull(tag);

        dataLock.writeLock().lock();

        try {
            boolean result = eDAO.getTagDAO().update(tag);

            final Message message = new Message(MessageChannel.TAG, result ? ChannelEvent.TAG_MODIFY
                                                                            : ChannelEvent.TAG_MODIFY_FAILED, this);
            message.setObject(MessageProperty.TAG, tag);
            messageBus.fireEvent(message);

            return result;
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Retrieves all Tags
     *
     * @return a Set of Tags.
     */
    @NotNull
    public Set<Tag> getTags() {
        dataLock.readLock().lock();

        try {
            return eDAO.getTagDAO().getTags();
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * Retrieves the Tags that are in use
     *
     * @return a Set of Tags.
     */
    @NotNull
    public Set<Tag> getTagsInUse() {
        dataLock.readLock().lock();

        try {
            return getTransactions()
                           .parallelStream()
                           .flatMap((Function<Transaction, Stream<Tag>>) transaction -> transaction.getTags().stream())
                           .collect(Collectors.toSet());
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * Removes a Tag from the database.
     * <p>
     * The removal will fail if the Tag is in use.
     *
     * @param tag Tag to remove
     * @return true if successful
     */
    public boolean removeTag(@NotNull Tag tag) {
        Objects.requireNonNull(tag);

        dataLock.writeLock().lock();

        try {
            boolean result = !getTagsInUse().contains(tag);  // make sure the tag is not used

            if (result) {
                result = moveObjectToTrash(tag);
            }

            final Message message = new Message(MessageChannel.TAG, result ? ChannelEvent.TAG_REMOVE
                                                                            : ChannelEvent.TAG_REMOVE_FAILED, this);
            message.setObject(MessageProperty.TAG, tag);
            messageBus.fireEvent(message);

            return result;
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Returns the unique identifier for this engine instance.
     *
     * @return uuid
     */
    public String getUuid() {
        return uuid;
    }

    public void setPreference(@NotNull final String key, @Nullable final String value) {
        dataLock.writeLock().lock();

        try {
            getConfig().setPreference(key, value);
            getConfigDAO().update(getConfig());

            config = null;  // clear stale cached reference

            Message message = new Message(MessageChannel.CONFIG, ChannelEvent.CONFIG_MODIFY, this);
            message.setObject(MessageProperty.CONFIG, getConfig());
            messageBus.fireEvent(message);
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    @Nullable
    public String getPreference(@NotNull final String key) {
        dataLock.readLock().lock();

        try {
            return getConfig().getPreference(key);
        } finally {
            dataLock.readLock().unlock();
        }
    }

    public boolean getBoolean(@NotNull final String key, final boolean defaultValue) {
        boolean value = defaultValue;

        final String stringResult = getPreference(key);

        if (stringResult != null && !stringResult.isEmpty()) {
            value = Boolean.parseBoolean(stringResult);
        }

        return value;
    }

    public void putBoolean(@NotNull final String key, final boolean value) {
        setPreference(key, Boolean.toString(value));
    }

    public boolean createBackups() {
        return getConfig().createBackups();
    }

    public void setCreateBackups(final boolean createBackups) {
        dataLock.writeLock().lock();

        try {
            final Config backupConfig = getConfig();

            backupConfig.setCreateBackups(createBackups);
            getConfigDAO().update(backupConfig);

            config = null;  // clear stale cached reference

            Message message = new Message(MessageChannel.CONFIG, ChannelEvent.CONFIG_MODIFY, this);
            message.setObject(MessageProperty.CONFIG, backupConfig);
            messageBus.fireEvent(message);
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    public int getRetainedBackupLimit() {
        return getConfig().getRetainedBackupLimit();
    }

    public void setRetainedBackupLimit(final int retainedBackupLimit) {
        dataLock.writeLock().lock();

        try {
            final Config backupConfig = getConfig();

            backupConfig.setRetainedBackupLimit(retainedBackupLimit);
            getConfigDAO().update(backupConfig);

            config = null;  // clear stale cached reference

            Message message = new Message(MessageChannel.CONFIG, ChannelEvent.CONFIG_MODIFY, this);
            message.setObject(MessageProperty.CONFIG, backupConfig);
            messageBus.fireEvent(message);
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    public boolean removeOldBackups() {
        return getConfig().removeOldBackups();
    }

    public void setRemoveOldBackups(final boolean removeOldBackups) {
        dataLock.writeLock().lock();

        try {
            final Config backupConfig = getConfig();

            backupConfig.setRemoveOldBackups(removeOldBackups);
            getConfigDAO().update(backupConfig);

            config = null;  // clear stale cached reference

            Message message = new Message(MessageChannel.CONFIG, ChannelEvent.CONFIG_MODIFY, this);
            message.setObject(MessageProperty.CONFIG, backupConfig);
            messageBus.fireEvent(message);
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Handles Background removal of {@code SecurityNode} history.  This can an expensive operation that block normal
     * operations, so the removal is partitioned into small events to prevent stalling.
     *
     * @param securityNode SecurityNode being processed
     * @param daysOfWeek   Collection of {code DayOfWeek} to remove
     */
    public void removeSecurityHistoryByDayOfWeek(final SecurityNode securityNode, final Collection<DayOfWeek> daysOfWeek) {

        final Thread thread = new Thread(() -> {
            long delay = 0;

            for (final SecurityHistoryNode historyNode : new ArrayList<>(securityNode.getHistoryNodes())) {
                for (final DayOfWeek dayOfWeek : daysOfWeek) {
                    if (historyNode.getLocalDate().getDayOfWeek() == dayOfWeek) {

                        backgroundExecutorService.schedule(new BackgroundCallable(() -> {
                            removeSecurityHistory(securityNode, historyNode.getLocalDate());
                            return true;
                        }), delay, TimeUnit.MILLISECONDS);

                        delay += 750;
                    }
                }
            }
        });

        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /**
     * Thread to monitor background update of securities and terminate is network errors are occurring
     */
    private class SecuritiesUpdateRunnable extends Thread {

        private final List<BackgroundCallable> backgroundCallables;
        private final int delay;

        SecuritiesUpdateRunnable(final List<BackgroundCallable> callables, final int delay) {
            this.backgroundCallables = callables;
            this.delay = delay;
        }

        @Override
        public void run() {

            try {
                TimeUnit.SECONDS.sleep(delay);  // for controlled delay at startup
            } catch (final InterruptedException ignored) {
                return;
            }

            int errors = 0;

            final CompletionService<Boolean> completionService = new ExecutorCompletionService<>(backgroundExecutorService);

            // submit the callables
            for (final BackgroundCallable backgroundCallable : backgroundCallables) {
                try {
                    completionService.submit(backgroundCallable);
                } catch (final RejectedExecutionException ignored) {
                    // ignore, race to shut down the executor was won
                }
            }

            // poll until complete or there have been too many errors
            while (errors < MAX_ERRORS && !Thread.currentThread().isInterrupted()) {
                try {
                    final Future<Boolean> future = completionService.poll(1, TimeUnit.MINUTES);

                    if (future == null) {   // all done, no issues
                        break;
                    }

                    errors += future.get() ? 0 : 1;

                } catch (final InterruptedException | ExecutionException e) {
                    errors = Integer.MAX_VALUE;
                    logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }

            // if there are too many errors, force cancellation
            if (errors > MAX_ERRORS || Thread.currentThread().isInterrupted()) {
                for (final BackgroundCallable backgroundCallable : backgroundCallables) {
                    backgroundCallable.cancel = true;   // stop all other callables
                }
            }
        }
    }

    /**
     * Decorates a Callable to indicate background engine activity is occurring.
     */
    private class BackgroundCallable implements Callable<Boolean> {

        private final Callable<Boolean> callable;

        volatile boolean cancel = false;    // may be set to true to interrupt operation

        BackgroundCallable(@NotNull final Callable<Boolean> callable) {
            this.callable = callable;
        }

        @Override
        public Boolean call() throws Exception {

            if (!cancel) {
                if (backGroundCounter.incrementAndGet() == 1) {
                    messageBus.fireEvent(new Message(MessageChannel.SYSTEM, ChannelEvent.BACKGROUND_PROCESS_STARTED,
                            Engine.this));
                }

                try {
                    return callable.call();
                } finally {
                    if (backGroundCounter.decrementAndGet() == 0) {
                        messageBus.fireEvent(new Message(MessageChannel.SYSTEM, ChannelEvent.BACKGROUND_PROCESS_STOPPED,
                                Engine.this));
                    }
                }
            }
            return false;
        }
    }
}
