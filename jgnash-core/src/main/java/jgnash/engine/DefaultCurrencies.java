/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2021 Craig Cavanaugh
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

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static methods for currency generation and discovery.
 * <p>
 * These are known to not show up because Java 1.4.2 and older does not have
 * a default NumberFormat defined for the currency:
 * <p>
 * {@code
 * "SGD"
 * "MYR"
 * }
 *
 * @author Craig Cavanaugh
 */
public class DefaultCurrencies {

    /**
     * Private Constructor, use static methods only.
     */
    private DefaultCurrencies() {
    }

    /**
     * Generates an array of default currency nodes that Java knows about.
     *
     * @return An array of default CurrencyNodes
     */
    public static Set<CurrencyNode> generateCurrencies() {
        TreeSet<CurrencyNode> set = new TreeSet<>();

        for (Locale locale : NumberFormat.getAvailableLocales()) {

            // only try if a valid county length is returned
            if (locale.getCountry().length() == 2) {
                try {
                    if (Currency.getInstance(locale) != null) {
                        set.add(buildNode(locale));
                    }
                } catch (final IllegalArgumentException ignored) {
                    // ignored, locale is not a supported ISO 3166 country code
                } catch (final Exception ex) {
                    Logger.getLogger(DefaultCurrencies.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
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
     * Creates a valid CurrencyNode given a locale.
     *
     * @param locale Locale to create a CurrencyNode for
     * @return The new CurrencyNode
     */
    public static CurrencyNode buildNode(final Locale locale) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        Currency c = symbols.getCurrency();

        CurrencyNode node = new CurrencyNode();
        node.setSymbol(c.getCurrencyCode());
        node.setPrefix(symbols.getCurrencySymbol());

        byte scale = (byte) c.getDefaultFractionDigits();

        if (scale == -1) {  // The JVM may return a negative value for some Locales
            scale = 0;      // scale may be -1, but this is not allowed for CurrencyNodes
        }

        node.setScale(scale);

        return node;
    }

    /**
     * Generates the default CurrencyNode for the current locale.
     *
     * @return The new CurrencyNode
     */
    public static CurrencyNode getDefault() {
        try {
            return buildNode(Locale.getDefault());
        } catch (final Exception e) {
            return buildNode(Locale.US);
        }
    }
}