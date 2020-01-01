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
package jgnash.engine;

import jgnash.resource.util.ResourceUtils;

/**
 * Tagging enumeration for special transaction types.
 * 
 * @author Craig Cavanaugh
 */
@SuppressWarnings("UnusedDeclaration")
public enum TransactionTag {

    BANK(ResourceUtils.getString("Tag.Bank")),
    DIVIDEND(ResourceUtils.getString("Tag.Dividend")),
    FEES_OFFSET(ResourceUtils.getString("Tag.FeesOffset")),
    GAIN_LOSS(ResourceUtils.getString("Tag.GainLoss")),
    GAINS_OFFSET(ResourceUtils.getString("Tag.GainsOffset")),
    INVESTMENT(ResourceUtils.getString("Tag.Investment")),
    INVESTMENT_FEE(ResourceUtils.getString("Tag.InvestmentFee")),
    INVESTMENT_CASH_TRANSFER(ResourceUtils.getString("Tag.InvestmentCashTransfer")),
    VAT(ResourceUtils.getString("Tag.Vat"));

    private final transient String description;

    TransactionTag(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
