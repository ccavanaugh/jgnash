/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
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
package jgnash.uifx.resource.cursor;

import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.image.Image;

/**
 * Custom cursor factory.
 *
 * @author Craig Cavanaugh
 */
public final class CustomCursor {

    private static final String ZOOM_IN_CURSOR = "/jgnash/resource/ZoomInCursor.gif";

    private static final String ZOOM_OUT_CURSOR = "/jgnash/resource/ZoomOutCursor.gif";

    private static Cursor zoomInCursor;

    private static Cursor zoomOutCursor;

    private CustomCursor() {
        // Utility class
    }

    public static synchronized Cursor getZoomInCursor() {
        if (zoomInCursor == null) {
            zoomInCursor = new ImageCursor(new Image(ZOOM_IN_CURSOR), 4, 4);
        }
        return zoomInCursor;
    }

    public static synchronized Cursor getZoomOutCursor() {
        if (zoomOutCursor == null) {
            zoomOutCursor = new ImageCursor(new Image(ZOOM_OUT_CURSOR), 4, 4);
        }
        return zoomOutCursor;
    }
}
