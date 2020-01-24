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
package jgnash.text;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import jgnash.engine.CommodityNode;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.util.NotNull;

/**
 * Utility class to provide Numeric formats
 *
 * @author Craig Cavanaugh
 */
public final class NumericFormats {

    private static final String FULL_FORMAT = "fullFormat";

    private static final String SHORT_FORMAT = "shortFormat";

    private static final CommodityListener listener;

    private static final Map<CommodityNode, ThreadLocal<DecimalFormat>> fullInstanceMap = new WeakHashMap<>();

    private static final Map<CommodityNode, ThreadLocal<DecimalFormat>> simpleInstanceMap = new WeakHashMap<>();

    private static final String CURRENCY_SYMBOL = "\u00A4"; // ¤, must be defined using unicode or some platforms fail test

    static {
        /*
         * Need to clear any references to CommodityNodes to prevent memory leaks when
         * files are loaded and unload.
         */
        listener = new CommodityListener();

        MessageBus.getInstance().registerListener(listener, MessageChannel.COMMODITY, MessageChannel.SYSTEM);
    }

    private NumericFormats() {
        // factory class
    }

    public static Set<String> getKnownFullPatterns() {

        Set<String> patternSet = new TreeSet<>();

        for (final Locale locale : Locale.getAvailableLocales()) {
            DecimalFormat df = (DecimalFormat)NumberFormat.getCurrencyInstance(locale);
            patternSet.add(df.toPattern());
        }

        // TODO: add missing US locale format, JDK 11 Bug
        patternSet.add("\u00A4#,##0.00;(\u00A4#,##0.00)");
        patternSet.add("\u00A4 #,##0.00;(\u00A4 #,##0.00)");

        patternSet.add(getFullFormatPattern()); // add the users own format

        return patternSet;
    }

    public static Set<String> getKnownShortPatterns() {

        final Pattern currencyReplacementPattern = Pattern.compile(CURRENCY_SYMBOL);
        final Set<String> patternSet = new TreeSet<>();

        for (final Locale locale : Locale.getAvailableLocales()) {
            final DecimalFormat df = (DecimalFormat)NumberFormat.getCurrencyInstance(locale);
            final String pattern = df.toPattern();

            patternSet.add(currencyReplacementPattern.matcher(pattern).replaceAll("").stripLeading());
        }

        patternSet.add("#,##0.00;(#,##0.00)");

        patternSet.add(getShortFormatPattern()); // add the users own format

        return patternSet;
    }

    public static String getFullFormatPattern() {
        return getFormatPattern(FULL_FORMAT, ((DecimalFormat)NumberFormat.getCurrencyInstance()).toPattern());
    }

    public static String getShortFormatPattern() {

        DecimalFormat df = (DecimalFormat)NumberFormat.getCurrencyInstance();

        // create the default short format
        final DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
        dfs.setCurrencySymbol("");
        df.setDecimalFormatSymbols(dfs);


        return getFormatPattern(SHORT_FORMAT, df.toPattern());
    }

    private static String getFormatPattern(@NotNull final String key, final String defaultPattern) {
        Objects.requireNonNull(key);

        final Preferences preferences = Preferences.userNodeForPackage(NumericFormats.class);
        return preferences.get(key, defaultPattern);
    }

    public static void setFullFormatPattern(@NotNull final String pattern) {
        if (!getFullFormatPattern().equals(pattern)) {
            setFormatPattern(FULL_FORMAT, pattern);

            fullInstanceMap.clear();    // flush the cached instance map
        }
    }

    public static void setShortFormatPattern(@NotNull final String pattern) {
        if (!getShortFormatPattern().equals(pattern)) {
            setFormatPattern(SHORT_FORMAT, pattern);

            simpleInstanceMap.clear();  // flush the cached instance map
        }
    }

    private static void setFormatPattern(@NotNull final String key, @NotNull final String pattern) {
        Objects.requireNonNull(pattern);

        if (!pattern.isBlank()) {
            final Preferences preferences = Preferences.userNodeForPackage(NumericFormats.class);
            preferences.put(key, pattern);
        }
    }

    /**
     * Returns a thread safe simplified {@code NumberFormat} for a given {@code CommodityNode}.
     *
     * @param node CommodityNode to format to
     * @return thread safe {@code NumberFormat}
     */
    public static NumberFormat getShortCommodityFormat(@NotNull final CommodityNode node) {
        final ThreadLocal<DecimalFormat> o = simpleInstanceMap.get(node);

        if (o != null) {
            return o.get();
        }

        final ThreadLocal<DecimalFormat> threadLocal = ThreadLocal.withInitial(() -> {

            final String pattern = getShortFormatPattern();

            // generate a full currency format
            if (pattern.contains(CURRENCY_SYMBOL)) {
                return generateFullFormat(node, pattern);
            }

            final DecimalFormat df = new DecimalFormat(getShortFormatPattern());

            // required for some locales
            df.setMaximumFractionDigits(node.getScale());
            df.setMinimumFractionDigits(df.getMaximumFractionDigits());

            // for positive suffix padding for fraction alignment
            int negSufLen = df.getNegativeSuffix().length();
            if (negSufLen > 0) {
                char[] pad = new char[negSufLen];
                for (int i = 0; i < negSufLen; i++) {
                    pad[i] = ' ';
                }
                df.setPositiveSuffix(new String(pad));
            }

            return df;
        });

        simpleInstanceMap.put(node, threadLocal);

        return threadLocal.get();
    }

    /**
     * Returns a thread safe {@code NumberFormat} for a given {@code CommodityNode}.
     *
     * @param node CommodityNode to format to
     * @return thread safe {@code NumberFormat}
     */
    public static NumberFormat getFullCommodityFormat(@NotNull final CommodityNode node) {
        final ThreadLocal<DecimalFormat> o = fullInstanceMap.get(node);

        if (o != null) {
            return o.get();
        }

        final ThreadLocal<DecimalFormat> threadLocal = ThreadLocal.withInitial(()
                -> generateFullFormat(node, getFullFormatPattern()));

        fullInstanceMap.put(node, threadLocal);

        return threadLocal.get();
    }

    private static DecimalFormat generateFullFormat(final CommodityNode node, final String pattern) {
        final DecimalFormat df = new DecimalFormat(pattern);

        final DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
        dfs.setCurrencySymbol(node.getPrefix());
        df.setDecimalFormatSymbols(dfs);
        df.setMaximumFractionDigits(node.getScale());

        // required for some locale
        df.setMinimumFractionDigits(df.getMaximumFractionDigits());

        if (node.getSuffix() != null && !node.getSuffix().isEmpty()) {
            df.setPositiveSuffix(node.getSuffix() + df.getPositiveSuffix());
            df.setNegativeSuffix(node.getSuffix() + df.getNegativeSuffix());
        }

        // for positive suffix padding for fraction alignment
        final int negSufLen = df.getNegativeSuffix().length();
        final int posSufLen = df.getPositiveSuffix().length();

        // pad the prefix and suffix as necessary so that they are the same length
        if (negSufLen > posSufLen) {
            df.setPositiveSuffix(df.getPositiveSuffix()
                    + " ".repeat(Math.max(0, negSufLen - (negSufLen - posSufLen) + 1)));
        } else if (posSufLen > negSufLen) {
            df.setNegativeSuffix(df.getNegativeSuffix()
                    + " ".repeat(Math.max(0, posSufLen - (posSufLen - negSufLen) + 1)));
        }

        return df;
    }


    private static String getConversion(final String cur1, final String cur2) {
        return cur1 + " > " + cur2;
    }

    public static String getConversion(final CommodityNode cur1, final CommodityNode cur2) {
        return getConversion(cur1.getSymbol(), cur2.getSymbol());
    }

    public static NumberFormat getFixedPrecisionFormat(final int scale) {
        final NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(scale);
        nf.setMinimumFractionDigits(scale);

        return nf;
    }

    public static NumberFormat getPercentageFormat() {
        final NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(2);

        return nf;
    }

    private static class CommodityListener implements MessageListener {

        @Override
        public void messagePosted(final Message event) {
            switch (event.getEvent()) {
                case FILE_CLOSING:
                case CURRENCY_MODIFY:
                    simpleInstanceMap.clear();
                    fullInstanceMap.clear();
                    break;
                default:
                    break;
            }
        }
    }
}