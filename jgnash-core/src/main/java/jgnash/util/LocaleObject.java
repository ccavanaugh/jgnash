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
package jgnash.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
        int hash = 7;
        hash = 47 * hash + this.locale.hashCode();
        return 47 * hash + this.display.hashCode();
    }

    private boolean equals(final LocaleObject obj) {
        return obj.locale.equals(locale);
    }

    public static LocaleObject[] getLocaleObjects() {
        final Locale[] tList = Locale.getAvailableLocales();
        final List<LocaleObject> localeObjects = new ArrayList<>();

        for (final Locale aTList : tList) {
            if (aTList.getDisplayName() != null && !aTList.getDisplayName().isEmpty()) {
                localeObjects.add(new LocaleObject(aTList));
            }
        }

        Collections.sort(localeObjects);
        return localeObjects.toArray(new LocaleObject[localeObjects.size()]);
    }
}
