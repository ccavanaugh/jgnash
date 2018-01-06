/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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

import java.awt.Cursor;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;

/**
 * Static cursor utilities
 *
 * @author Chad McHenry
 * @author Craig Cavanaugh
 *
 */
public class CursorUtils {

    private CursorUtils() {
    }

    /**
     * Cursor type for a magnifying glass cursor with a '+' in the lens.
     */
    public static final int ZOOM_IN = 0;

    /**
     * Cursor type for a magnifying glass cursor with a '-' in the lens.
     */
    public static final int ZOOM_OUT = 1;

    private static final Cursor predefined[] = new Cursor[2];

    /**
     * Cursor properties. Must be {{String, String, int[]}} indicating the
     * name, resource name, and a 2 value int array for the 'hotspot' of the
     * cursor.  Yeah, horrible, but it's for internal initialization only.
     */
    private static final Object[][] cursorProperties = {
            {"ZoomIn", "/jgnash/resource/ZoomInCursor.gif", new int[]{4, 4}},
            {"ZoomOut", "/jgnash/resource/ZoomOutCursor.gif", new int[]{4, 4}}};

    /**
     * Returns a custom cursor object of the type specified.
     *
     * @param type the type of the custom cursor as defined in this class
     * @return the specified custom cursor
     * @throws IllegalArgumentException if the specified cursor type is
     *                                  invalid
     */
    static public Cursor getCursor(int type) {
        if (type < ZOOM_IN || type > ZOOM_OUT) {
            throw new IllegalArgumentException("illegal cursor type");
        }
        if (predefined[type] == null) {
            try {
                // See comment above the static variable.
                final Object[] props = cursorProperties[type];

                final String name = (String) props[0];
                final String resource = (String) props[1];
                final int[] spot = (int[]) props[2];
                final Point point = new Point(spot[0], spot[1]);
                final Toolkit tk = Toolkit.getDefaultToolkit();

                Image image = IconUtils.getImage(resource);

                predefined[type] = tk.createCustomCursor(image, point, name);
            } catch (IndexOutOfBoundsException | HeadlessException e) {
                // this would be an error in the properties
                predefined[type] = Cursor.getDefaultCursor();
                throw new RuntimeException(e);
            }
        }
        return predefined[type];
    }
}
