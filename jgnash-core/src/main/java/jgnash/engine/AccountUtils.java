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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.engine;

/**
 * Static account utilities.
 * 
 * @author Craig Cavanaugh
 */
class AccountUtils {

    /**
     * Searches an account tree given the supplied parameters.
     * 
     * @param root Base account
     * @param name Account name
     * @param type Account type
     * @param depth Account depth
     * @return matched account if it exists
     */
    static Account searchTree(final Account root, final String name, final AccountType type, final int depth) {
        Account match = null;

        for (final Account a : root.getChildren()) {
            if (a.getName().equals(name) && a.getAccountType() == type && a.getDepth() == depth) {
                match = a;
            } else if (a.getChildCount() > 0) {
                match = searchTree(a, name, type, depth);
            }

            if (match != null) {
                break;
            }
        }
        return match;
    }

    private AccountUtils() {
    }
}
