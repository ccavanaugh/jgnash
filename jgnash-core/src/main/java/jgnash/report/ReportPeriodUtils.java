/*
 * jGnash, account personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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
package jgnash.report;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import jgnash.time.DateUtils;
import jgnash.util.NotNull;

/**
 * Utility Methods for reporting.
 *
 * @author Craig Cavanaugh
 */
public class ReportPeriodUtils {

    private ReportPeriodUtils() {
        // utility class
    }

    public static class Descriptor {

        private final LocalDate startDate;
        private final LocalDate endDate;
        private final String label;

        private Descriptor(@NotNull final LocalDate startDate, @NotNull final LocalDate endDate,
                           @NotNull final String label) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.label = label;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public String getLabel() {
            return label;
        }
    }

    public static List<Descriptor> getDescriptors(@NotNull ReportPeriod reportPeriod, @NotNull LocalDate startDate,
                                                  @NotNull LocalDate endDate) {

        final List<Descriptor> descriptors = new ArrayList<>();

        LocalDate start = startDate;
        LocalDate end = startDate;

        switch (reportPeriod) {
            case YEARLY:
                while (end.isBefore(endDate)) {
                    end = start.with(TemporalAdjusters.lastDayOfYear());
                    descriptors.add(new Descriptor(start, end, "    " + start.getYear()));
                    start = end.plusDays(1);
                }
                break;
            case QUARTERLY:
                int i = DateUtils.getQuarterNumber(start) - 1;
                while (end.isBefore(endDate)) {
                    end = DateUtils.getLastDayOfTheQuarter(start);
                    descriptors.add(new Descriptor(start, end, " " + start.getYear() + "-Q" + (1 + i++ % 4)));
                    start = end.plusDays(1);
                }
                break;
            case MONTHLY:
                while (end.isBefore(endDate)) {
                    end = DateUtils.getLastDayOfTheMonth(start);
                    final int month = start.getMonthValue();
                    descriptors.add(new Descriptor(start, end, " " + start.getYear() + (month < 10 ? "/0" + month
                            : "/" + month)));
                    start = end.plusDays(1);
                }
                break;
            default:
        }

        return descriptors;
    }

}
