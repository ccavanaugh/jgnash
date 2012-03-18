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
package jgnash.ui.register;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.WindowConstants;

import jgnash.engine.EngineFactory;
import jgnash.ui.components.JTextFieldEx;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * Displays a list of default user configurable items for transaction numbers
 *
 * @author Craig Cavanaugh
 *
 */
public class TransactionNumberDialog extends JDialog implements ActionListener {

    private JList<String> list;

    private DefaultListModel<String> model;

    private JTextField entryField;

    private JButton insertButton;

    private JButton okButton;

    private JButton cancelButton;

    private JButton removeButton;

    private JButton upButton;

    private JButton downButton;

    public static void showDialog(final JFrame parent) {
        try {
            final TransactionNumberDialog dlg = new TransactionNumberDialog(parent);
            dlg.setVisible(true);
        } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, e.toString(), e);
        }
    }

    private TransactionNumberDialog(JFrame parent) {
        super(parent);
        setModal(true);

        initComponents();

        layoutDialog();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        DialogUtils.addBoundsListener(this);
    }

    private void initComponents() {

        final Resource rb = Resource.get();

        setTitle(rb.getString("Title.DefTranNum"));

        okButton = new JButton(rb.getString("Button.Ok"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));
        insertButton = new JButton(rb.getString("Button.Insert"));
        removeButton = new JButton(rb.getString("Button.Remove"));

        upButton = new JButton(Resource.getIcon("/jgnash/resource/stock_up-16.png"));
        downButton = new JButton(Resource.getIcon("/jgnash/resource/stock_down-16.png"));

        insertButton.addActionListener(this);
        cancelButton.addActionListener(this);
        okButton.addActionListener(this);
        removeButton.addActionListener(this);
        upButton.addActionListener(this);
        downButton.addActionListener(this);

        model = new DefaultListModel<>();
        final List<String> items = EngineFactory.getEngine(EngineFactory.DEFAULT).getTransactionNumberList();

        for (String s : items) {
            model.addElement(s);
        }

        list = new JList<>(model);

        entryField = new JTextFieldEx(10);
    }

    private void layoutDialog() {

        final FormLayout layout = new FormLayout("p:g", "f:p:g(1.0)");

        final DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.append(layoutPanel());
        builder.nextLine();

        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(ButtonBarFactory.buildOKCancelBar(okButton, cancelButton));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        pack();

        setMinimumSize(getSize());
    }

    private JPanel layoutPanel() {
        final FormLayout layout = new FormLayout("75dlu:g, 8dlu, p", "f:p:g(1.0)");

        final DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        final JScrollPane scrollPane = new JScrollPane(list);

        builder.append(scrollPane, layoutButtonPanel());
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(entryField, insertButton);
        return builder.getPanel();
    }

    private JPanel layoutButtonPanel() {
        final FormLayout layout = new FormLayout("p:g", "d, 6dlu, d, f:p:g(1.0), d");

        final DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(upButton);
        builder.nextLine();
        builder.nextLine();
        builder.append(downButton);
        builder.nextLine();
        builder.nextLine();
        builder.append(removeButton);

        return builder.getPanel();
    }

    private void close() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void okAction() {
        final ListModel<String> listModel = list.getModel();
        final int size = listModel.getSize();
        final List<String> items = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            items.add(listModel.getElementAt(i).toString());
        }

        EngineFactory.getEngine(EngineFactory.DEFAULT).setTransactionNumberList(items);
        close();
    }

    private void addAction() {
        if (entryField.getText().length() > 0) {
            if (list.getSelectedIndex() >= 0) {
                model.add(list.getSelectedIndex(), entryField.getText());
            } else {
                model.addElement(entryField.getText());
            }
            entryField.setText(null);
        }
    }

    private void removeAction() {
        if (list.getSelectedIndex() >= 0) {
            model.remove(list.getSelectedIndex());
        }
    }

    private void upAction() {
        if (list.getSelectedIndex() >= 1) {
            final int index = list.getSelectedIndex();
            model.add(index - 1, model.remove(index));
        }
    }

    private void downAction() {
        if (list.getSelectedIndex() < model.getSize() - 1) {
            final int index = list.getSelectedIndex();
            model.add(index + 1, model.remove(index));
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == okButton) {
            okAction();
        } else if (e.getSource() == cancelButton) {
            close();
        } else if (e.getSource() == insertButton) {
            addAction();
        } else if (e.getSource() == removeButton) {
            removeAction();
        } else if (e.getSource() == upButton) {
            upAction();
        } else if (e.getSource() == downButton) {
            downAction();
        }
    }
}
