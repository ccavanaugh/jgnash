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
package jgnash.engine.budget;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.MathConstants;
import jgnash.engine.Transaction;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.engine.message.MessageProxy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Model for budget results
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

    /**
     * Message proxy
     */
    private final MessageProxy proxy = new MessageProxy();

    public BudgetResultsModel(final Budget budget, final int year, final CurrencyNode baseCurrency) {
        this.budget = budget;
        this.descriptorList = BudgetPeriodDescriptorFactory.getDescriptors(year, this.budget.getBudgetPeriod());
        this.baseCurrency = baseCurrency;

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
     * budget hierarchy
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
     * Determines if the account will be added to the model
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

        return result;
    }

    private void loadAccounts() {
        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        Set<Account> accountSet = new HashSet<>();

        for (Account account : engine.getAccountList()) {
            if (includeAccount(account)) {
                accountSet.add(account);
            }
        }

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
            EnumSet<AccountGroup> accountSet = EnumSet.noneOf(AccountGroup.class);

            for (Account account : accounts) {
                accountSet.add(account.getAccountType().getAccountGroup());
            }

            // create a list and sort
            List<AccountGroup> groups = new ArrayList<>(accountSet);
            Collections.sort(groups);

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

    final Set<Account> getAccounts(final AccountGroup group) {
        accountLock.readLock().lock();

        try {
            Set<Account> accountSet = new HashSet<>();

            for (Account account : accounts) {
                if (account.memberOf(group)) {
                    accountSet.add(account);
                }
            }

            return accountSet;
        } finally {
            accountLock.readLock().unlock();
        }
    }

    /**
     * Gets results by descriptor and account (per account results)
     *
     * @param descriptor BudgetPeriodDescriptor descriptor
     * @param account Account
     * @return cached or newly created BudgetPeriodResults
     */
    public BudgetPeriodResults getResults(final BudgetPeriodDescriptor descriptor, final Account account) {
        cacheLock.lock();

        try {

            Map<Account, BudgetPeriodResults> resultsMap = descriptorAccountResultsCache.get(descriptor);

            if (resultsMap == null) {
                resultsMap = new HashMap<>();
                descriptorAccountResultsCache.put(descriptor, resultsMap);
            }

            BudgetPeriodResults results = resultsMap.get(account);

            if (results == null) {
                results = buildAccountResults(descriptor, account);
                resultsMap.put(account, results);
            }

            return results;
        } finally {
            cacheLock.unlock();
        }
    }

    private void clear(final BudgetPeriodDescriptor descriptor, final Account account) {
        cacheLock.lock();

        try {
            Map<Account, BudgetPeriodResults> resultsMap = descriptorAccountResultsCache.get(descriptor);

            if (resultsMap != null) {
                resultsMap.remove(account);
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
     * Gets summary result by descriptor and account group (column summary by
     * AccountGroup)
     *
     * @param descriptor BudgetPeriodDescriptor for summary
     * @param group AccountGroup for summary
     * @return summary results
     */
    public BudgetPeriodResults getResults(final BudgetPeriodDescriptor descriptor, final AccountGroup group) {
        cacheLock.lock();

        try {
            Map<AccountGroup, BudgetPeriodResults> resultsMap = descriptorAccountGroupResultsCache.get(descriptor);

            if (resultsMap == null) {
                resultsMap = new EnumMap<>(AccountGroup.class);
                descriptorAccountGroupResultsCache.put(descriptor, resultsMap);
            }

            BudgetPeriodResults results = resultsMap.get(group);

            if (results == null) {
                results = buildResults(descriptor, group);
                resultsMap.put(group, results);
            }

            return results;
        } finally {
            cacheLock.unlock();
        }
    }

    private void clear(final BudgetPeriodDescriptor descriptor, final AccountGroup group) {
        cacheLock.lock();

        try {
            Map<AccountGroup, BudgetPeriodResults> resultsMap = descriptorAccountGroupResultsCache.get(descriptor);

            if (resultsMap != null) {
                resultsMap.remove(group);
            }
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Gets summary result by account (row summary)
     *
     * @param account Account for summary
     * @return summary results
     */
    public BudgetPeriodResults getResults(final Account account) {
        cacheLock.lock();

        try {
            BudgetPeriodResults results = accountResultsCache.get(account);

            if (results == null) {
                results = buildResults(account);
                accountResultsCache.put(account, results);
            }

            return results;
        } finally {
            cacheLock.unlock();
        }
    }

    private void clear(final Account account) {
        cacheLock.lock();

        try {
            BudgetPeriodResults results = accountResultsCache.get(account);

            if (results != null) {
                accountResultsCache.remove(account);
            }
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Gets summary result by account group (corner summary)
     *
     * @param accountGroup AccountGroup for summary
     * @return summary results
     */
    public BudgetPeriodResults getResults(final AccountGroup accountGroup) {
        cacheLock.lock();

        try {
            BudgetPeriodResults results = accountGroupResultsCache.get(accountGroup);

            if (results == null) {
                results = buildResults(accountGroup);
                accountGroupResultsCache.put(accountGroup, results);
            }

            return results;
        } finally {
            cacheLock.unlock();
        }
    }

    private void clear(final AccountGroup accountGroup) {
        cacheLock.lock();

        try {
            BudgetPeriodResults results = accountGroupResultsCache.get(accountGroup);

            if (results != null) {
                accountGroupResultsCache.remove(accountGroup);
            }
        } finally {
            cacheLock.unlock();
        }
    }

    private BudgetPeriodResults buildAccountResults(final BudgetPeriodDescriptor descriptor, final Account account) {
        final BudgetPeriodResults results = new BudgetPeriodResults();

        accountLock.readLock().lock();

        try {

            // calculate the this account's results
            if (accounts.contains(account)) {
                BudgetGoal goal = budget.getBudgetGoal(account);

                results.setBudgeted(goal.getGoal(descriptor.getStartPeriod(), descriptor.getEndPeriod()));
                results.setChange(account.getBalance(descriptor.getStartDate(), descriptor.getEndDate()).abs());

                // calculate the remaining amount for the budget
                BigDecimal remaining = results.getBudgeted().subtract(results.getChange());

                // reverse the sign if this is an income account
                if (account.getAccountType() == AccountType.INCOME) {
                    remaining = remaining.negate();
                }

                results.setRemaining(remaining);
            }

            // recursive decent to add child account results and handle exchange rates
            for (Account child : account.getChildren()) {
                BudgetPeriodResults childResults = buildAccountResults(descriptor, child);

                BigDecimal exchangeRate = child.getCurrencyNode().getExchangeRate(account.getCurrencyNode());

                results.setChange(results.getChange().add(childResults.getChange().multiply(exchangeRate)));
                results.setBudgeted(results.getBudgeted().add(childResults.getBudgeted().multiply(exchangeRate)));
                results.setRemaining(results.getRemaining().add(childResults.getRemaining().multiply(exchangeRate)));
            }
        } finally {
            accountLock.readLock().unlock();
        }

        results.setChange(results.getChange().setScale(account.getCurrencyNode().getScale(), MathConstants.roundingMode));
        results.setBudgeted(results.getBudgeted().setScale(account.getCurrencyNode().getScale(), MathConstants.roundingMode));
        results.setRemaining(results.getRemaining().setScale(account.getCurrencyNode().getScale(), MathConstants.roundingMode));

        return results;
    }

    private BudgetPeriodResults buildResults(final BudgetPeriodDescriptor descriptor, final AccountGroup group) {
        BigDecimal remainingTotal = BigDecimal.ZERO;
        BigDecimal totalChange = BigDecimal.ZERO;
        BigDecimal totalBudgeted = BigDecimal.ZERO;

        accountLock.readLock().lock();

        try {
            for (Account account : getAccounts(group)) {

                // only sum for the top level accounts within the budget model
                // top level account if the parent is not included in the budget model
                if (!accounts.contains(account.getParent())) {

                    BudgetPeriodResults periodResults = getResults(descriptor, account);

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

        BudgetPeriodResults results = new BudgetPeriodResults();

        results.setBudgeted(totalBudgeted.setScale(baseCurrency.getScale(), MathConstants.roundingMode));
        results.setRemaining(remainingTotal.setScale(baseCurrency.getScale(), MathConstants.roundingMode));
        results.setChange(totalChange.setScale(baseCurrency.getScale(), MathConstants.roundingMode));

        return results;
    }

    BudgetPeriodResults buildResults(final Account account) {
        BigDecimal change = BigDecimal.ZERO;
        BigDecimal budgeted = BigDecimal.ZERO;
        BigDecimal remaining = BigDecimal.ZERO;

        accountLock.readLock().lock();

        try {
            for (BudgetPeriodDescriptor descriptor : descriptorList) {
                BudgetPeriodResults periodResults = getResults(descriptor, account);

                change = change.add(periodResults.getChange());
                budgeted = budgeted.add(periodResults.getBudgeted());
                remaining = remaining.add(periodResults.getRemaining());
            }
        } finally {
            accountLock.readLock().unlock();
        }

        BudgetPeriodResults results = new BudgetPeriodResults();

        results.setChange(change);
        results.setBudgeted(budgeted);
        results.setRemaining(remaining);

        return results;
    }

    BudgetPeriodResults buildResults(final AccountGroup group) {
        BigDecimal totalChange = BigDecimal.ZERO;
        BigDecimal totalBudgeted = BigDecimal.ZERO;
        BigDecimal totalRemaining = BigDecimal.ZERO;

        accountLock.readLock().lock();

        try {
            for (BudgetPeriodDescriptor descriptor : descriptorList) {
                BudgetPeriodResults periodResults = getResults(descriptor, group);

                totalChange = totalChange.add(periodResults.getChange());
                totalBudgeted = totalBudgeted.add(periodResults.getBudgeted());
                totalRemaining = totalRemaining.add(periodResults.getRemaining());
            }
        } finally {
            accountLock.readLock().unlock();
        }

        BudgetPeriodResults results = new BudgetPeriodResults();

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
                for (Account ancestor : account.getAncestors()) {
                    if (accounts.contains(ancestor)) {
                        for (BudgetPeriodDescriptor descriptor : descriptorList) {
                            clear(ancestor);
                            clear(descriptor, ancestor);
                            clear(ancestor.getAccountType().getAccountGroup()); // could be mixed group tree
                            clear(descriptor, ancestor.getAccountType().getAccountGroup());
                        }
                    }
                }
            } finally {
                cacheLock.unlock();
            }
        } finally {
            accountLock.readLock().unlock();
        }
    }

    private void processAccountEvent(final Message message) {
        Account account = (Account) message.getObject(MessageProperty.ACCOUNT);

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
        Budget messageBudget = (Budget) message.getObject(MessageProperty.BUDGET);

        if (budget.equals(messageBudget)) {
            switch (message.getEvent()) {
                case BUDGET_UPDATE:
                    clearCached();
                    break;
                case BUDGET_GOAL_UPDATE:
                    Account account = (Account) message.getObject(MessageProperty.ACCOUNT);
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
        final Transaction transaction = (Transaction) message.getObject(MessageProperty.TRANSACTION);

        for (BudgetPeriodDescriptor descriptor : descriptorList) {
            if (descriptor.isBetween(transaction.getDate())) {
                final Set<Account> accountSet = new HashSet<>();

                for (Account account : transaction.getAccounts()) {
                    accountSet.addAll(account.getAncestors());
                }

                for (Account account : accountSet) {
                    clearCached(account);
                }
            }
        }
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
