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
package jgnash.convert.imports.qif;

import java.util.ArrayList;

/**
 * @author Craig Cavanaugh
 */
public class QifAccount {

    public String name;

    public String type;

    public String description = "";

    public final ArrayList<QifTransaction> items = new ArrayList<>();

    public void addTransaction(QifTransaction item) {
        items.add(item);
    }

    public int numItems() {
        return items.size();
    }

    public QifTransaction get(int index) {
        return items.get(index);
    }

    @Override
    public String toString() {
        return "Name: " + name + '\n' + "Type: " + type + '\n' + "Description: " + description + '\n';
    }
}
