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

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import jgnash.util.Resource;

/**
 * JComboBox intended for selecting predetermined periods of times
 * The period returned is in milliseconds.  getPeriods and getDescriptions
 * can be overridden to change the available periods
 *
 * @author Craig Cavanaugh
 * @version $Id: TimePeriodCombo.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class TimePeriodCombo extends JComboBox {

    private int[] periods = new int[0];

    public TimePeriodCombo() {
        super();

        periods = getPeriods();
        String[] descriptions = getDescriptions();

        assert periods.length == descriptions.length;

        setModel(new DefaultComboBoxModel(descriptions));
    }

    static int[] getPeriods() {
        return new int[]{300000, 600000, 900000, 1800000, 3600000, 7200000, 28800000, 86400000, 0};
    }

    static String[] getDescriptions() {

        final Resource rb = Resource.get();

        return new String[]{rb.getString("Period.5Min"), rb.getString("Period.10Min"), rb.getString("Period.15Min"),
                rb.getString("Period.30Min"), rb.getString("Period.1Hr"), rb.getString("Period.2Hr"),
                rb.getString("Period.8Hr"), rb.getString("Period.1Day"), rb.getString("Period.NextStart")};
    }

    /**
     * Sets the selected period.  Period must be a valid period
     * or no change will occur.
     *
     * @param period period in milliseconds to select
     */
    public void setSelectedPeriod(final int period) {
        for (int i = 0; i < periods.length; i++) {
            if (period == periods[i]) {
                setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Returns the selected period in milliseconds
     *
     * @return selected period in milliseconds
     */
    public int getSelectedPeriod() {
        return periods[getSelectedIndex()];
    }
}