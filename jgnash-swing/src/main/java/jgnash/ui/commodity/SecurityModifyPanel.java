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
package jgnash.ui.commodity;

import com.jgoodies.forms.builder.ButtonBarBuilder2;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityNode;
import jgnash.message.Message;
import jgnash.message.MessageBus;
import jgnash.message.MessageChannel;
import jgnash.message.MessageListener;
import jgnash.message.MessageProperty;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.components.CurrencyComboBox;
import jgnash.ui.components.GenericCloseDialog;
import jgnash.ui.components.JIntegerField;
import jgnash.ui.components.JTextFieldEx;
import jgnash.ui.components.QuoteSourceComboBox;
import jgnash.ui.components.SortedListModel;
import jgnash.util.Resource;

/**
 * CommodityModifyPanel is used for modifying the currency list and exchange rates.
 *
 * @author Craig Cavanaugh
 *
 */
public class SecurityModifyPanel extends JPanel implements MessageListener, ActionListener {

    private final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
    private final Resource rb = Resource.get();
    private JButton newButton;
    private JList<SecurityNode> securityList;
    private JButton deleteButton;
    private JTextField symbolField;
    private JTextField isinField;
    private JTextField descriptionField;
    private JTextField scaleField;
    private JButton cancelButton;
    private JButton applyButton;
    private SortedListModel<SecurityNode> model;
    private CurrencyComboBox currencyCombo;
    private QuoteSourceComboBox sourceComboBox;
    private boolean isModifying = false;

    public static void showDialog(final JFrame parent) {
        final Resource rb = Resource.get();

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                GenericCloseDialog d = new GenericCloseDialog(parent, new SecurityModifyPanel(), rb.getString("Title.CreateModifyCommodities"));
                d.setMinimumSize(d.getSize());
                d.setLocationRelativeTo(parent);
                d.setVisible(true);
            }
        });
    }

    private SecurityModifyPanel() {
        layoutMainPanel();
        MessageBus.getInstance().registerListener(this, MessageChannel.COMMODITY);
    }

    private void initComponents() {
        securityList = new JList<>();
        securityList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        symbolField = new JTextFieldEx();
        descriptionField = new JTextFieldEx();
        scaleField = new JIntegerField();
        scaleField.setToolTipText(rb.getString("ToolTip.Scale"));

        isinField = new JTextFieldEx();
        isinField.setToolTipText(rb.getString("ToolTip.ISIN"));

        sourceComboBox = new QuoteSourceComboBox();

        currencyCombo = new CurrencyComboBox();
        currencyCombo.setSelectedNode(EngineFactory.getEngine(EngineFactory.DEFAULT).getDefaultCurrency());

        newButton = new JButton(rb.getString("Button.New"));
        deleteButton = new JButton(rb.getString("Button.Delete"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));
        applyButton = new JButton(rb.getString("Button.Apply"));

        applyButton.addActionListener(this);
        cancelButton.addActionListener(this);
        deleteButton.addActionListener(this);
        newButton.addActionListener(this);

        buildLists();
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p:g, 8dlu, p", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.appendSeparator(rb.getString("Title.CommoditiesSecurities"));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.appendRow(RowSpec.decode("fill:p:g"));
        builder.append(new JScrollPane(securityList), layoutRightPanel());
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(layoutButtonPanel(), 3);
    }

    private JPanel layoutRightPanel() {
        FormLayout layout = new FormLayout("right:p, $lcgap, 90dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setRowGroupingEnabled(true);

        builder.append(rb.getString("Label.Symbol"), symbolField);
        builder.append(rb.getString("Label.ISIN"), isinField);
        builder.append(rb.getString("Label.QuoteSource"), sourceComboBox);
        builder.append(rb.getString("Label.Description"), descriptionField);
        builder.append(rb.getString("Label.Scale"), scaleField);
        builder.append(rb.getString("Label.ReportedCurrency"), currencyCombo);
        return builder.getPanel();
    }

    private JPanel layoutButtonPanel() {
        ButtonBarBuilder2 builder = new ButtonBarBuilder2();

        builder.addButton(new JButton[] { newButton, deleteButton, cancelButton });
        builder.addUnrelatedGap();
        builder.addGlue();
        builder.addButton(applyButton);

        return builder.getPanel();
    }

    private void buildLists() {
        model = new SortedListModel<>(engine.getSecurities());
        ListListener listener = new ListListener();
        securityList.setModel(model);
        securityList.addListSelectionListener(listener);
    }

    void updateForm() {
        SecurityNode node = securityList.getSelectedValue();
        if (node != null) {
            symbolField.setText(node.getSymbol());
            isinField.setText(node.getISIN());
            descriptionField.setText(node.getDescription());
            scaleField.setText(Short.toString(node.getScale()));
            currencyCombo.setSelectedNode(node.getReportedCurrencyNode());
            sourceComboBox.setSelectedItem(node.getQuoteSource());
            isModifying = true;
        }
    }

    private void clearForm() {
        securityList.clearSelection();
        symbolField.setText(null);
        isinField.setText(null);
        descriptionField.setText(null);
        scaleField.setText(null);
        currencyCombo.setSelectedNode(EngineFactory.getEngine(EngineFactory.DEFAULT).getDefaultCurrency());
        isModifying = false;
    }

    private boolean validateForm() {
        if (scaleField.getText().equals("")) {
            scaleField.setText("2"); // force to something valid
        }

        if (symbolField.getText().equals("")) {
            return false;
        }

        if (currencyCombo.getSelectedNode() == null) {
            return false;
        }

        //        if (!sourceComboBox.getSelectedItem().equals(QuoteSource.NONE)) {
        //            if (isinField.getText().equals("")) {
        //                return false;
        //            }
        //        }

        return true;
    }

    private SecurityNode buildSecurityNode() {
        SecurityNode node = new SecurityNode(currencyCombo.getSelectedNode());

        node.setDescription(descriptionField.getText());
        node.setScale(Byte.parseByte(scaleField.getText()));
        node.setSymbol(symbolField.getText().trim());
        node.setISIN(isinField.getText().trim());
        node.setQuoteSource((QuoteSource) sourceComboBox.getSelectedItem());

        return node;
    }

    private void commitSecurityNode() {
        SecurityNode oldNode = securityList.getSelectedValue();

        if (validateForm()) {
            SecurityNode newNode = buildSecurityNode();
            if (isModifying && oldNode != null) {
                if (!engine.updateCommodity(oldNode, newNode)) {
                    StaticUIMethods.displayError(MessageFormat.format(rb.getString("Message.Error.SecurityUpdate"), newNode.getSymbol()));
                }
            } else {
                engine.addCommodity(newNode);
            }
        }
    }

    private void deleteSecurityNode() {
        SecurityNode node = securityList.getSelectedValue();
        if (node != null) {
            if (!engine.removeCommodity(node)) {
                 throw new RuntimeException("Unable to remove the security");
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == applyButton) {
            commitSecurityNode();
        } else if (e.getSource() == cancelButton) {
            clearForm();
        } else if (e.getSource() == deleteButton) {
            deleteSecurityNode();
        } else if (e.getSource() == newButton) {
            clearForm();
        }
    }

    private class ListListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent listSelectionEvent) {
            if (!listSelectionEvent.getValueIsAdjusting()) {
                if (listSelectionEvent.getSource() == securityList) {
                    updateForm();
                }
            }
        }
    }

    @Override
    public void messagePosted(final Message event) {

        if (event.getObject(MessageProperty.COMMODITY) instanceof SecurityNode) {

            final SecurityNode node = (SecurityNode) event.getObject(MessageProperty.COMMODITY);

            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    switch (event.getEvent()) {
                        case CURRENCY_REMOVE:
                            model.removeElement(node);
                            clearForm();
                            break;
                        case CURRENCY_REMOVE_FAILED:
                            String message = "Commodity " + node.toString() + " cannot be removed";
                            JOptionPane.showMessageDialog(SecurityModifyPanel.this, message, rb.getString("Message.WarnCommodityInUse"), JOptionPane.WARNING_MESSAGE);
                            break;
                        case CURRENCY_ADD:
                            clearForm();
                            model.addElement(node);
                            break;
                        case CURRENCY_ADD_FAILED:
                            JOptionPane.showMessageDialog(SecurityModifyPanel.this, rb.getString("Message.ErrorAddCommodity"), rb.getString("Title.Error"), JOptionPane.ERROR_MESSAGE);
                            break;
                        case CURRENCY_MODIFY:
                            clearForm();
                            model.removeElement(node); // force load of new instance
                            model.addElement(node);
                            model.fireContentsChanged();
                            break;
                        case CURRENCY_MODIFY_FAILED:
                            JOptionPane.showMessageDialog(SecurityModifyPanel.this, rb.getString("Message.ErrorModifyCommodity"), rb.getString("Title.Error"), JOptionPane.ERROR_MESSAGE);
                            break;
                        default:
                    }
                }
            });
        }
    }
}
