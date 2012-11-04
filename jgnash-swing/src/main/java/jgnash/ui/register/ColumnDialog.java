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
import com.jgoodies.forms.layout.RowSpec;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import jgnash.ui.UIApplication;
import jgnash.ui.components.SortedListModel;
import jgnash.ui.register.table.AbstractRegisterTableModel;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * This dialog will display the available columns in a transaction register and
 * allows the user to manipulate the visibility of the columns. If the dialog is
 * exited with an okAction, the changes are applied.
 *
 * @author Craig Cavanaugh
 *
 */
class ColumnDialog extends JDialog implements ActionListener {

    private final Resource rb = Resource.get();

    private JButton addButton;

    private JButton cancelButton;

    private JButton okButton;

    private JButton removeButton;

    private JList<String> showList;

    private JList<String> hideList;

    private SortedListModel<String> showModel;

    private SortedListModel<String> hideModel;

    private final AbstractRegisterTableModel model;

    private boolean result = false;

    /**
     * @param model register model
     */
    private ColumnDialog(AbstractRegisterTableModel model) {
        super(UIApplication.getFrame(), true); // must be modal for showDialog
        // to work properly
        setModal(true);
        setTitle(rb.getString("Title.ColVis"));
        this.model = model;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        layoutMainPanel();

        pack();
        DialogUtils.addBoundsListener(this);
    }

    static boolean showDialog(AbstractRegisterTableModel model) {
        ColumnDialog d = new ColumnDialog(model);
        d.setVisible(true);
        return d.result;
    }

    private void initComponents() {
        okButton = new JButton(rb.getString("Button.Ok"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));

        addButton = new JButton(rb.getString("Button.Add"));
        addButton.setIcon(Resource.getIcon("/jgnash/resource/list-add.png"));
        addButton.setHorizontalTextPosition(SwingConstants.LEADING);

        removeButton = new JButton(rb.getString("Button.Remove"));
        removeButton.setIcon(Resource.getIcon("/jgnash/resource/list-remove.png"));

        // generate the Jlists
        String[] names = model.getColumnNames();

        showModel = new SortedListModel<>();
        hideModel = new SortedListModel<>();

        for (int i = 0; i < names.length; i++) {
            if (model.isColumnVisible(i)) {
                showModel.addElement(names[i]);
            } else {
                hideModel.addElement(names[i]);
            }
        }

        showList = new JList<>(showModel);
        hideList = new JList<>(hideModel);
        showList.setPrototypeCellValue("prototypeCellValue");
        hideList.setPrototypeCellValue("prototypeCellValue");

        // install actionListener
        addButton.addActionListener(this);
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        removeButton.addActionListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("d:g(0.5), 8dlu, p, 8dlu, d:g(0.5)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendTitle(rb.getString("Title.Available"));
        builder.append("");
        builder.appendTitle(rb.getString("Title.Visible"));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.appendRow(RowSpec.decode("fill:80dlu:g"));
        builder.append(new JScrollPane(hideList));
        builder.append(buildCenterPanel());
        builder.append(new JScrollPane(showList));
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(ButtonBarFactory.buildOKCancelBar(okButton, cancelButton), 5);

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
    }

    private JPanel buildCenterPanel() {
        FormLayout layout = new FormLayout("p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(addButton);
        builder.append(removeButton);
        return builder.getPanel();
    }

    private void commitVisibility() {
        Object[] list = showModel.toArray();

        for (Object o : list) {
            model.setColumnVisible((String) o, true);
        }

        list = hideModel.toArray();
        for (Object o : list) {
            model.setColumnVisible((String) o, false);
        }
    }

    private void okAction() {
        result = true;
        closeDialog();
        commitVisibility();
    }

    private void addAction() {        
        for (String o : hideList.getSelectedValuesList()) {
            hideModel.removeElement(o);
            showModel.addElement(o);
        }
    }

    private void removeAction() {        
        for (String o : showList.getSelectedValuesList()) {
            showModel.removeElement(o);
            hideModel.addElement(o);
        }
    }

    /**
     * Closes the dialog
     */
    private void closeDialog() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    /*
      * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
      */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            okAction();
        } else if (e.getSource() == cancelButton) {
            closeDialog();
        } else if (e.getSource() == addButton) {
            addAction();
        } else if (e.getSource() == removeButton) {
            removeAction();
        }
    }
}