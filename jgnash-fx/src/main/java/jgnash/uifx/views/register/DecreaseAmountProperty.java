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
 * UI helper.  A negative BigDecimal is shown as positive or not shown at all.
 *
* @author Craig Cavanaugh
*/
class DecreaseAmountProperty extends SimpleObjectProperty<BigDecimal> {
    DecreaseAmountProperty(final BigDecimal value) {
        if (value.signum() < 0) {
            setValue(value.abs());
        } else {
            setValue(null);
        }
    }
}
