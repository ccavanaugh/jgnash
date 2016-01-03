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
package jgnash.ui.components;

import java.awt.EventQueue;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.prefs.Preferences;

import javax.swing.JTextField;

/** Extended JTextField that selects existing text when it receives the focus
 * 
 * @author Craig Cavanaugh
 */
public class JTextFieldEx extends JTextField {

    private static final String SELECT_ON_FOCUS = "selectOnFocus";

    private static boolean select = false;

    static {
        Preferences p = Preferences.userNodeForPackage(JTextFieldEx.class);
        select = p.getBoolean(SELECT_ON_FOCUS, false);
    }

    public JTextFieldEx() {
        installFocusListener();
    }

    /*public JTextFieldEx(final String text) {
        super(text);
        installFocusListener();
    }*/

    public JTextFieldEx(final int columns) {
        super(columns);
        installFocusListener();
    }

    /*public JTextFieldEx(final String text, final int columns) {
        super(text, columns);
        installFocusListener();
    }

    public JTextFieldEx(final Document doc, final String text, final int columns) {
        super(doc, text, columns);
        installFocusListener();
    }*/

    private void installFocusListener() {
        FocusListener focusListener = new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                EventQueue.invokeLater(() -> {
                    if (isEditable() && JTextFieldEx.select) {
                        selectAll();
                    }
                });
            }
        };
        addFocusListener(focusListener);
    }

    public static void setSelectOnFocus(final boolean select) {
        Preferences p = Preferences.userNodeForPackage(JTextFieldEx.class);
        p.putBoolean(SELECT_ON_FOCUS, select);
        JTextFieldEx.select = select;
    }

    public static boolean isSelectOnFocus() {
        return JTextFieldEx.select;
    }
}
