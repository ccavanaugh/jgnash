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

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import jgnash.engine.SecurityHistoryEvent;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.util.NotNull;

/**
 * Interface for security parser.
 *
 * @author Craig Cavanaugh
 */
public interface SecurityParser {

    /**
     * Sets a {@code Supplier} that can provide an API token when requested.
     *
     * @param supplier token {@code Supplier}
     */
    void setTokenSupplier(@NotNull final Supplier<String> supplier);

    /**
     * Retrieves historical pricing
     *
     * @param securityNode SecurityNode to retrieve events for
     * @param startDate    start date
     * @param endDate      end date
     * @return List of SecurityHistoryNode
     * @throws IOException indicates if IO / Network error has occurred
     */
    @NotNull
    List<SecurityHistoryNode> retrieveHistoricalPrice(@NotNull final SecurityNode securityNode,
                                                      final LocalDate startDate, final LocalDate endDate) throws IOException;

    /**
     * Retrieves historical events
     *
     * @param securityNode SecurityNode to retrieve events for
     * @param endDate      end date
     * @return Set of SecurityHistoryEvent
     * @throws IOException indicates if IO / Network error has occurred
     */
    @NotNull
    Set<SecurityHistoryEvent> retrieveHistoricalEvents(@NotNull final SecurityNode securityNode,
                                                       final LocalDate endDate) throws IOException;
}
