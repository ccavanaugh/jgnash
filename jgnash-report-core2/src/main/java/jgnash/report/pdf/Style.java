package jgnash.report.pdf;

import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfPCell;

import java.awt.Color;

public class Style {

    public static void headerCellStyle(PdfPCell cell){

        // alignment
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        // padding
        cell.setPaddingLeft(2);
        cell.setPaddingRight(2);
        cell.setPaddingBottom(2);

        // background color
        cell.setBackgroundColor(Color.LIGHT_GRAY);

        // border
        cell.setBorder(0);
        cell.setBorderWidthBottom(1);
        cell.setBorderColorBottom(Color.GRAY);

        // height
        cell.setMinimumHeight(18);

    }

    public static void valueCellStyle(PdfPCell cell){
        // alignment
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        // padding
        cell.setPaddingLeft(2);
        cell.setPaddingRight(2);

        // border
        cell.setBorder(0);
        cell.setBorderWidthBottom(0.5f);

        // height
        cell.setMinimumHeight(18);
    }

    public static void numericValueCellStyle(PdfPCell cell){
        valueCellStyle(cell);

        // alignment
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    }
}
