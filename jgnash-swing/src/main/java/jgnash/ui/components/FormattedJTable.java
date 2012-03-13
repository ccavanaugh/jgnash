/*
 * jGnash, a personal finance application
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
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import jgnash.ui.ThemeManager;
import jgnash.ui.register.RegisterFactory;
import jgnash.util.DateUtils;

/**
 * 
 * @author Craig Cavanaugh
 * @version $Id: FormattedJTable.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class FormattedJTable extends JTable {

    private final AtomicBoolean defaultSaved = new AtomicBoolean();

    private int defaultAlignment;

    private Font defaultFont;

    private Color defaultColor;

    private final boolean stripe = stripe();

    private final DateFormat dateFormatter = DateUtils.getShortDateFormat();

    public FormattedJTable() {
        super();
    }

    public FormattedJTable(final TableModel model) {
        super(model);
    }

    /**
     * Override prepareRenderer instead of using a custom renderer so the look and feel is preserved
     * 
     * @see javax.swing.JTable#prepareRenderer(javax.swing.table.TableCellRenderer, int, int)
     */
    @Override
    public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {

        Component c = super.prepareRenderer(renderer, row, column);

        if (c instanceof JLabel) {

            if (!defaultSaved.get()) { // save the defaults so they can be restored
                defaultAlignment = ((JLabel) c).getHorizontalAlignment();
                defaultFont = c.getFont();
                defaultColor = c.getForeground();

                defaultSaved.set(true);
            }

            c.setFont(defaultFont);
            c.setForeground(defaultColor);

            ((JLabel) c).setIcon(null);

            if (stripe) { // stripe the background
                if (isOdd(row)) {
                    c.setBackground(RegisterFactory.getOddColor());
                } else {
                    c.setBackground(RegisterFactory.getEvenColor());
                }
            }
            
            if (Number.class.isAssignableFrom(getColumnClass(column))) {                         
                ((JLabel) c).setHorizontalAlignment(SwingConstants.RIGHT);
            } else if (String.class.isAssignableFrom(getColumnClass(column))) {
                ((JLabel) c).setHorizontalAlignment(defaultAlignment);   
            } else if (Date.class.isAssignableFrom(getColumnClass(column))) {  // special handling for dates
                ((JLabel) c).setHorizontalAlignment(defaultAlignment);
                ((JLabel) c).setText(dateFormatter.format(getModel().getValueAt(row, column)));
            } else {
                ((JLabel) c).setHorizontalAlignment(defaultAlignment);
            }
        }

        return c;
    }

    private static boolean isOdd(final int i) {
        return (i & 1) == 1;
    }

    private static boolean stripe() {

        if (ThemeManager.isLookAndFeelSubstance()) {
            return false;
        }

        return !ThemeManager.isLookAndFeelNimbus();
    }
}
