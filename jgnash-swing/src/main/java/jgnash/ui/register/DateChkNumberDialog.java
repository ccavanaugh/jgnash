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
package jgnash.ui.register;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import jgnash.engine.Account;
import jgnash.ui.UIApplication;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.TransactionNumberComboBox;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * A Dialog for getting a date and transaction number.
 * 
 * @author Craig Cavanaugh
 *
 */

class DateChkNumberDialog extends JDialog implements ActionListener {

    private final Resource rb = Resource.get();

    private JButton okButton;

    private JButton cancelButton;

    DatePanel datePanel;

    TransactionNumberComboBox numberCombo;

    private final Account account;

    private boolean result = false;

    /**
     * Creates new form AbstractDateChkNumberDialog
     * 
     * @param a the account for the transaction. This can be null.
     * @param title Title for the dialog
     */
    DateChkNumberDialog(final Account a, final String title) {
        super(UIApplication.getFrame(), ModalityType.APPLICATION_MODAL);

        setLocationRelativeTo(UIApplication.getFrame());

        setTitle(title);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        this.account = a;

        buildPanel();

        if (a != null) {
            numberCombo.setText(a.getNextTransactionNumber());
        }

        pack();
        setMinimumSize(getSize());

        DialogUtils.addBoundsListener(this);
    }

    /**
     * Closes the dialog
     */
    private void closeDialog() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void initComponents() {
        datePanel = new DatePanel();
        numberCombo = new TransactionNumberComboBox(account);

        okButton = new JButton(rb.getString("Button.Ok"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

        getRootPane().setDefaultButton(okButton);
    }

    private void buildPanel() {
        initComponents();

        FormLayout layout = new FormLayout("max(20dlu;d), 4dlu, 75dlu:grow(1.0)", "f:d, 3dlu, f:d, 10dlu, f:d");
        CellConstraints cc = new CellConstraints();

        layout.setRowGroups(new int[][] { { 1, 3, 5 } });

        JPanel p = new JPanel(layout);

        p.setBorder(Borders.DIALOG_BORDER);
        p.add(new JLabel(rb.getString("Label.Date")), cc.xy(1, 1));
        p.add(datePanel, cc.xy(3, 1));
        p.add(new JLabel(rb.getString("Label.Number")), cc.xy(1, 3));
        p.add(numberCombo, cc.xy(3, 3));
        p.add(ButtonBarFactory.buildOKCancelBar(okButton, cancelButton), cc.xyw(1, 5, 3));

        getContentPane().add(p, BorderLayout.CENTER);
    }

    void okAction() {
        result = true;
        closeDialog();
    }

    /**
     * Gets the date entered in the form
     * 
     * @return The date entered in the form
     */
    public Date getDate() {
        return datePanel.getDate();
    }

    /**
     * Gets the number entered in the form
     * 
     * @return The number entered in the form
     */
    public String getNumber() {
        return numberCombo.getText();
    }

    /**
     * @return true if closed with the Ok Button
     */
    public boolean getResult() {
        return result;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == okButton) {
            okAction();
        } else if (e.getSource() == cancelButton) {
            closeDialog();
        }
    }
}
