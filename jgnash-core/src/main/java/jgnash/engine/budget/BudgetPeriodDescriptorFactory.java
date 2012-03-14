/*
 * jGnash, a personal finance application
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
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.engine.budget;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jgnash.util.DateUtils;

/**
 * Factory class for generating descriptors
 * 
 * @author Craig Cavanaugh
 * @version $Id: BudgetPeriodDescriptorFactory.java 3086 2012-01-08 11:21:42Z ccavanaugh $
 */
public final class BudgetPeriodDescriptorFactory {

    private static Map<String, List<BudgetPeriodDescriptor>> cache = new HashMap<>();

    private static ReadWriteLock rwl = new ReentrantReadWriteLock(false);

    private BudgetPeriodDescriptorFactory() {
    }

    public static List<BudgetPeriodDescriptor> getDescriptors(final int budgetYear, final BudgetPeriod budgetPeriod) {
        final String cacheKey = budgetYear + budgetPeriod.name();

        List<BudgetPeriodDescriptor> descriptors = null;

        rwl.readLock().lock();
        try {
            descriptors = cache.get(cacheKey);
        } finally {
            rwl.readLock().unlock();
        }

        // build the descriptor List if not cached
        if (descriptors == null) {
            Date[] dates = new Date[0];

            switch (budgetPeriod) {
                case DAILY:
                    dates = DateUtils.getAllDays(budgetYear);
                    break;
                case WEEKLY:
                    dates = DateUtils.getFirstDayWeekly(budgetYear);
                    break;
                case BI_WEEKLY:
                    dates = DateUtils.getFirstDayBiWeekly(budgetYear);
                    break;
                case MONTHLY:
                    dates = DateUtils.getFirstDayMonthly(budgetYear);
                    break;
                case QUARTERLY:
                    dates = DateUtils.getFirstDayQuarterly(budgetYear);
                    break;
                case YEARLY:                    
                    Date date = DateUtils.getDateOfTheYear(budgetYear, 1);
                    dates = new Date[] {date};
                    break;
            }

            descriptors = new ArrayList<>(dates.length);

            for (Date date : dates) {
                // Calendar day 1 is 1.  Must subtract 1 to get correct index
                int day = DateUtils.getDayOfTheYear(date) - 1;
                descriptors.add(new BudgetPeriodDescriptor(budgetYear, budgetPeriod, day));
            }

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
