/*
 * jGnash, account personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.engine.budget;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;

import jgnash.engine.Account;
import jgnash.engine.StoredObject;
import jgnash.util.NotNull;
import jgnash.util.ResourceUtils;

/**
 * Budget Object
 *
 * @author Craig Cavanaugh
 */
@Entity
public class Budget extends StoredObject implements Comparable<Budget>, Cloneable {

    /**
     * Budget name
     */
    private String name = "Default";

    /**
     * Budget description
     */
    private String description = "";

    /**
     * Period to report the budget in
     */
    @Enumerated(EnumType.STRING)
    private BudgetPeriod budgetPeriod = BudgetPeriod.MONTHLY;

    /**
     * Account goals are stored internally by the account UUID.
     */
    @JoinTable
    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
    private Map<String, BudgetGoal> accountGoals = new HashMap<>();

    private boolean assetAccountsIncluded = false;

    private boolean incomeAccountsIncluded = true;

    private boolean expenseAccountsIncluded = true;

    private boolean liabilityAccountsIncluded = false;

    /**
     * Transient property for the working budget year
     */
    private transient int workingYear = LocalDate.now().getYear();

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        Objects.requireNonNull(name);

        if (name.isEmpty()) {
            throw new IllegalArgumentException("name may not be zero length");
        }

        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = Objects.requireNonNull(description);
    }

    /**
     * Sets the goals for an {@code Account}
     *
     * @param account    Account
     * @param budgetGoal budget goals
     */
    public void setBudgetGoal(final Account account, final BudgetGoal budgetGoal) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(budgetGoal);

        accountGoals.put(account.getUuid(), budgetGoal);
    }

    public void removeBudgetGoal(final Account account) {
        Objects.requireNonNull(account);

        accountGoals.remove(account.getUuid());
    }

    /**
     * Returns and accounts goals. If goals have not yet been specified, then an empty set is automatically assigned and
     * returned
     *
     * @param account {@code Account} to retrieve the goals for
     * @return the goals
     */
    public BudgetGoal getBudgetGoal(final Account account) {
        Objects.requireNonNull(account);

        BudgetGoal goal = accountGoals.get(account.getUuid());

        if (goal == null) {
            goal = new BudgetGoal();
            goal.setBudgetPeriod(getBudgetPeriod());

            accountGoals.put(account.getUuid(), goal);
        }

        return goal;
    }

    @Override
    public int compareTo(@NotNull final Budget budget) {

        int result = getName().compareTo(budget.getName());

        if (result == 0) {
            result = getDescription().compareTo(budget.getDescription());
        }

        if (result == 0) {
            return getUuid().compareTo(budget.getUuid());
        }

        return result;
    }

    @Override
    public boolean equals(final Object other) {
        return this == other || other instanceof Budget && getUuid().equals(((Budget) other).getUuid());
    }

    @Override
    public int hashCode() {
        return getUuid().hashCode();
    }

    /**
     * Returns the global display period for this budget
     *
     * @return The period for this budget
     */
    public BudgetPeriod getBudgetPeriod() {
        return budgetPeriod;
    }

    /**
     * Sets the global display period for this budget
     *
     * @param budgetPeriod The budget period
     */
    public void setBudgetPeriod(final BudgetPeriod budgetPeriod) {
        this.budgetPeriod = Objects.requireNonNull(budgetPeriod);
    }

    /**
     * Returns a clone of this {@code Budget}
     *
     * @return clone
     */
    @Override
    public Object clone() throws CloneNotSupportedException {

        Budget budget = (Budget) super.clone();

        budget.setDescription(getDescription());
        budget.setName(getName() + "(" + ResourceUtils.getString("Word.Copy") + ")");

        budget.accountGoals = new HashMap<>();

        for (final Map.Entry<String, BudgetGoal> entry : accountGoals.entrySet()) {
            budget.accountGoals.put(entry.getKey(), (BudgetGoal) entry.getValue().clone());
        }

        budget.setBudgetPeriod(getBudgetPeriod());

        return budget;
    }

    /**
     * Returns the working year for the budget
     *
     * @return the activeYear
     */
    public int getWorkingYear() {
        return workingYear;
    }

    /**
     * Sets the working year for the budget.
     * <p/>
     * This is a transient property primarily used to help UI component communicate the working year.
     *
     * @param workingYear the working year to set
     */
    public void setWorkingYear(int workingYear) {
        this.workingYear = workingYear;
    }

    /**
     * @return the assetAccountsIncluded
     */
    public boolean areAssetAccountsIncluded() {
        return assetAccountsIncluded;
    }

    /**
     * @param assetAccountsIncluded the assetAccountsIncluded to set
     */
    public void setAssetAccountsIncluded(final boolean assetAccountsIncluded) {
        this.assetAccountsIncluded = assetAccountsIncluded;
    }

    /**
     * @return the incomeAccountsIncluded
     */
    public boolean areIncomeAccountsIncluded() {
        return incomeAccountsIncluded;
    }

    /**
     * @param incomeAccountsIncluded the incomeAccountsIncluded to set
     */
    public void setIncomeAccountsIncluded(final boolean incomeAccountsIncluded) {
        this.incomeAccountsIncluded = incomeAccountsIncluded;
    }

    /**
     * @return the expenseAccountsIncluded
     */
    public boolean areExpenseAccountsIncluded() {
        return expenseAccountsIncluded;
    }

    /**
     * @param expenseAccountsIncluded the expenseAccountsIncluded to set
     */
    public void setExpenseAccountsIncluded(final boolean expenseAccountsIncluded) {
        this.expenseAccountsIncluded = expenseAccountsIncluded;
    }

    /**
     * @return the liabilityAccountsIncluded
     */
    public boolean areLiabilityAccountsIncluded() {
        return liabilityAccountsIncluded;
    }

    /**
     * @param liabilityAccountsIncluded the liabilityAccountsIncluded to set
     */
    public void setLiabilityAccountsIncluded(final boolean liabilityAccountsIncluded) {
        this.liabilityAccountsIncluded = liabilityAccountsIncluded;
    }

    /**
     * Overridden to return the name of the budget for convience.
     *
     * @return name of the budget
     */
    @Override
    public String toString() {
        return getName();
    }

    protected Object readResolve() {
        postLoad();
        return this;
    }

    @PostLoad
    protected void postLoad() {
        workingYear = LocalDate.now().getYear();
    }
}
