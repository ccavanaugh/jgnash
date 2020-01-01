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

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jgnash.time.DateUtils;
import jgnash.time.Period;

import org.apache.commons.collections4.map.ReferenceMap;

/**
 * Factory class for generating descriptors.
 *
 * @author Craig Cavanaugh
 */
public final class BudgetPeriodDescriptorFactory {

    private static final Map<String, List<BudgetPeriodDescriptor>> cache = new ReferenceMap<>();

    private static final ReadWriteLock rwl = new ReentrantReadWriteLock(false);

    private BudgetPeriodDescriptorFactory() {
    }

    public static List<BudgetPeriodDescriptor> getDescriptors(final int budgetYear, final Month startingMonth, final Period budgetPeriod) {

        final String cacheKey = budgetYear + budgetPeriod.name() + startingMonth.ordinal();

        List<BudgetPeriodDescriptor> descriptors;

        rwl.readLock().lock();
        try {
            descriptors = cache.get(cacheKey);
        } finally {
            rwl.readLock().unlock();
        }

        // build the descriptor List if not cached
        if (descriptors == null) {

            LocalDate[] dates = new LocalDate[0];

            switch (budgetPeriod) {
                case DAILY:
                    dates = DateUtils.getAllDays(startingMonth, budgetYear, DateUtils.getDaysInYear(budgetYear) + 1);
                    break;
                case WEEKLY:
                    dates = DateUtils.getFirstDayWeekly(startingMonth, budgetYear, 52 + 1);
                    break;
                case BI_WEEKLY:
                    dates = DateUtils.getFirstDayBiWeekly(startingMonth, budgetYear, 26 + 1);
                    break;
                case MONTHLY:
                    dates = DateUtils.getFirstDayMonthly(startingMonth, budgetYear, 12 + 1);
                    break;
                case QUARTERLY:
                    dates = DateUtils.getFirstDayQuarterly(startingMonth, budgetYear, 4 + 1);
                    break;
                case YEARLY:
                    dates = DateUtils.getFirstDayWeekly(startingMonth, budgetYear, 2, 52);
                    break;
            }

            descriptors = new ArrayList<>(dates.length);

            for (int i = 0; i < dates.length - 1; i++) {
                descriptors.add(new BudgetPeriodDescriptor(dates[i], dates[i + 1].minusDays(1), budgetPeriod));
            }

            // Debugging info
            /*System.out.println("Descriptor list length: " + descriptors.size());
            for (final BudgetPeriodDescriptor descriptor : descriptors) {
                System.out.println(descriptor.toString());
            }*/

            rwl.writeLock().lock();
            try {
                cache.put(cacheKey, descriptors);
            } finally {
                rwl.writeLock().unlock();
            }
        }

        return descriptors;
    }
}
