/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.ui.report.compiled;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.HighLowItemLabelGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.HighLowRenderer;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.DefaultHighLowDataset;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import javax.swing.JPanel;

import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.ui.components.GenericCloseDialog;
import jgnash.ui.components.SecurityComboBox;
import jgnash.util.Resource;

/**
 * Security High / Low Chart
 *
 * @author Craig Cavanaugh
 */
public class SecurityHighLowChart {

    private SecurityComboBox combo;

    private final Resource rb = Resource.get();

    private ChartPanel chartPanel;

    public static void show() {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {

                Resource rb = Resource.get();

                SecurityHighLowChart chart = new SecurityHighLowChart();
                JPanel p = chart.createPanel();

                GenericCloseDialog d = new GenericCloseDialog(p, rb.getString("Title.AccountBalance"));
                d.pack();
                d.setModal(false);
                d.setVisible(true);
            }
        });
    }

    private static JFreeChart createHighLowChart(String title, String timeAxisLabel, String valueAxisLabel, AbstractXYDataset data, boolean legend) {

        ValueAxis timeAxis = new DateAxis(timeAxisLabel);
        NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
        valueAxis.setAutoRangeIncludesZero(false);

        HighLowRenderer renderer = new HighLowRenderer();
        renderer.setBaseToolTipGenerator(new HighLowItemLabelGenerator());

        XYPlot plot = new XYPlot(data, timeAxis, valueAxis, renderer);

        return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
    }

    private static AbstractXYDataset createHighLowDataset(final SecurityNode node) {
        Objects.requireNonNull(node);

        List<SecurityHistoryNode> hNodes = node.getHistoryNodes();

        int count = hNodes.size();

        Date[] date = new Date[count];
        double[] high = new double[count];
        double[] low = new double[count];
        double[] open = new double[count];
        double[] close = new double[count];
        double[] volume = new double[count];

        for (int i = 0; i < count; i++) {
            SecurityHistoryNode hNode = hNodes.get(i);

            date[i] = hNode.getDate();
            high[i] = hNode.getHigh().doubleValue();
            low[i] = hNode.getLow().doubleValue();
            open[i] = hNode.getPrice().doubleValue();
            close[i] = hNode.getPrice().doubleValue();
            volume[i] = hNode.getVolume();
        }

        return new DefaultHighLowDataset(node.getDescription(), date, high, low, open, close, volume);
    }

    private void updateChart() {
        try {
            SecurityNode sNode = combo.getSelectedSecurityNode();

            if (sNode != null) {
                AbstractXYDataset dataset = createHighLowDataset(sNode);

                JFreeChart chart = createHighLowChart(sNode.getDescription(), rb.getString("Column.Date"), rb.getString("Column.Price"), dataset, false);

                chart.setBackgroundPaint(null);

                chartPanel.setChart(chart);
                chartPanel.validate();
            }
        } catch (Exception ex) {
            Logger.getAnonymousLogger().severe(ex.toString());
        }
    }

    JPanel createPanel() {

        combo = new SecurityComboBox();

        // create an empty chart for panel construction
        chartPanel = new ChartPanel(new JFreeChart(new XYPlot()));

        FormLayout layout = new FormLayout("p, 4dlu:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(combo);
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.appendRow(RowSpec.decode("fill:p:g"));
        builder.append(chartPanel, 2);

        combo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateChart();
            }
        });

        return builder.getPanel();
    }
}
