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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.engine.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.time.Period;

/**
 * Budget Factory for automatic generation of Budgets.
 *
 * @author Craig Cavanaugh
 */
public class BudgetFactory {

    /**
     * Utility class, no public constructor.
     */
    private BudgetFactory() {
    }

    public static Budget buildAverageBudget(final Period budgetPeriod, final String name, final boolean round) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final Budget budget = new Budget();
        budget.setName(name);
        budget.setBudgetPeriod(budgetPeriod);

        int year = LocalDate.now().getYear() - 1;

        final List<BudgetPeriodDescriptor> descriptors
                = BudgetPeriodDescriptorFactory.getDescriptors(year, budget.getStartMonth(), budgetPeriod);

        final List<Account> accounts = new ArrayList<>();
        accounts.addAll(engine.getIncomeAccountList());
        accounts.addAll(engine.getExpenseAccountList());

        for (final Account account : accounts) {
            budget.setBudgetGoal(account, buildAverageBudgetGoal(account, descriptors, round));
        }

        return budget;
    }

    public static BudgetGoal buildAverageBudgetGoal(final Account account,
                                                    final List<BudgetPeriodDescriptor> descriptors,
                                                    final boolean round) {
        BudgetGoal goal = new BudgetGoal();

        goal.setBudgetPeriod(descriptors.get(0).getBudgetPeriod());

        for (final BudgetPeriodDescriptor descriptor : descriptors) {
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

            goal.setGoal(descriptor.getStartPeriod(), descriptor.getEndPeriod(), amount, descriptor.getStartDate().isLeapYear());
        }

        return goal;
    }

    /**
     * Creates a {@code BudgetGoal} with an alternating pattern.
     *
     * @param baseBudgetGoal {@code BudgetGoal} to clone
     * @param descriptors descriptors to use
     * @param pattern Pattern to use
     * @param startRow starting row, 0 based index is assumed
     * @param endRow ending row, 0 based index is assumed
     * @param amount amount to use
     * @return new {@code BudgetGoal}
     */
    public static BudgetGoal buildBudgetGoal(final BudgetGoal baseBudgetGoal,
                                             final List<BudgetPeriodDescriptor> descriptors,
                                             final Pattern pattern, final int startRow, final int endRow,
                                             final BigDecimal amount) {
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

            final BudgetPeriodDescriptor descriptor = descriptors.get(i);

            goal.setBudgetPeriod(descriptor.getBudgetPeriod());
            goal.setGoal(descriptor.getStartPeriod(), descriptor.getEndPeriod(), amount, descriptor.getStartDate().isLeapYear());
        }

        return goal;
    }
}
