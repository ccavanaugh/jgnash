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
package jgnash.engine;

import java.io.Serializable;
import java.util.Comparator;

/**
 * High level StoredObject comparator.  When serializing to an XML file, this helps with readability of references
 *
 * @author Craig Cavanaugh
 */
public class StoredObjectComparator implements Comparator<StoredObject>, Serializable {

    @Override
    public int compare(final StoredObject o1, final StoredObject o2) {

        if ((o1 instanceof CurrencyNode || o2 instanceof CurrencyNode) && !(o1 instanceof CurrencyNode && o2 instanceof CurrencyNode)) {
            if (o1 instanceof CurrencyNode) {
                return -1;
            }
            return 1;
        }

        if ((o1 instanceof SecurityNode || o2 instanceof SecurityNode) && !(o1 instanceof SecurityNode && o2 instanceof SecurityNode)) {

            if (o1 instanceof SecurityNode) {
                return -1;
            }
            return 1;
        }

        if (o1 instanceof Config && o2 instanceof RootAccount) {
            return -1;
        }

        if (o1 instanceof RootAccount && o2 instanceof Config) {
            return 1;
        }

        // two config objects should never occur
        if ((o1 instanceof Config || o2 instanceof Config) && !(o1 instanceof Config && o2 instanceof Config)) {
            if (o1 instanceof Config) {
                return -1;
            }
            return 1;
        }

        // two root accounts should never occur
        if ((o1 instanceof RootAccount || o2 instanceof RootAccount) && !(o1 instanceof RootAccount && o2 instanceof RootAccount)) {
            if (o1 instanceof RootAccount) {
                return -1;
            }
            return 1;
        }

        if (!o1.getClass().equals(o2.getClass())) {
            return o1.getClass().getName().compareTo(o2.getClass().getName());
        }

        return o1.getUuid().compareTo(o2.getUuid());
    }
}
