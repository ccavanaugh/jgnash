/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionListener;

import jgnash.engine.CommodityNode;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.components.JIntegerField;
import jgnash.ui.components.JTextFieldEx;
import jgnash.ui.components.SortedListModel;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * CurrencyModifyPanel is used for modifying currencies.
 * 
 * @author Craig Cavanaugh
 */
public class CurrencyModifyDialog extends JDialog implements MessageListener, ListSelectionListener, ActionListener {

    private final Resource rb = Resource.get();

    private SortedListModel<CurrencyNode> model;

    private CurrencyNode currentCurrency;

    private JTextField suffixField;

    private JList<CurrencyNode> sourceList;

    private JTextField symbolField;

    private JTextField prefixField;

    private JTextField descriptionField;

    private JTextField scaleField;

    private JButton clearButton;

    private JButton applyButton;

    private JButton closeButton;

    public static void showDialog() {

        EventQueue.invokeLater(() -> {
            CurrencyModifyDialog d = new CurrencyModifyDialog();
            DialogUtils.addBoundsListener(d);
            d.setVisible(true);
        });
    }

    private CurrencyModifyDialog() {
        super(UIApplication.getFrame(), true);
        setTitle(rb.getString("Title.ModifyCurrencies"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        layoutMainPanel();
        buildLists();
        registerListeners();
    }

    private void registerListeners() {
        MessageBus.getInstance().registerListener(this, MessageChannel.COMMODITY);
    }

    private void unregisterListeners() {
        MessageBus.getInstance().unregisterListener(this, MessageChannel.COMMODITY);
        currentCurrency = null;
    }

    @Override
    protected void processWindowEvent(final WindowEvent e) {
        super.processWindowEvent(e);

        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            unregisterListeners();
        }
    }

    private static Engine getEngine() {
        return EngineFactory.getEngine(EngineFactory.DEFAULT);
    }

    private void initComponents() {
        closeButton = new JButton(rb.getString("Button.Close"));

        sourceList = new JList<>();
        sourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        symbolField = new JTextFieldEx();
        descriptionField = new JTextFieldEx();
        scaleField = new JIntegerField();
        prefixField = new JTextFieldEx();
        suffixField = new JTextFieldEx();

        clearButton = new JButton(rb.getString("Button.Clear"));
        applyButton = new JButton(rb.getString("Button.Apply"));

        applyButton.addActionListener(this);
        clearButton.addActionListener(this);
        closeButton.addActionListener(this);
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
        builder.appendRow(RowSpec.decode("f:max(65dlu;p):g"));
        builder.append(layoutTopPanel());
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildCloseBar(closeButton));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        pack();

        setMinimumSize(getSize());
    }

    private JPanel layoutTopPanel() {
        FormLayout layout = new FormLayout("r:p, $lcgap, max(55dlu;p)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.rowGroupingEnabled(true);
        builder.append(rb.getString("Label.Symbol"), symbolField);
        builder.append(rb.getString("Label.Description"), descriptionField);
        builder.append(rb.getString("Label.Scale"), scaleField);
        builder.append(rb.getString("Label.Prefix"), prefixField);
        builder.append(rb.getString("Label.Suffix"), suffixField);

        JPanel right = builder.getPanel();

        layout = new FormLayout("max(60dlu;p):g, 8dlu, p", "");
        builder = new DefaultFormBuilder(layout);

        JScrollPane pane = new JScrollPane(sourceList);
        pane.setPreferredSize(new Dimension(50, 50));

        builder.appendRow(RowSpec.decode("f:max(35dlu;p):g"));
        builder.append(pane, right);

        builder.appendRelatedComponentsGapRow();
        builder.nextRow();
        builder.append(StaticUIMethods.buildLeftAlignedBar(clearButton, applyButton), 3);

        return builder.getPanel();
    }

    private void buildLists() {
        model = new SortedListModel<>(getEngine().getCurrencies());
        sourceList.setModel(model);
        sourceList.addListSelectionListener(this);

    }

    private void updateForm() {
        CurrencyNode node = sourceList.getSelectedValue();
        if (node != null) {
            symbolField.setText(node.getSymbol());
            symbolField.setEnabled(false);
            descriptionField.setText(node.getDescription());
            scaleField.setText(Short.toString(node.getScale()));
            prefixField.setText(node.getPrefix());
            suffixField.setText(node.getSuffix());
            currentCurrency = node;
        }
    }

    void clearForm() {
        sourceList.clearSelection();
        symbolField.setText(null);
        symbolField.setEnabled(true);
        descriptionField.setText(null);
        scaleField.setText(null);
        prefixField.setText(null);
        suffixField.setText(null);
        currentCurrency = null;
    }

    private boolean validateForm() {
        if (scaleField.getText().length() <= 0) {
            return false;
        }
        return !symbolField.getText().isEmpty();
    }

    private CurrencyNode buildCommodityNode() {
        CurrencyNode node = new CurrencyNode();
        node.setDescription(descriptionField.getText());
        node.setPrefix(prefixField.getText());
        node.setScale(Byte.parseByte(scaleField.getText()));
        node.setSuffix(suffixField.getText());
        if (currentCurrency != null) {
            node.setSymbol(currentCurrency.getSymbol());
        } else {
            node.setSymbol(symbolField.getText());
        }
        return node;
    }

    private void commitCommodityNode() {
        if (validateForm()) {

            CurrencyNode oldNode = sourceList.getSelectedValue();

            CurrencyNode newNode = buildCommodityNode();
            if (getEngine().getCurrency(newNode.getSymbol()) != null && oldNode != null) {
                if (!getEngine().updateCommodity(oldNode, newNode)) {
                    StaticUIMethods.displayError(rb.getString("Message.Error.CurrencyUpdate", newNode.getSymbol()));
                }
            } else {
                getEngine().addCurrency(newNode);
            }
        }
    }

    @Override
    public void valueChanged(javax.swing.event.ListSelectionEvent listSelectionEvent) {
        if (!listSelectionEvent.getValueIsAdjusting()) {
            if (listSelectionEvent.getSource() == sourceList) {
                updateForm();
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == applyButton) {
            commitCommodityNode();
        } else if (e.getSource() == clearButton) {
            clearForm();
        } else if (e.getSource() == closeButton) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }

    @Override
    public void messagePosted(final Message event) {
        final CommodityNode node = event.getObject(MessageProperty.COMMODITY);

        if (node instanceof CurrencyNode) {

            EventQueue.invokeLater(() -> {
                switch (event.getEvent()) {
                    case CURRENCY_REMOVE:
                        model.removeElement((CurrencyNode) node);
                        if (currentCurrency.equals(node)) {
                            clearForm();
                        }
                        break;
                    case CURRENCY_REMOVE_FAILED:
                        JOptionPane.showMessageDialog(CurrencyModifyDialog.this, rb.getString("Message.Warn.CurrencyInUse"), rb.getString("Title.Error"), JOptionPane.ERROR_MESSAGE);
                        break;
                    case CURRENCY_ADD:

                        clearForm();
                        model.addElement((CurrencyNode) node);

                        break;
                    case CURRENCY_ADD_FAILED:
                        JOptionPane.showMessageDialog(CurrencyModifyDialog.this, rb.getString("Message.Error.AddCurrency"), rb.getString("Title.Error"), JOptionPane.ERROR_MESSAGE);
                        break;
                    case CURRENCY_MODIFY:

                        // node will be stale
                        model.removeElement((CurrencyNode) node);
                        model.addElement(getEngine().getCurrency(node.getSymbol()));

                        if (currentCurrency.equals(node)) {
                            updateForm();
                        }

                        break;
                    case CURRENCY_MODIFY_FAILED:
                        JOptionPane.showMessageDialog(CurrencyModifyDialog.this, rb.getString("Message.Error.ModifyCurrency"), rb.getString("Title.Error"), JOptionPane.ERROR_MESSAGE);
                        break;
                    default:
                        break;
                }

            });
        }
    }
}
