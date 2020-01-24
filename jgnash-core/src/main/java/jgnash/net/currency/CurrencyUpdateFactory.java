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
package jgnash.net.currency;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;

/**
 * Fetches latest exchange rates in the background.
 *
 * @author Craig Cavanaugh
 */
public class CurrencyUpdateFactory {

    private static final String UPDATE_ON_STARTUP = "updateCurrenciesOnStartup";

    private CurrencyUpdateFactory() {
    }

    public static void setUpdateOnStartup(final boolean update) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            engine.putBoolean(UPDATE_ON_STARTUP, update);
        }
    }

    public static boolean getUpdateOnStartup() {
        boolean result = false;

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            result = engine.getBoolean(UPDATE_ON_STARTUP, false);
        }

        return result;
    }

    public static Optional<BigDecimal> getExchangeRate(final CurrencyNode source, final CurrencyNode target) {
        Optional<BigDecimal> optional = Optional.empty();

        final CurrencyParser parser = new CurrencyConverterParser();

        if (parser.parse(source.getSymbol(), target.getSymbol())) {
            final BigDecimal exchangeRate = parser.getConversion();

            if (exchangeRate != null && exchangeRate.compareTo(BigDecimal.ZERO) != 0) {
                optional = Optional.of(exchangeRate);
            }
        }

        return optional;
    }

    public static class UpdateExchangeRatesCallable implements Callable<Boolean> {

        @Override
        public Boolean call() {

            boolean result = false;

            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

            if (engine != null) {
                final List<CurrencyNode> list = engine.getCurrencies();

                for (final CurrencyNode source : list) {
                    list.stream().filter(target -> !source.equals(target)
                            && source.getSymbol().compareToIgnoreCase(target.getSymbol()) > 0).forEach(target -> {

                        final Optional<BigDecimal> rate = CurrencyUpdateFactory.getExchangeRate(source, target);

                        rate.ifPresent(value -> engine.setExchangeRate(source, target, value));
                    });
                }
                result = true;
            }

            return result;
        }
    }
}
