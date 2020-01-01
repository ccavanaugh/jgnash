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
package jgnash.report.table;

/**
 * Available report column styles
 *
 * @author Craig Cavanaugh
 */
public enum ColumnStyle {
    AMOUNT_SUM,
    BALANCE,
    BALANCE_WITH_SUM,
    BALANCE_WITH_SUM_AND_GLOBAL,
    GROUP,
    GROUP_NO_HEADER,
    PERCENTAGE,
    QUANTITY,
    SHORT_AMOUNT,
    SHORT_DATE,
    STRING,
    TIMESTAMP
}
