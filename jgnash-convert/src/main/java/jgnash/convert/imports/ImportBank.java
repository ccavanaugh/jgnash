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
package jgnash.convert.imports;

import java.util.ArrayList;
import java.util.List;

/**
 * Common superclass for OFX and MT940 imports
 * 
 * @author Craig Cavanaugh
 * @author Arnout Engelen
 */
public class ImportBank {
    private List<ImportTransaction> transactions = new ArrayList<>();

    public void setTransactions(List<ImportTransaction> transactions) {
        this.transactions = transactions;
    }

    public List<ImportTransaction> getTransactions() {
        return transactions;
    }

    public void addTransaction(ImportTransaction transaction) {
        transactions.add(transaction);
    }
}
