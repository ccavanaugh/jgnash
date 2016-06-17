/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import java.math.BigDecimal;

import jgnash.engine.MathConstants;

/**
 * Returns a "word" representation of a BigDecimal.
 * 
 * @author Craig Cavanaugh
 */
public class BigDecimalToWords {

    private static final String[] tens = { "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty",
                    "ninety" };

    private static final String[] ones = { "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
                    "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen",
                    "nineteen" };

    private BigDecimalToWords() {
    }

    public static String convert(final BigDecimal decimal) {
        BigDecimal temp = decimal.setScale(2, MathConstants.roundingMode);
        StringBuilder val = new StringBuilder(convert(temp.longValue()));

        if (val.charAt(val.length() - 1) != ' ') {
            val.append(' ');
        }

        val.setCharAt(0, Character.toUpperCase(val.charAt(0)));
        String t = temp.toPlainString();
        int index = t.indexOf('.');
        val.append("and ");
        if (index >= 0) {
            return val + t.substring(index + 1) + "/100";
        }
        return val + "00/100";
    }

    private static String convert(final long number) {
        long temp = number;
        long billions = temp / 1000000000;
        temp %= 1000000000;
        long millions = temp / 1000000;
        temp %= 1000000;
        long thousands = temp / 1000;
        temp %= 1000;
        long hundreds = temp / 100;
        temp %= 100;

        StringBuilder result = new StringBuilder();

        if (billions > 0) {
            result.append(convert(billions)).append(" billion ");
        }
        if (millions > 0) {
            result.append(convert(millions)).append(" million ");
        }
        if (thousands > 0) {
            result.append(convert(thousands)).append(" thousand ");
        }
        if (hundreds > 0) {
            result.append(convert(hundreds)).append(" hundred ");
        }
        if (temp != 0) {
            if (0 < temp && temp <= 19) {
                result.append(ones[(int) temp]);
            } else {
                long ten = temp / 10;
                long one = temp % 10;
                result.append(tens[(int) ten]);
                if (one > 0) {
                    result.append('-');
                    result.append(ones[(int) one]);
                }
            }
        }
        return result.toString();
    }
}
