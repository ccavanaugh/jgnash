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

import jgnash.engine.CurrencyNode;
import org.junit.jupiter.api.Test;

import java.text.NumberFormat;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NumericFormatsTests {

    @Test
    void localeTest() {

        Locale.setDefault(new Locale("el", "GR"));

        CurrencyNode currencyNode = new CurrencyNode();
        currencyNode.setSymbol("EUR");
        currencyNode.setScale((byte) 2);
        currencyNode.setPrefix("€");

        final NumberFormat numberFormat = NumericFormats.getFullCommodityFormat(currencyNode);

        assertEquals("EUR", numberFormat.getCurrency().getCurrencyCode());

        assertThrows(NullPointerException.class, () -> NumericFormats.getFullCommodityFormat(null));

    }

    @Test
    void knowCurrencyFormats() {
        System.out.println("Full formats");
        for (String pattern: NumericFormats.getKnownFullPatterns()) {
            System.out.println(pattern);
        }


        System.out.println("Short formats");
        for (String pattern: NumericFormats.getKnownShortPatterns()) {
            System.out.println(pattern);
        }
    }
}
