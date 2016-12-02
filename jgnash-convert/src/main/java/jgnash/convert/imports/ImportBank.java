/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
public class ImportBank<E extends ImportTransaction> {

    private List<E> transactions = new ArrayList<>();

    protected List<ImportSecurity> securityList = new ArrayList<>();

    public void addSecurity(final ImportSecurity importSecurity) {
        securityList.add(importSecurity);
    }

    public void setTransactions(List<E> transactions) {
        this.transactions = transactions;
    }

    public List<E> getTransactions() {
        return transactions;
    }

    public void addTransaction(E transaction) {
        transactions.add(transaction);
    }
}
