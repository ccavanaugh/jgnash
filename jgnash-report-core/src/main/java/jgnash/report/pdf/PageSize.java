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
package jgnash.report.pdf;

import static jgnash.report.pdf.Constants.POINTS_PER_INCH;
import static jgnash.report.pdf.Constants.POINTS_PER_MM;

/**
 * Enumeration of standard page sizes.
 * <p>
 * Page sizes are in Points
 *
 * @author Craig Cavanaugh
 */
public enum PageSize {

    A2("ISO A2", 420 * POINTS_PER_MM, 594 * POINTS_PER_MM),
    A3("ISO A3", 297 * POINTS_PER_MM, 420 * POINTS_PER_MM),
    A4("ISO A4", 210 * POINTS_PER_MM, 297 * POINTS_PER_MM),
    LETTER("US Letter", 8.5f * POINTS_PER_INCH, 11f * POINTS_PER_INCH),
    LEGAL("US Legal", 8.5f * POINTS_PER_INCH, 14f * POINTS_PER_INCH),
    TABLOID("US Tabloid", 11f * POINTS_PER_INCH, 17f * POINTS_PER_INCH);

    public final transient float width;
    public final transient float height;

    private final transient String description;

    PageSize(final String description, final float width, final float height) {
        this.description = description;
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return description;
    }

}
