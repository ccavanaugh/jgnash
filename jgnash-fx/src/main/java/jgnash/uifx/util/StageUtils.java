/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.uifx.util;

import java.awt.geom.Rectangle2D;
import java.util.prefs.Preferences;

import jgnash.uifx.MainApplication;
import jgnash.util.EncodeDecode;

import javafx.stage.Stage;

/**
 * Saves and restores Stage sizes
 *
 * @author Craig Cavanaugh
 */
public class StageUtils {

    public static final String DEFAULT_KEY = "bounds";

    /**
     * Restores and saves the size and location of a stage
     *
     * @param stage The stage to save and restore size and position
     * @param prefNode This should typically be the calling controller
     */
    public static void addBoundsListener(final Stage stage, final Class<?> prefNode) {
        addBoundsListener(stage, prefNode.getName().replace('.', '/'), DEFAULT_KEY);
    }

    private static void addBoundsListener(final Stage stage, final String prefNode, final String key) {
        final String bounds = Preferences.userRoot().node(prefNode).get(key, null);

        if (bounds != null) { // restore to previous size and position
            final Rectangle2D.Double rectangle = EncodeDecode.decodeRectangle2D(bounds);

            stage.setX(rectangle.getX());
            stage.setY(rectangle.getY());

            if (stage.isResizable()) {
                stage.setWidth(rectangle.getWidth());
                stage.setHeight(rectangle.getHeight());
            }
        }

        stage.setOnCloseRequest(windowEvent -> {
            final Preferences p = Preferences.userRoot().node(prefNode);
            p.put(key, EncodeDecode.encodeRectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()));

            //stage.onCloseRequestProperty().removeListener(this);  // make gc easier
        });
    }
}
