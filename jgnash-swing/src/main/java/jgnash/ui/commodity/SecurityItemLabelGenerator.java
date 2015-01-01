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
package jgnash.ui.commodity;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

import jgnash.engine.SecurityNode;
import jgnash.text.CommodityFormat;
import jgnash.util.Resource;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;

/**
 * Tooltip generator for the security history chart
 *
 * @author Craig Cavanaugh
 *
 */
class SecurityItemLabelGenerator implements XYToolTipGenerator {
    /** The date formatter. */
    private DateFormat dateFormatter;

    /** The number formatter. */
    private NumberFormat numberFormatter;

    private String dateLabel;

    private String closeLabel;

    /**
     * Creates an item label generator using the default date and number 
     * formats.
     * @param node SecurityNode to base format on
     */
    public SecurityItemLabelGenerator(final SecurityNode node) {
        this(DateFormat.getDateInstance(DateFormat.SHORT), CommodityFormat.getShortNumberFormat(node.getReportedCurrencyNode()));
    }

    /**
     * Creates a tool tip generator using the supplied date formatter.
     *
     * @param dateFormatter  the date formatter ({@code null} not permitted).
     * @param numberFormatter  the number formatter ({@code null} not permitted).
     */
    private SecurityItemLabelGenerator(final DateFormat dateFormatter, final NumberFormat numberFormatter) {
        if (dateFormatter == null) {
            throw new IllegalArgumentException("Null 'dateFormatter' argument.");
        }
        if (numberFormatter == null) {
            throw new IllegalArgumentException("Null 'numberFormatter' argument.");
        }
        this.dateFormatter = dateFormatter;
        this.numberFormatter = numberFormatter;

        Resource rb = Resource.get();

        dateLabel = rb.getString("Label.Date");

        closeLabel = rb.getString("Label.Close");
    }

    /**
     * Generates a tooltip text item for a particular item within a series.
     *
     * @param dataset  the dataset.
     * @param series  the series (zero-based index).
     * @param item  the item (zero-based index).
     *
     * @return The tooltip text.
     */
    @Override
    public String generateToolTip(final XYDataset dataset, final int series, final int item) {

        String result = null;

        if (dataset instanceof OHLCDataset) {
            OHLCDataset d = (OHLCDataset) dataset;

            Number close = d.getClose(series, item);

            Number x = d.getX(series, item);

            if (x != null) {
                Date date = new Date(x.longValue());
                result = dateLabel + " " + dateFormatter.format(date);

                if (close != null) {
                    result = result + " " + closeLabel + "  " + numberFormatter.format(close.doubleValue());
                }
            }
        }

        return result;
    }
}
