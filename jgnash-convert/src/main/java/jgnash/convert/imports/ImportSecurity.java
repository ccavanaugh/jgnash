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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Security Import Object
 *
 * @author Craig Cavanaugh
 */
public class ImportSecurity {

    public String ticker;
    public String securityName;
    public BigDecimal unitPrice;
    public LocalDate localDate;
    public String id;
    public String idType;

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append("ticker: ").append(ticker).append('\n');
        b.append("securityName: ").append(securityName).append('\n');
        b.append("unitPrice: ").append(unitPrice).append('\n');
        b.append("localDate: ").append(localDate).append('\n');

        if (id != null) {
            b.append("id: ").append(id).append('\n');
        }

        if (idType != null) {
            b.append("idType: ").append(idType).append('\n');
        }

        return b.toString();
    }

}
