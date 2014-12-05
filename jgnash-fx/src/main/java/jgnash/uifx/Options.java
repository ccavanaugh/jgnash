/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.uifx;

import java.util.prefs.Preferences;

/**
 * Manages application preferences
 *
 * @author Craig Cavanaugh
 */
public class Options {
    private static final String CONFIRM_ON_DELETE = "confirmDelete";

    private static final String ACCOUNTING_TERMS = "accountingTerms";

    private static boolean useAccountingTerms;

    private static boolean confirmTransactionDelete;

    static {
        Preferences p = Preferences.userNodeForPackage(Options.class);
        useAccountingTerms = p.getBoolean(ACCOUNTING_TERMS, false);
        confirmTransactionDelete = p.getBoolean(CONFIRM_ON_DELETE, true);
    }

    /**
     * Sets if confirm on transaction delete is enabled
     *
     * @param enabled true if deletion confirmation is required
     */
    public static void setConfirmTransactionDeleteEnabled(final boolean enabled) {
        confirmTransactionDelete = enabled;
        Preferences p = Preferences.userNodeForPackage(Options.class);
        p.putBoolean(CONFIRM_ON_DELETE, confirmTransactionDelete);
    }

    /**
     * Returns the availability of sortable registers
     *
     * @return true if confirm on transaction delete is enabled, false otherwise
     */
    public static boolean isConfirmTransactionDeleteEnabled() {
        return confirmTransactionDelete;
    }

    public static void setAccountingTermsEnabled(final boolean enabled) {
        useAccountingTerms = enabled;
        Preferences p = Preferences.userNodeForPackage(Options.class);
        p.putBoolean(ACCOUNTING_TERMS, useAccountingTerms);
    }

    public static boolean isAccountingTermsEnabled() {
        return useAccountingTerms;
    }

}
