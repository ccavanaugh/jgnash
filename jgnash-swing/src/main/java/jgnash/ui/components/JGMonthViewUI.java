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

package jgnash.ui.components;

import org.jdesktop.swingx.plaf.basic.BasicMonthViewUI;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Extension to the JXMonthView component, so that date from the calendar
 * can be selected by double click.
 *
 * @author Peter Vida
 *
 */
public class JGMonthViewUI extends BasicMonthViewUI {
    private MouseListener mouseListener;

    public JGMonthViewUI() {
        super();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static ComponentUI createUI(final JComponent c) {
        return new JGMonthViewUI();
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        mouseListener = new MouseListener() {
            @Override
            public void mouseEntered(final MouseEvent e) {

            }
            @Override
            public void mouseExited(final MouseEvent e) {

            }
            @Override
            public void mousePressed(final MouseEvent e) {

            }
            @Override
            public void mouseReleased(final MouseEvent e) {

            }
            @Override
            public void mouseClicked(final MouseEvent e) {
                if(e.getClickCount()==2 && !monthView.isSelectionEmpty() && getDayAtLocation(e.getX(), e.getY()) != null) {
                    if(monthView instanceof JGJXMonthView) {
                        ((JGJXMonthView)monthView).dateAccepted();
                    }
                }
            }
        };
        monthView.addMouseListener(mouseListener);
    }

    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        monthView.removeMouseListener(mouseListener);
        mouseListener = null;
    }
}
