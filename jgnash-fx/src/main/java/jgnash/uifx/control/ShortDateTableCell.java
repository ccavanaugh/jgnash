/*
 * jGnash, account personal finance application
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
 *  You should have received account copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package jgnash.uifx.control;

import java.text.DateFormat;
import java.util.Date;

import javafx.scene.control.TableCell;

import jgnash.util.DateUtils;

/**
 * {@code TableCell} for {@code Dates} formatted in a simplified manner
 *
 * @author Craig Cavanaugh
 */
public class ShortDateTableCell<S> extends TableCell<S, Date> {

    private final DateFormat dateFormatter = DateUtils.getShortDateFormat();

    @Override
    protected void updateItem(final Date date, final boolean empty) {
        super.updateItem(date, empty);  // required

        if (!empty && date != null) {
            setText(dateFormatter.format(date));
        } else {
            setText(null);
        }
    }
}
