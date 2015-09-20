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
package jgnash.convert.imports;

import java.math.BigDecimal;
import java.time.LocalDate;

import jgnash.engine.Account;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

/**
 * Common interface for imported transactions from OFX and mt940
 *
 * @author Craig Cavanaugh
 * @author Arnout Engelen
 * @author Nicolas Bouillon
 */
public class ImportTransaction implements Comparable<ImportTransaction> {

    public enum ImportState {
        NEW,
        EQUAL,
        IGNORE,
        NOT_EQUAL
    }

    /**
     * Destination account
     */
    public Account account;

    /**
     * Deposits get positive 'amounts', withdrawals negative
     */
    public BigDecimal amount;

    @NotNull public LocalDate datePosted = LocalDate.now();

    /**
     * Date user initiated the transaction, optional, may be null
     */
    @Nullable public LocalDate dateUser = null;

    public String memo = ""; // memo

    @NotNull public String payee = ""; // previously: 'name'

    public String checkNumber = ""; // check number (?)

    private ImportState state = ImportState.NEW;

    public ImportState getState() {
        return state;
    }

    public void setState(ImportState state) {
        this.state = state;
    }

    @Override
    public int compareTo(@NotNull final ImportTransaction importTransaction) {
        if (importTransaction == this) {
            return 0;
        }

        int result = datePosted.compareTo(importTransaction.datePosted);
        if (result != 0) {
            return result;
        }

        result = payee.compareTo(importTransaction.payee);
        if (result != 0) {
            return result;
        }

        return Integer.compare(hashCode(), importTransaction.hashCode());
    }
}
