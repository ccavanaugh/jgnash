/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
package jgnash.ui.util;

import jgnash.util.EncodeDecode;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

/**
 * Static Dialog Utilities
 *
 * @author Craig Cavanaugh
 *
 */
public class DialogUtils {

    /**
     * Listens to a JDialog to save and restore windows bounds automatically.
     * <p/>
     * <code>setVisible(false)</code> and <code>dispose()</code> must not be used
     * to close the window.  Instead, dispatch a window closing event.
     * <p/>
     * <PRE>
     * dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
     * </PRE>
     * and the dialog must be set to dispose on close
     * <PRE>
     * setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
     * </PRE>
     *
     * @param w        <code>Window</code> to listen to
     * @param prefNode String identifier to preference node to save and restore from
     * @param key      the key to save and restore from
     */
    private static void addBoundsListener(final Window w, final String prefNode, final String key) {
        String bounds = Preferences.userRoot().node(prefNode).get(key, null);

        if (bounds != null) { // restore to previous size and position

            if (w instanceof JDialog) {
                if (((JDialog) w).isResizable()) {
                    w.setBounds(EncodeDecode.decodeRectangle(bounds));
                } else {
                    w.setLocation(EncodeDecode.decodeRectangle(bounds).getLocation());
                }
            } else {
                w.setBounds(EncodeDecode.decodeRectangle(bounds));
            }

            Window owner = w.getOwner();

            if (owner != null) {
                w.setLocationRelativeTo(owner);
            }

        }

        /* listen for a window closing event and deal with it */
        w.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent evt) {
                // save position and size
                Preferences p = Preferences.userRoot().node(prefNode);

                p.put(key, EncodeDecode.encodeRectangle(w.getBounds()));
                w.removeWindowListener(this); // make GC easy
            }
        });

        if (w instanceof JDialog) {
            addEscapeListener((JDialog) w);
        }
    }

    public static void addBoundsListener(final Window w, final String key) {
        addBoundsListener(w, w.getClass().getName().replace('.', '/'), key);
    }

    public static void addBoundsListener(final Window w) {
        addBoundsListener(w, "bounds");
    }   

    private static void addEscapeListener(final JDialog dialog) {

        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

        dialog.getRootPane().registerKeyboardAction(new ActionListener() {

            /**
             * Invoked when an action occurs.
             */
            @Override
            public void actionPerformed(final ActionEvent e) {
                dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
            }
        }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private DialogUtils() {
    }
}