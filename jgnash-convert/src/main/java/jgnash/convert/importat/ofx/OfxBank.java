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
package jgnash.convert.importat.ofx;

import java.math.BigDecimal;
import java.time.LocalDate;

import jgnash.convert.importat.ImportBank;
import jgnash.convert.importat.ImportSecurity;
import jgnash.convert.importat.ImportTransaction;
import jgnash.util.Nullable;

/**
 * OFX Bank Object
 *
 * @author Craig Cavanaugh
 * @author Nicolas Bouillon
 */
public class OfxBank extends ImportBank<ImportTransaction> {

    public String currency;

    String bankId;

    /**
     * Branch identifier. May be required for some non-US banks
     */
    String branchId;

    public String accountId;

    String accountType;

    LocalDate dateStart;

    LocalDate dateEnd;

    BigDecimal ledgerBalance;

    LocalDate ledgerBalanceDate;

    BigDecimal availBalance;

    LocalDate availBalanceDate;

    int statusCode;

    String statusSeverity;

    @Nullable
    String statusMessage;

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append("statusCode: ").append(statusCode).append('\n');
        b.append("statusSeverity: ").append(statusSeverity).append('\n');
        b.append("statusMessage: ").append(statusMessage).append('\n');
        b.append("currency: ").append(currency).append('\n');
        b.append("bankId: ").append(bankId).append('\n');
        b.append("branchId: ").append(branchId).append('\n');
        b.append("accountId: ").append(accountId).append('\n');
        b.append("accountType: ").append(accountType).append('\n');
        b.append("dateStart: ").append(dateStart).append('\n');
        b.append("dateEnd: ").append(dateEnd).append('\n');
        b.append("ledgerBalance: ").append(ledgerBalance).append('\n');
        b.append("ledgerBalanceDate: ").append(ledgerBalanceDate).append('\n');

        if (availBalance != null) {
            b.append("availBalance: ").append(availBalance).append('\n');
        }

        if (availBalanceDate != null) {
            b.append("availBalanceDate: ").append(availBalanceDate).append('\n');
        }

        for (final ImportTransaction t : getTransactions()) {
            b.append(t).append('\n');
        }

        for (final ImportSecurity importSecurity : securityList) {
            b.append(importSecurity.toString()).append('\n');
        }

        return b.toString();
    }

    /* <CURDEF>USD
    <BANKACCTFROM>
    <BANKID>074914229
    <ACCTID>10076164
    <ACCTTYPE>CHECKING
    </BANKACCTFROM>
     */
}
