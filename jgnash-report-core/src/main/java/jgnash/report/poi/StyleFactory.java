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
package jgnash.report.poi;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Objects;

import jgnash.engine.CurrencyNode;
import jgnash.engine.MathConstants;
import jgnash.text.NumericFormats;
import jgnash.util.NotNull;

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

    static final int DEFAULT_HEIGHT = 10;
    static final int HEADER_FOOTER_HEIGHT = 11;
    static final int MARGIN = 4;
    static final int TITLE_HEIGHT = 14;
    static final int GROUP_HEIGHT = 12;

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
        footerStyle.setAlignment(HorizontalAlignment.RIGHT);
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

    /**
     * Applies a currency format to a {@code CellStyle}
     *
     * @param wb           the {@code Workbook} the numeric format is being created for
     * @param currencyNode the {@code CurrencyNode} to extract symbol information from
     * @param cellStyle    the {@code CellStyle} being updated
     * @return the {@code CellStyle} being updated
     */
    static CellStyle applyCurrencyFormat(final Workbook wb, final CurrencyNode currencyNode, final CellStyle cellStyle) {
        final DecimalFormat format = (DecimalFormat) NumericFormats.getFullCommodityFormat(currencyNode);
        final String pattern = format.toLocalizedPattern().replace("¤", currencyNode.getPrefix());
        final DataFormat df = wb.createDataFormat();
        cellStyle.setDataFormat(df.getFormat(pattern));
        cellStyle.setAlignment(HorizontalAlignment.RIGHT);

        return cellStyle;
    }

    /**
     * Applies a percentage format to a {@code CellStyle}
     *
     * @param wb        the {@code Workbook} the numeric format is being created for
     * @param cellStyle the {@code CellStyle} being updated
     * @return the {@code CellStyle} being updated
     */
    static CellStyle applyPercentageFormat(final Workbook wb, final CellStyle cellStyle) {

        final NumberFormat percentageFormat = NumericFormats.getPercentageFormat();

        final DataFormat df = wb.createDataFormat();
        cellStyle.setDataFormat(df.getFormat(((DecimalFormat) percentageFormat).toPattern()));
        cellStyle.setAlignment(HorizontalAlignment.RIGHT);

        return cellStyle;
    }

    /**
     * Applies a Security quantity format to a {@code CellStyle}
     *
     * @param wb        the {@code Workbook} the numeric format is being created for
     * @param cellStyle the {@code CellStyle} being updated
     * @return the {@code CellStyle} being updated
     */
    static CellStyle applySecurityQuantityFormat(final Workbook wb, final CellStyle cellStyle) {

        final NumberFormat qtyFormat = NumericFormats.getFixedPrecisionFormat(MathConstants.SECURITY_QUANTITY_ACCURACY);

        final DataFormat df = wb.createDataFormat();
        cellStyle.setDataFormat(df.getFormat(((DecimalFormat) qtyFormat).toPattern()));
        cellStyle.setAlignment(HorizontalAlignment.RIGHT);

        return cellStyle;
    }

    /**
     * Applies a short date format to a {@code CellStyle}
     *
     * @param wb        the {@code Workbook} the numeric format is being created for
     * @param cellStyle the {@code CellStyle} being updated
     * @return the {@code CellStyle} being updated
     */
    static CellStyle applyShortDateFormat(final Workbook wb, final CellStyle cellStyle) {
        cellStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("mm/dd/yy"));
        cellStyle.setAlignment(HorizontalAlignment.LEFT);

        return cellStyle;
    }

    /**
     * Applies a short date format to a {@code CellStyle}
     *
     * @param wb        the {@code Workbook} the numeric format is being created for
     * @param cellStyle the {@code CellStyle} being updated
     * @return the {@code CellStyle} being updated
     */
    static CellStyle applyTimestampFormat(final Workbook wb, final CellStyle cellStyle) {
        cellStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("YYYY-MM-DD HH:MM:SS"));
        cellStyle.setAlignment(HorizontalAlignment.LEFT);

        return cellStyle;
    }

    /**
     * Creates the default {@code CellStyle} for currency value
     *
     * @param wb the {@code Workbook} the numeric format is being created for
     * @return the {@code CellStyle} being created
     */
    static CellStyle createDefaultAmountStyle(@NotNull final Workbook wb, @NotNull final CurrencyNode currencyNode) {
        return applyCurrencyFormat(wb, currencyNode, createDefaultStyle(wb));
    }

    /**
     * Creates the default {@code CellStyle} for a {@code Workbook}
     *
     * @param wb the {@code Workbook} the default format is being created for
     * @return the {@code CellStyle} being created
     */
    static CellStyle createDefaultStyle(@NotNull final Workbook wb) {
        final Font defaultFont = createDefaultFont(wb);
        final CellStyle cellStyle = wb.createCellStyle();

        cellStyle.setFont(defaultFont);

        return cellStyle;
    }

    /**
     * Creates the Title {@code CellStyle} for a {@code Workbook}
     *
     * @param wb the {@code Workbook} the default format is being created for
     * @return the {@code CellStyle} being created
     */
    static CellStyle createTitleStyle(@NotNull final Workbook wb) {
        final Font font = createTitleFont(wb);
        final CellStyle cellStyle = wb.createCellStyle();

        cellStyle.setFont(font);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);

        return cellStyle;
    }

    /**
     * Creates the Title {@code CellStyle} for a {@code Workbook}
     *
     * @param wb the {@code Workbook} the default format is being created for
     * @return the {@code CellStyle} being created
     */
    static CellStyle createGroupStyle(@NotNull final Workbook wb) {
        final Font font = createGroupFont(wb);
        final CellStyle cellStyle = wb.createCellStyle();

        cellStyle.setFont(font);
        cellStyle.setAlignment(HorizontalAlignment.LEFT);

        return cellStyle;
    }

    /**
     * Creates the Title {@code CellStyle} for a {@code Workbook}
     *
     * @param wb the {@code Workbook} the default format is being created for
     * @return the {@code CellStyle} being created
     */
    static CellStyle createSubTitleStyle(@NotNull final Workbook wb) {
        final Font font = createSubTitleFont(wb);
        final CellStyle cellStyle = wb.createCellStyle();

        cellStyle.setFont(font);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);

        return cellStyle;
    }

    /**
     * Creates the default header font
     *
     * @param wb {@code Workbook} font is to be assigned to
     * @return a new {@code Font} instance
     */
    private static Font createFooterFont(@NotNull final Workbook wb) {
        Objects.requireNonNull(wb);

        final Font font = wb.createFont();
        font.setFontHeightInPoints((short) HEADER_FOOTER_HEIGHT);
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
    static Font createHeaderFont(@NotNull final Workbook wb) {
        Objects.requireNonNull(wb);

        final Font font = wb.createFont();
        font.setFontHeightInPoints((short) HEADER_FOOTER_HEIGHT);
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
    static Font createDefaultFont(@NotNull final Workbook wb) {
        Objects.requireNonNull(wb);

        final Font font = wb.createFont();
        font.setFontHeightInPoints((short) DEFAULT_HEIGHT);
        font.setColor(IndexedColors.BLACK.getIndex());

        return font;
    }

    /**
     * Creates the default title font
     *
     * @param wb {@code Workbook} font is to be assigned to
     * @return a new {@code Font} instance
     */
    private static Font createTitleFont(@NotNull final Workbook wb) {
        Objects.requireNonNull(wb);

        final Font font = wb.createFont();
        font.setFontHeightInPoints((short) TITLE_HEIGHT);
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());

        return font;
    }

    /**
     * Creates the default group font
     *
     * @param wb {@code Workbook} font is to be assigned to
     * @return a new {@code Font} instance
     */
    private static Font createGroupFont(@NotNull final Workbook wb) {
        Objects.requireNonNull(wb);

        final Font font = wb.createFont();
        font.setFontHeightInPoints((short) GROUP_HEIGHT);
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());

        return font;
    }

    /**
     * Creates the default title font
     *
     * @param wb {@code Workbook} font is to be assigned to
     * @return a new {@code Font} instance
     */
    private static Font createSubTitleFont(@NotNull final Workbook wb) {
        Objects.requireNonNull(wb);

        final Font font = createDefaultFont(wb);
        font.setItalic(true);

        return font;
    }
}
