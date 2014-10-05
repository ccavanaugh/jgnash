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
package jgnash.ui.util;

import java.awt.Component;
import java.util.regex.Pattern;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import jgnash.ui.register.table.PackableTableModel;

/**
 * Methods for working with JTables
 *
 * @author Craig Cavanaugh
 *
 */
public class JTableUtils {

    /**
     * Pads the minimum calculated with of the column. Depending on the platform
     * and the fonts used, java does not always return a good calculated minimum
     * width
     */
    private static final int MIN_WIDTH_PADDING = 15;
    /**
     * Pattern for splitting test using a space delimiter
     */
    private static final Pattern SPACE_DELIMITER_PATTERN = Pattern.compile(" ");

    /**
     * Static methods only
     */
    private JTableUtils() {
    }

    /**
     * Returns the column order of a table in a formatted
     * {@code String}.
     *
     * @param table The table to get the column order from
     * @return A string in the format "0 1 2 3"
     */
    public static String getColumnOrder(final JTable table) {
        StringBuilder buffer = new StringBuilder();
        int count = table.getColumnCount();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                buffer.append(' ');
            }
            buffer.append(table.convertColumnIndexToModel(i));

        }
        return buffer.toString();
    }

    /**
     * Sets the column order of a table given a correctly formatted
     * {@code String}.
     *
     * @param table the table to set the column positions
     * @param positions A string in the format "0 1 2 3"
     */
    public static void setColumnOrder(final JTable table, final String positions) {
        if (positions == null) {
            return;
        }
        String[] array = SPACE_DELIMITER_PATTERN.split(positions);
        int count = table.getColumnCount();
        if (array.length == count) {
            for (int i = 0; i < count; i++) {
                int index = table.convertColumnIndexToView(i);
                int position = Integer.parseInt(array[i]);
                table.getColumnModel().moveColumn(index, position);
            }
        }
    }

    /**
     * Returns the column widths of a table in a formatted
     * {@code String}.
     *
     * @param table The table to collect column widths from
     * @return A String in the format "34 56 56 56"
     */
    public static String getColumnWidths(final JTable table) {
        assert table != null;

        TableColumnModel model = table.getColumnModel();
        int count = model.getColumnCount();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                buffer.append(' ');
            }
            int index = table.convertColumnIndexToView(i);
            buffer.append(model.getColumn(index).getWidth());
        }
        return buffer.toString();
    }

    /**
     * Sets the columns widths of a table given a correctly formatted
     * {@code String}. If the provided String is null, the table columns
     * are sized to default values.
     *
     * @param table The table to set the columns widths
     * @param widths A String in the format "34 56 56 56"
     */
    public static void setColumnWidths(final JTable table, final String widths) {
        assert table != null;

        if (widths == null) {
            packTable(table); // size the table columns correctly
            return;
        }

        String[] array = SPACE_DELIMITER_PATTERN.split(widths);
        TableColumnModel model = table.getColumnModel();
        int count = model.getColumnCount();
        if (count == array.length) {
            for (int i = 0; i < count; i++) {
                int width = Integer.parseInt(array[i]);
                model.getColumn(i).setPreferredWidth(width);
            }
        } else { // column size does not match, columns were added or removed
            packTable(table); // repack the table
        }
    }

    /**
     * Sizes the columns in a JTable
     *
     * @param table The table to size the columns in
     */
    public static void packGenericTable(final JTable table) {
        TableColumnModel model = table.getColumnModel();
        int columns = model.getColumnCount();

        for (int i = 0; i < columns; i++) {
            TableColumn col = model.getColumn(i);

            // Get the column header width
            TableCellRenderer renderer = col.getHeaderRenderer();
            if (renderer == null) {
                renderer = table.getTableHeader().getDefaultRenderer();
            }
            Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
            int width = comp.getPreferredSize().width;

            // Find the largest width in the column
            int rows = table.getRowCount();
            for (int r = 0; r < rows; r++) {
                renderer = table.getCellRenderer(r, i);
                comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, i), false, false, r, i);
                width = Math.max(width, comp.getPreferredSize().width);
            }

            // Set the column width
            col.setPreferredWidth(width + MIN_WIDTH_PADDING);
        }
    }

    public static void packTable(final JTable table) {

        if (!(table.getModel() instanceof PackableTableModel)) {
            packGenericTable(table);
            return;
        }

        PackableTableModel model = (PackableTableModel) table.getModel();

        /*
         * Get width of printable portion of the page to control column widths
         */
        int tableWidth = table.getWidth();
        int[] widths = new int[model.getColumnCount()]; // calculated optimal widths
        int tWidth = 0; // total of calculated widths

        for (int i = 0; i < model.getColumnCount(); i++) {
            TableColumn col = table.getColumnModel().getColumn(i);

            // Get the column header width
            TableCellRenderer renderer = col.getHeaderRenderer();
            if (renderer == null) {
                renderer = table.getTableHeader().getDefaultRenderer();
            }
            Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);

            int width = comp.getMinimumSize().width;

            // Find the largest width in the column
            int rows = table.getRowCount();
            for (int r = 0; r < rows; r++) {
                renderer = table.getCellRenderer(r, i);
                comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, i), false, false, r, i);
                width = Math.max(width, comp.getMinimumSize().width);
            }
            widths[i] = width;

            if (i != model.getColumnCount() - 1) {
                widths[i] += 4;
            } else {
                widths[i] += MIN_WIDTH_PADDING;
            }

            tWidth += widths[i];
        }

        int[] optimizedWidths = widths.clone();

        if (tWidth > tableWidth) { // calculated width is wider than the page... need to compress columns

            // integer widths to calc percentage widths
            int[] columnWeights = model.getPreferredColumnWeights().clone(); // create a clone so return array is not modified

            int fixedWidth = 0; // total fixed width of columns

            for (int i = 0; i < optimizedWidths.length; i++) {
                if (columnWeights[i] == 0) {
                    fixedWidth += optimizedWidths[i];
                }
            }

            int diff = tableWidth - fixedWidth; // remaining non fixed width that must be compressed
            int totalWeight = 0; // used to calculate percentages

            for (int columnWeight : columnWeights) {
                totalWeight += columnWeight;
            }

            int i = 0;
            while (i < columnWeights.length) {
                if (columnWeights[i] > 0) {
                    int adj = (int) ((float) columnWeights[i] / (float) totalWeight * diff);

                    if (optimizedWidths[i] > adj) { // only change if necessary
                        optimizedWidths[i] = adj;
                    } else {
                        diff -= optimizedWidths[i]; // available difference is reduced
                        totalWeight -= columnWeights[i]; // adjust the weighting
                        optimizedWidths = widths.clone(); // reset widths
                        columnWeights[i] = 0; // do not try to adjust width again
                        i = -1; // restart the loop from the beginning
                    }
                }
                i++;
            }
        }

        for (int i = 0; i < model.getColumnCount(); i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setPreferredWidth(optimizedWidths[i]);
        }
    }

    /**
     * Packs two table and forces them to have the same column width. 
     * Assumes both tables have the same number of columns
     * 
     * @param tableOne table 1
     * @param tableTwo table 2
     */
    public static void packTables(final JTable tableOne, final JTable tableTwo) {
        packGenericTable(tableOne);
        packGenericTable(tableTwo);        

        for (int i = 0; i < tableOne.getModel().getColumnCount(); i++) {
            int tableWidth = tableOne.getColumnModel().getColumn(i).getPreferredWidth();
            int footerTableWidth = tableTwo.getColumnModel().getColumn(i).getPreferredWidth();

            if (tableWidth > footerTableWidth) {
                tableTwo.getColumnModel().getColumn(i).setPreferredWidth(tableWidth);
            } else {
                tableOne.getColumnModel().getColumn(i).setPreferredWidth(footerTableWidth);
            }
        }
    }
}
