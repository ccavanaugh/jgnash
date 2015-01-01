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
package jgnash.engine;

import jgnash.util.Resource;

/**
 * Transaction type class.
 *
 * @author Craig Cavanaugh
 *
 */
public enum TransactionType {

    // bank transactions
    DOUBLEENTRY(Resource.get().getString("Transaction.DoubleEntry")),
    SPLITENTRY(Resource.get().getString("Transaction.SplitEntry")),
    SINGLENTRY(Resource.get().getString("Transaction.SingleEntry")),

    // investment transactions
    ADDSHARE(Resource.get().getString("Transaction.AddShare")),
    BUYSHARE(Resource.get().getString("Transaction.BuyShare")),
    DIVIDEND(Resource.get().getString("Transaction.Dividend")),
    REINVESTDIV(Resource.get().getString("Transaction.ReinvestDiv")),
    REMOVESHARE(Resource.get().getString("Transaction.RemoveShare")),
    RETURNOFCAPITAL(Resource.get().getString("Transaction.ReturnOfCapital")),
    SELLSHARE(Resource.get().getString("Transaction.SellShare")),
    SPLITSHARE(Resource.get().getString("Transaction.SplitShare")),
    MERGESHARE(Resource.get().getString("Transaction.MergeShare")),

    INVALID(Resource.get().getString("Word.Invalid"));

    final transient private String transactionTypeName;

    private TransactionType(String name) {
        transactionTypeName = name;
    }

    @Override
    public String toString() {
        return transactionTypeName;
    }
}
