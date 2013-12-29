/*
 * @(#)JideScrollPane.java
 *
 * Copyright 2002 - 2005 JIDE Software Inc. All rights reserved.
 */
package com.jidesoft.swing;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import java.awt.*;

/**
 * <code>JideScrollPane</code> is an enhanced version of <code>JScrollPane</code>. In <code>JScrollPane</code>, you can
 * have rowHeader and columnHeader. However you can't have rowFooter and columnFooter. However rowFooter and
 * columnFooter are very useful in table. For example they can be used to display "total" or "summary" type of
 * information.
 * <p/>
 * Several methods related to rowFooter and columnFooter are added such as {@link #setRowFooter(javax.swing.JViewport)},
 * and {@link #setColumnFooter(javax.swing.JViewport)} which will set the viewport to rowFooter and columnFooter area
 * respectively. The usage of those methods are exactly the same as {@link JScrollPane#setRowHeader(javax.swing.JViewport)}.
 * <p/>
 * To fully leverage the power of JideScrollPane, we also create a class called <code>TableScrollPane</code> which is
 * part of JIDE Grids package. It will allow you to easily create table with row header, row footer and column footer.
 * <p><code>JideScrollPane</code> also provides support for scrollbar corners. You can set them using
 * setScrollBarCorner(String, java.awt.Component). Available key for scroll bar corner is defined at {@link
 * JideScrollPaneConstants}  which can be access from <code>JideScrollPane</code>.
 * <p/>
 * <b>Credit:</b> This implementation of scroll bar corner is based on work from Santhosh Kumar -
 * santhosh@in.fiorano.com.
 */
public final class JideScrollPane extends JScrollPane implements JideScrollPaneConstants {

    /**
     * The row footer child.  Default is <code>null</code>.
     *
     * @see #setRowFooter(javax.swing.JViewport)
     */
    private JViewport _rowFooter;

    /**
     * The component under upper left corner.  Default is <code>null</code>.
     *
     * @see #setCorner(String, java.awt.Component)
     */
    private Component _subUpperLeft;
    /**
     * The component under upper right corner.  Default is <code>null</code>.
     *
     * @see #setCorner(String, java.awt.Component)
     */
    private Component _subUpperRight;
    /**
     * The column footer child.  Default is <code>null</code>.
     *
     * @see #setColumnFooter(javax.swing.JViewport)
     */
    private JViewport _columnFooter;

    private boolean _columnHeadersHeightUnified;

    private static final String PROPERTY_COLUMN_HEADERS_HEIGHT_UNIFIED = "columnHeadersHeightUnified";

    public static final String CLIENT_PROPERTY_SLAVE_VIEWPORT = "synchronizeViewSlaveViewport";
    public static final String CLIENT_PROPERTY_MASTER_VIEWPORT = "synchronizeViewMasterViewport";

    /**
     * Creates a <code>JideScrollPane</code> that displays the view component in a viewport whose view position can be
     * controlled with a pair of scrollbars. The scrollbar policies specify when the scrollbars are displayed, For
     * example, if <code>vsbPolicy</code> is <code>VERTICAL_SCROLLBAR_AS_NEEDED</code> then the vertical scrollbar only
     * appears if the view doesn't fit vertically. The available policy settings are listed at {@link
     * #setVerticalScrollBarPolicy(int)} and {@link #setHorizontalScrollBarPolicy(int)}.
     *
     * @param view      the component to display in the scrollpanes viewport
     * @param vsbPolicy an integer that specifies the vertical scrollbar policy
     * @param hsbPolicy an integer that specifies the horizontal scrollbar policy
     * @see #setViewportView(java.awt.Component)
     */
    @SuppressWarnings("MagicConstant")
    private JideScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
        setLayout(new JideScrollPaneLayout.UIResource());
        setVerticalScrollBarPolicy(vsbPolicy);
        setHorizontalScrollBarPolicy(hsbPolicy);
        setViewport(createViewport());
        setVerticalScrollBar(createVerticalScrollBar());
        setHorizontalScrollBar(createHorizontalScrollBar());
        if (null != view) {
            setViewportView(view);
        }
        setOpaque(true);
        updateUI();

        if (!getComponentOrientation().isLeftToRight()) {
            viewport.setViewPosition(new Point(Integer.MAX_VALUE, 0));
        }
    }

    /**
     * Creates an empty (no viewport view) <code>JideScrollPane</code> where both horizontal and vertical scrollbars
     * appear when needed.
     */
    public JideScrollPane() {
        this(null, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    @Override
    public void setViewport(JViewport viewport) {
        JViewport old = getViewport();
        super.setViewport(viewport);
        if (old != null) {
            if (rowHeader != null) {
                JideSwingUtilities.unsynchronizeView(rowHeader, old);
            }
            if (_rowFooter != null) {
                JideSwingUtilities.unsynchronizeView(_rowFooter, old);
                JideSwingUtilities.unsynchronizeView(old, _rowFooter);
            }
            if (_columnFooter != null) {
                JideSwingUtilities.unsynchronizeView(_columnFooter, old);
                JideSwingUtilities.unsynchronizeView(old, _columnFooter);
            }
            if (columnHeader != null) {
                JideSwingUtilities.unsynchronizeView(columnHeader, old);
            }
        }
        if (viewport != null) {
            if (rowHeader != null) {
                JideSwingUtilities.synchronizeView(rowHeader, getViewport(), SwingConstants.VERTICAL);
            }
            if (_rowFooter != null) {
                JideSwingUtilities.synchronizeView(_rowFooter, getViewport(), SwingConstants.VERTICAL);
                JideSwingUtilities.synchronizeView(getViewport(), _rowFooter, SwingConstants.VERTICAL);
            }
            if (_columnFooter != null) {
                JideSwingUtilities.synchronizeView(_columnFooter, getViewport(), SwingConstants.HORIZONTAL);
                JideSwingUtilities.synchronizeView(getViewport(), _columnFooter, SwingConstants.HORIZONTAL);
            }
            if (columnHeader != null) {
                JideSwingUtilities.synchronizeView(columnHeader, getViewport(), SwingConstants.HORIZONTAL);
            }
        }
    }

    /**
     * Returns the row footer.
     *
     * @return the <code>rowFooter</code> property
     *
     * @see #setRowFooter(javax.swing.JViewport)
     */
    public JViewport getRowFooter() {
        return _rowFooter;
    }


    /**
     * Removes the old rowFooter, if it exists.  If the new rowFooter isn't <code>null</code>, syncs the y coordinate of
     * its viewPosition with the viewport (if there is one) and then adds it to the scrollpane.
     *
     * @param rowFooter the new row footer to be used; if <code>null</code> the old row footer is still removed and the
     *                  new rowFooter is set to <code>null</code>
     * @see #getRowFooter
     * @see #setRowFooterView(java.awt.Component)
     */
    void setRowFooter(JViewport rowFooter) {
        JViewport old = getRowFooter();
        _rowFooter = rowFooter;
        if (null != rowFooter) {
            add(rowFooter, ROW_FOOTER);
        }
        else if (null != old) {
            remove(old);
        }
        firePropertyChange("rowFooter", old, rowFooter);
        revalidate();
        repaint();
        if (old != null) {
            JideSwingUtilities.unsynchronizeView(old, getViewport());
            JideSwingUtilities.unsynchronizeView(getViewport(), old);
        }
        if (rowFooter != null) {
            JideSwingUtilities.synchronizeView(rowFooter, getViewport(), SwingConstants.VERTICAL);
            JideSwingUtilities.synchronizeView(getViewport(), rowFooter, SwingConstants.VERTICAL);
        }
    }

    /**
     * Override setRowHeader method in JScrollPane and synchronize the view with the main viewport. Swing tried to
     * implement this feature but it will break if the view position changes starts from rowHeader.
     *
     * @param rowHeader the new row header
     */
    @Override
    public void setRowHeader(JViewport rowHeader) {
        JViewport old = getRowHeader();
        super.setRowHeader(rowHeader);
        if (old != null) {
            JideSwingUtilities.unsynchronizeView(old, getViewport());
            JideSwingUtilities.unsynchronizeView(getViewport(), old);
        }
        if (getRowHeader() != null) {
            JideSwingUtilities.synchronizeView(getRowHeader(), getViewport(), SwingConstants.VERTICAL);
            JideSwingUtilities.synchronizeView(getViewport(), getRowHeader(), SwingConstants.VERTICAL);
        }
    }

    /**
     * Creates a row-footer viewport if necessary, sets its view and then adds the row-footer viewport to the
     * scrollpane.  For example:
     * <pre>
     * JScrollPane scrollpane = new JideScrollPane();
     * scrollpane.setViewportView(myBigComponentToScroll);
     * scrollpane.setRowFooterView(myBigComponentsRowFooter);
     * </pre>
     *
     * @param view the component to display as the row footer
     * @see #setRowFooter(javax.swing.JViewport)
     * @see JViewport#setView(java.awt.Component)
     */
    public void setRowFooterView(Component view) {
        if (null == getRowFooter()) {
            setRowFooter(createViewport());
        }
        getRowFooter().setView(view);
    }


    /**
     * Returns the column footer.
     *
     * @return the <code>columnFooter</code> property
     *
     * @see #setColumnFooter(javax.swing.JViewport)
     */
    public JViewport getColumnFooter() {
        return _columnFooter;
    }


    /**
     * Removes the old columnFooter, if it exists.  If the new columnFooter isn't <code>null</code>, sync the x
     * coordinate of the its viewPosition with the viewport (if there is one) and then add it to the scrollpane.
     *
     * @param columnFooter the new column footer to be used; if <code>null</code> the old column footer is still removed
     *                     and the new columnFooter is set to <code>null</code>
     * @see #getColumnFooter
     * @see #setColumnFooterView(java.awt.Component)
     */
    void setColumnFooter(JViewport columnFooter) {
        JViewport old = getColumnFooter();
        _columnFooter = columnFooter;
        if (null != columnFooter) {
            add(columnFooter, COLUMN_FOOTER);
        }
        else if (null != old) {
            remove(old);
        }
        firePropertyChange("columnFooter", old, columnFooter);

        revalidate();
        repaint();

        if (old != null) {
            JideSwingUtilities.unsynchronizeView(old, getViewport());
            JideSwingUtilities.unsynchronizeView(getViewport(), old);
        }
        if (_columnFooter != null) {
            JideSwingUtilities.synchronizeView(_columnFooter, getViewport(), SwingConstants.HORIZONTAL);
            JideSwingUtilities.synchronizeView(getViewport(), _columnFooter, SwingConstants.HORIZONTAL);
        }
    }

    /**
     * Overrides to make column header viewport synchronizing with the main viewport.
     *
     * @param columnHeader the column header
     */
    @Override
    public void setColumnHeader(JViewport columnHeader) {
        JViewport old = getColumnHeader();
        super.setColumnHeader(columnHeader);
        if (old != null) {
            JideSwingUtilities.unsynchronizeView(old, getViewport());
        }
        if (getColumnHeader() != null) {
            JideSwingUtilities.synchronizeView(getColumnHeader(), getViewport(), SwingConstants.HORIZONTAL);
        }
    }

    /**
     * Creates a column-footer viewport if necessary, sets its view, and then adds the column-footer viewport to the
     * scrollpane.  For example:
     * <pre>
     * JScrollPane scrollpane = new JideScrollPane();
     * scrollpane.setViewportView(myBigComponentToScroll);
     * scrollpane.setColumnFooterView(myBigComponentsColumnFooter);
     * </pre>
     *
     * @param view the component to display as the column footer
     * @see #setColumnFooter(javax.swing.JViewport)
     * @see JViewport#setView(java.awt.Component)
     */
    public void setColumnFooterView(Component view) {
        if (null == getColumnFooter()) {
            setColumnFooter(createViewport());
        }
        getColumnFooter().setView(view);
    }

    @Override
    public Component getCorner(String key) {
        if (key == null) {
            return null;
        }
        if (key.equals(SUB_UPPER_LEFT)) {
            return _subUpperLeft;
        }
        else if (key.equals(SUB_UPPER_RIGHT)) {
            return _subUpperRight;
        }
        return super.getCorner(key);
    }

    @Override
    public void setCorner(String key, Component corner) {
        if (key == null) {
            return;
        }
        if (key.equals(SUB_UPPER_LEFT) || key.equals(SUB_UPPER_RIGHT)) {
            Component old;
            if (key.equals(SUB_UPPER_LEFT)) {
                old = _subUpperLeft;
                _subUpperLeft = corner;
            }
            else {
                old = _subUpperRight;
                _subUpperRight = corner;
            }
            if (old != null) {
                remove(old);
            }
            if (corner != null) {
                add(corner, key);
            }
            firePropertyChange(key, old, corner);
            revalidate();
            repaint();
            return;
        }
        super.setCorner(key, corner);
    }

    /**
     * Returns the component at the specified scroll bar corner. The <code>key</code> value specifying the corner is one
     * of: <ul> <li>{@link JideScrollPane#HORIZONTAL_LEFT} <li>{@link JideScrollPane#HORIZONTAL_RIGHT} <li>{@link
     * JideScrollPane#VERTICAL_TOP} <li>{@link JideScrollPane#VERTICAL_BOTTOM} <li>{@link
     * JideScrollPane#HORIZONTAL_LEADING} <li>{@link JideScrollPane#HORIZONTAL_TRAILING} </ul>
     *
     * @param key one of the values as shown above
     * @return one of the components listed below or <code>null</code> if <code>key</code> is invalid: <ul>
     *         <li>lowerLeft <li>lowerRight <li>upperLeft <li>upperRight </ul>
     *
     * @see #setCorner(String, java.awt.Component)
     */
    public Component getScrollBarCorner(String key) {
        boolean isLeftToRight = getComponentOrientation().isLeftToRight();
        if (key.equals(HORIZONTAL_LEADING)) {
            key = isLeftToRight ? HORIZONTAL_LEFT : HORIZONTAL_RIGHT;
        }
        else if (key.equals(HORIZONTAL_TRAILING)) {
            key = isLeftToRight ? HORIZONTAL_RIGHT : HORIZONTAL_LEFT;
        }

        switch (key) {
            default:
                return null;
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setLayout(new JideScrollPaneLayout.UIResource());
        if (getBorder() instanceof UIResource) {
            LookAndFeel.installBorder(this, "JideScrollPane.border");
        }
    }

    @Override
    public void setLayout(LayoutManager layout) {
        if (!(layout instanceof JideScrollPaneLayout)) {
            super.setLayout(new JideScrollPaneLayout.UIResource());
        }
        else {
            super.setLayout(layout);
        }
    }

    public boolean isVerticalScrollBarCoversWholeHeight() {
        return false;
    }

    public boolean isHorizontalScrollBarCoversWholeWidth() {
        return false;
    }

    /**
     * If true, the top-right, top-left corners the column header will have the same height. If false, three of them
     * will keep their own preferred height.
     *
     * @return true or false.
     */
    public boolean isColumnHeadersHeightUnified() {
        return _columnHeadersHeightUnified;
    }

    /**
     * Sets the flag if the top-right, top-left corner and the column header will have the same height or different
     * heights.
     *
     * @param columnHeadersHeightUnified true or false.
     */
    public void setColumnHeadersHeightUnified(boolean columnHeadersHeightUnified) {
        boolean old = _columnHeadersHeightUnified;
        if (old != columnHeadersHeightUnified) {
            _columnHeadersHeightUnified = columnHeadersHeightUnified;
            firePropertyChange(PROPERTY_COLUMN_HEADERS_HEIGHT_UNIFIED, old, false);
            invalidate();
            doLayout();
        }
    }

    /**
     * If true, the bottom-right, bottom-left corners the column footer will have the same height. If false, three of
     * them will keep their own preferred height.
     *
     * @return true or false.
     */
    public boolean isColumnFootersHeightUnified() {
        return false;
    }

    /**
     * Get the flag indicating if JideScrollPane should keep the corner visible when it has corner components defined
     * even when the scroll bar is not visible.
     * <p/>
     * This flag will take effect only when the scroll bar policy is <code>HORIZONTAL_SCROLLBAR_AS_NEEDED</code> or
     * <code>VERTICAL_SCROLLBAR_AS_NEEDED</code>
     * <p/>
     * The default value of this flag is false.
     *
     * @return the flag.
     */
    public boolean isKeepCornerVisible() {
        return false;
    }
}
