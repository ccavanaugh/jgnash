/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
package jgnash.engine;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static methods for currency generation and discovery
 *
 * These are known to not show up because Java 1.4.2 and older does not have
 * a default NumberFormat defined for the currency:<br>
 * "SGD"<br>
 * "MYR"
 *
 * @author Craig Cavanaugh
 */
public class DefaultCurrencies {

    /**
     * Private Constructor, use static methods only
     */
    private DefaultCurrencies() {
    }

    /**
     * Generates an array of default currency nodes that Java knows about.
     *
     * @return An array of default CurrencyNodes
     */
    public static Set<CurrencyNode> generateCurrencies() {
        final TreeSet<CurrencyNode> set = new TreeSet<>();

        for (Currency currency : Currency.getAvailableCurrencies()) {
            set.add(buildNode(currency));
        }

        return set;
    }

    /**
     * Creates a custom CurrencyNode given an ISO code.  If the ISO code is
     * not valid, then a node will be generated using the supplied code.  The
     * locale is assumed to be the default locale.
     *
     * @param ISOCode The custom currency to generate
     * @return The custom CurrencyNode
     */
    public static CurrencyNode buildCustomNode(final String ISOCode) {
        final CurrencyNode node = new CurrencyNode();
        Currency c;

        try {
            c = Currency.getInstance(ISOCode);
            node.setSymbol(c.getCurrencyCode());
        } catch (Exception e) {
            node.setSymbol(ISOCode);
            Logger.getLogger(DefaultCurrencies.class.getName()).log(Level.FINE, null, e);
        } finally {
            node.setDescription(Locale.getDefault().toString());
        }
        return node;
    }

    /**
     * Creates a valid CurrencyNode given a Currency
     *
     * @param currency Currency to create a CurrencyNode for
     * @return The new CurrencyNode
     */
    public static CurrencyNode buildNode(final Currency currency) {
        final CurrencyNode node = new CurrencyNode();

        node.setSymbol(currency.getCurrencyCode());
        node.setPrefix(currency.getSymbol());
        node.setScale((byte)currency.getDefaultFractionDigits());

        return node;
    }

    /**
     * Generates the default CurrencyNode for the current locale
     * @return The new CurrencyNode
     */
    public static CurrencyNode getDefault() {
        return buildNode(NumberFormat.getCurrencyInstance().getCurrency());
    }
}