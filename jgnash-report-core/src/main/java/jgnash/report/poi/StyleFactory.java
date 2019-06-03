/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
package jgnash.report.poi;

import java.util.Objects;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Factory class for  generating consistent POI styles
 */
public class StyleFactory {

    /**
     * Creates the default header style
     *
     * @param wb {@code Workbook} the new style is to be assigned to
     * @return a new {@code CellStyle} instance
     */
    public static CellStyle createHeaderStyle(final Workbook wb) {
        Objects.requireNonNull(wb);

        final CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        return headerStyle;
    }

    /**
     * Creates the default header font
     *
     * @param wb {@code Workbook} font is to be assigned to
     * @return a new {@code Font} instance
     */
    public static Font createHeaderFont(final Workbook wb) {
        Objects.requireNonNull(wb);

        final Font headerFont = wb.createFont();
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setColor(IndexedColors.BLACK.getIndex());
        headerFont.setBold(true);

        return headerFont;
    }

    /**
     * Creates the default font
     *
     * @param wb {@code Workbook} font is to be assigned to
     * @return a new {@code Font} instance
     */
    public static Font createDefaultFont(final Workbook wb) {
        Objects.requireNonNull(wb);

        final Font headerFont = wb.createFont();
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setColor(IndexedColors.BLACK.getIndex());

        return headerFont;
    }
}
