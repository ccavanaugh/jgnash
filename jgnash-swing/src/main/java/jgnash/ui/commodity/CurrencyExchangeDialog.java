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
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.MathConstants;
import jgnash.engine.ExchangeRate;
import jgnash.engine.ExchangeRateHistoryNode;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.net.currency.CurrencyUpdateFactory.ExchangeRateUpdateWorker;
import jgnash.ui.components.CurrencyComboBox;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.FormattedJTable;
import jgnash.ui.components.JFloatField;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * CurrencyModifyPanel is used for modifying the currencies and exchange rates.
 * <p/>
 * <b>Note:</b> By default the exchange rate history list is always returned with the exchange occurring in a set
 * direction. The rate must be inverted accordingly.
 * 
 * @author Craig Cavanaugh
 */
public class CurrencyExchangeDialog extends JDialog implements MessageListener, ActionListener, ListSelectionListener {

    private static final int DELAY_MILLIS = 1500;

    private final Resource rb = Resource.get();

    private CurrencyComboBox baseCurrencyCombo;

    private CurrencyComboBox exchangeCurrencyCombo;

    private Timer timer;

    private final static int DELAY = 250;

    private ExchangeRateUpdateWorker updateWorker;

    private final JFloatField rateField = new JFloatField(0, 6, 2);

    private JButton updateButton;

    private JButton deleteButton;

    private JButton stopButton;

    private JButton clearButton;

    private JButton addButton;

    private JProgressBar progressBar;

    private JButton closeButton;

    private DatePanel dateField;

    private JTable table;

    private HistoryModel model;

    public static void showDialog(final JFrame parent) {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                CurrencyExchangeDialog d = new CurrencyExchangeDialog(parent);
                DialogUtils.addBoundsListener(d);
                d.setVisible(true);
            }
        });
    }

    private CurrencyExchangeDialog(final JFrame parent) {
        super(parent, true);
        setTitle(rb.getString("Title.EditExchangeRates"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        layoutMainPanel();

        MessageBus.getInstance().registerListener(this, MessageChannel.COMMODITY);
    }

    private static Engine getEngine() {
        return EngineFactory.getEngine(EngineFactory.DEFAULT);
    }

    private void updateModel() {
        CurrencyNode base = baseCurrencyCombo.getSelectedNode();
        CurrencyNode exchange = exchangeCurrencyCombo.getSelectedNode();

        if (base != null && exchange != null) {
            ExchangeRate rate = getEngine().getExchangeRate(base, exchange);
            model.setExchangeRate(rate);
        }
    }

    private void initComponents() {

        baseCurrencyCombo = new CurrencyComboBox();
        exchangeCurrencyCombo = new CurrencyComboBox();

        model = new HistoryModel();

        dateField = new DatePanel();

        baseCurrencyCombo.addActionListener(this);
        exchangeCurrencyCombo.addActionListener(this);

        table = new FormattedJTable(model);
        table.setPreferredScrollableViewportSize(new java.awt.Dimension(150, 150));
        table.setCellSelectionEnabled(false);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(true);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowSorter(new TableRowSorter<>(model));
        table.setFillsViewportHeight(true);

        table.getSelectionModel().addListSelectionListener(this);

        closeButton = new JButton(rb.getString("Button.Close"));

        deleteButton = new JButton(rb.getString("Button.Delete"));
        clearButton = new JButton(rb.getString("Button.Clear"));

        addButton = new JButton(rb.getString("Button.Add"));
        updateButton = new JButton(rb.getString("Button.UpdateOnline"));
        updateButton.setIcon(Resource.getIcon("/jgnash/resource/applications-internet.png"));
        progressBar = new JProgressBar();
        stopButton = new JButton(rb.getString("Button.Stop"));
        stopButton.setIcon(Resource.getIcon("/jgnash/resource/process-stop.png"));
        stopButton.setEnabled(false);

        addButton.addActionListener(this);
        clearButton.addActionListener(this);
        closeButton.addActionListener(this);
        deleteButton.addActionListener(this);
        updateButton.addActionListener(this);
        stopButton.addActionListener(this);

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                updateModel();
            }
        });
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("f:p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);
        builder.appendSeparator(rb.getString("Title.Currencies"));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(layoutTopPanel());
        builder.appendSeparator(rb.getString("Title.ExchangeRate"));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.appendRow(RowSpec.decode("f:max(50dlu;p):g"));
        builder.append(layoutMiddlePanel());
        builder.appendSeparator();
        builder.append(layoutBottomPanel());
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(new ButtonBarBuilder().addGlue().addButton(clearButton).build());

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        pack();

        setMinimumSize(getSize());
    }

    private JPanel layoutTopPanel() {
        FormLayout layout = new FormLayout("p, $lcgap, p, 4dlu, p, $lcgap, p", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(rb.getString("Word.Exchange"), baseCurrencyCombo);
        builder.append(rb.getString("Word.Into"), exchangeCurrencyCombo);

        return builder.getPanel();
    }

    private JPanel layoutMiddlePanel() {
        FormLayout layout = new FormLayout("p, $lcgap, max(55dlu;p), 6dlu, p, $lcgap, max(45dlu;p), p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(rb.getString("Label.Date"), dateField);
        builder.append(rb.getString("Label.ExchangeRate"), rateField);
        builder.appendUnrelatedComponentsGapRow();
        builder.nextRow();

        builder.append(new ButtonBarBuilder().addButton(addButton, deleteButton, clearButton).build(), 8);

        builder.nextRow();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextRow();
        builder.appendRow(RowSpec.decode("f:p:g"));
        builder.append(new JScrollPane(table), 8);

        return builder.getPanel();
    }

    private JPanel layoutBottomPanel() {
        FormLayout layout = new FormLayout("p, 8dlu, 60dlu:g, 8dlu, p", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.append(updateButton, progressBar, stopButton);
        return builder.getPanel();
    }

    private void stopOnlineUpdate() {
        if (updateWorker != null && !updateWorker.isDone()) {
            updateWorker.cancel(false);
        }
    }

    private void updateForm() {

        int row = table.getSelectedRow();

        if (row >= 0) {
            ExchangeRateHistoryNode node = getSelectedExchangeRate().getHistory().get(row);

            rateField.setDecimal(node.getRate());
            dateField.setDate(node.getDate());
        }
    }

    private void addExchangeRate() {
        if (validateForm()) {

            CurrencyNode src = baseCurrencyCombo.getSelectedNode();
            CurrencyNode dst = exchangeCurrencyCombo.getSelectedNode();

            getEngine().setExchangeRate(src, dst, rateField.getDecimal(), dateField.getDate());

            clearForm();
        }
    }

    private ExchangeRate getSelectedExchangeRate() {
        CurrencyNode src = baseCurrencyCombo.getSelectedNode();
        CurrencyNode dst = exchangeCurrencyCombo.getSelectedNode();

        return getEngine().getExchangeRate(src, dst);
    }

    private void removeExchangeRates() {

        ExchangeRate rate = getSelectedExchangeRate();

        if (rate != null) {
            int rows[] = table.getSelectedRows();

            List<ExchangeRateHistoryNode> history = rate.getHistory();

            /* Capture a list of references */
            ExchangeRateHistoryNode[] temp = new ExchangeRateHistoryNode[rows.length];
            for (int i = 0; i < rows.length; i++) {
                temp[i] = history.get(rows[i]);
            }

            for (int i = rows.length - 1; i >= 0; i--) {
                EngineFactory.getEngine(EngineFactory.DEFAULT).removeExchangeRateHistory(rate, temp[i]);
            }
        }

    }

    private void clearForm() {
        table.clearSelection();
        rateField.setDecimal(null);
    }

    private boolean validateForm() {

        if (rateField.getText().isEmpty()) {
            return false;
        }

        if (rateField.getDecimal().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        return !dateField.getDateField().getText().isEmpty();

    }

    private void updateExchangeRates() {
        updateButton.setEnabled(false);
        stopButton.setEnabled(true);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);

        updateWorker = new ExchangeRateUpdateWorker();

        createTimer();
        timer.start();
        updateWorker.execute();
    }

    private void createTimer() {
        timer = new javax.swing.Timer(DELAY, new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent evt) {

                progressBar.setValue(updateWorker.getProgress());

                if (updateWorker.isDone()) {
                    progressBar.setValue(100);
                    timer.stop();
                    try {
                        Thread.sleep(DELAY_MILLIS);
                    } catch (Exception e) {
                        Logger.getLogger(CurrencyExchangeDialog.class.getName()).log(Level.INFO, e.getLocalizedMessage(), e);
                    }
                    timer = null;
                    stopButton.setEnabled(false);
                    updateButton.setEnabled(true);
                    progressBar.setValue(0);
                }
            }
        });
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == addButton) {
            addExchangeRate();
        } else if (e.getSource() == clearButton) {
            clearForm();
        } else if (e.getSource() == deleteButton) {
            removeExchangeRates();
        } else if (e.getSource() == updateButton) {
            updateExchangeRates();
        } else if (e.getSource() == stopButton) {
            stopOnlineUpdate();
        } else if (e.getSource() == closeButton) {
            MessageBus.getInstance().unregisterListener(this, MessageChannel.COMMODITY);
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getSource() == this.baseCurrencyCombo) {
            updateModel();
        } else if (e.getSource() == this.exchangeCurrencyCombo) {
            updateModel();
        }
    }

    @Override
    public void messagePosted(final Message event) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                switch (event.getEvent()) {
                    case EXCHANGE_RATE_ADD:
                    case EXCHANGE_RATE_REMOVE:
                        ExchangeRate rate = (ExchangeRate) event.getObject(MessageProperty.EXCHANGE_RATE);
                        if (rate.equals(getSelectedExchangeRate())) {
                            updateModel();
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private class HistoryModel extends DefaultTableModel {

        private ExchangeRate exchangeRate;

        private final String[] cNames = { rb.getString("Column.Date"), rb.getString("Column.ExchangeRate") };

        private final Class<?>[] cClass = { Date.class, BigDecimal.class };

        private final NumberFormat decimalFormat;

        private List<ExchangeRateHistoryNode> history = new ArrayList<>();

        private boolean invert = false;

        public HistoryModel() {
            decimalFormat = NumberFormat.getInstance();

            if (decimalFormat instanceof DecimalFormat) {
                decimalFormat.setMinimumFractionDigits(6);
                decimalFormat.setMaximumFractionDigits(6);
            }
        }

        public void setExchangeRate(final ExchangeRate exchangeRate) {
            this.exchangeRate = exchangeRate;

            if (exchangeRate == null) {
                history = null;
            } else {
                history = this.exchangeRate.getHistory();

                CurrencyNode base = baseCurrencyCombo.getSelectedNode();

                // do reported exchange values need inverted
                invert = !exchangeRate.getRateId().startsWith(base.getSymbol());
            }

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
        public int getRowCount() {
            if (history != null) {
                return history.size();
            }
            return 0;
        }

        @Override
        public boolean isCellEditable(final int row, final int column) {
            return false;
        }

        @Override
        public Object getValueAt(final int row, final int col) {
            if (history != null) {
                switch (col) {
                    case 0:
                        return history.get(row).getDate();
                    case 1:
                        BigDecimal rate = history.get(row).getRate();

                        if (invert) {
                            rate = BigDecimal.ONE.divide(rate, rate.scale(), MathConstants.roundingMode);
                        }
                        return decimalFormat.format(rate);
                    default:
                        return "Error";
                }
            }
            return null;
        }
    }

    @Override
    public void valueChanged(final ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            updateForm();
        }
    }
}
