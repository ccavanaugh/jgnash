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
package jgnash.engine;

import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Math constants utility class.
 *
 * @author Craig Cavanaugh
 */
public final class MathConstants {

    /**
     * Default rounding mode.
     */
    public static final RoundingMode roundingMode = RoundingMode.HALF_UP;

    /**
     * Default math context.
     */
    public static final MathContext mathContext = new MathContext(16, roundingMode);

    /**
     * Default math context for budget values.  Reduces precision to decrease file size.
     */
    public static final MathContext budgetMathContext = new MathContext(8, roundingMode);

    /**
     * Number of significant digits to the right of the decimal SEPARATOR.
     */
    public static final int EXCHANGE_RATE_ACCURACY = 6;

    /**
     * Number of significant digits to the right of the decimal SEPARATOR.
     */
    public static final int SECURITY_PRICE_ACCURACY = 6;

    /**
     * Number of significant digits to the right of the decimal SEPARATOR.
     */
    public static final int SECURITY_QUANTITY_ACCURACY = 6;

    public static final int DEFAULT_COMMODITY_PRECISION = 4;

    private MathConstants() {
        // restrict instantiation
    }
}
