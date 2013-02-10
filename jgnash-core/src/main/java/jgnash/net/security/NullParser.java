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

package jgnash.net.security;

import java.math.BigDecimal;
import java.util.Date;

import jgnash.engine.SecurityNode;
import jgnash.util.DateUtils;

/**
 * Null security history parser
 *
 * @author Craig Cavanaugh
 *
 */
public class NullParser implements SecurityParser {

    @Override
    public BigDecimal getPrice() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getHigh() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getLow() {
        return BigDecimal.ZERO;
    }

    @Override
    public long getVolume() {
        return 0;
    }

    @Override
    public boolean parse(SecurityNode node) {
        return false;
    }

    @Override
    public Date getDate() {
       return DateUtils.today();
    }

}
