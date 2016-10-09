/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
package jgnash.ui.components.expandingtable;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import jgnash.ui.ThemeManager;
import jgnash.ui.util.IconUtils;

/**
 * A JTable that renders a tree structure in the first column using {@code prepareRenderer}.
 *
 * @author Craig Cavanaugh
 */
public class ExpandingTable<E extends Comparable<? super E>> extends JTable {

    private final int iconIndent;

    private final Icon openIcon = UIManager.getIcon("Tree.openIcon");

    private final Icon closedIcon = UIManager.getIcon("Tree.closedIcon");

    private final Icon leafIcon = UIManager.getIcon("Tree.leafIcon");

    private final Icon collapsedIcon = UIManager.getIcon("Tree.collapsedIcon");

    private final Icon expandedIcon = UIManager.getIcon("Tree.expandedIcon");

    private int iconSpacing = 0;

    private final int selectionRange = getSelectionRange();

    private final AtomicBoolean defaultSaved = new AtomicBoolean();

    protected Color defaultForeground;

    protected int defaultAlignment;

    private final AbstractExpandingTableModel<E> model;

    private static final int VERTICAL_ICON_PADDING = 5;

    private final Map<String, Icon> iconCache = new HashMap<>();

    public ExpandingTable(final AbstractExpandingTableModel<E> model) {
        super(model);

        this.model = model;

        iconIndent = expandedIcon.getIconWidth();

        if (ThemeManager.isLookAndFeelJGoodies() || ThemeManager.isLookAndFeelMotif()) {
            iconSpacing = 5;
        }

        setShowGrid(false);

        setRowHeight();

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent e) {

                Point point = e.getPoint();

                int range = selectionRange;

                E object = getSelectedObject(point);

                if (object != null) {
                    range = range + iconIndent * ExpandingTable.this.model.getVisibleDepth(object);
                }

                if (e.getX() < range + 5) {
                    processMouseClicked(getSelectedObject(point));
                }
            }
        });

        addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                    processMouseClicked(getSelectedObject());
                }
            }

            @Override
            public void keyTyped(final KeyEvent e) {
                if (Character.isLetterOrDigit(e.getKeyChar())) {
                    EventQueue.invokeLater(() -> handleKeyboardSelect(e.getKeyChar()));
                }
            }
        });
    }

    public E getSelectedObject(final Point p) {
        E selected = null;

        int row = rowAtPoint(p);

        if (row >= 0) {
            selected = model.get(row);
        }

        return selected;
    }

    public E getSelectedObject() {
        E selected = null;

        int row = getSelectedRow();

        if (row >= 0) {
            selected = model.get(row);
        }

        return selected;
    }

    public void setSelectedObject(final E object) {
        int index = model.indexOf(object);

        if (index >= 0) {
            getSelectionModel().setSelectionInterval(index, index);
        }
    }

    private void processMouseClicked(final E object) {
        if (object != null) {

            EventQueue.invokeLater(() -> {
                model.toggleExpansion(object);

                EventQueue.invokeLater(() -> setSelectedObject(object));
            });
        }
    }

    /**
     * Searches for the next available matching character and selects the accounts
     *
     * @param c char to search for
     */
    private void handleKeyboardSelect(final char c) {

        int startRow = getSelectedRow();

        int lastRow = getRowCount() - 1;

        int currentRow = startRow + 1;

        while (currentRow != startRow) {

            // wrap to start if needed
            if (currentRow > lastRow) {
                currentRow = 0;
            }

            E object = model.get(currentRow);

            String searchString = model.getSearchString(object);

            if (!searchString.isEmpty() && Character.toUpperCase(searchString.charAt(0)) == Character.toUpperCase(c)) {
                setSelectedObject(object);
                break;
            }

            currentRow++;
        }
    }

    /**
     * Override prepareRenderer instead of using a custom renderer so the look and feel is preserved
     *
     * @see javax.swing.JTable#prepareRenderer(javax.swing.table.TableCellRenderer, int, int)
     */
    @SuppressWarnings("MagicConstant")
    @Override
    public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {

        Component c = super.prepareRenderer(renderer, row, column);

        if (c instanceof JLabel) {

            if (!defaultSaved.get()) { // save the defaults so they can be restored

                defaultForeground = c.getForeground();
                defaultAlignment = ((JLabel) c).getHorizontalAlignment();

                defaultSaved.set(true);
            }

            E object = model.get(row);

            if (column == 0) {
                c.setForeground(defaultForeground);

                ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);

                if (object != null) {
                    
                    Icon cachedIcon;

                    int indent = iconIndent * (model.getVisibleDepth(object) - 1);

                    if (model.isParent(object)) {
                        if (model.isExpanded(object)) {

                            String key = "eo" + indent;

                            cachedIcon = iconCache.computeIfAbsent(key, k -> IconUtils
                                    .createImageIcon(new IndentedIcon(new Icon[]{expandedIcon, openIcon}, indent, iconSpacing)));

                        } else {
                            String key = "co" + indent;

                            cachedIcon = iconCache.computeIfAbsent(key, k -> IconUtils
                                    .createImageIcon(new IndentedIcon(new Icon[]{collapsedIcon, closedIcon}, indent, iconSpacing)));

                        }
                    } else { // child without children
                        String key = "el" + indent;

                        cachedIcon = iconCache.computeIfAbsent(key, k -> IconUtils
                                .createImageIcon(new IndentedIcon(new Icon[]{new EmptyIndentedIcon(new Icon[]{expandedIcon}, 0), leafIcon}, indent, iconSpacing)));

                    }
                    
                    ((JLabel) c).setIcon(cachedIcon); 
                    
                }
            } else {
                c.setForeground(defaultForeground);
                ((JLabel) c).setHorizontalAlignment(defaultAlignment);
                ((JLabel) c).setIcon(null);
            }
        }

        return c;
    }

    private int getSelectionRange() {
        return Math.max(expandedIcon.getIconWidth(), collapsedIcon.getIconWidth());
    }

    private void setRowHeight() {
        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();

        int height = (int) Math.floor(((JLabel) new JTree().getCellRenderer()).getFont().getSize() * 1f / 72f * dpi) + 2;

        if (openIcon != null) {
            height = Math.max(height, openIcon.getIconHeight() + VERTICAL_ICON_PADDING);
        }

        if (closedIcon != null) {
            height = Math.max(height, closedIcon.getIconHeight() + VERTICAL_ICON_PADDING);
        }

        if (leafIcon != null) {
            height = Math.max(height, leafIcon.getIconHeight() + VERTICAL_ICON_PADDING);
        }

        if (collapsedIcon != null) {
            height = Math.max(height, collapsedIcon.getIconHeight() + VERTICAL_ICON_PADDING);
        }

        height = Math.max(height, expandedIcon.getIconHeight() + VERTICAL_ICON_PADDING);

        setRowHeight(height);
    }

    /**
     * Null checks have to be performed on icons because look and feel can set it as null
     */
    private static class IndentedIcon implements Icon {

        private final Icon[] icons;

        private final int indent;

        private int height;

        private int width;

        private final int iconSpacing;

        IndentedIcon(final Icon[] icons, final int indent, final int iconSpacing) {
            this.icons = icons;
            this.indent = indent;
            this.iconSpacing = iconSpacing;

            // calculate icon width
            for (Icon icon : icons) {
                if (icon != null) {
                    width += icon.getIconWidth();
                    width += iconSpacing;
                }
            }
            width += indent;
            width -= iconSpacing;

            // calculate icon height;
            for (Icon icon : icons) {
                if (icon != null) {
                    height = Math.max(height, icon.getIconHeight());
                }
            }
        }

        @Override
        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            int paintX = x + indent;

            for (Icon icon : icons) {
                if (icon != null) {
                    int paintY = y;

                    if (icon.getIconHeight() < getIconHeight()) {
                        paintY = getIconHeight() / 2 - icon.getIconHeight() / 2;
                    }

                    icon.paintIcon(c, g, paintX, y + paintY);

                    paintX += icon.getIconWidth();
                    paintX += iconSpacing;
                }
            }
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }

    /**
     * An empty icon that indents based on a reference icon
     * <p>
     * Null checks have to be performed on icons because look and feel can set it as null
     */
    private static class EmptyIndentedIcon implements Icon {

        private int height;

        private int width;

        EmptyIndentedIcon(final Icon[] referenceIcons, final int indent) {

            // calculate icon width
            for (Icon icon : referenceIcons) {
                if (icon != null) {
                    width += icon.getIconWidth();
                }
            }
            width += indent;

            // calculate icon height;
            for (Icon icon : referenceIcons) {
                if (icon != null) {
                    height = Math.max(height, icon.getIconHeight());
                }
            }
        }

        @Override
        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            // do nothing, it's an empty icon
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }
}
