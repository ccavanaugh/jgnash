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

import java.text.DecimalFormat;
import java.util.Objects;

import jgnash.engine.CurrencyNode;
import jgnash.text.NumericFormats;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Factory class for generating consistent POI styles
 *
 * @author Craig Cavanaugh
 */
class StyleFactory {

    private StyleFactory() {
        // Utility class
    }

    /**
     * Creates the default header style
     *
     * @param wb {@code Workbook} the new style is to be assigned to
     * @return a new {@code CellStyle} instance
     */
    static CellStyle createFooterStyle(final Workbook wb) {
        Objects.requireNonNull(wb);

        final CellStyle footerStyle = wb.createCellStyle();
        footerStyle.setBorderBottom(BorderStyle.THIN);
        footerStyle.setBorderTop(BorderStyle.THIN);
        footerStyle.setBorderLeft(BorderStyle.THIN);
        footerStyle.setBorderRight(BorderStyle.THIN);
        footerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        footerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        footerStyle.setAlignment(HorizontalAlignment.CENTER);
        footerStyle.setFont(createFooterFont(wb));

        return footerStyle;
    }

    /**
     * Creates the default header style
     *
     * @param wb {@code Workbook} the new style is to be assigned to
     * @return a new {@code CellStyle} instance
     */
    static CellStyle createHeaderStyle(final Workbook wb) {
        Objects.requireNonNull(wb);

        final CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setFont(createHeaderFont(wb));

        return headerStyle;
    }

    static CellStyle createDefaultAmountStyle(final Workbook wb, final CurrencyNode currencyNode) {
        Objects.requireNonNull(wb);
        Objects.requireNonNull(currencyNode);

        final DecimalFormat format = (DecimalFormat) NumericFormats.getFullCommodityFormat(currencyNode);
        final String pattern = format.toLocalizedPattern().replace("¤", currencyNode.getPrefix());

        final Font defaultFont = createDefaultFont(wb);
        final CellStyle amountStyle = wb.createCellStyle();
        final DataFormat df = wb.createDataFormat();

        amountStyle.setFont(defaultFont);
        amountStyle.setDataFormat(df.getFormat(pattern));

        return amountStyle;
    }

    /**
     * Creates the default header font
     *
     * @param wb {@code Workbook} font is to be assigned to
     * @return a new {@code Font} instance
     */
    private static Font createFooterFont(final Workbook wb) {
        Objects.requireNonNull(wb);

        final Font font = wb.createFont();
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.BLACK.getIndex());
        font.setBold(true);

        return font;
    }

    /**
     * Creates the default header font
     *
     * @param wb {@code Workbook} font is to be assigned to
     * @return a new {@code Font} instance
     */
    static Font createHeaderFont(final Workbook wb) {
        Objects.requireNonNull(wb);

        final Font font = wb.createFont();
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);

        return font;
    }

    /**
     * Creates the default font
     *
     * @param wb {@code Workbook} font is to be assigned to
     * @return a new {@code Font} instance
     */
    static Font createDefaultFont(final Workbook wb) {
        Objects.requireNonNull(wb);

        final Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.BLACK.getIndex());

        return font;
    }
}
