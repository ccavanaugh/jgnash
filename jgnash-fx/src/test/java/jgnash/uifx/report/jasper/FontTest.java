package jgnash.uifx.report.jasper;

import ar.com.fdvs.dj.domain.Style;

import jgnash.ui.report.jasper.AWTFontUtilities;
import jgnash.ui.report.jasper.ReportFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Font width unit test.
 *
 * @author Craig Cavanaugh
 */
public class FontTest {

    @Test
    public void getWidth() {

        Style style = new Style();
        style.setFont(ReportFactory.getDefaultMonoFont(12));

        int width = AWTFontUtilities.getStringWidth("test", style);

        assertEquals(42, width);
    }
}
