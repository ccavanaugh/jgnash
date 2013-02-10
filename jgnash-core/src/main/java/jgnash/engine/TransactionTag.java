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
package jgnash.engine;

import jgnash.util.Resource;

/**
 * Tagging enumeration for special transaction types
 * 
 * @author Craig Cavanaugh
 *
 */
public enum TransactionTag {

    BANK(Resource.get().getString("Tag.Bank")),
    DIVIDEND(Resource.get().getString("Tag.Dividend")),
    FEES_OFFSET(Resource.get().getString("Tag.FeesOffset")),
    GAIN_LOSS(Resource.get().getString("Tag.GainLoss")),
    GAINS_OFFSET(Resource.get().getString("Tag.GainsOffset")),
    INVESTMENT(Resource.get().getString("Tag.Investment")),
    INVESTMENT_FEE(Resource.get().getString("Tag.InvestmentFee")),
    INVESTMENT_CASH_TRANSFER(Resource.get().getString("Tag.InvestmentCashTransfer")),
    VAT(Resource.get().getString("Tag.Vat"));

    private final transient String description;

    private TransactionTag(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
