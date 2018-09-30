package jgnash;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jgnash.report.pdf.Style;
import jgnash.report.pdf.TruncateContentPdfCellEvent;
import jgnash.util.LogUtil;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TableTest {

    @Test
    void generateTable() throws IOException {

        Path tempPath = null;

        TruncateContentPdfCellEvent.setEllipsis("…");

        try {

            tempPath = Files.createTempFile("test", ".pdf");
            System.out.println(tempPath);

            Document document = new Document();
            document.setPageSize(PageSize.LETTER.rotate());


            PdfWriter.getInstance(document, Files.newOutputStream(tempPath));

            document.open();

            // step 4 create PDF contents
            document.add(createTable());

            //step 5
            document.close();

            System.out.println("PDF Created!");

            assertTrue(Files.exists(tempPath));

        } catch (IOException e) {
            LogUtil.logSevere(TableTest.class, e);
        } finally {
            if (tempPath != null) {
                //Files.deleteIfExists(tempPath);
            }
        }
    }

    PdfPTable createTable() throws DocumentException {

        // create 6 column table
        PdfPTable table = new PdfPTable(9);

        // set the width of the table to 100% of page
        table.setWidthPercentage(100);

        // set relative columns width
        table.setWidths(new float[]{0.6f, .8f, 1.5f, 1.8f, 1.2f, .5f, 1, 1, 1});

        // table header
        table.addCell(createHeaderCell("Date"));
        table.addCell(createHeaderCell("Num"));
        table.addCell(createHeaderCell("Payee"));
        table.addCell(createHeaderCell("Memo"));
        table.addCell(createHeaderCell("Account"));
        table.addCell(createHeaderCell("Clr"));
        table.addCell(createHeaderCell("Deposit"));
        table.addCell(createHeaderCell("Withdrawal"));
        table.addCell(createHeaderCell("Balance"));

        // table row
        table.addCell(createValueCell("06/19/03"));
        table.addCell(createValueCell("101"));
        table.addCell(createValueCell("Fast Auto Parts"));
        table.addCell(createValueCell("New brake pads for the truck"));
        table.addCell(createValueCell("Service"));
        table.addCell(createValueCell("R"));
        table.addCell(createNumericValueCell(""));
        table.addCell(createNumericValueCell("46.61"));
        table.addCell(createNumericValueCell("255.67"));

        // table row
        table.addCell(createValueCell("06/21/03"));
        table.addCell(createValueCell("101"));
        table.addCell(createValueCell("XYZ Corp"));
        table.addCell(createValueCell("Pay check"));
        table.addCell(createValueCell("Income"));
        table.addCell(createValueCell("R"));
        table.addCell(createNumericValueCell("850.00"));
        table.addCell(createNumericValueCell(" "));
        table.addCell(createNumericValueCell("1105.67"));

        return table;
    }

    // create cells
    private static PdfPCell createHeaderCell(String text) {

        // create cell
        PdfPCell cell = new PdfPCell();
        cell.setCellEvent(new TruncateContentPdfCellEvent(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED,
                Color.DARK_GRAY, 8, text));


        // set style
        Style.headerCellStyle(cell);

        return cell;
    }

    // create cells
    private static PdfPCell createValueCell(String text) {
        // create cell
        PdfPCell cell = new PdfPCell();
        cell.setCellEvent(new TruncateContentPdfCellEvent(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED,
                Color.BLACK, 8, text));

        // set style
        Style.valueCellStyle(cell);
        return cell;
    }

    // create cells
    private static PdfPCell createNumericValueCell(String text) {
        // create cell
        PdfPCell cell = new PdfPCell();
        cell.setCellEvent(new TruncateContentPdfCellEvent(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED,
                Color.BLACK, 8, text));

        // set style
        Style.numericValueCellStyle(cell);
        return cell;
    }
}
