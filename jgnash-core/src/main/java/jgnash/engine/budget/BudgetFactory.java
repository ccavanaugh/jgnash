/*
 * jGnash, account personal finance application
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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.engine.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.util.DateUtils;

/**
 * Budget Factory for automatic generation of Budgets
 *
 * @author Craig Cavanaugh
 */
public class BudgetFactory {

    /**
     * Utility class, no public constructor
     */
    private BudgetFactory() {
    }

    public static Budget buildAverageBudget(final BudgetPeriod budgetPeriod, final String name, final boolean round) {
        Budget budget = new Budget();
        budget.setName(name);
        budget.setBudgetPeriod(budgetPeriod);

        int year = DateUtils.getCurrentYear() - 1;

        List<BudgetPeriodDescriptor> descriptors = BudgetPeriodDescriptorFactory.getDescriptors(year, budgetPeriod);

        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        List<Account> accounts = new ArrayList<>();
        accounts.addAll(engine.getIncomeAccountList());
        accounts.addAll(engine.getExpenseAccountList());

        for (Account account : accounts) {
            budget.setBudgetGoal(account, buildAverageBudgetGoal(account, descriptors, round));
        }

        return budget;
    }

    public static BudgetGoal buildAverageBudgetGoal(final Account account, final List<BudgetPeriodDescriptor> descriptors, final boolean round) {
        BudgetGoal goal = new BudgetGoal();

        goal.setBudgetPeriod(descriptors.get(0).getBudgetPeriod());

        for (BudgetPeriodDescriptor descriptor : descriptors) {
            BigDecimal amount = account.getBalance(descriptor.getStartDate(), descriptor.getEndDate());

            if (account.getAccountType() == AccountType.INCOME) {
                amount = amount.negate();
            }

            if (round) {
                if (account.getAccountType() == AccountType.INCOME) {
                    amount = amount.setScale(0, RoundingMode.DOWN);
                } else {
                    amount = amount.setScale(0, RoundingMode.UP);
                }
            }

            goal.setGoal(descriptor.getStartPeriod(), descriptor.getEndPeriod(), amount);
        }

        return goal;
    }

    /**
     * Creates a <code>BudgetGoal</code> with an alternating pattern
     *
     * @param account  Account for BudgetGoal
     * @param descriptors descriptors to use
     * @param pattern Pattern to use
     * @param startRow starting row, 0 based index is assumed
     * @param endRow ending row, 0 based index is assumed
     * @param amount amount to use
     * @return new <code>BudgetGoal</code>
     */
    public static BudgetGoal buildBudgetGoal(final BudgetGoal baseBudgetGoal, final Account account, final List<BudgetPeriodDescriptor> descriptors, final Pattern pattern, final int startRow, final int endRow, final BigDecimal amount) {
        BudgetGoal goal;

        try {
            goal = (BudgetGoal)baseBudgetGoal.clone();
        } catch (CloneNotSupportedException e) {
            Logger.getLogger(BudgetFactory.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            goal = new BudgetGoal();
        }

        goal.setBudgetPeriod(descriptors.get(0).getBudgetPeriod());

        //System.out.println("Pattern: " + pattern.getIncrement());

        for (int i = startRow; i <= endRow; i += pattern.getIncrement()) {

            //System.out.println(i);

            BudgetPeriodDescriptor descriptor = descriptors.get(i);

            goal.setBudgetPeriod(descriptor.getBudgetPeriod());

            if (account.getAccountType() == AccountType.INCOME) {   // negate for income only
                goal.setGoal(descriptor.getStartPeriod(), descriptor.getEndPeriod(), amount.negate());
            } else {
                goal.setGoal(descriptor.getStartPeriod(), descriptor.getEndPeriod(), amount);
            }
        }

        return goal;
    }
}
