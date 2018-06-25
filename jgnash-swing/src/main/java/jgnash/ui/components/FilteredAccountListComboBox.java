/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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
package jgnash.ui.components;

import java.util.List;

import jgnash.engine.Account;
import jgnash.engine.Comparators;

/**
 * ComboBox for displaying a list of accounts. It can be filtered by locked and placeholder accounts. Automatically
 * refreshes itself when necessary.
 * 
 * @author Peter Vida
 *
 */
public class FilteredAccountListComboBox extends AccountListComboBox {

    private boolean hideLocked;

    private boolean hidePlaceholder;

    public FilteredAccountListComboBox(final boolean hideLocked, final boolean hidePlaceholder) {
        super(new FilteredModel(null, hideLocked, hidePlaceholder));
        this.hideLocked = hideLocked;
        this.hidePlaceholder = hidePlaceholder;
    }

    public void setHideLocked(final boolean value) {
        hideLocked = value;
        setModel(new FilteredModel(null, hideLocked, hidePlaceholder));
    }

    public void setHidePlaceholder(final boolean value) {
        hidePlaceholder = value;
        setModel(new FilteredModel(null, hideLocked, hidePlaceholder));
    }

    private final static class FilteredModel extends AbstractModel {

        private final boolean hideLocked;

        private final boolean hidePlaceholder;

        FilteredModel(final Account exclude, final boolean hideLocked, final boolean hidePlaceholder) {
            super(exclude);
            this.hideLocked = hideLocked;
            this.hidePlaceholder = hidePlaceholder;
            loadAccounts();
        }

        @Override
        protected void loadChildren(final Account acc, final List<Account> array) {

            for (final Account tAcc : acc.getChildren(Comparators.getAccountByCode())) {
                if (!(hideLocked && tAcc.isLocked() || hidePlaceholder && tAcc.isPlaceHolder())) { // honor the account lock and placeHolder attribute
                    if (baseAccount != tAcc) {
                        array.add(tAcc);
                    }
                }
                if (tAcc.getChildCount() > 0) { // recursively load the account list
                    loadChildren(tAcc, array);
                }
            }
        }
    }
}
