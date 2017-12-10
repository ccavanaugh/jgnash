/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import jgnash.util.NotNull;

/**
 * Represents security history events such as splits and dividends.
 *
 * Equality is assumed if the date and type match.
 *
 * @author Craig Cavanaugh
 */
@Entity
public class SecurityHistoryEvent implements Comparable<SecurityHistoryEvent>, Serializable {

    @SuppressWarnings("unused")
    @Id @GeneratedValue(strategy= GenerationType.TABLE)
    public long id;

    @Enumerated(EnumType.STRING)
    private SecurityHistoryEventType type = SecurityHistoryEventType.DIVIDEND;

    @Column(precision = 19, scale = 4)
    private BigDecimal value = BigDecimal.ZERO;

    private LocalDate date = LocalDate.now();

    /**
     * public no-argument constructor for reflection.
     */
    @SuppressWarnings("unused")
    public SecurityHistoryEvent() {
    }

    public SecurityHistoryEvent(@NotNull final SecurityHistoryEventType type, @NotNull final LocalDate date,
                                @NotNull final BigDecimal value) {
        this.type = type;
        this.date = date;
        this.value = value;
    }

    public SecurityHistoryEventType getType() {
        return type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SecurityHistoryEvent event = (SecurityHistoryEvent) o;

        return Objects.equals(type, event.type) && date.compareTo(((SecurityHistoryEvent) o).getDate()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, date);
    }

    @Override
    public int compareTo(@NotNull final SecurityHistoryEvent historyEvent) {
        if (historyEvent == this) {
            return 0;
        }

        int result = date.compareTo(historyEvent.getDate());
        if (result != 0) {
            return result;
        }

        return type.compareTo(historyEvent.getType());
    }
}
