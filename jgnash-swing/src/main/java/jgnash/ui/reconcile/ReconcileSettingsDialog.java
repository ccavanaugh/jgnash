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
package jgnash.ui.reconcile;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JDialog;

import jgnash.engine.Account;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.JFloatField;
import jgnash.ui.register.AccountBalanceDisplayManager;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * Account reconcile settings dialog.
 * 
 * @author Craig Cavanaugh
 *
 */
public class ReconcileSettingsDialog extends JDialog implements ActionListener {

    private final Resource rb = Resource.get();

    private final JButton okButton;

    private final JButton cancelButton;

    private final JFloatField openField = new JFloatField();

    private final JFloatField endField = new JFloatField();

    private final DatePanel datePanel = new DatePanel();

    private boolean returnValue = false;

    public ReconcileSettingsDialog(final Account account) {
        super(UIApplication.getFrame(), true);

        okButton = new JButton(rb.getString("Button.Ok"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));

        datePanel.setDate(account.getFirstUnreconciledTransactionDate());

        layoutMainPanel();

        setTitle(rb.getString("Title.ReconcileSettings"));
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

        openField.setDecimal(AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), account.getOpeningBalanceForReconcile()));
        endField.setDecimal(AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), account.getBalance()));

        DialogUtils.addBoundsListener(this);
    }

    private void layoutMainPanel() {
        final FormLayout layout = new FormLayout("p:g, $lcgap, max(70dlu;min)", "");
        final DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);
        builder.rowGroupingEnabled(true);

        builder.append(rb.getString("Label.OpenStateDate"), datePanel);
        builder.append(rb.getString("Label.OpeningBalance"), openField);
        builder.append(rb.getString("Label.EndingBalance"), endField);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildOKCancelBar(okButton, cancelButton), 3);

        getContentPane().add(builder.getPanel());
        pack();
    }

    public BigDecimal getEndingBalance() {
        return endField.getDecimal();
    }

    public BigDecimal getOpeningBalance() {
        return openField.getDecimal();
    }

    public Date getOpeningDate() {
        return datePanel.getDate();
    }

    /**
     * Invoked when an action occurs
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        returnValue = e.getSource() == okButton;
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    public boolean showDialog() {
        setVisible(true);
        return returnValue;
    }
}
