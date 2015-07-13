/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
import java.util.Locale;
import java.util.Objects;

/**
 * Utility Class for display and ordering of {@code Locale} objects in a nice readable and sorted order
 */
public class LocaleObject implements Comparable<LocaleObject> {

    final Locale locale;

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
    public int compareTo(final LocaleObject o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public boolean equals(final Object obj) {
        assert obj instanceof LocaleObject;

        return equals((LocaleObject) obj);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + this.locale.hashCode();
        return 47 * hash + this.display.hashCode();
    }

    public boolean equals(final LocaleObject obj) {
        return obj.locale.equals(locale);
    }

    public static LocaleObject[] getLocaleObjects() {
        Locale[] tList = Locale.getAvailableLocales();

        LocaleObject[] list = new LocaleObject[tList.length];

        for (int i = 0; i < list.length; i++) {
            list[i] = new LocaleObject(tList[i]);
        }

        Arrays.sort(list);
        return list;
    }
}
