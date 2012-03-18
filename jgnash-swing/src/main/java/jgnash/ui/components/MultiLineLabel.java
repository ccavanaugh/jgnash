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
package jgnash.ui.components;

import javax.swing.JTextArea;

/**
 * This class is used to create the effect of a multiline JLabel without
 * the use of HTML in the text
 *
 * @author Craig Cavanaugh
 *
 */
public final class MultiLineLabel extends JTextArea {

    /**
     * @param text text to display
     */
    public MultiLineLabel(String text) {
        super(text);
        setOpaque(false);
        setEditable(false);
    }
}
