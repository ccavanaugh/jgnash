/*
 * jGnash, a personal finance application
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
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.report.ui.openhtml;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jgnash.util.LogUtil;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PDFReportFactory {

    private PDFReportFactory() {
        // private constructor
    }

    public static void htmlToPDF(final Path htmlFile, final Path pdfFile) {
        try (OutputStream os = Files.newOutputStream(pdfFile)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withFile(htmlFile.toFile());
            builder.toStream(os);
            builder.run();
        } catch (final Exception e) {
            LogUtil.logSevere(PDFReportFactory.class, e);
        }
    }
}
