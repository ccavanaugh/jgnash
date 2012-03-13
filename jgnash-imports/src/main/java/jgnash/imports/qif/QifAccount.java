/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
package jgnash.imports.qif;

import java.util.ArrayList;

/**
 * @author Craig Cavanaugh
 * @version $Id: QifAccount.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class QifAccount {

    public String name;

    public String type;

    public String description = "";

    public String creditLimit;

    public String statementBalanceDate;

    public String statementBalance;

    public ArrayList<QifTransaction> items = new ArrayList<QifTransaction>();

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
        StringBuilder buf = new StringBuilder();
        buf.append("Name: ").append(name).append('\n');
        buf.append("Type: ").append(type).append('\n');
        buf.append("Description: ").append(description).append('\n');
        return buf.toString();
    }
}
