package jgnash.ui.report.jasper;

import ar.com.fdvs.dj.domain.Style;

import jgnash.ui.report.FontUtilities;
import jgnash.ui.report.ReportFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Craig Cavanaugh
 */
public class FontTest {

    @Test
    public void getWidth() {

        Style style = new Style();
        style.setFont(ReportFactory.getDefaultMonoFont(12));

        int width = FontUtilities.getStringWidth("test", style);

        assertEquals(42, width);
    }
}
