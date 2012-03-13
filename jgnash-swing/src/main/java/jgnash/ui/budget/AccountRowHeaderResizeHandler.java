/*
 * jGnash, account personal finance application
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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.ui.budget;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * Attaches mouse listeners to resize the JScrollPane row header
 *
 * @author Craig Cavanaugh
 * @version $Id: AccountRowHeaderResizeHandler.java 3020 2011-12-28 16:52:55Z
 * ccavanaugh $
 */
final class AccountRowHeaderResizeHandler extends MouseInputAdapter {

    private JScrollPane scrollPane;

    private JViewport rowHeaderViewport;

    private AccountRowHeaderPanel rowHeader;

    private Component corner;

    private volatile boolean resizing;

    private int startX;

    private int startWidth;

    private int minimumWidth, maximumWidth;

    private static final int DRAG_MARGIN = 5;

    private static final Cursor RESIZE_CURSOR = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);

    private Cursor oldCursor;

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(AccountRowHeaderResizeHandler.class);

    private static final String HEADER_WIDTH = "headerWidth";

    private int borderWidth;

    private ModelListener listener = new ModelListener();

    public AccountRowHeaderResizeHandler(final JScrollPane scrollPane) {
        this.scrollPane = scrollPane;
    }

    public void attachListeners() {
        attach();

        addMouseListeners();
        restoreWidth();
    }

    public void detachListeners() {
        if (rowHeader != null) {
            rowHeader.getTable().getModel().removeTableModelListener(listener);

            if (corner != null) {
                corner.removeMouseListener(this);
                corner.removeMouseMotionListener(this);
                corner = null;
            }

            rowHeaderViewport = null;
            rowHeader = null;
        }
    }

    private void addMouseListeners() {
        if (corner != null) {
            corner.addMouseListener(this);
            corner.addMouseMotionListener(this);
        } else {
            throw new IllegalArgumentException("JScrollPane does not have a corner component");
        }
    }

    private void attach() {
        rowHeaderViewport = scrollPane.getRowHeader();

        if (rowHeaderViewport == null) {
            throw new IllegalArgumentException("JScrollPane does not have a row header");
        }
        rowHeader = (AccountRowHeaderPanel) rowHeaderViewport.getView();

        minimumWidth = rowHeader.getMinimumSize().width;
        maximumWidth = rowHeader.getMaximumSize().width;

        corner = scrollPane.getCorner(JScrollPane.UPPER_LEFT_CORNER);

        if (corner instanceof JComponent) {
            Border border = ((JComponent) corner).getBorder();

            Insets insets = border.getBorderInsets(corner);
            borderWidth = insets.left + insets.right;
        } else {
            borderWidth = 0;
        }

        rowHeader.getTable().getModel().addTableModelListener(listener);
    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        if (!resizing) {
            return;
        }

        Dimension size = rowHeaderViewport.getPreferredSize();

        size.width = startWidth + e.getX() - startX;

        if (size.width < minimumWidth) {
            size.width = minimumWidth;
        } else if (size.width > maximumWidth) {
            size.width = maximumWidth;
        }

        setSize(size);
    }

    @Override
    public void mouseEntered(final MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        if (oldCursor != null) {
            corner.setCursor(oldCursor);
            oldCursor = null;
        }
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        if (corner.getWidth() - e.getX() <= DRAG_MARGIN) {
            if (oldCursor == null) {
                oldCursor = corner.getCursor();
                corner.setCursor(RESIZE_CURSOR);
            }
        } else if (oldCursor != null) {
            corner.setCursor(oldCursor);
            oldCursor = null;
        }
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        startX = e.getX();

        startWidth = rowHeaderViewport.getWidth();

        if (startWidth - startX > DRAG_MARGIN) {
            return;
        }

        resizing = true;

        if (oldCursor == null) {
            oldCursor = corner.getCursor();
            corner.setCursor(RESIZE_CURSOR);
        }
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        resizing = false;

        saveWidth();
    }

    private void restoreWidth() {
        if (rowHeaderViewport != null) {
            int width = PREFERENCES.getInt(HEADER_WIDTH, 0);

            if (width > 0) {
                final Dimension size = rowHeaderViewport.getPreferredSize();
                size.width = width;

                setSize(size);
            }
        }
    }

    private void saveWidth() {
        PREFERENCES.putInt(HEADER_WIDTH, rowHeaderViewport.getSize().width);
    }

    private void setSize(final Dimension size) {
        final Dimension d = (Dimension) size.clone();
        d.width += borderWidth;

        rowHeaderViewport.setPreferredSize(d);
        rowHeader.setPreferredSize(size);

        scrollPane.revalidate();
        rowHeader.setSize(size);
    }

    /**
     * Forces the rowHeader to resize when the model is changed, otherwise the
     * underlying table is not painted correctly when the model changes
     */
    private class ModelListener implements TableModelListener {

        @Override
        public void tableChanged(final TableModelEvent e) {

            if (resizing) {
                return;
            }

            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    restoreWidth();
                }
            });
        }
    }
}
