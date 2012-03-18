/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
import java.awt.Window;

import javax.swing.JOptionPane;

import jgnash.util.Resource;

/**
 * Keep various odds and ends here instead of the main class
 * 
 * @author Craig Cavanaugh
 *
 */
public class StaticUIMethods {

    /**
     * Display an error message
     * 
     * @param message error message to display
     */
    public static void displayError(final String message) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                Resource rb = Resource.get();

                Frame frame = UIApplication.getFrame();

                KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                Window window = keyboardFocusManager.getActiveWindow();

                if (window != null && window instanceof Frame) {
                    frame = (Frame) window;
                }

                JOptionPane.showMessageDialog(frame, message, rb.getString("Title.Error"), JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private StaticUIMethods() {
    }
}
