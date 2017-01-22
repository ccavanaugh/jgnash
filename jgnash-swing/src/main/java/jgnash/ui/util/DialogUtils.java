/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import static jgnash.util.EncodeDecode.COMMA_DELIMITER_PATTERN;

/**
 * Static Dialog Utilities
 *
 * @author Craig Cavanaugh
 */
public class DialogUtils {

    private static final char COMMA_DELIMITER = ',';

    /**
     * Listens to a JDialog to save and restore windows bounds automatically.
     * <p>
     * {@code setVisible(false)} and {@code dispose()} must not be used
     * to close the window.  Instead, dispatch a window closing event.
     * <PRE>
     * dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
     * </PRE>
     * and the dialog must be set to dispose on close
     * <PRE>
     * setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
     * </PRE>
     *
     * @param w        {@code Window} to listen to
     * @param prefNode String identifier to preference node to save and restore from
     * @param key      the key to save and restore from
     */
    private static void addBoundsListener(final Window w, final String prefNode, final String key) {
        String bounds = Preferences.userRoot().node(prefNode).get(key, null);

        if (bounds != null) { // restore to previous size and position

            if (w instanceof JDialog) {
                if (((JDialog) w).isResizable()) {
                    w.setBounds(decodeRectangle(bounds));
                } else {
                    w.setLocation(decodeRectangle(bounds).getLocation());
                }
            } else {
                w.setBounds(decodeRectangle(bounds));
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

                p.put(key, encodeRectangle(w.getBounds()));
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

        dialog.getRootPane().registerKeyboardAction(e ->
                        dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING)), stroke,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private static String encodeRectangle(final Rectangle bounds) {
        return String.valueOf(bounds.x) + COMMA_DELIMITER + bounds.y + COMMA_DELIMITER + bounds.width
                + COMMA_DELIMITER + bounds.height;
    }

    private static Rectangle decodeRectangle(final String bounds) {
        if (bounds == null) {
            return null;
        }

        Rectangle rectangle = null;
        String[] array = COMMA_DELIMITER_PATTERN.split(bounds);
        if (array.length == 4) {
            try {
                rectangle = new Rectangle();
                rectangle.x = Integer.parseInt(array[0]);
                rectangle.y = Integer.parseInt(array[1]);
                rectangle.width = Integer.parseInt(array[2]);
                rectangle.height = Integer.parseInt(array[3]);
            } catch (final NumberFormatException nfe) {
                Logger.getLogger(DialogUtils.class.getName()).log(Level.SEVERE, null, nfe);
                rectangle = null;
            }
        }
        return rectangle;
    }

    private DialogUtils() {
    }
}