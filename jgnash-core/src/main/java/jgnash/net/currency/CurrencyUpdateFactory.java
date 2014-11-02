/*
 * jGnash, a personal finance application
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
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.net.currency;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.prefs.Preferences;

import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;

/**
 * Fetches latest exchange rates in the background
 *
 * @author Craig Cavanaugh
 */
public class CurrencyUpdateFactory {

    private static final String UPDATE_ON_STARTUP = "updateOnStartup";

    private CurrencyUpdateFactory() {
    }

    public static void setUpdateOnStartup(boolean update) {
        Preferences pref = Preferences.userNodeForPackage(CurrencyUpdateFactory.class);
        pref.putBoolean(UPDATE_ON_STARTUP, update);
    }

    public static boolean getUpdateOnStartup() {
        Preferences pref = Preferences.userNodeForPackage(CurrencyUpdateFactory.class);
        return pref.getBoolean(UPDATE_ON_STARTUP, false);
    }

    public static class UpdateExchangeRatesCallable implements Callable<Void> {

        @Override
        public Void call() throws Exception {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

            if (engine != null) {
                final List<CurrencyNode> list = engine.getCurrencies();

                for (final CurrencyNode sourceCurrency : list) {
                    final String source = sourceCurrency.getSymbol();
                    for (final CurrencyNode targetCurrency : list) {
                        final String target = targetCurrency.getSymbol();

                        if (Thread.currentThread().isInterrupted()) {
                            return null;
                        }

                        if (!source.equals(target) && source.compareToIgnoreCase(target) > 0) {
                            CurrencyParser parser = new YahooParser();

                            if (parser.parse(source, target)) {
                                BigDecimal exchangeRate = parser.getConversion();

                                if (exchangeRate != null && exchangeRate.compareTo(BigDecimal.ZERO) != 0) {
                                    engine.setExchangeRate(sourceCurrency, targetCurrency, exchangeRate);
                                }
                            }
                        }
                    }
                }
            }

            return null;
        }
    }
}
