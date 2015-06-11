/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import jgnash.resource.font.FontAwesomeImageView;

import org.controlsfx.control.decoration.Decoration;
import org.controlsfx.control.decoration.Decorator;
import org.controlsfx.control.decoration.GraphicDecoration;

/**
 * Validation factory methods
 *
 * @author Craig Cavanaugh
 */
public class ValidationFactory {

    private static final double ALERT_SIZE = 14.0;  // icon size

    private ValidationFactory() {
        // utility  class
    }

    private static Decoration createErrorDecoration() {
        final FontAwesomeImageView glyphIcon = new FontAwesomeImageView(FontAwesomeIcon.EXCLAMATION_TRIANGLE,
                ALERT_SIZE, Color.DARKRED);

        return new GraphicDecoration(glyphIcon, Pos.BOTTOM_LEFT, 0, 0);
    }

    private static Decoration createWarningDecoration() {
        final FontAwesomeImageView glyphIcon = new FontAwesomeImageView(FontAwesomeIcon.INFO_CIRCLE,
                ALERT_SIZE, Color.DARKGOLDENROD);

        return new GraphicDecoration(glyphIcon, Pos.BOTTOM_LEFT, 0, 0);
    }

    /**
     * Show a validation error on a control
     *
     * @param control {@code Control} to attach to
     * @param error tooltip to display
     */
    public static void showValidationError(final Control control, final String error) {
        showValidation(control, createErrorDecoration(), error, false);
    }

    /**
     * Show a validation error on a control
     *
     * @param control {@code Control} to attach to
     * @param error tooltip to display
     * @param sticky {@code true} if not removed when focus changes
     */
    private static void showValidation(final Control control, final Decoration decoration, final String error,
                                       final boolean sticky) {
        Decorator.addDecoration(control, decoration);

        final Tooltip oldToolTip = control.getTooltip();
        control.setTooltip(new Tooltip(error));

        if (!sticky) {
            final ChangeListener<Boolean> focusListener = new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    control.setTooltip(oldToolTip);
                    Decorator.removeDecoration(control, decoration);
                    control.focusedProperty().removeListener(this);
                }
            };

            control.focusedProperty().addListener(focusListener);
        }
    }

    /**
     * Show a validation error on a control
     *
     * @param control {@code Control} to attach to
     * @param error tooltip to display
     * @param sticky {@code true} if not removed when focus changes
     */
    @SuppressWarnings("SameParameterValue")
    public static void showValidationWarning(final Control control, final String error,  final boolean sticky) {
        final Decoration decoration = createWarningDecoration();

        showValidation(control, decoration, error, sticky);
    }

    public static void removeStickyValidation(final Control control) {
        Decorator.removeAllDecorations(control);
    }

    /**
     * Show a validation error on the first control that is not a label
     *
     * @param pane pane
     * @param error tooltip error
     */
    public static void showValidationError(final Pane pane, final String error) {

        for (final Node node : pane.getChildrenUnmodifiable()) {
            final Decoration decoration = createErrorDecoration();

            if (node instanceof Control && !(node instanceof Label)) {

                Decorator.addDecoration(node, createErrorDecoration());

                final Tooltip oldToolTip = ((Control)node).getTooltip();
                ((Control)node).setTooltip(new Tooltip(error));

                final ChangeListener<Boolean> focusListener = new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        ((Control)node).setTooltip(oldToolTip);
                        Decorator.removeDecoration(pane, decoration);
                        node.focusedProperty().removeListener(this);
                    }
                };

                node.focusedProperty().addListener(focusListener);

                break;
            }
        }
    }
}
