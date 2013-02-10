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
package jgnash.ui;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import jgnash.util.Resource;

/**
 * Keep various odds and ends here instead of in the main class
 *
 * @author Craig Cavanaugh
 */
public class StaticUIMethods {

    private StaticUIMethods() {
    }

    /**
     * Display an error message
     *
     * @param message error message to display
     */
    public static void displayError(final String message) {
        displayMessage(message, Resource.get().getString("Title.Error"), JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Display an error message
     *
     * @param message error message to display
     */
    public static void displayWarning(final String message) {
        displayMessage(message, Resource.get().getString("Title.Warning"), JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Display an error message
     *
     * @param message error message to display
     */
    public static void displayMessage(final String message, final String title, final int type) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {               
                Frame frame = UIApplication.getFrame();

                KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                Window window = keyboardFocusManager.getActiveWindow();

                if (window != null && window instanceof Frame) {
                    frame = (Frame) window;
                }

                JOptionPane.showMessageDialog(frame, message, title, type);
            }
        });
    }

    static void fixWindowManager() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();

        if (toolkit.getClass().getName().equals("sun.awt.X11.XToolkit")) {

            // Oracle Bug #6528430 - provide proper app name on Linux
            try {
                Field awtAppClassNameField = toolkit.getClass().getDeclaredField("awtAppClassName");
                awtAppClassNameField.setAccessible(true);
                awtAppClassNameField.set(toolkit, Resource.getAppName());
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                Logger.getLogger(StaticUIMethods.class.getName()).log(Level.INFO, ex.getLocalizedMessage(), ex);
            }

            // Workaround for main menu, pop-up & mouse issues for Gnome 3 shell and Cinnamon          
            if ("gnome-shell".equals(System.getenv("DESKTOP_SESSION"))
                    || "cinnamon".equals(System.getenv("DESKTOP_SESSION"))
                    || "gnome".equals(System.getenv("DESKTOP_SESSION"))
                    || (System.getenv("XDG_CURRENT_DESKTOP") != null && System.getenv("XDG_CURRENT_DESKTOP").contains("GNOME"))) {
                try {
                    Class<?> x11_wm = Class.forName("sun.awt.X11.XWM");

                    Field awt_wMgr = x11_wm.getDeclaredField("awt_wmgr");
                    awt_wMgr.setAccessible(true);

                    Field other_wm = x11_wm.getDeclaredField("OTHER_WM");
                    other_wm.setAccessible(true);

                    if (awt_wMgr.get(null).equals(other_wm.get(null))) {
                        Field metaCity_Wm = x11_wm.getDeclaredField("METACITY_WM");
                        metaCity_Wm.setAccessible(true);
                        awt_wMgr.set(null, metaCity_Wm.get(null));
                        Logger.getLogger(StaticUIMethods.class.getName()).info("Installed window manager workaround");
                    }
                } catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(StaticUIMethods.class.getName()).log(Level.INFO, ex.getLocalizedMessage(), ex);
                }
            }
        }
    }
}
