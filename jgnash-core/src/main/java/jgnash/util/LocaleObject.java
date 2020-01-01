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
package jgnash.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility Class for display and ordering of {@code Locale} objects in a nice readable and sorted order.
 *
 * @author Craig Cavanaugh
 */
public class LocaleObject implements Comparable<LocaleObject> {

    private final Locale locale;

    private final String display;

    public LocaleObject(final Locale locale) {
        this.locale = Objects.requireNonNull(locale);

        display = locale.getDisplayName() + " - " + locale + "  [" + locale.getDisplayName(locale) + "]";
    }

    public Locale getLocale() {
        return locale;
    }

    @Override
    public final String toString() {
        return display;
    }

    @Override
    public int compareTo(@NotNull final LocaleObject o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof LocaleObject) {
            return equals((LocaleObject) obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(locale, display);
    }

    private boolean equals(final LocaleObject obj) {
        return obj.locale.equals(locale);
    }

    public static Collection<LocaleObject> getLocaleObjects() {
        return Arrays.stream(Locale.getAvailableLocales())
            .filter(l -> l.getDisplayName() != null
                && !l.getDisplayName().isEmpty()
                && !l.getCountry().isEmpty())
            .map(LocaleObject::new)
            .sorted().collect(Collectors.toList());
    }
}
