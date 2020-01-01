/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
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
package jgnash.uifx.views.register;

import java.time.LocalDate;
import java.time.Month;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import jgnash.engine.Transaction;
import jgnash.time.DateUtils;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.util.NotNull;

/**
 * Transaction slip interface.
 *
 * @author Craig Cavanaugh.
 */
interface Slip extends BaseSlip {

    /**
     * Manages the date column width to compensate for date format and font scale
     */
    DoubleProperty dateColumnWidth = new SimpleDoubleProperty();

    int ICON_BORDER_WIDTH = 43;

    /**
     * Loads a {@code Transaction} into the form and sets it up for modification.
     *
     * @param transaction {@code Transaction} to load for modification
     */
    void modifyTransaction(@NotNull final Transaction transaction);

    /**
     * Builds and returns a new {@code Transaction} based on form contents.
     *
     * @return new {@code Transaction} instance
     */
    @NotNull
    Transaction buildTransaction();

    default DoubleBinding getDateColumnWidth(final String style) {

        double textWidth = Math.ceil(JavaFXUtils.getDisplayedTextWidth(DateUtils.getShortDateManualEntryFormatter()
                .format(LocalDate.of(2028, Month.DECEMBER, 28)), style)
                / ThemeManager.fontScaleProperty().get());

        return ThemeManager.fontScaleProperty().multiply(textWidth).add(ICON_BORDER_WIDTH);
    }

}
