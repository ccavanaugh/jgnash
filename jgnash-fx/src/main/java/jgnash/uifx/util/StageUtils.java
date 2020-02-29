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
package jgnash.uifx.util;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.Nullable;

import org.apache.commons.math3.util.Precision;

/**
 * Saves and restores Stage sizes.
 *
 * @author Craig Cavanaugh
 */
public class StageUtils {

    private static final String DEFAULT_KEY = "bounds";

    private static final char COMMA_DELIMITER = ',';

    private static final int X = 0;

    private static final int Y = 1;

    private static final int WIDTH = 2;

    private static final int HEIGHT = 3;

    private static final int UPDATE_PERIOD = 2; // update period in seconds

    /**
     * Restores and saves the size and location of a stage.
     *
     * @param stage    The stage to save and restore size and position
     * @param prefNode This should typically be the calling controller
     */
    public static void addBoundsListener(final Stage stage, final Class<?> prefNode) {
        addBoundsListener(stage, prefNode.getName().replace('.', '/'), null);
    }

    /**
     * Restores and saves the size and location of a stage.
     *
     * @param stage    The stage to save and restore size and position
     * @param prefNode This should typically be the calling controller
     */
    public static void addBoundsListener(final Stage stage, final Class<?> prefNode, @Nullable final Stage parent) {
        addBoundsListener(stage, prefNode.getName().replace('.', '/'), parent);
    }

    public static void addBoundsListener(final Stage stage, final String prefNode, @Nullable final Stage parent) {
        final String bounds = Preferences.userRoot().node(prefNode).get(DEFAULT_KEY, null);

        if (bounds != null) { // restore to previous size and position
            Rectangle2D rectangle = decodeRectangle(bounds);

            // relative window placement requested.  Modify the coordinates to the current parent placement
            if (parent != null) {
                rectangle = new Rectangle2D(rectangle.getMinX() + parent.getX(),
                        rectangle.getMinY() + parent.getY(), rectangle.getWidth(), rectangle.getHeight());
            }

            // Do not try to restore bounds if they exceed available screen space.. user dropped a monitor
            if (getMaxVisualBounds().contains(rectangle)) {
                final boolean resizable = stage.isResizable();

                // Stage will not reposition if resizable is false... JavaFx bug?
                stage.setResizable(false);

                stage.setX(rectangle.getMinX());
                stage.setY(rectangle.getMinY());

                if (resizable) { // don't resize if originally false
                    if (!Precision.equals(stage.getMinWidth(), stage.getMaxWidth())) {   // width may be locked
                        final double width = rectangle.getWidth();

                        if (stage.isShowing()) {
                            JavaFXUtils.runNow(() -> stage.setWidth(width));
                        } else {
                            stage.setWidth(width);
                        }

                    }

                    if (!Precision.equals(stage.getMinHeight(), stage.getMaxHeight())) { // height may be locked
                        final double height = rectangle.getHeight();

                        if (stage.isShowing()) {
                            JavaFXUtils.runNow(() -> stage.setHeight(height));
                        } else {
                            stage.setHeight(height);
                        }
                    }
                }
                stage.setResizable(resizable); // restore the resize property
            }
        }

        final ChangeListener<Number> boundsListener = new BoundsListener(stage, prefNode, parent);

        stage.widthProperty().addListener(boundsListener);
        stage.heightProperty().addListener(boundsListener);
        stage.xProperty().addListener(boundsListener);
        stage.yProperty().addListener(boundsListener);
    }

    /**
     * Save Window bounds.  Limits rate of saves to the preferences system
     */
    private static class BoundsListener implements ChangeListener<Number> {
        private final ScheduledThreadPoolExecutor executor;
        private final Preferences p;
        private final Window window;
        private final Stage parent;

        BoundsListener(final Window window, final String prefNode, @Nullable final Stage parent) {
            executor = new ScheduledThreadPoolExecutor(1,
                    new DefaultDaemonThreadFactory("Stage Bounds Listener Executor"),
                    new ThreadPoolExecutor.DiscardPolicy());

            p = Preferences.userRoot().node(prefNode);
            this.window = window;
            this.parent = parent;
        }

        @Override
        public void changed(final ObservableValue<? extends Number> observable, final Number old, final Number newNum) {
            executor.schedule(() -> {
                if (executor.getQueue().size() < 1) {   // ignore if we already have one waiting in the queue
                    // window size and location requests must be pushed to the EDT to prevent a race condition
                    JavaFXUtils.runLater(() -> {
                        if (parent != null) {
                            p.put(DEFAULT_KEY, encodeRectangle(window.getX() - parent.getX(),
                                    window.getY() - parent.getY(), window.getWidth(), window.getHeight()));
                        } else {
                            p.put(DEFAULT_KEY, encodeRectangle(window.getX(), window.getY(), window.getWidth(),
                                    window.getHeight()));
                        }
                    });
                }
            }, UPDATE_PERIOD, TimeUnit.SECONDS);
        }
    }

    private static String encodeRectangle(final double x, final double y, final double width, final double height) {
        return Double.toString(x) + COMMA_DELIMITER + y + COMMA_DELIMITER + width + COMMA_DELIMITER + height;
    }

    private static Rectangle2D decodeRectangle(final String bounds) {
        if (bounds == null) {
            return null;
        }

        Rectangle2D rectangle = null;

        final String[] array = bounds.split(String.valueOf(COMMA_DELIMITER));

        if (array.length == 4) {
            try {
                rectangle = new Rectangle2D(Double.parseDouble(array[X]), Double.parseDouble(array[Y]),
                        Double.parseDouble(array[WIDTH]), Double.parseDouble(array[HEIGHT]));
            } catch (final NumberFormatException nfe) {
                Logger.getLogger(StageUtils.class.getName()).log(Level.SEVERE, null, nfe);
            }
        }

        return rectangle;
    }

    /**
     * Returns the maximum visual bounds of the users desktop.
     *
     * @return maximum usable desktop bounds
     */
    private static Rectangle2D getMaxVisualBounds() {
        double maxX = 0;
        double maxY = 0;
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;

        for (final Screen screen : Screen.getScreens()) {
            minX = Math.min(minX, screen.getVisualBounds().getMinX());
            minY = Math.min(minY, screen.getVisualBounds().getMinY());

            maxX = Math.max(maxX, screen.getVisualBounds().getMaxX());
            maxY = Math.max(maxY, screen.getVisualBounds().getMaxY());
        }

        return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }
}
