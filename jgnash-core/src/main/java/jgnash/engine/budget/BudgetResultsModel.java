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
package jgnash.engine.budget;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.Comparators;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.RootAccount;
import jgnash.engine.Transaction;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.engine.message.MessageProxy;

/**
 * Model for budget results.
 *
 * @author Craig Cavanaugh
 */
public class BudgetResultsModel implements MessageListener {

    private Set<Account> accounts = new HashSet<>();

    private final Budget budget;

    private final CurrencyNode baseCurrency;

    private List<AccountGroup> accountGroupList;

    private final List<BudgetPeriodDescriptor> descriptorList;

    private final ReentrantReadWriteLock accountLock = new ReentrantReadWriteLock();

    private final ReentrantLock cacheLock = new ReentrantLock();

    private final Map<Account, BudgetPeriodResults> accountResultsCache;

    private final Map<AccountGroup, BudgetPeriodResults> accountGroupResultsCache;

    private final Map<BudgetPeriodDescriptor, Map<Account, BudgetPeriodResults>> descriptorAccountResultsCache;

    private final Map<BudgetPeriodDescriptor, Map<AccountGroup, BudgetPeriodResults>> descriptorAccountGroupResultsCache;

    private final boolean useRunningTotals;

    /**
     * Message proxy.
     */
    private final MessageProxy proxy = new MessageProxy();

    public BudgetResultsModel(final Budget budget, final int year, final CurrencyNode baseCurrency, final boolean useRunningTotals) {
        this.budget = budget;
        this.descriptorList = BudgetPeriodDescriptorFactory.getDescriptors(year, budget.getStartMonth(), budget.getBudgetPeriod());

        this.baseCurrency = baseCurrency;
        this.useRunningTotals = useRunningTotals;

        accountResultsCache = new HashMap<>();
        accountGroupResultsCache = new EnumMap<>(AccountGroup.class);
        descriptorAccountResultsCache = new HashMap<>();
        descriptorAccountGroupResultsCache = new HashMap<>();

        loadAccounts();
        loadAccountGroups();

        registerListeners();
    }

    public Budget getBudget() {
        return budget;
    }

    public CurrencyNode getBaseCurrency() {
        return baseCurrency;
    }

    /**
     * Returns the depth of the account relative to topmost account in the
     * budget hierarchy.
     *
     * @param account Account to get depth for
     * @return depth depth relative to accounts to be shown in the budget
     * @see Account#getDepth()
     */
    public int getDepth(final Account account) {

        int depth = 0;

        Account parent = account.getParent();

        while (parent != null) {
            if (accounts.contains(parent)) {
                depth++;
            }
            parent = parent.getParent();
        }

        return depth;
    }

    private boolean areParentsIncluded(final Account account) {
        boolean result = true;

        Account parent = account.getParent();

        if (parent != null && !(parent instanceof RootAccount)) {
            if (!includeAccount(parent)) {
                result = false;
            } else {
                result = areParentsIncluded(parent);
            }
        }

        return result;
    }

    public List<BudgetPeriodDescriptor> getDescriptorList() {
        return descriptorList;
    }

    private void registerListeners() {
        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT, MessageChannel.BUDGET, MessageChannel.SYSTEM, MessageChannel.TRANSACTION);
    }

    private void unregisterListeners() {
        MessageBus.getInstance().unregisterListener(this, MessageChannel.ACCOUNT, MessageChannel.BUDGET, MessageChannel.SYSTEM, MessageChannel.TRANSACTION);
    }

    public synchronized void addMessageListener(final MessageListener messageListener) {
        proxy.addMessageListener(messageListener);
    }

    public synchronized void removeMessageListener(final MessageListener messageListener) {
        proxy.removeMessageListener(messageListener);
    }

    /**
     * Determines if the account will be added to the model.
     *
     * @param account Account to test
     * @return true if it should be added
     */
    public boolean includeAccount(final Account account) {

        boolean result = false;

        // account must be visible and not marked for exclusion from a budget
        if (account.isVisible() && !account.isExcludedFromBudget()) {
            if (account.memberOf(AccountGroup.INCOME) && budget.areIncomeAccountsIncluded()) {
                result = true;
            } else if (account.memberOf(AccountGroup.EXPENSE) && budget.areExpenseAccountsIncluded()) {
                result = true;
            } else if (account.memberOf(AccountGroup.ASSET) && budget.areAssetAccountsIncluded()) {
                result = true;
            } else if (account.memberOf(AccountGroup.LIABILITY) && budget.areLiabilityAccountsIncluded()) {
                result = true;
            }
        }

        if (result) {
            result = areParentsIncluded(account);
        }

        return result;
    }

    private void loadAccounts() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        Set<Account> accountSet = engine.getAccountList().stream()
                .filter(this::includeAccount).collect(Collectors.toSet());

        accountLock.writeLock().lock();

        try {
            accounts = accountSet;
        } finally {
            accountLock.writeLock().unlock();
        }
    }

    private void loadAccountGroups() {
        accountLock.writeLock().lock();

        try {
            final EnumSet<AccountGroup> accountSet = EnumSet.noneOf(AccountGroup.class);

            accountSet.addAll(accounts.stream()
                    .map(account -> account.getAccountType().getAccountGroup()).collect(Collectors.toList()));

            // create a list and sort
            List<AccountGroup> groups = new ArrayList<>(accountSet);

            // Set an explicit sort order
            groups.sort(new Comparators.ExplicitComparator<>(AccountGroup.INCOME,
                    AccountGroup.EXPENSE, AccountGroup.ASSET, AccountGroup.LIABILITY));

            accountGroupList = groups;
        } finally {
            accountLock.writeLock().unlock();
        }
    }

    public List<AccountGroup> getAccountGroupList() {
        accountLock.readLock().lock();

        try {
            return accountGroupList;
        } finally {
            accountLock.readLock().unlock();
        }
    }

    public final Set<Account> getAccounts() {
        accountLock.readLock().lock();

        try {
            // return a defensive copy
            return new HashSet<>(accounts);
        } finally {
            accountLock.readLock().unlock();
        }
    }

    private Set<Account> getAccounts(final AccountGroup group) {
        accountLock.readLock().lock();

        try {
            return accounts.stream().filter(account -> account.memberOf(group)).collect(Collectors.toSet());
        } finally {
            accountLock.readLock().unlock();
        }
    }

    private void clear(final BudgetPeriodDescriptor descriptor, final Account account) {
        cacheLock.lock();

        try {
            final Map<Account, BudgetPeriodResults> resultsMap = descriptorAccountResultsCache.get(descriptor);

            if (resultsMap != null) {
                resultsMap.remove(account);
            }
        } finally {
            cacheLock.unlock();
        }
    }

    private void clear(final BudgetPeriodDescriptor descriptor, final AccountGroup group) {
        cacheLock.lock();

        try {
            final Map<AccountGroup, BudgetPeriodResults> resultsMap = descriptorAccountGroupResultsCache.get(descriptor);

            if (resultsMap != null) {
                resultsMap.remove(group);
            }
        } finally {
            cacheLock.unlock();
        }
    }

    private void clear(final Account account) {
        cacheLock.lock();

        try {
            accountResultsCache.remove(account);
        } finally {
            cacheLock.unlock();
        }
    }

    private void clear(final AccountGroup accountGroup) {
        cacheLock.lock();

        try {
            final BudgetPeriodResults results = accountGroupResultsCache.get(accountGroup);

            if (results != null) {
                accountGroupResultsCache.remove(accountGroup);
            }
        } finally {
            cacheLock.unlock();
        }
    }

    private void clearCached() {
        cacheLock.lock();

        try {
            accountResultsCache.clear();
            accountGroupResultsCache.clear();
            descriptorAccountResultsCache.clear();
            descriptorAccountGroupResultsCache.clear();
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Gets results by descriptor and account (per account results).
     *
     * @param descriptor BudgetPeriodDescriptor descriptor
     * @param account    Account
     * @return cached or newly created BudgetPeriodResults
     */
    public BudgetPeriodResults getResults(final BudgetPeriodDescriptor descriptor, final Account account) {
        cacheLock.lock();

        try {
            final Map<Account, BudgetPeriodResults> resultsMap
                    = descriptorAccountResultsCache.computeIfAbsent(descriptor, k -> new HashMap<>());

            return resultsMap.computeIfAbsent(account, k -> buildAccountResults(descriptor, account, true));
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Gets summary result by descriptor and account group (column summary by
     * AccountGroup).
     *
     * @param descriptor BudgetPeriodDescriptor for summary
     * @param group      AccountGroup for summary
     * @return summary results
     */
    public BudgetPeriodResults getResults(final BudgetPeriodDescriptor descriptor, final AccountGroup group) {
        cacheLock.lock();

        try {
            final Map<AccountGroup, BudgetPeriodResults> resultsMap = descriptorAccountGroupResultsCache
                    .computeIfAbsent(descriptor, k -> new EnumMap<>(AccountGroup.class));

            return resultsMap.computeIfAbsent(group, k -> buildResults(descriptor, group));
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Gets summary result by account (row summary).
     *
     * @param account Account for summary
     * @return summary results
     */
    public BudgetPeriodResults getResults(final Account account) {
        cacheLock.lock();

        try {
            return accountResultsCache.computeIfAbsent(account, k -> buildResults(account));
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Gets summary result by account group (corner summary).
     *
     * @param accountGroup AccountGroup for summary
     * @return summary results
     */
    public BudgetPeriodResults getResults(final AccountGroup accountGroup) {
        cacheLock.lock();

        try {
            return accountGroupResultsCache.computeIfAbsent(accountGroup, k -> buildResults(accountGroup));
        } finally {
            cacheLock.unlock();
        }
    }


    private BudgetPeriodResults buildAccountResults(final BudgetPeriodDescriptor descriptor, final Account account,
                                                    final boolean includeBaseAccountResults) {
        final BudgetPeriodResults results = new BudgetPeriodResults();

        accountLock.readLock().lock();

        try {
            // calculate this account's results
            if (accounts.contains(account)) {
                final BudgetGoal goal = budget.getBudgetGoal(account);

                results.setBudgeted(goal.getGoal(descriptor.getStartPeriod(), descriptor.getEndPeriod(),
                        descriptor.getStartDate().isLeapYear()));

                // calculate the change and remaining amount for the budget
                if (account.getAccountType() == AccountType.INCOME) {
                    results.setChange(account.getBalance(descriptor.getStartDate(), descriptor.getEndDate()).negate());
                    results.setRemaining(results.getChange().subtract(results.getBudgeted()));
                } else {
                    results.setChange(account.getBalance(descriptor.getStartDate(), descriptor.getEndDate()));
                    results.setRemaining(results.getBudgeted().subtract(results.getChange()));
                }

                final int index = descriptorList.indexOf(descriptor);

                // per account running total
                if (useRunningTotals && index > 0 && includeBaseAccountResults) {
                    final BudgetPeriodResults priorResults = getResults(descriptorList.get(index - 1), account);

                    results.setBudgeted(results.getBudgeted().add(priorResults.getBudgeted()));

                    results.setChange(results.getChange().add(priorResults.getChange()));
                    results.setRemaining(results.getRemaining().add(priorResults.getRemaining()));
                }
            }

            // recursive decent to add child account results and handle exchange rates
            for (final Account child : account.getChildren(Comparators.getAccountByCode())) {
                final BudgetPeriodResults childResults = buildAccountResults(descriptor, child, false);

                final BigDecimal exchangeRate = child.getCurrencyNode().getExchangeRate(account.getCurrencyNode());

                // reverse sign if the parent account is an income account but the child is not, or vice versa
                final BigDecimal sign = ((account.getAccountType() == AccountType.INCOME) !=
                        (child.getAccountType() == AccountType.INCOME)) ? BigDecimal.ONE.negate() : BigDecimal.ONE;

                results.setChange(results.getChange().add(childResults.getChange().multiply(exchangeRate).multiply(sign)));
                results.setBudgeted(results.getBudgeted().add(childResults.getBudgeted().multiply(exchangeRate).multiply(sign)));
                results.setRemaining(results.getRemaining().add(childResults.getRemaining().multiply(exchangeRate)));
            }

        } finally {
            accountLock.readLock().unlock();
        }

        // rescale the results
        results.setChange(results.getChange().setScale(budget.getRoundingScale(), budget.getRoundingMode()));
        results.setBudgeted(results.getBudgeted().setScale(budget.getRoundingScale(), budget.getRoundingMode()));
        results.setRemaining(results.getRemaining().setScale(budget.getRoundingScale(), budget.getRoundingMode()));

        return results;
    }

    private BudgetPeriodResults buildResults(final BudgetPeriodDescriptor descriptor, final AccountGroup group) {
        BigDecimal remainingTotal = BigDecimal.ZERO;
        BigDecimal totalChange = BigDecimal.ZERO;
        BigDecimal totalBudgeted = BigDecimal.ZERO;

        accountLock.readLock().lock();

        try {
            for (final Account account : getAccounts(group)) {

                // only sum for the top level accounts within the budget model
                // top level account if the parent is not included in the budget model
                if (!accounts.contains(account.getParent())) {

                    final BudgetPeriodResults periodResults = getResults(descriptor, account);

                    final BigDecimal remaining = periodResults.getRemaining();
                    remainingTotal = remainingTotal.add(remaining.multiply(baseCurrency.getExchangeRate(account.getCurrencyNode())));

                    final BigDecimal change = periodResults.getChange();
                    totalChange = totalChange.add(change.multiply(baseCurrency.getExchangeRate(account.getCurrencyNode())));

                    final BigDecimal budgeted = periodResults.getBudgeted();
                    totalBudgeted = totalBudgeted.add(budgeted.multiply(baseCurrency.getExchangeRate(account.getCurrencyNode())));
                }
            }
        } finally {
            accountLock.readLock().unlock();
        }

        final BudgetPeriodResults results = new BudgetPeriodResults();

        if (useRunningTotals) {
            final int index = descriptorList.indexOf(descriptor);
            if (index > 0) {
                final BudgetPeriodResults priorResults = getResults(descriptorList.get(index - 1), group);

                results.setBudgeted(results.getBudgeted().add(priorResults.getBudgeted()));
                results.setChange(results.getChange().add(priorResults.getChange()));
                results.setRemaining(results.getRemaining().add(priorResults.getRemaining()));
            }
        }

        // rescale the results
        results.setBudgeted(totalBudgeted.setScale(budget.getRoundingScale(), budget.getRoundingMode()));
        results.setRemaining(remainingTotal.setScale(budget.getRoundingScale(), budget.getRoundingMode()));
        results.setChange(totalChange.setScale(budget.getRoundingScale(), budget.getRoundingMode()));

        return results;
    }

    private BudgetPeriodResults buildResults(final Account account) {
        BigDecimal change = BigDecimal.ZERO;
        BigDecimal budgeted = BigDecimal.ZERO;
        BigDecimal remaining = BigDecimal.ZERO;

        accountLock.readLock().lock();

        try {
            if (useRunningTotals) { // just use the last descriptor
                final BudgetPeriodResults periodResults
                        = getResults(descriptorList.get(descriptorList.size() - 1), account);

                change = periodResults.getChange();
                budgeted = periodResults.getBudgeted();
                remaining = periodResults.getRemaining();
            } else {
                for (final BudgetPeriodDescriptor descriptor : descriptorList) {
                    final BudgetPeriodResults periodResults = getResults(descriptor, account);

                    change = change.add(periodResults.getChange());
                    budgeted = budgeted.add(periodResults.getBudgeted());
                    remaining = remaining.add(periodResults.getRemaining());
                }
            }
        } finally {
            accountLock.readLock().unlock();
        }

        final BudgetPeriodResults results = new BudgetPeriodResults();

        results.setChange(change);
        results.setBudgeted(budgeted);
        results.setRemaining(remaining);

        return results;
    }

    private BudgetPeriodResults buildResults(final AccountGroup group) {
        BigDecimal totalChange = BigDecimal.ZERO;
        BigDecimal totalBudgeted = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;

        accountLock.readLock().lock();

        try {

            if (useRunningTotals) {
                final BudgetPeriodResults periodResults
                        = getResults(descriptorList.get(descriptorList.size() - 1), group);

                totalChange = periodResults.getChange();
                totalBudgeted = periodResults.getBudgeted();
                totalRemaining = periodResults.getRemaining();
            } else {
                for (final BudgetPeriodDescriptor descriptor : descriptorList) {
                    final BudgetPeriodResults periodResults = getResults(descriptor, group);

                    totalChange = totalChange.add(periodResults.getChange());
                    totalBudgeted = totalBudgeted.add(periodResults.getBudgeted());
                    totalRemaining = totalRemaining.add(periodResults.getRemaining());
                }
            }
        } finally {
            accountLock.readLock().unlock();
        }

        final BudgetPeriodResults results = new BudgetPeriodResults();

        results.setChange(totalChange);
        results.setBudgeted(totalBudgeted);
        results.setRemaining(totalRemaining);

        return results;
    }

    private void clearCached(final Account account) {
        accountLock.readLock().lock();

        try {
            cacheLock.lock();

            try {
                // clear cached results
                // could be mixed group tree
                account.getAncestors().stream().filter(accounts::contains).forEach(ancestor -> {
                    for (BudgetPeriodDescriptor descriptor : descriptorList) {
                        clear(ancestor);
                        clear(descriptor, ancestor);
                        clear(ancestor.getAccountType().getAccountGroup()); // could be mixed group tree
                        clear(descriptor, ancestor.getAccountType().getAccountGroup());
                    }
                });
            } finally {
                cacheLock.unlock();
            }
        } finally {
            accountLock.readLock().unlock();
        }
    }

    private void processAccountEvent(final Message message) {
        Account account = message.getObject(MessageProperty.ACCOUNT);

        switch (message.getEvent()) {
            case ACCOUNT_ADD:
                accounts.add(account);
                clearCached(account);
                break;
            case ACCOUNT_REMOVE:
                accounts.remove(account);
                clearCached(account);
                break;
            case ACCOUNT_MODIFY:
                loadAccounts(); // force a reload of accounts, structure is indeterminate
                clearCached();  // indeterminate structure, so dump all cached results
                break;
            default:
                break;
        }

        loadAccountGroups();    // force reload of account groups after accounts have changed
    }

    private void processBudgetEvent(final Message message) {
        Budget messageBudget = message.getObject(MessageProperty.BUDGET);

        if (budget.equals(messageBudget)) {
            switch (message.getEvent()) {
                case BUDGET_UPDATE:
                    clearCached();
                    break;
                case BUDGET_GOAL_UPDATE:
                    Account account = message.getObject(MessageProperty.ACCOUNT);
                    clearCached(account);
                    break;
                case BUDGET_REMOVE:
                    unregisterListeners();
                    clearCached();
                    break;
                default:
            }
        }
    }

    private void processTransactionEvent(final Message message) {
        final Transaction transaction = message.getObject(MessageProperty.TRANSACTION);

        descriptorList.stream().filter(descriptor -> descriptor.isBetween(transaction.getLocalDate()))
                .forEach(descriptor -> {
                    final Set<Account> accountSet = new HashSet<>();

                    for (Account account : transaction.getAccounts()) {
                        accountSet.addAll(account.getAncestors());
                    }

                    accountSet.forEach(this::clearCached);
                });
    }

    @Override
    public void messagePosted(final Message message) {
        switch (message.getEvent()) {
            case ACCOUNT_ADD:
            case ACCOUNT_MODIFY:
            case ACCOUNT_MODIFY_FAILED:
                processAccountEvent(message);
                break;
            case BUDGET_UPDATE:
            case BUDGET_GOAL_UPDATE:
            case BUDGET_REMOVE:
                processBudgetEvent(message);
                break;
            case TRANSACTION_ADD:
            case TRANSACTION_REMOVE:
                processTransactionEvent(message);
                break;
            case FILE_CLOSING:
                unregisterListeners();
                clearCached();
                break;
            default:
        }

        proxy.forwardMessage(message);
    }
}
