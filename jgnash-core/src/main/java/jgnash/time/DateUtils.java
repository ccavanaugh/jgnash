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
package jgnash.time;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import jgnash.util.NotNull;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

/**
 * Static methods to make working with dates a bit easier.
 *
 * @author Craig Cavanaugh
 * @author Vincent Frison
 */

public class DateUtils {

    private static final int MONTHS_PER_YEAR = 12;

    private static final int DAYS_PER_WEEK = 7;

    private static final String DATE_FORMAT = "dateFormat";

    private static final int MILLISECONDS_PER_SECOND = 1000;

    private static final Pattern MONTH_PATTERN = Pattern.compile("M{1,2}");

    private static final Pattern DAY_PATTERN = Pattern.compile("d{1,2}");

    private static final Pattern HOUR_PATTERN = Pattern.compile("h{1,2}");

    private static DateTimeFormatter shortDateFormatter;

    private static final DateTimeFormatter shortDateTimeFormatter;

    private static DateTimeFormatter shortDateManualEntryFormatter;

    static {
        shortDateFormatter = DateTimeFormatter.ofPattern(getShortDatePattern()).withResolverStyle(ResolverStyle.SMART);

        shortDateTimeFormatter = DateTimeFormatter.ofPattern(getShortDateTimePattern())
                .withResolverStyle(ResolverStyle.SMART);

        shortDateManualEntryFormatter = DateTimeFormatter.ofPattern(getShortDateManualEntryPattern())
                .withResolverStyle(ResolverStyle.SMART);
    }

    private DateUtils() {
    }

    public static void setShortDateFormatPattern(@NotNull final String pattern) throws IllegalArgumentException {
        Objects.requireNonNull(pattern);
        final Preferences preferences = Preferences.userNodeForPackage(DateUtils.class);

        preferences.put(DATE_FORMAT, pattern);

        shortDateFormatter = DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.SMART);

        shortDateManualEntryFormatter = DateTimeFormatter.ofPattern(getShortDateManualEntryPattern())
                .withResolverStyle(ResolverStyle.SMART);
    }

    public static Set<String> getAvailableShortDateFormats() {
        final Set<String> dateFormats = new TreeSet<>();

        for (final Locale locale : Locale.getAvailableLocales()) {
            final DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, locale);

            if (df instanceof SimpleDateFormat) {
                dateFormats.add(((SimpleDateFormat) df).toPattern());
            }
        }

        return dateFormats;
    }

    public static String getShortDatePattern() {
        final Preferences preferences = Preferences.userNodeForPackage(DateUtils.class);

        String pattern = preferences.get(DATE_FORMAT, "");

        if (pattern.isEmpty()) {    // create a default for the current locale
            final DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);

            if (df instanceof SimpleDateFormat) {
                pattern = ((SimpleDateFormat) df).toPattern();
                pattern = DAY_PATTERN.matcher(MONTH_PATTERN.matcher(pattern).replaceAll("MM")).replaceAll("dd");
            } else {
                throw new RuntimeException("Unexpected class");
            }
        }

        return pattern;
    }

    private static String getShortDateTimePattern() {
        final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

        String pattern = ((SimpleDateFormat) df).toPattern();
        pattern = DAY_PATTERN.matcher(MONTH_PATTERN.matcher(pattern).replaceAll("MM")).replaceAll("dd");
        pattern = HOUR_PATTERN.matcher(pattern).replaceAll("hh");

        return pattern;
    }

    /**
     * Returns a variant of the default format with required days reduced to one to make manual entry easier.
     *
     * @return date format
     */
    private static String getShortDateManualEntryPattern() {
        String pattern = getShortDatePattern();

        // Relax date entry
        if (pattern.contains("dd")) {
            pattern = pattern.replace("dd", "d");
        }

        // Relax month entry
        if (pattern.contains("MM")) {
            pattern = pattern.replace("MM", "M");
        }

        return pattern;
    }

    /**
     * Returns a {@code LocalDate} compatible {@code DateTimeFormatter} that Excel understands
     * @return Excel compatible {@code DateTimeFormatter}
     */
    public static DateTimeFormatter getExcelDateFormatter() {
        return new DateTimeFormatterBuilder().appendValue(YEAR, 4).appendValue(MONTH_OF_YEAR, 2)
                .appendValue(DAY_OF_MONTH, 2)
                .toFormatter();
    }

    /**
     * Returns a {@code LocalDateTime} compatible {@code DateTimeFormatter} that Excel understands
     * @return Excel compatible {@code DateTimeFormatter}
     */
    public static DateTimeFormatter getExcelTimestampFormatter() {
        return new DateTimeFormatterBuilder()
                .appendValue(YEAR, 4).appendLiteral('-').appendValue(MONTH_OF_YEAR, 2)
                .appendLiteral('-').appendValue(DAY_OF_MONTH, 2).appendLiteral(' ')
                .appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2)
                .appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2)
                .toFormatter();
    }

    /**
     * Converts a {@code LocalDate} into a {@code Date} using the default timezone.
     *
     * @param localDate {@code LocalDate} to convert
     * @return an equivalent {@code Date}
     */
    public static Date asDate(final LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Converts a {@code LocalDate} into a {@code Date} using the default timezone.
     *
     * @param localDate {@code LocalDate} to convert
     * @return an equivalent {@code Date}
     */
    public static Date asDate(final LocalDateTime localDate) {
        return Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Converts a {@code Date} into a {@code LocalDate} using the default timezone.
     *
     * @param date {@code Date} to convert
     * @return an equivalent {@code LocalDate} or {@code null} if the supplied date was {@code null}
     */
    public static LocalDate asLocalDate(final Date date) {
        if (date != null) {
            return asLocalDate(date.getTime());
        }

        return null;
    }

    /**
     * Converts milliseconds from the epoch of 1970-01-01T00:00:00Z into a {@code LocalDate} using the default timezone.
     *
     * @param milli milliseconds from the epoch of 1970-01-01T00:00:00Z.
     * @return an equivalent {@code LocalDate} or {@code null} if the supplied date was {@code null}
     */
    public static LocalDate asLocalDate(final long milli) {
        return Instant.ofEpochMilli(milli).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * Converts a LocaleDate into milliseconds from the epoch of 1970-01-01T00:00:00Z.
     *
     * @param localDate {@code LocalDate} to convert
     * @return and equivalent milliseconds from the epoch of 1970-01-01T00:00:00Z
     */
    public static long asEpochMilli(final LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * MILLISECONDS_PER_SECOND;
    }

    /**
     * Determines is {@code LocalDate} d1 occurs after {@code LocalDate} d2. The specified dates are
     * inclusive.
     *
     * @param d1 date 1
     * @param d2 date 2
     * @return true if d1 is after d2
     */
    public static boolean after(final LocalDate d1, final LocalDate d2) {
        return before(d2, d1, true);
    }

    /**
     * Determines if {@code LocalDate} d1 occurs before {@code LocalDate} d2. The specified dates are
     * inclusive
     *
     * @param d1 date 1
     * @param d2 date 2
     * @return true if d1 is before d2 or the same date
     */
    public static boolean before(final LocalDate d1, final LocalDate d2) {
        return before(d1, d2, true);
    }

    /**
     * Determines if {@code LocalDate} d1 occurs before {@code LocalDate} d2.
     *
     * @param d1        {@code LocalDate} 1
     * @param d2        {@code LocalDate} 2
     * @param inclusive {@code true} is comparison is inclusive
     * @return {@code true} if d1 occurs before d2
     */
    public static boolean before(final LocalDate d1, final LocalDate d2, final boolean inclusive) {
        if (inclusive) {
            return d1.isEqual(d2) || d1.isBefore(d2);
        }

        return d1.isBefore(d2);
    }

    /**
     * Determines if {@code LocalDate} d1 occurs after {@code LocalDate} d2 and before {@code LocalDate} d3.
     * The specified dates are inclusive.
     *
     * @param d1 {@code LocalDate} 1
     * @param d2    {@code LocalDate} 2
     * @param d3    {@code LocalDate} 3
     * @return {@code true} if d1 occurs between d2 and d3
     */
    public static boolean between(final LocalDate d1, final LocalDate d2, final LocalDate d3) {
        return after(d1, d2) && before(d1, d3);
    }

    /**
     * Returns the number of days in the year.
     *
     * @param year calendar year
     * @return the number of days in the year
     */
    public static int getDaysInYear(final int year) {
        return LocalDate.ofYearDay(year, 1).lengthOfYear();
    }

    /**
     * Returns an array of the first days bi-weekly for a given year.
     *
     * @param year The year to generate the array for
     * @return The array of dates
     */
    public static LocalDate[] getFirstDayBiWeekly(final int year) {
        return getFirstDayWeekly(Month.JANUARY, year,
                (int) Math.ceil((float) DateUtils.getNumberOfWeeksInYear(year) / 2.0), 2);
    }

    /**
     * Returns an array of the first days bi-weekly given a stating month, year, and the number of weeks
     *
     * @param statingMonth The Month to begin with
     * @param year         The year to generate the array for
     * @param weeks        The number of dates to return
     * @see #getFirstDayWeekly(Month, int, int)
     */
    public static LocalDate[] getFirstDayBiWeekly(final Month statingMonth, final int year, final int weeks) {
        return getFirstDayWeekly(statingMonth, year, weeks, 2);
    }

    /**
     * Generates an array of dates starting on the first day of every month in
     * the specified year.
     *
     * @param year The year to generate the array for
     * @return The array of dates
     */
    public static LocalDate[] getFirstDayMonthly(final int year) {
        return getFirstDayMonthly(Month.JANUARY, year, MONTHS_PER_YEAR);
    }

    /**
     * Generates an array of dates starting on the first day of every month in
     * the specified year.
     *
     * @param month  That month to start with
     * @param year   The year to generate the array for
     * @param months The number of months
     * @return The array of dates
     */
    public static LocalDate[] getFirstDayMonthly(final Month month, final int year, final int months) {
        return getFirstDayMonthly(month, year, months, 1);
    }

    /**
     * Generates an array of dates starting on the first day of every month in
     * the specified year.
     *
     * @param month  That month to start with
     * @param year   The year to generate the array for
     * @param months The number of months
     * @param step   increment
     * @return The array of dates
     */
   private static LocalDate[] getFirstDayMonthly(final Month month, final int year, final int months, final int step) {
        LocalDate[] list = new LocalDate[months];

        int index = 0;
        int _month = month.getValue();
        int _year = year;

        while (index < months) {
            list[index] = getFirstDayOfTheMonth(_month, _year);
            _month += step;

            if (_month > MONTHS_PER_YEAR) {
                _year++;
                _month = _month - MONTHS_PER_YEAR;
            }

            index++;
        }
        return list;
    }

    /**
     * Returns a leveled date representing the first day of the month based on a
     * specified date.
     *
     * @param date the base date to work from
     * @return The last day of the month and year specified
     */
    public static LocalDate getFirstDayOfTheMonth(final LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }

    /**
     * Returns a date representing the first day of the month.
     *
     * @param month The month (index starts at 1)
     * @param year  The year (index starts at 1)
     * @return The last day of the month and year specified
     */
    private static LocalDate getFirstDayOfTheMonth(final int month, final int year) {
        return LocalDate.of(year, month, 1);
    }

    /**
     * Returns an array of the starting date of each quarter in a year.
     *
     * @param year The year to generate the array for
     * @return The array of quarter bound dates
     */
    public static LocalDate[] getFirstDayQuarterly(final int year) {
        return getFirstDayMonthly(Month.JANUARY, year, 4, 3);
    }

    /**
     * Returns an array of the starting date of each quarter in a year.
     *
     * @param year The year to generate the array for
     * @return The array of quarter bound dates
     */
    public static LocalDate[] getFirstDayQuarterly(final Month statingMonth, final int year, final int weeks) {
        return getFirstDayMonthly(statingMonth, year, weeks, 3);
    }

    /**
     * Returns an array of Dates starting with the first day of each week of the
     * year per ISO 8601.
     *
     * @param year The year to generate the array for
     * @return The array of dates
     * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO_8601</a>
     */
    public static LocalDate[] getFirstDayWeekly(final int year) {
        return getFirstDayWeekly(Month.JANUARY, year, getNumberOfWeeksInYear(year), 1);
    }

    /**
     * Returns an array of weekly Dates per ISO 8601.
     *
     * @param statingMonth The Month to begin with
     * @param year         The year to generate the array for
     * @param weeks        The number of dates to return
     * @return The array of dates
     * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO_8601</a>
     */
    public static LocalDate[] getFirstDayWeekly(final Month statingMonth, final int year, final int weeks) {
        return getFirstDayWeekly(statingMonth, year, weeks, 1);
    }

    /**
     * Returns an array of weekly Dates per ISO 8601.
     *
     * @param statingMonth The Month to begin with
     * @param year         The year to generate the array for
     * @param weeks        The number of dates to return
     * @param step         The week increment
     * @return The array of dates
     * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO_8601</a>
     */
    public static LocalDate[] getFirstDayWeekly(final Month statingMonth, final int year, final int weeks, final int step) {

        LocalDate date = LocalDate.of(year, statingMonth, 1);

        // Weeks begin on Monday per ISO_8601
        date = date.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));

        // shift the stating date per ISO_8601 rules
        switch (LocalDate.of(year, Month.JANUARY, 1).getDayOfWeek()) {
            case FRIDAY:
            case SATURDAY:
            case SUNDAY:
                break;
            default:
                date = date.minusDays(DAYS_PER_WEEK);

                break;
        }

        final List<LocalDate> dates = new ArrayList<>();

        dates.add(date);

        for (int i = 1; i < weeks; i++) {
            date = date.plusDays((long) DAYS_PER_WEEK * step);
            dates.add(date);
        }

        return dates.toArray(new LocalDate[0]);
    }

    /**
     * Returns the number of weeks in a year per ISO 8601.
     *
     * @param year year
     * @return number of weeks
     * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO_8601</a>
     */
    public static int getNumberOfWeeksInYear(final int year) {
        LocalDate midYear = LocalDate.of(year, Month.JUNE, 1);
        return (int) midYear.range(WeekFields.ISO.weekOfWeekBasedYear()).getMaximum();
    }

    /**
     * Returns an array of every day in a given year.
     *
     * @param year The year to generate the array for
     * @return The array of dates
     */
    public static LocalDate[] getAllDays(final int year) {
        return getAllDays(Month.JANUARY, year, getDaysInYear(year));
    }

    public static LocalDate[] getAllDays(final Month statingMonth, final int year, final int days) {
        final List<LocalDate> dates = new ArrayList<>();

        LocalDate start = LocalDate.of(year, statingMonth, 1);

        dates.add(start);

        for (int i = 1; i < days; i++) {
            start = start.plusDays(1);
            dates.add(start);
        }

        return dates.toArray(new LocalDate[0]);
    }

    /**
     * Returns date representing the last day of the month given a specified date.
     *
     * @param date the base date to work from
     * @return The last day of the month of the supplied date
     */
    public static LocalDate getLastDayOfTheMonth(@NotNull final LocalDate date) {
        Objects.requireNonNull(date);

        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * Generates an array of dates ending on the last day of every month between
     * the start and stop dates.
     *
     * @param startDate The date to start at
     * @param endDate   The data to stop at
     * @return The array of dates
     */
    public static List<LocalDate> getLastDayOfTheMonths(final LocalDate startDate, final LocalDate endDate) {
        final ArrayList<LocalDate> list = new ArrayList<>();

        final LocalDate end = DateUtils.getLastDayOfTheMonth(endDate);
        LocalDate t = DateUtils.getLastDayOfTheMonth(startDate);

        /*
         * add a month at a time to the previous date until all of the months
         * have been captured
         */
        while (before(t, end)) {
            list.add(t);

            t = t.plusMonths(1);
            t = t.with(TemporalAdjusters.lastDayOfMonth());
        }
        return list;
    }

    /**
     * Generates an array of dates starting on the first day of every month
     * between the start and stop dates.
     *
     * @param startDate The date to start at
     * @param endDate   The data to stop at
     * @return The array of dates
     */
    public static List<LocalDate> getFirstDayOfTheMonths(final LocalDate startDate, final LocalDate endDate) {
        final ArrayList<LocalDate> list = new ArrayList<>();

        final LocalDate end = DateUtils.getFirstDayOfTheMonth(endDate);
        LocalDate t = DateUtils.getFirstDayOfTheMonth(startDate);

        /*
         * add a month at a time to the previous date until all of the months
         * have been captured
         */
        while (before(t, end)) {
            list.add(t);
            t = t.with(TemporalAdjusters.firstDayOfNextMonth());
        }
        return list;
    }

    /**
     * Returns a {@code LocalDate} date representing the last day of the quarter based on
     * a specified date.
     *
     * @param date the base date to work from
     * @return The last day of the quarter specified
     */
    public static LocalDate getLastDayOfTheQuarter(final LocalDate date) {
        Objects.requireNonNull(date);

        LocalDate result;

        LocalDate[] bounds = getQuarterBounds(date);

        if (date.compareTo(bounds[2]) < 0) {
            result = bounds[1];
        } else if (date.compareTo(bounds[4]) < 0) {
            result = bounds[3];
        } else if (date.compareTo(bounds[6]) < 0) {
            result = bounds[5];
        } else {
            result = bounds[DAYS_PER_WEEK];
        }

        return result;
    }

    /**
     * Returns a {@code LocalDate} representing the last day of the year based on a
     * specified date.
     *
     * @param date the base date to work from
     * @return The last day of the year specified
     */
    public static LocalDate getLastDayOfTheYear(@NotNull final LocalDate date) {
        Objects.requireNonNull(date);

        return date.with(TemporalAdjusters.lastDayOfYear());
    }

    /**
     * Returns an array of quarter bound dates of the year based on a specified
     * date. The order is q1s, q1e, q2s, q2e, q3s, q3e, q4s, q4e.
     *
     * @param date the base date to work from
     * @return The array of quarter bound dates
     */
    private static LocalDate[] getQuarterBounds(final LocalDate date) {
        Objects.requireNonNull(date);

        final LocalDate[] bounds = new LocalDate[8];

        bounds[0] = date.with(TemporalAdjusters.firstDayOfYear());
        bounds[1] = date.withMonth(Month.MARCH.getValue()).with(TemporalAdjusters.lastDayOfMonth());
        bounds[2] = date.withMonth(Month.APRIL.getValue()).with(TemporalAdjusters.firstDayOfMonth());
        bounds[3] = date.withMonth(Month.JUNE.getValue()).with(TemporalAdjusters.lastDayOfMonth());
        bounds[4] = date.withMonth(Month.JULY.getValue()).with(TemporalAdjusters.firstDayOfMonth());
        bounds[5] = date.withMonth(Month.SEPTEMBER.getValue()).with(TemporalAdjusters.lastDayOfMonth());
        bounds[6] = date.withMonth(Month.OCTOBER.getValue()).with(TemporalAdjusters.firstDayOfMonth());
        bounds[DAYS_PER_WEEK] = date.with(TemporalAdjusters.lastDayOfYear());

        return bounds;
    }

    /**
     * Returns the number of the quarter (i.e. 1, 2, 3 or 4) based on a
     * specified date.
     *
     * @param date the base date to work from
     * @return The number of the quarter specified
     */
    public static int getQuarterNumber(final LocalDate date) {
        Objects.requireNonNull(date);

        int result;

        LocalDate[] bounds = getQuarterBounds(date);

        if (date.compareTo(bounds[2]) < 0) {
            result = 1;
        } else if (date.compareTo(bounds[4]) < 0) {
            result = 2;
        } else if (date.compareTo(bounds[6]) < 0) {
            result = 3;
        } else {
            result = 4;
        }

        return result;
    }

    public static DateTimeFormatter getShortDateFormatter() {
        return shortDateFormatter;
    }

    public static DateTimeFormatter getShortDateTimeFormatter() {
        return shortDateTimeFormatter;
    }

    public static DateTimeFormatter getShortDateManualEntryFormatter() {
        return shortDateManualEntryFormatter;
    }

    /**
     * Returns the numerical week of the year given a date per the ISO 8601 standard.
     *
     * @param dateOfYear the base date to work from
     * @return the week of the year
     */
    public static int getWeekOfTheYear(final LocalDate dateOfYear) {
        return dateOfYear.get(WeekFields.ISO.weekOfWeekBasedYear());
    }
}
