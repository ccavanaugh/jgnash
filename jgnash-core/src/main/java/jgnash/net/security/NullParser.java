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

package jgnash.net.security;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import jgnash.engine.SecurityHistoryEvent;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.util.NotNull;

/**
 * Null security history parser.
 *
 * Returns empty results
 *
 * @author Craig Cavanaugh
 */
public class NullParser implements SecurityParser {

    /**
     * Sets a {@code Supplier} that can provide an API token when requested.
     *
     * @param supplier token {@code Supplier}
     */
    @Override
    public void setTokenSupplier(Supplier<String> supplier) {
        // do nothing...
    }

    @Override
    public List<SecurityHistoryNode> retrieveHistoricalPrice(final SecurityNode securityNode, final LocalDate startDate, final LocalDate endDate) {
        return Collections.emptyList();
    }

    @Override
    public Set<SecurityHistoryEvent> retrieveHistoricalEvents(@NotNull final SecurityNode securityNode, final LocalDate endDate) {
        return Collections.emptySet();
    }
}
