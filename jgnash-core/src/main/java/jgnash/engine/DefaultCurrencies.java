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
 * Static methods for currency generation and discovery
 *
 * These are known to not show up because Java 1.4.2 and older does not have
 * a default NumberFormat defined for the currency:<br>
 * "SGD"<br>
 * "MYR"
 *
 * @author Craig Cavanaugh
 * @version $Id: DefaultCurrencies.java 3051 2012-01-02 11:27:23Z ccavanaugh $
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
        TreeSet<CurrencyNode> set = new TreeSet<>();

        for (Locale locale : NumberFormat.getAvailableLocales()) {

             // only try if a valid county length is returned
            if (locale.getCountry().length() == 2) {
                try {
                    if (Currency.getInstance(locale) != null) {
                        set.add(buildNode(locale));
                    }
                } catch (Exception ex) {
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
    public static CurrencyNode buildCustomNode(String ISOCode) {
        CurrencyNode node = new CurrencyNode();
        Currency c;

        try {
            c = Currency.getInstance(ISOCode);
            node.setSymbol(c.getCurrencyCode());
        } catch (Exception e) {
            node.setSymbol(ISOCode);
        } finally {
            node.setDescription(Locale.getDefault().toString());
        }
        return node;
    }

    /**
     * Creates a valid CurrencyNode given a locale
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
        node.setScale((byte) c.getDefaultFractionDigits());

        return node;
    }
}