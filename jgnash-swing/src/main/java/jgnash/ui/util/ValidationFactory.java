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
package jgnash.ui.util;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLayer;

import jgnash.ui.AbstractLayerUI;

/**
 * Validation factory methods
 * 
 * @author Craig Cavanaugh
 *
 */
public class ValidationFactory {

    private static final Icon errorIcon;

    private static final String VALIDATION_PROPERTY = "validationProperty";

    static {
        errorIcon = IconUtils.getIcon("/jgnash/resource/validation-error.png");
    }

    private ValidationFactory() {

    }

    public static JComponent wrap(final JComponent component) {
        JLayer<JComponent> layer = new JLayer<>(component);
        layer.setUI(new ValidationUI());
        return layer;
    }

    public static void showValidationError(final String error, final JComponent component) {
        Container parent = component.getParent();

        if (parent instanceof JLayer) {
            component.putClientProperty(ValidationFactory.VALIDATION_PROPERTY, error);
            component.repaint();
        }
    }

    private static class ValidationUI extends AbstractLayerUI<JComponent> {
               
        @Override
        protected void paintLayer(final Graphics2D graphics, final JLayer<? extends JComponent> layer) {
            super.paintLayer(graphics, layer);

            if (layer.getView().getClientProperty(VALIDATION_PROPERTY) != null) {

                final JComponent view = layer.getView();

                String toolTip = (String) view.getClientProperty(VALIDATION_PROPERTY);
                view.setToolTipText(toolTip);
                errorIcon.paintIcon(view, graphics, 0, 0);
            }
        }

        @Override
        protected void processMouseEvent(final MouseEvent e, final JLayer<? extends JComponent> layer) {
            if (e.getID() == MouseEvent.MOUSE_CLICKED) {
                layer.getView().putClientProperty(VALIDATION_PROPERTY, null);
            }
        }

        @Override
        protected void processKeyEvent(final KeyEvent e, final JLayer<? extends JComponent> layer) {
            layer.getView().putClientProperty(VALIDATION_PROPERTY, null);
        }
    }
}
