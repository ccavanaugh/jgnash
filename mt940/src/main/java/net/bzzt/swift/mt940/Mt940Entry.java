/*
 * Copyright (C) 2008 Arnout Engelen
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
package net.bzzt.swift.mt940;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class Mt940Entry {
    // 'Credit', in mt940, means money was transferred 
    // to the current account.
    public enum SollHabenKennung {
        CREDIT,
        DEBIT,
        //STORNO_DEBIT,
        //STORNO_CREDIT
    }

    private LocalDate valutaDatum;  // date

    private SollHabenKennung sollHabenKennung;

    private BigDecimal betrag;  // amount

    private String mehrzweckfeld;   // multi purpose field

    private String kontobezeichnung;

    public LocalDate getValutaDatum() {
        return valutaDatum;
    }

    public void setValutaDatum(final LocalDate valutaDatum) {
        this.valutaDatum = valutaDatum;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("At ").append(valutaDatum);
        if (kontobezeichnung != null) {
            sb.append(" (");
            sb.append(kontobezeichnung);
            sb.append(")");
        }
        sb.append(", ");
        sb.append(sollHabenKennung);
        sb.append(": ");
        if (betrag != null) {
            // Set scale to 2 digits and round if necessary, then to plain string for nicer output.
            sb.append(betrag.setScale(2, RoundingMode.HALF_EVEN).toPlainString());
        } else {
            sb.append("-");
        }
        sb.append(" for ");
        sb.append(mehrzweckfeld);
        return sb.toString();
    }

    public SollHabenKennung getSollHabenKennung() {
        return sollHabenKennung;
    }

    public void setSollHabenKennung(SollHabenKennung sollHabenKennung) {
        this.sollHabenKennung = sollHabenKennung;
    }

    public String getMehrzweckfeld() {
        return mehrzweckfeld;
    }

//    public void setMehrzweckfeld(String mehrzweckfeld) {
//        this.mehrzweckfeld = mehrzweckfeld;
//    }

    public BigDecimal getBetrag() {
        return betrag;
    }

    public void setBetrag(final BigDecimal betrag) {
        this.betrag = betrag;
    }

    public void addToMehrzweckfeld(final String string) {
        if (mehrzweckfeld == null || mehrzweckfeld.trim().isEmpty()) {
            mehrzweckfeld = string;
        } else {
            mehrzweckfeld += " ";
            mehrzweckfeld += string;
        }
    }

    public void setKontobezeichnung(final String kontobezeichnung) {
        this.kontobezeichnung = kontobezeichnung;
    }

    public String getKontobezeichnung() {
        return kontobezeichnung;
    }
}
