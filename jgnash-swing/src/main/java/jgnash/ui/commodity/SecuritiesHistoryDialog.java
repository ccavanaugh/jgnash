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
package jgnash.ui.commodity;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import jgnash.engine.CommodityNode;
import jgnash.engine.EngineFactory;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.message.Message;
import jgnash.message.MessageBus;
import jgnash.message.MessageChannel;
import jgnash.message.MessageListener;
import jgnash.message.MessageProperty;
import jgnash.net.security.SecurityUpdateFactory;
import jgnash.text.CommodityFormat;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.FormattedJTable;
import jgnash.ui.components.JFloatField;
import jgnash.ui.components.JIntegerField;
import jgnash.ui.components.SecurityComboBox;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.ui.RectangleInsets;

/**
 * A Dialog for manipulation security histories.
 * 
 * @author Craig Cavanaugh
 *
 */
public class SecuritiesHistoryDialog extends JDialog implements ActionListener {

    private DatePanel dateField;

    private JFloatField closeField;

    private JFloatField lowField;

    private JFloatField highField;

    private SecurityComboBox securityCombo;

    private JIntegerField volumeField;

    private HistoryModel model;

    private JButton closeButton;

    private JButton applyButton;

    private JButton clearButton;

    private JButton deleteButton;

    private JTable table;

    private JButton updateButton;

    private ChartPanel chartPanel;

    private final Resource rb = Resource.get();

    public static void showDialog(final JFrame parent) {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                SecuritiesHistoryDialog d = new SecuritiesHistoryDialog(parent);
                d.setVisible(true);
            }
        });
    }

    private SecuritiesHistoryDialog(final JFrame parent) {
        super(parent);
        setTitle(rb.getString("Title.ModifySecHistory"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().add(layoutMainPanel(), BorderLayout.CENTER);
        changeNode();

        pack();

        setMinimumSize(getSize());

        DialogUtils.addBoundsListener(SecuritiesHistoryDialog.this);
    }

    private void initComponents() {
        dateField = new DatePanel();
        closeField = new JFloatField();
        lowField = new JFloatField();
        highField = new JFloatField();
        securityCombo = new SecurityComboBox();
        volumeField = new JIntegerField();
        model = new HistoryModel();
        table = new FormattedJTable(model);

        updateButton = new JButton(rb.getString("Button.UpdateOnline"), Resource.getIcon("/jgnash/resource/applications-internet.png"));

        deleteButton = new JButton(rb.getString("Button.Delete"));
        clearButton = new JButton(rb.getString("Button.Clear"));
        applyButton = new JButton(rb.getString("Button.Add"));
        closeButton = new JButton(rb.getString("Button.Close"));

        table.setPreferredScrollableViewportSize(new Dimension(150, 120));

        table.setCellSelectionEnabled(false);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(true);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowSorter(new TableRowSorter<>(model));
        table.setFillsViewportHeight(true);

        // create an empty chart for panel construction
        chartPanel = new ChartPanel(new JFreeChart(new XYPlot()));
        chartPanel.setPreferredSize(new Dimension(150, 90));

        applyButton.addActionListener(this);
        clearButton.addActionListener(this);
        deleteButton.addActionListener(this);
        updateButton.addActionListener(this);
        securityCombo.addActionListener(this);
        closeButton.addActionListener(this);
    }

    private JPanel layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("r:p, $lcgap, max(75dlu;p):g(0.5), 8dlu, r:p, $lcgap, max(75dlu;p):g(0.5)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.border(Borders.DIALOG);

        builder.appendRow(RowSpec.decode("f:p:g"));
        builder.append(new JScrollPane(table), 7);
        builder.nextLine();
        builder.append(chartPanel, 7);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.rowGroupingEnabled(true);
        builder.append(rb.getString("Label.Security"), securityCombo, 5);
        builder.nextLine();
        builder.append(rb.getString("Label.Date"), dateField);
        builder.append("", updateButton);
        builder.nextLine();
        builder.append(rb.getString("Label.Close"), closeField);
        builder.append(rb.getString("Label.Volume"), volumeField);
        builder.nextLine();
        builder.append(rb.getString("Label.High"), highField);
        builder.append(rb.getString("Label.Low"), lowField);
        builder.rowGroupingEnabled(false);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(buildButtonBar(), 7);

        return builder.getPanel();
    }

    private JPanel buildButtonBar() {
        ButtonBarBuilder builder = new ButtonBarBuilder();

        builder.addButton(deleteButton, clearButton, applyButton);
        builder.addGlue();
        builder.addButton(closeButton);

        return builder.getPanel();
    }

    private void clearForm() {
        dateField.setDate(new Date());
        closeField.setDecimal(null);
        volumeField.setText(null);
        lowField.setDecimal(null);
        highField.setDecimal(null);
    }

    private void changeNode() {
        SecurityNode node = securityCombo.getSelectedSecurityNode();

        if (node != null) {

            updateChart();

            model.setSecurity(node);

            closeField.setScale(node.getScale());
            lowField.setScale(node.getScale());
            highField.setScale(node.getScale());

            updateButton.setEnabled(node.getQuoteSource() != QuoteSource.NONE);
        }
    }

    private void netAddNode() {
        SecurityNode node = securityCombo.getSelectedSecurityNode();

        if (node != null) {
            SecurityUpdateFactory.updateOne(node);
        }
    }

    private void addNode() {
        SecurityHistoryNode history = new SecurityHistoryNode();
        history.setDate(dateField.getDate());
        history.setPrice(closeField.getDecimal());
        history.setVolume(volumeField.longValue());

        if (highField.getText().length() > 0) {
            history.setHigh(highField.getDecimal());
        }

        if (lowField.getText().length() > 0) {
            history.setLow(lowField.getDecimal());
        }

        EngineFactory.getEngine(EngineFactory.DEFAULT).addSecurityHistory(securityCombo.getSelectedSecurityNode(), history);
    }

    /**
     * Delete the history nodes selected in the table model
     */
    private void removeNode() {
        SecurityNode node = securityCombo.getSelectedSecurityNode();

        int selection[] = table.getSelectedRows();

        for (int i = 0; i < selection.length; i++) {
            selection[i] = table.convertRowIndexToModel(selection[i]);
        }

        List<SecurityHistoryNode> history = node.getHistoryNodes();

        /* Capture a list of references */
        SecurityHistoryNode[] temp = new SecurityHistoryNode[selection.length];
        for (int i = 0; i < selection.length; i++) {
            temp[i] = history.get(selection[i]);
        }

        for (int i = selection.length - 1; i >= 0; i--) {
            EngineFactory.getEngine(EngineFactory.DEFAULT).removeSecurityHistory(node, temp[i]);
        }
    }

    private static JFreeChart createChart(final SecurityNode node) {

        assert node != null;

        // build the data set for the chart
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

        Date max = Collections.max(Arrays.asList(date));
        Date min = Collections.min(Arrays.asList(date));

        AbstractXYDataset data = new DefaultHighLowDataset(node.getDescription(), date, high, low, open, close, volume);

        DateAxis timeAxis = new DateAxis(null);
        timeAxis.setVisible(false);
        timeAxis.setAutoRange(false);
        timeAxis.setRange(min, max);

        NumberAxis valueAxis = new NumberAxis(null);
        valueAxis.setAutoRangeIncludesZero(false);
        valueAxis.setVisible(false);

        XYAreaRenderer renderer = new XYAreaRenderer();
        renderer.setBaseToolTipGenerator(new SecurityItemLabelGenerator(node));
        renderer.setOutline(true);
        renderer.setSeriesPaint(0, new Color(225, 247, 223));

        XYPlot plot = new XYPlot(data, timeAxis, valueAxis, renderer);
        plot.setInsets(new RectangleInsets(1, 1, 1, 1));

        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(null);

        return chart;
    }

    private void updateChart() {
        try {
            SecurityNode sNode = securityCombo.getSelectedSecurityNode();

            JFreeChart chart = createChart(sNode);

            chartPanel.setChart(chart);
            chartPanel.validate();

        } catch (Exception ex) {
            Logger.getAnonymousLogger().severe(ex.toString());
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == applyButton) {
            addNode();
        } else if (e.getSource() == clearButton) {
            clearForm();
        } else if (e.getSource() == deleteButton) {
            removeNode();
        } else if (e.getSource() == updateButton) {
            netAddNode();
        } else if (e.getSource() == securityCombo) {
            changeNode();
        } else if (e.getSource() == closeButton) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }

    private class HistoryModel extends DefaultTableModel implements MessageListener {

        private static final long serialVersionUID = -1928531589902526946L;

        SecurityNode node = null;

        List<SecurityHistoryNode> history;        

        private final NumberFormat volumeFormatter = NumberFormat.getIntegerInstance();

        private NumberFormat commodityFormatter;

        private final String[] cNames = { rb.getString("Column.Date"), rb.getString("Column.Close"),
                        rb.getString("Column.Low"), rb.getString("Column.High"), rb.getString("Column.Volume") };

        private final Class<?>[] cClass = { Date.class, BigDecimal.class, BigDecimal.class, BigDecimal.class,
                        Long.class };

        public HistoryModel() {
            MessageBus.getInstance().registerListener(this, MessageChannel.COMMODITY);
        }

        public void setSecurity(final SecurityNode node) {
            this.node = node;
            history = EngineFactory.getEngine(EngineFactory.DEFAULT).getSecurityHistory(node);

            commodityFormatter = CommodityFormat.getShortNumberFormat(node.getReportedCurrencyNode());

            fireTableDataChanged();
        }

        @Override
        public int getColumnCount() {
            return cNames.length;
        }

        @Override
        public String getColumnName(final int column) {
            return cNames[column];
        }

        @Override
        public Class<?> getColumnClass(final int column) {
            return cClass[column];
        }

        @Override
        public Object getValueAt(final int row, final int col) {
            if (node != null) {
                switch (col) {
                    case 0:
                        return history.get(row).getDate();
                    case 1:
                        return commodityFormatter.format(history.get(row).getPrice());
                    case 2:
                        return commodityFormatter.format(history.get(row).getLow());
                    case 3:
                        return commodityFormatter.format(history.get(row).getHigh());
                    case 4:
                        return volumeFormatter.format(history.get(row).getVolume());
                    default:
                        return "Error";
                }
            }
            return null;
        }

        @Override
        public int getRowCount() {
            if (node != null) {
                return history.size();
            }
            return 0;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public void messagePosted(final Message event) {
            CommodityNode eNode = (CommodityNode) event.getObject(MessageProperty.COMMODITY);

            if (node != null) {
                if (node.equals(eNode)) {
                    EventQueue.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            switch (event.getEvent()) {
                                case COMMODITY_HISTORY_ADD:
                                case COMMODITY_HISTORY_REMOVE:
                                    history = EngineFactory.getEngine(EngineFactory.DEFAULT).getSecurityHistory(node);
                                    fireTableDataChanged();
                                    updateChart();
                                    return;
                                default:
                            }
                        }
                    });
                }
            }
        }
    }
}
