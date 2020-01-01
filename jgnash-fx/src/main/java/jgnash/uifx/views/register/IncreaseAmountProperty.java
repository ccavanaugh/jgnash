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
package jgnash.uifx.views.register;

import java.math.BigDecimal;

import javafx.beans.property.SimpleObjectProperty;

/**
 * UI helper. If a BigDecimal is negative, do not show it.
 *
 * @author Craig Cavanaugh
 */
class IncreaseAmountProperty extends SimpleObjectProperty<BigDecimal> {
    IncreaseAmountProperty(final BigDecimal value) {
        if (value.signum() >= 0) {
            setValue(value);
        } else {
            setValue(null);
        }
    }
}
