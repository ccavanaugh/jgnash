/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
import java.util.HashMap;
import java.util.Map;

import jgnash.engine.CommodityNode;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.util.NotNull;

/**
 * Formats commodities for display.
 *
 * @author Craig Cavanaugh
 */
public class CommodityFormat {

    private static final CommodityListener listener;

    private static final Map<CommodityNode, ThreadLocal<DecimalFormat>> fullInstanceMap = new HashMap<>();

    private static final Map<Integer, ThreadLocal<DecimalFormat>> simpleInstanceMap = new HashMap<>();

    static {
        /*
         * Need to clear any references to CommodityNodes to prevent memory leaks when
         * files are loaded and unload.
         */
        listener = new CommodityListener();

        MessageBus.getInstance().registerListener(listener, MessageChannel.COMMODITY, MessageChannel.SYSTEM);
    }

    /**
     * Returns a thread safe simplified {@code NumberFormat} for a given {@code CommodityNode}.
     *
     * @param node CommodityNode to format to
     * @return thread safe {@code NumberFormat}
     */
    public static NumberFormat getShortNumberFormat(@NotNull final CommodityNode node) {
        return getShortNumberFormat(node.getScale());
    }

    /**
     *
     * @param scale scale of the simple number
     * @return thread safe {@code NumberFormat}
     */
    public static NumberFormat getShortNumberFormat(final int scale) {
        final ThreadLocal<DecimalFormat> o = simpleInstanceMap.get(scale);

        if (o != null) {
            return o.get();
        }

        final ThreadLocal<DecimalFormat> threadLocal = ThreadLocal.withInitial(() -> {
            final DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance();
            final DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
            dfs.setCurrencySymbol("");
            df.setDecimalFormatSymbols(dfs);
            df.setMaximumFractionDigits(scale);

            // required for some locale
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

        simpleInstanceMap.put(scale, threadLocal);

        return threadLocal.get();
    }

    /**
     * Returns a thread safe {@code NumberFormat} for a given {@code CommodityNode}.
     *
     * @param node CommodityNode to format to
     * @return thread safe {@code NumberFormat}
     */
    public static NumberFormat getFullNumberFormat(@NotNull final CommodityNode node) {
        final ThreadLocal<DecimalFormat> o = fullInstanceMap.get(node);

        if (o != null) {
            return o.get();
        }

        final ThreadLocal<DecimalFormat> threadLocal = ThreadLocal.withInitial(() -> {
            final DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance();

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
        });

        fullInstanceMap.put(node, threadLocal);

        return threadLocal.get();
    }


    private static String getConversion(final String cur1, final String cur2) {
        return cur1 + " > " + cur2;
    }

    public static String getConversion(final CommodityNode cur1, final CommodityNode cur2) {
        return getConversion(cur1.getSymbol(), cur2.getSymbol());
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