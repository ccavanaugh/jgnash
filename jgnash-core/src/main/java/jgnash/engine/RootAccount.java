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

import javax.persistence.Entity;

/**
 * The RootAccount class.
 *
 * @author Craig Cavanaugh
 */
@Entity
public class RootAccount extends Account {

    /**
     * No argument constructor for reflection purposes.
     * <b>Do not use to create a new instance</b>
     */
    public RootAccount() {
        super();
    }

    /**
     * Public constructor.
     *
     * @param node The base commodity for this data set.
     */
    protected RootAccount(final CurrencyNode node) {
        super(AccountType.ROOT, node);
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.ROOT;
    }

    /**
     * Override the super.  This account does not have a valid or usable path.
     *
     * @return returns an empty string.
     */
    @Override
    public synchronized String getPathName() {
        return "";
    }
}
