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

import jgnash.engine.CurrencyNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test void testPercentages() {
        final NumberFormat format = NumericFormats.getPercentageFormat();

        assertEquals("12.34%", format.format(.12344));
        assertEquals("12.35%", format.format(.12345));
    }

    @Test void testFixedPrecisionFormat() {
        assertEquals("12.34", NumericFormats.getFixedPrecisionFormat(2).format(12.344));
        assertEquals("12.35", NumericFormats.getFixedPrecisionFormat(2).format(12.345));
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

    @Test
    void testFormats() {
        Locale.setDefault(Locale.US);

        final CurrencyNode node = new CurrencyNode();
        node.setSymbol("USD");
        node.setScale((byte) 2);
        node.setPrefix("$");

        // preserve the configured format
        final String oldShortPattern = NumericFormats.getShortFormatPattern();
        final String oldFullPattern = NumericFormats.getFullFormatPattern();

        NumericFormats.setFullFormatPattern("\u00A4#,##0.00;(\u00A4#,##0.00)");
        NumericFormats.setShortFormatPattern("\u00A4#,##0.00;-\u00A4#,##0.00");

        NumberFormat shortFormat = NumericFormats.getShortCommodityFormat(node);
        NumberFormat fullFormat = NumericFormats.getFullCommodityFormat(node);

        assertNotNull(fullFormat);
        assertNotNull(shortFormat);

        assertEquals("$10.00", shortFormat.format(BigDecimal.TEN));
        assertEquals("$10.00 ", fullFormat.format(BigDecimal.TEN));

        assertEquals("-$10.00", shortFormat.format(BigDecimal.TEN.negate()));
        assertEquals("($10.00)", fullFormat.format(BigDecimal.TEN.negate()));

        NumericFormats.setShortFormatPattern("#,##0.00;-#,##0.00");
        shortFormat = NumericFormats.getShortCommodityFormat(node);

        assertEquals("10.00", shortFormat.format(BigDecimal.TEN));
        assertEquals("-10.00", shortFormat.format(BigDecimal.TEN.negate()));

        // restore the old patterns
        NumericFormats.setShortFormatPattern(oldShortPattern);
        NumericFormats.setFullFormatPattern(oldFullPattern);
    }
}
