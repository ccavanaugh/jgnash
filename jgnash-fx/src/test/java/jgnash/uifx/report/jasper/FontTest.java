package jgnash.uifx.report.jasper;

import ar.com.fdvs.dj.domain.Style;

import jgnash.ui.report.jasper.AWTFontUtilities;
import jgnash.ui.report.jasper.ReportFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Font width unit test.
 *
 * @author Craig Cavanaugh
 */
class FontTest {

    @Test
    void getWidth() {

        Style style = new Style();
        style.setFont(ReportFactory.getDefaultMonoFont(12));

        int width = AWTFontUtilities.getStringWidth("test", style);

        assertTrue(width > 0);
    }
}
