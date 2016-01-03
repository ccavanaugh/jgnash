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
package jgnash.ui.report;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.table.AbstractTableModel;

import jgnash.engine.CurrencyNode;
import jgnash.text.CommodityFormat;
import jgnash.util.DateUtils;

/**
 * Report model interface
 *
 * @author Craig Cavanaugh
 */
public abstract class AbstractReportTableModel extends AbstractTableModel {

    public abstract CurrencyNode getCurrency();

    public abstract ColumnStyle getColumnStyle(int columnIndex);

    public abstract ColumnHeaderStyle getColumnHeaderStyle(int columnIndex);

    /** Return true if the column should be fixed width
     *
     * @param columnIndex column to verify
     * @return true if fixed width
     */
    public boolean isColumnFixedWidth(final int columnIndex) {
        return false;
    }

    private int getGroupColumn() {
        int column = -1;

        for (int i = 0; i < getColumnCount(); i++) {
            if (getColumnStyle(i) == ColumnStyle.GROUP) {
                column = i;
                break;
            }
        }
        return column;
    }

    /**
     * Returns the longest value for the specified column
     *
     * @param columnIndex column to check
     * @return String representing the longest value
     */
    public String getColumnPrototypeValueAt(final int columnIndex) {

        int groupColumn = getGroupColumn();

        String longest = "";

        if (getColumnClass(columnIndex).isAssignableFrom(BigDecimal.class)) {

            // does the column need to be summed
            boolean sum = getColumnStyle(columnIndex) == ColumnStyle.BALANCE_WITH_SUM || getColumnStyle(columnIndex) == ColumnStyle.BALANCE_WITH_SUM_AND_GLOBAL || getColumnStyle(columnIndex) == ColumnStyle.AMOUNT_SUM;

            // mapping to sum groups
            HashMap<String, BigDecimal> groupMap = new HashMap<>();

            // number format to use
            NumberFormat nf;

            switch (getColumnStyle(columnIndex)) {
                case QUANTITY:
                    nf = ReportFactory.getQuantityFormat();
                    break;
                case PERCENTAGE:
                    nf = ReportFactory.getPercentageFormat();
                    break;
                default:
                    nf = CommodityFormat.getFullNumberFormat(getCurrency());
                    break;
            }

            BigDecimal total = BigDecimal.ZERO; // end total

            for (int i = 0; i < getRowCount(); i++) {
                BigDecimal value = (BigDecimal) getValueAt(i, columnIndex);

                if (value != null) {

                    String prototype = nf.format(value);

                    // look and individual values
                    if (prototype.length() > longest.length()) {
                        longest = prototype;
                    }

                    if (sum) {
                        total = total.add(value); // global value
                    }

                    if (groupColumn >= 0) {
                        String group = (String) getValueAt(i, groupColumn);

                        BigDecimal o = groupMap.get(group);
                        if (o != null) {
                            groupMap.put(group, o.add(value));
                        } else {
                            groupMap.put(group, value);
                        }
                    }
                }
            }

            if (sum) {
                if (nf.format(total).length() > longest.length()) { // look at column total
                    longest = nf.format(total);
                }

                for (BigDecimal value : groupMap.values()) {
                    if (nf.format(value).length() > longest.length()) { // look at group totals
                        longest = nf.format(value);
                    }
                }
            }

        } else if (getColumnStyle(columnIndex) == ColumnStyle.STRING) {
            for (int i = 0; i < getRowCount(); i++) {
                String val = (String) getValueAt(i, columnIndex);

                if (val != null && val.length() > longest.length()) {
                    longest = val;
                }
            }
        } else if (getColumnStyle(columnIndex) == ColumnStyle.SHORT_DATE) {
            final DateTimeFormatter dateTimeFormatter = DateUtils.getShortDateTimeFormat();

            for (int i = 0; i < getRowCount(); i++) {
                try {
                    LocalDate date = (LocalDate) getValueAt(i, columnIndex);

                    if (date != null) {
                        String val = dateTimeFormatter.format(date);
                        if (val.length() > longest.length()) {
                            longest = val;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    Logger.getLogger(AbstractReportTableModel.class.getName()).log(Level.INFO, e.getLocalizedMessage(), e);
                }
            }
        }

        return longest;
    }
}
