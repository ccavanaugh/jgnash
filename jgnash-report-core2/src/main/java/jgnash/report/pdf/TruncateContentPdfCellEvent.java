/*
 * jGnash, account personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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
package jgnash.report.pdf;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.ExceptionConverter;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import jgnash.util.LogUtil;

import java.awt.Color;

import java.io.IOException;

/**
 * Handles automatic truncation of text
 */
public class TruncateContentPdfCellEvent implements PdfPCellEvent {

    private String content;

    private static String ellipsis = "...";

    private BaseFont bf;

    private final float fontSize;

    private final Color textColor;

    public TruncateContentPdfCellEvent(final String name, final String encoding, final boolean embedded,
                                       final Color color, final float size, final String content) {
        this.content = content;
        this.fontSize = size;
        this.textColor = color;

        try {
            bf = BaseFont.createFont(name, encoding, embedded);
        } catch (IOException e) {
            LogUtil.logSevere(TruncateContentPdfCellEvent.class, e);
        }
    }

    /**
     * Depending on locale or font, the suffix for truncation may need to change.
     *
     * @param truncationSuffix truncation suffix
     */
    public static void setEllipsis(final String truncationSuffix) {
        ellipsis = truncationSuffix;
    }

    @Override
    public void cellLayout(final PdfPCell cell, final Rectangle position, final PdfContentByte[] canvases) {

        float horizontalShift = 0;  // used to right align the cell content

        try {
            // available cell width factoring in margins
            final float availableWidth = position.getWidth() - cell.getPaddingLeft() - cell.getPaddingRight();

            // if content is too wide... truncate it
            if (bf.getWidthPoint(content, fontSize) > availableWidth) {

                // munch down the end of the string until it fits
                while (bf.getWidthPoint(content + ellipsis, fontSize) > availableWidth) {
                    content = content.substring(0, content.length() - 1);
                }

                content = content + ellipsis;
            }

            final Font font = new Font(bf, fontSize);
            font.setColor(textColor);

            final PdfContentByte canvas = canvases[PdfPTable.TEXTCANVAS];

            final ColumnText ct = new ColumnText(canvas);

            // calculate the shift if alignment is to the right
            if (cell.getHorizontalAlignment() == Element.ALIGN_RIGHT) {
                horizontalShift = availableWidth - bf.getWidthPoint(content, fontSize) - cell.getEffectivePaddingRight();
            }

            ct.setSimpleColumn(position.getLeft() + cell.getPaddingLeft() + horizontalShift,
                    position.getBottom() + cell.getEffectivePaddingBottom(),
                    position.getRight() - cell.getEffectivePaddingRight(),
                    position.getTop() - cell.getPaddingTop());

            ct.addElement(new Paragraph(content, font));
            ct.go();
        } catch (final DocumentException e) {
            throw new ExceptionConverter(e);
        }
    }
}
