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

import java.awt.GraphicsEnvironment;

import javax.swing.JComboBox;

/**
 * Provides a UI to the user to select an available font family
 * <p/>
 * 
 * @author Craig Cavanaugh
 * @version $Id: FontComboBox.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public final class FontComboBox extends JComboBox {

    public FontComboBox(final String font) {
        super(getFonts());
        setEditable(false);
        setSelectedFont(font);
    }

    /**
     * Returns the name of the selected font
     * 
     * @return the name of the selected font
     */
    public String getSelectedFont() {
        return (String) getSelectedItem();
    }

    private static String[] getFonts() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    }

    /**
     * Set the selected font
     * 
     * @param font The name of the font to select
     */
    void setSelectedFont(String font) {
        setSelectedItem(font);
    }
}
