/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
package jgnash.ui.register;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Component;
import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jgnash.engine.Transaction;

/**
 * Abstract JPanel that implements common code used in all TransactionPanels.
 * This class does not perform any layout or container assignment.
 *
 * @author Craig Cavanaugh
 *
 */
public abstract class AbstractTransactionPanel extends AbstractEntryFormPanel {

    private static final String REMEMBER_DATE = "rememberDate";

    private static boolean rememberDate;

    static {
        Preferences p = Preferences.userNodeForPackage(AbstractTransactionPanel.class);
        rememberDate = p.getBoolean(REMEMBER_DATE, true);
    }

    public static void setRememberLastDate(final boolean reset) {
        rememberDate = reset;
        Preferences p = Preferences.userNodeForPackage(AbstractTransactionPanel.class);
        p.putBoolean(REMEMBER_DATE, rememberDate);
    }

    /**
     * Determines if the last date used for a transaction is reset
     * to the current date or remembered.
     *
     * @return true if the last date should be reused
     */
    public static boolean getRememberLastDate() {
        return rememberDate;
    }

    /**
     * A method to help create one row sub panels.  This helps to work around
     * a layout limitation of components spanning multiple columns.
     * If a String is passed as a component, it will be localized and
     * converted to a JLabel.
     *
     * @param columnSpec The column spec for the layout
     * @param components The components for the sub-panel
     * @return The resulting JPanel
     */
    protected JPanel buildHorizontalSubPanel(final String columnSpec, final Object... components) {
        FormLayout layout = new FormLayout(columnSpec, "f:d:g");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        for (Object component1 : components) {
            if (component1 instanceof String) {
                builder.append(new JLabel(rb.getString((String) component1))); // add a label
            } else {
                builder.append((Component) component1); // add a component
            }
        }
        return builder.getPanel();
    }

    /**
     * Creates a transaction using the contents of the form.
     *
     * @return The generated transaction
     */
    protected abstract Transaction buildTransaction();
}
