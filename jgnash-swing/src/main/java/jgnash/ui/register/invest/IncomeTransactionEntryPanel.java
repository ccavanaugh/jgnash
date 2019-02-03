/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
package jgnash.ui.register.invest;

import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionTag;
import jgnash.ui.register.PanelType;
import jgnash.ui.register.SplitTransactionEntryPanel;
import jgnash.ui.register.table.SplitsRegisterTableModel;

/**
 * Entry panel for investment fee transactions
 *
 * @author Craig Cavanaugh
 *
 */
public class IncomeTransactionEntryPanel extends SplitTransactionEntryPanel {

    IncomeTransactionEntryPanel(SplitsRegisterTableModel model, PanelType type) {
        super(model, type);
    }

    @Override
    public TransactionEntry buildTransactionEntry() {
        TransactionEntry entry = super.buildTransactionEntry();
        entry.setTransactionTag(TransactionTag.GAIN_LOSS);

        return entry;
    }
}
