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
package jgnash.ui.register.table;

import java.util.Date;

/**
 * Interface for a clipping register table model.  The top and bottom of the data will be
 * clipped by starting and ending date
 * <p/>
 *
 * @author Craig Cavanaugh
 *
 */
public interface ClippingModel extends RegisterModel {
    /**
     * @param startDate The startDate to set.
     */
    public void setStartDate(Date startDate);

    /**
     * @return Returns the startDate.
     */
    public Date getStartDate();

    /**
     * @param stopDate The stopDate to set.
     */
    public void setEndDate(Date stopDate);

    /**
     * @return Returns the stopDate.
     */
    public Date getEndDate();

    public void setEndIndex(int end);

    public void setStartIndex(int start);
}
