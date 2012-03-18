/*
 * jGnash, account personal finance application
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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.engine.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.util.DateUtils;

/**
 * Budget Factory for automatic generation of Budgets
 *
 * @author Craig Cavanaugh
 *
 */
public class BudgetFactory {

    /**
     * Utility class, no public constructor
     */
    private BudgetFactory() {
    }

    public static Budget buildBuildBudget(final BudgetPeriod budgetPeriod, final String name, final boolean round) {
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

    private static BudgetGoal buildAverageBudgetGoal(final Account account, final List<BudgetPeriodDescriptor> descriptors, final boolean round) {
        BudgetGoal goal = new BudgetGoal();

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

            goal.setBudgetPeriod(descriptor.getBudgetPeriod());
            goal.setGoal(descriptor.getStartPeriod(), descriptor.getEndPeriod(), amount);
        }

        return goal;
    }
}
