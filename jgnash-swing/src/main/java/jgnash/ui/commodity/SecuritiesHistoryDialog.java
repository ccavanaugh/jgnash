/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import jgnash.engine.CommodityNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.net.security.UpdateFactory;
import jgnash.text.CommodityFormat;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.FormattedJTable;
import jgnash.ui.components.JFloatField;
import jgnash.ui.components.JIntegerField;
import jgnash.ui.components.SecurityComboBox;
import jgnash.ui.util.DialogUtils;
import jgnash.ui.util.IconUtils;
import jgnash.time.DateUtils;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

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

    private HistoryTable table;

    private JButton updateButton;

    private ChartPanel chartPanel;

    private final ResourceBundle rb = ResourceUtils.getBundle();

    public static void showDialog(final JFrame parent) {

        EventQueue.invokeLater(() -> {
            SecuritiesHistoryDialog d = new SecuritiesHistoryDialog(parent);
            d.setVisible(true);
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

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                model.unregisterListeners();
            }
        });
    }

    private void initComponents() {
        dateField = new DatePanel();
        closeField = new JFloatField();
        lowField = new JFloatField();
        highField = new JFloatField();
        securityCombo = new SecurityComboBox();
        volumeField = new JIntegerField();

        updateButton = new JButton(rb.getString("Button.UpdateOnline"), IconUtils.getIcon("/jgnash/resource/applications-internet.png"));

        deleteButton = new JButton(rb.getString("Button.Delete"));
        clearButton = new JButton(rb.getString("Button.Clear"));
        applyButton = new JButton(rb.getString("Button.Add"));
        closeButton = new JButton(rb.getString("Button.Close"));

        model = new HistoryModel();

        table = new HistoryTable();
        table.setModel(model);
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
        dateField.setDate(LocalDate.now());
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
        final SecurityNode node = securityCombo.getSelectedSecurityNode();

        if (node != null) {
            if (!UpdateFactory.updateOne(node)) {
                StaticUIMethods.displayWarning(ResourceUtils.getString("Message.Error.SecurityUpdate", node.getSymbol()));
            }
        }
    }

    private void addNode() {
        final SecurityHistoryNode history = new SecurityHistoryNode(dateField.getLocalDate(), closeField.getDecimal(),
                volumeField.longValue(), highField.getDecimal(), lowField.getDecimal());

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            engine.addSecurityHistory(securityCombo.getSelectedSecurityNode(), history);
        }
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
        LocalDate[] temp = new LocalDate[selection.length];
        for (int i = 0; i < selection.length; i++) {
            temp[i] = history.get(selection[i]).getLocalDate();
        }

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            for (int i = selection.length - 1; i >= 0; i--) {
                engine.removeSecurityHistory(node, temp[i]);
            }
        }
    }

    private static JFreeChart createChart(final SecurityNode node) {
        Objects.requireNonNull(node);

        final List<SecurityHistoryNode> hNodes = node.getHistoryNodes();
        final Date max = DateUtils.asDate(hNodes.get(hNodes.size() - 1).getLocalDate());
        final Date min = DateUtils.asDate(hNodes.get(0).getLocalDate());

        final DateAxis timeAxis = new DateAxis(null);
        timeAxis.setVisible(false);
        timeAxis.setAutoRange(false);
        timeAxis.setRange(min, max);

        final NumberAxis valueAxis = new NumberAxis(null);
        valueAxis.setAutoRangeIncludesZero(false);
        valueAxis.setVisible(false);

        final XYAreaRenderer renderer = new XYAreaRenderer();
        renderer.setBaseToolTipGenerator(new SecurityItemLabelGenerator(node));
        renderer.setOutline(true);
        renderer.setSeriesPaint(0, new Color(225, 247, 223));

        final XYPlot plot = new XYPlot(null, timeAxis, valueAxis, renderer);

        final List<List<SecurityHistoryNode>> groups = node.getHistoryNodeGroupsBySplits();

        for (int i = 0; i < groups.size(); i++) {
            int size = groups.get(i).size();

            Date[] date = new Date[size];
            double[] high = new double[size];
            double[] low = new double[size];
            double[] open = new double[size];
            double[] close = new double[size];
            double[] volume = new double[size];

            for (int j = 0; j < size; j++) {
                final SecurityHistoryNode hNode = groups.get(i).get(j);

                date[j] = DateUtils.asDate(hNode.getLocalDate());
                high[j] = hNode.getAdjustedHigh().doubleValue();
                low[j] = hNode.getAdjustedLow().doubleValue();
                open[j] = hNode.getAdjustedPrice().doubleValue();
                close[j] = hNode.getAdjustedPrice().doubleValue();
                volume[j] = hNode.getVolume();
            }

            final AbstractXYDataset data = new DefaultHighLowDataset(node.getDescription() + i, date, high, low, open,
                    close, volume);
            plot.setDataset(i, data);
        }

        plot.setInsets(new RectangleInsets(1, 1, 1, 1));

        final JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(null);

        return chart;
    }

    private void updateChart() {
        try {
            final SecurityNode sNode = securityCombo.getSelectedSecurityNode();

            final JFreeChart chart = createChart(sNode);

            chartPanel.setChart(chart);
            chartPanel.validate();

        } catch (final Exception ex) {
            Logger.getLogger(SecuritiesHistoryDialog.class.getName()).severe(ex.toString());
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

    private static class HistoryTable extends FormattedJTable {
        private final DateTimeFormatter dateTimeFormatter = DateUtils.getShortDateFormatter();

        private final NumberFormat volumeFormat = NumberFormat.getIntegerInstance();

        /**
         * Override prepareRenderer instead of using a custom renderer so the look and feel is preserved
         *
         * @see javax.swing.JTable#prepareRenderer(javax.swing.table.TableCellRenderer, int, int)
         */
        @Override
        public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {
            Component c = super.prepareRenderer(renderer, row, column);

            HistoryModel model = (HistoryModel) getModel();

            // column and row may have been reordered
            final Object value = model.getValueAt(convertRowIndexToModel(row), convertColumnIndexToModel(column));

            if (LocalDate.class.isAssignableFrom(getColumnClass(column)) && c instanceof JLabel) {
                if (value != null && value instanceof LocalDate) {
                    ((JLabel) c).setText(dateTimeFormatter.format((TemporalAccessor) value));
                }
            } else if (Long.class.isAssignableFrom(getColumnClass(column)) && c instanceof JLabel) {
                ((JLabel) c).setText(volumeFormat.format(value));
            } else if (BigDecimal.class.isAssignableFrom(getColumnClass(column)) && c instanceof JLabel) {
                final NumberFormat commodityFormatter = CommodityFormat.getShortNumberFormat(model.getNode().getReportedCurrencyNode());

                ((JLabel) c).setText(commodityFormatter.format(value));
            }

            return c;
        }
    }

    private class HistoryModel extends AbstractTableModel implements MessageListener {

        private SecurityNode node = null;

        List<SecurityHistoryNode> history;

        private final String[] cNames = {rb.getString("Column.Date"), rb.getString("Column.Close"),
                rb.getString("Column.Low"), rb.getString("Column.High"), rb.getString("Column.Volume")};

        private final Class<?>[] cClass = {LocalDate.class, BigDecimal.class, BigDecimal.class, BigDecimal.class,
                Long.class};

        public HistoryModel() {
            MessageBus.getInstance().registerListener(this, MessageChannel.COMMODITY);
        }

        private void unregisterListeners() {
            MessageBus.getInstance().unregisterListener(this, MessageChannel.COMMODITY);
            Logger.getLogger(HistoryModel.class.getName()).info("unregistered listeners");
        }

        public void setSecurity(final SecurityNode node) {
            this.node = node;

            history = node.getHistoryNodes();

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
                        return history.get(row).getLocalDate();
                    case 1:
                        return history.get(row).getPrice();
                    case 2:
                        return history.get(row).getLow();
                    case 3:
                        return history.get(row).getHigh();
                    case 4:
                        return history.get(row).getVolume();
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
            CommodityNode eNode = event.getObject(MessageProperty.COMMODITY);

            if (node != null) {
                if (node.equals(eNode)) {
                    EventQueue.invokeLater(() -> {
                        switch (event.getEvent()) {
                            case SECURITY_HISTORY_ADD:
                            case SECURITY_HISTORY_REMOVE:
                                history = node.getHistoryNodes();

                                fireTableDataChanged();
                                updateChart();
                                return;
                            default:
                        }
                    });
                }
            }
        }

        public SecurityNode getNode() {
            return node;
        }
    }
}
