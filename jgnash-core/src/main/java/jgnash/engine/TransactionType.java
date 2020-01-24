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
 * Transaction type class.
 *
 * @author Craig Cavanaugh
 *
 */
public enum TransactionType {

    // bank transactions
    DOUBLEENTRY(ResourceUtils.getString("Transaction.DoubleEntry")),
    SPLITENTRY(ResourceUtils.getString("Transaction.SplitEntry")),
    SINGLENTRY(ResourceUtils.getString("Transaction.SingleEntry")),

    // investment transactions
    ADDSHARE(ResourceUtils.getString("Transaction.AddShare")),
    BUYSHARE(ResourceUtils.getString("Transaction.BuyShare")),
    DIVIDEND(ResourceUtils.getString("Transaction.Dividend")),
    REINVESTDIV(ResourceUtils.getString("Transaction.ReinvestDiv")),
    REMOVESHARE(ResourceUtils.getString("Transaction.RemoveShare")),
    RETURNOFCAPITAL(ResourceUtils.getString("Transaction.ReturnOfCapital")),
    SELLSHARE(ResourceUtils.getString("Transaction.SellShare")),
    SPLITSHARE(ResourceUtils.getString("Transaction.SplitShare")),
    MERGESHARE(ResourceUtils.getString("Transaction.MergeShare")),

    INVALID(ResourceUtils.getString("Word.Invalid"));

    private final transient String transactionTypeName;

    TransactionType(String name) {
        transactionTypeName = name;
    }

    @Override
    public String toString() {
        return transactionTypeName;
    }
}
