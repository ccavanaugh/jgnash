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
package jgnash.convert.imports;

import java.math.BigDecimal;
import java.util.Date;

import jgnash.engine.Account;

/**
 * Common interface for imported transactions from OFX and mt940
 *
 * @author Craig Cavanaugh
 * @author Arnout Engelen
 * @author Nicolas Bouillon
 */
public class ImportTransaction {
    public static enum ImportState {
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

    public Date datePosted;

    /**
     * Date user initiated the transaction, optional, may be null
     */
    public Date dateUser = null;

    public String memo = ""; // memo

    public String payee = ""; // previously: 'name'

    public String checkNumber = ""; // check number (?)

    private ImportState state = ImportState.NEW;

    public ImportState getState() {
        return state;
    }

    public void setState(ImportState state) {
        this.state = state;
    }
}
