/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
package jgnash.ui.budget;

import java.awt.Color;
import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.encoders.ImageEncoder;
import org.jfree.chart.encoders.ImageEncoderFactory;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleInsets;

/**
 * Sparkline Utility class for budgets
 * 
 * @author Craig Cavanaugh
 *
 */
final class BudgetSparkline {

    private static final String CATEGORY = "amount";

    private static final int DEFAULT_WIDTH = 120;

    private static final int DEFAULT_HEIGHT = 24;

    private static final Icon EMPTY_ICON = new ImageIcon(new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT, BufferedImage.TYPE_INT_RGB));

    private static final ImageEncoder ENCODER = ImageEncoderFactory.newInstance(ImageFormat.PNG, true);

    /**
     * static clear color constant to reduce GC
     */
    private static final Color CLEAR = new Color(0, 0, 0, 0);

    private static final RectangleInsets INSETS = new RectangleInsets(-1, -1, 0, 0);

    /**
     * Private constructor
     */
    private BudgetSparkline() {
    }

    public static Icon getSparklineImage(final List<BigDecimal> amounts) {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        final boolean[] negate = new boolean[amounts.size()];

        for (int i = 0; i < amounts.size(); i++) {
            dataset.addValue(amounts.get(i), CATEGORY, i);
            negate[i] = amounts.get(i).signum() == -1;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarksVisible(false);
        xAxis.setAxisLineVisible(false);
        xAxis.setVisible(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelsVisible(false);
        yAxis.setTickMarksVisible(false);
        yAxis.setAxisLineVisible(false);
        yAxis.setNegativeArrowVisible(false);
        yAxis.setPositiveArrowVisible(false);
        yAxis.setAutoRangeIncludesZero(true);
        yAxis.setAutoRange(true);
        yAxis.setVisible(false);

        BarRenderer renderer = new BarRenderer() {

            @Override
            public Paint getItemPaint(final int row, final int column) {
                return negate[column] ? Color.RED : Color.BLACK;
            }
        };

        renderer.setShadowVisible(false);
        renderer.setBarPainter(new StandardBarPainter());

        CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
        plot.setInsets(INSETS);
        plot.setDomainGridlinesVisible(false);
        plot.setDomainCrosshairVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.setRangeCrosshairVisible(false);
        plot.setBackgroundPaint(CLEAR);

        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBorderVisible(false);
        chart.setBackgroundPaint(CLEAR);

        Icon icon = EMPTY_ICON;

        try {
            byte[] image = ENCODER.encode(chart.createBufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT, BufferedImage.BITMASK, null));
            icon = new ImageIcon(image);
        } catch (IOException ex) {
            Logger.getLogger(BudgetSparkline.class.getName()).log(Level.SEVERE, null, ex);
        }

        return icon;
    }
}
