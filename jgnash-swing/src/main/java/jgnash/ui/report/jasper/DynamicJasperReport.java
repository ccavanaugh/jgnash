/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
package jgnash.ui.report.jasper;

import ar.com.fdvs.dj.core.DynamicJasperHelper;
import ar.com.fdvs.dj.core.layout.ClassicLayoutManager;
import ar.com.fdvs.dj.core.layout.HorizontalBandAlignment;
import ar.com.fdvs.dj.domain.AutoText;
import ar.com.fdvs.dj.domain.DJCalculation;
import ar.com.fdvs.dj.domain.DJGroupLabel;
import ar.com.fdvs.dj.domain.DynamicReport;
import ar.com.fdvs.dj.domain.Style;
import ar.com.fdvs.dj.domain.builders.ColumnBuilder;
import ar.com.fdvs.dj.domain.builders.DynamicReportBuilder;
import ar.com.fdvs.dj.domain.builders.GroupBuilder;
import ar.com.fdvs.dj.domain.constants.Border;
import ar.com.fdvs.dj.domain.constants.Font;
import ar.com.fdvs.dj.domain.constants.GroupLayout;
import ar.com.fdvs.dj.domain.constants.HorizontalAlign;
import ar.com.fdvs.dj.domain.constants.LabelPosition;
import ar.com.fdvs.dj.domain.constants.Page;
import ar.com.fdvs.dj.domain.constants.Transparency;
import ar.com.fdvs.dj.domain.constants.VerticalAlign;
import ar.com.fdvs.dj.domain.entities.DJGroup;
import ar.com.fdvs.dj.domain.entities.columns.AbstractColumn;
import ar.com.fdvs.dj.domain.entities.columns.PropertyColumn;

import java.awt.Color;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JPanel;

import jgnash.text.CommodityFormat;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.report.AbstractReportTableModel;
import jgnash.ui.report.ColumnHeaderStyle;
import jgnash.ui.report.ColumnStyle;
import jgnash.ui.report.FontUtilities;
import jgnash.ui.report.ReportFactory;
import jgnash.ui.report.ReportPrintFactory;
import jgnash.util.DateUtils;
import jgnash.util.Resource;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;

/**
 * Abstract report class that must be extended to create a report
 * 
 * @author Craig Cavanaugh
 *
 */
public abstract class DynamicJasperReport {

    protected final Resource rb = Resource.get();

    private DynamicJasperReportPanel viewer;

    private final static String COLUMN_PROPERTY = "COLUMN_";

    private final static String BASE_FONT_SIZE = "baseFontSize";

    protected static final Logger logger = Logger.getLogger(DynamicJasperReport.class.getName());

    void setViewer(final DynamicJasperReportPanel viewer) {
        this.viewer = viewer;
    }

    protected void refreshReport() {
        viewer.refreshReport();
    }

    final public void showReport() {
        DynamicJasperReportFrame.viewReport(this); //finally display the report report
    }

    final public void setPageFormat(final PageFormat pageFormat) {
        ReportPrintFactory.savePageFormat(this, pageFormat);
    }

    final public PageFormat getPageFormat() {
        return ReportPrintFactory.getPageFormat(this);
    }

    final int getBaseFontSize() {
        Preferences p = getPreferences();
        return p.getInt(BASE_FONT_SIZE, 7);
    }

    final void setBaseFontSize(final int size) {
        Preferences p = getPreferences();
        p.putInt(BASE_FONT_SIZE, size);
    }

    private Page assignPageFormat(final DynamicReportBuilder builder) {
        PageFormat format = getPageFormat();

        Paper paper = format.getPaper();

        int orientation = format.getOrientation();

        int topMargin;
        int rightMargin;
        int leftMargin;
        int bottomMargin;

        Page page;

        if (orientation == PageFormat.PORTRAIT) {
            page = new Page((int) paper.getHeight(), (int) paper.getWidth(), true);
            leftMargin = (int) paper.getImageableX();
            rightMargin = (int) paper.getWidth() - (int) paper.getImageableWidth() - leftMargin;
            topMargin = (int) paper.getImageableY();
            bottomMargin = (int) paper.getHeight() - (int) paper.getImageableHeight() - topMargin;
        } else {
            page = new Page((int) paper.getWidth(), (int) paper.getHeight(), false);
            rightMargin = (int) paper.getImageableY();
            leftMargin = (int) paper.getHeight() - (int) paper.getImageableHeight() - rightMargin;
            topMargin = (int) paper.getImageableX();
            bottomMargin = (int) paper.getWidth() - (int) paper.getImageableWidth() - topMargin;
        }

        builder.setPageSizeAndOrientation(page);
        builder.setMargins(topMargin, bottomMargin, leftMargin, rightMargin);

        return page;
    }

    public final Preferences getPreferences() {
        return Preferences.userNodeForPackage(getClass()).node(getClass().getSimpleName());
    }

    /**
     * Creates a JasperPrint object.
     * 
     * @param formatForCSV <code>true<code> if the report should be formated for CSV export
     * @return JasperPrint object
     */
    public abstract JasperPrint createJasperPrint(final boolean formatForCSV);

    /**
     * Creates a report control panel. May return null if a panel is not used The ReportController is responsible for
     * dynamic report options with the exception of page format options
     * 
     * @return control panel
     */
    public abstract JPanel getReportController();

    /**
     * Returns the name of the report
     * 
     * @return report name
     */
    public abstract String getReportName();

    /**
     * Returns the legend for the grand total
     * 
     * @return report name
     */
    protected abstract String getGrandTotalLegend();

    /**
     * Returns the general label for the group footer
     * 
     * @return footer label
     */
    protected abstract String getGroupFooterLabel();

    /**
     * Returns the subtitle for the report
     * 
     * @return subtitle
     */
    protected String getSubTitle() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
        return df.format(new Date());
    }

    /**
     * Creates and add the default title to the report
     * 
     * @param drb <code>DynamicReportBuilder</code> to add title to
     */
    private void buildTitle(final DynamicReportBuilder drb) {
        Font font = ReportFactory.getDefaultProportionalFont(getBaseFontSize());

        Style titleStyle = new Style();
        titleStyle.setFont(ReportFactory.getDefaultProportionalFont(getBaseFontSize() + 8, true));
        titleStyle.setHorizontalAlign(HorizontalAlign.CENTER);
        titleStyle.setVerticalAlign(VerticalAlign.BOTTOM);

        Style subtitleStyle = new Style();
        font.setItalic(true);
        subtitleStyle.setFont(font);
        subtitleStyle.setHorizontalAlign(HorizontalAlign.CENTER);
        subtitleStyle.setVerticalAlign(VerticalAlign.TOP);

        drb.setTitleStyle(titleStyle);
        drb.setSubtitleStyle(subtitleStyle);
        drb.setTitle(getReportName());

        drb.setSubtitle(getSubTitle());
    }

    private Style getTypeHeaderStyle() {
        Style style = new Style();
        style.setFont(ReportFactory.getDefaultProportionalFont(getBaseFontSize() + 2, true));
        style.setHorizontalAlign(HorizontalAlign.LEFT);
        style.setBorderBottom(Border.THIN);
        style.setPaddingTop(getBaseFontSize());
        return style;
    }

    private Style getPageFooterStyle() {
        Style style = new Style();
        style.setFont(ReportFactory.getDefaultProportionalFont(getBaseFontSize(), false, true));
        return style;
    }

    private Style getTypeFooterStyle() {
        Style style = new Style();
        style.setFont(ReportFactory.getDefaultMonoFont(getBaseFontSize(), true));
        style.setHorizontalAlign(HorizontalAlign.RIGHT);
        style.setBorderTop(Border.THIN);
        return style;
    }

    private Style getGlobalFooterStyle() {
        Style style = new Style();
        style.setFont(ReportFactory.getDefaultMonoFont(getBaseFontSize(), true));
        style.setHorizontalAlign(HorizontalAlign.RIGHT);
        style.setBorderTop(Border.THIN);
        return style;
    }

    private Style getStyle(final ColumnStyle style, final boolean formatForCSV) {
        switch (style) {
            case SHORT_DATE:
            case STRING:
                Style stringStyle = new Style();
                stringStyle.setFont(ReportFactory.getDefaultMonoFont(getBaseFontSize()));
                stringStyle.setHorizontalAlign(HorizontalAlign.LEFT);
                if (!formatForCSV) {
                    stringStyle.setStretchWithOverflow(false);
                }
                return stringStyle;
            case AMOUNT_SUM:
            case BALANCE:
            case BALANCE_WITH_SUM:
            case BALANCE_WITH_SUM_AND_GLOBAL:
            case PERCENTAGE:
            case QUANTITY:
            case SHORT_AMOUNT:
                Style amountStyle = new Style();
                amountStyle.setFont(ReportFactory.getDefaultMonoFont(getBaseFontSize()));
                amountStyle.setHorizontalAlign(HorizontalAlign.RIGHT);
                if (!formatForCSV) {
                    amountStyle.setStretchWithOverflow(false);
                }
                return amountStyle;
            case CROSSTAB_TOTAL:
                Style totalStyle = new Style();
                totalStyle.setFont(ReportFactory.getDefaultMonoFont(getBaseFontSize(), true));
                totalStyle.setHorizontalAlign(HorizontalAlign.RIGHT);
                if (!formatForCSV) {
                    totalStyle.setStretchWithOverflow(false);
                }
                return totalStyle;
            case GROUP:
            case GROUP_NO_HEADER:
                return getTypeHeaderStyle();
        }
        System.err.println("Returning a null style");
        return null;
    }

    private Style getStyle(final ColumnHeaderStyle style, final boolean formatForCSV) {
        Style headerStyle = new Style();
        headerStyle.setFont(ReportFactory.getDefaultProportionalFont(getBaseFontSize() + 1, true));
        headerStyle.setHorizontalAlign(HorizontalAlign.RIGHT);
        headerStyle.setBorderTop(Border.THIN);
        headerStyle.setBorderBottom(Border.THIN);
        headerStyle.setBackgroundColor(Color.decode("#E0E9F1"));
        headerStyle.setTransparency(Transparency.OPAQUE);
        headerStyle.setVerticalAlign(VerticalAlign.MIDDLE);

        if (!formatForCSV) {
            headerStyle.setStretchWithOverflow(false);
        }

        switch (style) {
            case LEFT:
                headerStyle.setHorizontalAlign(HorizontalAlign.LEFT);
                break;
            case CENTER:
                headerStyle.setHorizontalAlign(HorizontalAlign.CENTER);
                break;
            case RIGHT:
                headerStyle.setHorizontalAlign(HorizontalAlign.RIGHT);
                break;
            case NONE:
                headerStyle.setHorizontalAlign(HorizontalAlign.JUSTIFY);
                break;
        }

        return headerStyle;
    }

    @SuppressWarnings("ConstantConditions")
    protected final JasperPrint createJasperPrint(final AbstractReportTableModel model, final boolean formatForCSV) {

        logger.info(rb.getString("Message.ProcessingReportData"));

        logger.info("Creating report builder");
        DynamicReportBuilder drb = new DynamicReportBuilder();

        try {
            if (formatForCSV) {
                drb.setIgnorePagination(true);
            }

            AbstractColumn[] columns = new AbstractColumn[model.getColumnCount()];

            assignPageFormat(drb);

            drb.setHeaderHeight(getStyle(model.getColumnHeaderStyle(0), formatForCSV).getFont().getFontSize() * 2);
            drb.setDetailHeight(getBaseFontSize() * 2);

            logger.info("Creating column model for report");
            // create columns and add to the list
            for (int i = 0; i < model.getColumnCount(); i++) {
                if (model.getColumnStyle(i) != ColumnStyle.GROUP && model.getColumnStyle(i) != ColumnStyle.GROUP_NO_HEADER) {
                    Style columnTypeStyle = getStyle(model.getColumnStyle(i), formatForCSV);
                    Style columnHeaderStyle = getStyle(model.getColumnHeaderStyle(i), formatForCSV);

                    int width = FontUtilities.getStringWidth(model.getColumnPrototypeValueAt(i), getTypeFooterStyle());
                    int hWidth = FontUtilities.getStringWidth(model.getColumnName(i), columnHeaderStyle);

                    if (hWidth > width) {
                        width = hWidth;
                    }

                    ColumnBuilder builder = ColumnBuilder.getNew();

                    builder.setColumnProperty(COLUMN_PROPERTY + i, model.getColumnClass(i).getName());
                    builder.setTitle(model.getColumnName(i));
                    builder.setWidth(width);
                    builder.setStyle(columnTypeStyle);
                    builder.setHeaderStyle(columnHeaderStyle);
                    builder.setTruncateSuffix("\u2026");

                    // set the format pattern for decimal values
                    if (model.getColumnStyle(i) == ColumnStyle.AMOUNT_SUM || model.getColumnStyle(i) == ColumnStyle.BALANCE || model.getColumnStyle(i) == ColumnStyle.BALANCE_WITH_SUM || model.getColumnStyle(i) == ColumnStyle.BALANCE_WITH_SUM_AND_GLOBAL || model.getColumnStyle(i) == ColumnStyle.CROSSTAB_TOTAL) {
                        String pattern = CommodityFormat.getFullNumberPattern(model.getCurrency());
                        builder.setPattern(pattern);
                    } else if (model.getColumnStyle(i) == ColumnStyle.PERCENTAGE) {
                        NumberFormat nf = ReportFactory.getPercentageFormat();
                        String pattern = ((DecimalFormat) nf).toPattern();
                        builder.setPattern(pattern);
                    } else if (model.getColumnStyle(i) == ColumnStyle.QUANTITY) {
                        NumberFormat nf = ReportFactory.getQuantityFormat();
                        String pattern = ((DecimalFormat) nf).toPattern();
                        builder.setPattern(pattern);
                    } else if (model.getColumnStyle(i) == ColumnStyle.SHORT_DATE) {
                        String pattern = ((SimpleDateFormat) DateUtils.getShortDateFormat()).toPattern();
                        builder.setPattern(pattern);
                    } else if (model.getColumnStyle(i) == ColumnStyle.SHORT_AMOUNT) {
                        String pattern = CommodityFormat.getShortNumberPattern(model.getCurrency());
                        builder.setPattern(pattern);
                    }

                    if (model.isColumnFixedWidth(i) && !formatForCSV) {
                        builder.setFixedWidth(true);
                    }

                    columns[i] = builder.build();
                } else if (model.getColumnStyle(i) == ColumnStyle.GROUP || model.getColumnStyle(i) == ColumnStyle.GROUP_NO_HEADER) {
                    ColumnBuilder builder = ColumnBuilder.getNew();
                    builder.setColumnProperty(COLUMN_PROPERTY + i, model.getColumnClass(i).getName());
                    builder.setTitle(model.getColumnName(i));
                    builder.setStyle(getTypeHeaderStyle());

                    columns[i] = builder.build();
                }
            }

            boolean group = false;
            boolean header = true;

            logger.info("Searching for report groups");
            // determine if a group needs to be created
            for (int i = 0; i < model.getColumnCount(); i++) {
                if (model.getColumnStyle(i) == ColumnStyle.GROUP) {
                    group = true;
                    break;
                } else if (model.getColumnStyle(i) == ColumnStyle.GROUP_NO_HEADER) {
                    group = true;
                    header = false;
                    break;
                }
            }

            if (group) { // group columns

                logger.info("Building report groups");

                GroupBuilder gb = new GroupBuilder();
                gb.setDefaultHeaderVariableStyle(getTypeHeaderStyle());
                gb.setDefaultFooterVariableStyle(getTypeFooterStyle());

                for (int i = 0; i < model.getColumnCount(); i++) {
                    if (model.getColumnStyle(i) == ColumnStyle.GROUP || model.getColumnStyle(i) == ColumnStyle.GROUP_NO_HEADER) {
                        gb.setCriteriaColumn((PropertyColumn) columns[i]);
                    } else if (model.getColumnStyle(i) == ColumnStyle.AMOUNT_SUM || model.getColumnStyle(i) == ColumnStyle.BALANCE_WITH_SUM || model.getColumnStyle(i) == ColumnStyle.BALANCE_WITH_SUM_AND_GLOBAL || model.getColumnStyle(i) == ColumnStyle.CROSSTAB_TOTAL) {
                        gb.addFooterVariable(columns[i], DJCalculation.SUM);
                    }
                }

                if (header) {
                    gb.setGroupLayout(GroupLayout.VALUE_IN_HEADER);
                } else {
                    gb.setGroupLayout(GroupLayout.EMPTY);
                }

                // adds a group footer label if it is not null or zero length
                if (getGroupFooterLabel() != null && getGroupFooterLabel().length() > 0) {
                    DJGroupLabel label = new DJGroupLabel(getGroupFooterLabel(), getTypeFooterStyle(), LabelPosition.LEFT);
                    gb.setFooterLabel(label);
                }

                DJGroup group1 = gb.build();

                boolean global = false;

                for (int i = 0; i < model.getColumnCount(); i++) {
                    AbstractColumn c = columns[i];

                    drb.addColumn(c);

                    if (model.getColumnStyle(i) == ColumnStyle.BALANCE_WITH_SUM_AND_GLOBAL || model.getColumnStyle(i) == ColumnStyle.CROSSTAB_TOTAL) {
                        drb.addGlobalFooterVariable(c, DJCalculation.SUM, getTypeFooterStyle());
                        global = true;
                    }
                }

                drb.addGroup(group1);

                if (global) {
                    drb.setGrandTotalLegendStyle(getGlobalFooterStyle());
                    drb.setGrandTotalLegend(getGrandTotalLegend());
                }
            } else { // no groups exist, just add the columns
                for (int i = 0; i < model.getColumnCount(); i++) {
                    drb.addColumn(columns[i]);
                }

            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);           
        }

        buildTitle(drb);

        drb.setUseFullPageWidth(true); // use the full page width

        if (!formatForCSV) {
            Style footerStyle = getPageFooterStyle();
            
            int halfWidth = (int)(getPageFormat().getWidth() * .5);
            
            AutoText date = new AutoText(AutoText.AUTOTEXT_CREATED_ON, AutoText.POSITION_FOOTER, HorizontalBandAlignment.LEFT, AutoText.PATTERN_DATE_DATE_TIME, halfWidth);
            date.setStyle(footerStyle);

            AutoText pageNum = new AutoText(AutoText.AUTOTEXT_PAGE_X_SLASH_Y, AutoText.POSITION_FOOTER, HorizontalBandAlignment.RIGHT, (byte) 0, 80, 50);
            pageNum.setStyle(footerStyle);

            drb.addAutoText(date);
            drb.addAutoText(pageNum);
        }

        logger.info(rb.getString("Message.CompilingReport"));

        DynamicReport dr = drb.build();

        logger.info(rb.getString("Message.ReportCompileComplete"));
        JRDataSource ds = new JRTableModelDataSource(model);

        JasperPrint jp = null;
        try {
            logger.info(rb.getString("Message.ReportCreateView"));

            jp = DynamicJasperHelper.generateJasperPrint(dr, new ClassicLayoutManager(), ds);
        } catch (final JRException e) {
            logger.log(Level.WARNING, "Exception", e);
            StaticUIMethods.displayError(rb.getString("Message.ReduceFont"));
        }

        return jp;
    }
}
