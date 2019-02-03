/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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

import java.awt.event.ActionEvent;

import javax.swing.JButton;

import jgnash.engine.Account;
import jgnash.engine.ReconcileManager;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.account.AccountListDialog;
import jgnash.ui.util.IconUtils;
import jgnash.ui.util.ValidationFactory;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Handles making Single entry transactions.
 * 
 * @author Craig Cavanaugh
 * @author Don Brown
 * @author axnotizes
 */
public final class AdjustmentPanel extends AbstractBankTransactionPanel {

    private final JButton convertButton;

    AdjustmentPanel(final Account account) {
        super(account);

        convertButton = new JButton(IconUtils.getIcon("/jgnash/resource/edit-redo.png"));
        convertButton.setToolTipText(rb.getString("ToolTip.ConvertSEntry"));
        convertButton.addActionListener(this);

        layoutMainPanel();
        clearForm();
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("right:d, $lcgap, 50dlu:g, 8dlu, right:d, $lcgap, max(48dlu;min)", "f:d, $nlgap, f:d, $nlgap, f:d, $nlgap, f:d");

        layout.setRowGroups(new int[][] { { 1, 3, 5, 7 } });
        CellConstraints cc = new CellConstraints();

        setLayout(layout);
        setBorder(Borders.DIALOG);

        add("Label.Payee", cc.xy(1, 1));
        add(payeeField, cc.xy(3, 1));
        add("Label.Number", cc.xy(5, 1));
        add(numberField, cc.xy(7, 1));

        add("Label.Memo", cc.xy(1, 3));
        add(memoField, cc.xy(3, 3));
        add("Label.Date", cc.xy(5, 3));
        add(datePanel, cc.xy(7, 3));

        add(getReconcileCheckBox(), cc.xywh(1, 5, 3, 1));
        add("Label.Amount", cc.xy(5, 5));
        add(ValidationFactory.wrap(amountField), cc.xy(7, 5));

        add(StaticUIMethods.buildHelpBar(convertButton, enterButton, cancelButton), cc.xywh(1, 7, 7, 1));
    }

    @Override
    protected Transaction buildTransaction() {
        return TransactionFactory.generateSingleEntryTransaction(account, amountField.getDecimal(),
                datePanel.getLocalDate(), memoField.getText(), payeeField.getText(), numberField.getText());
    }

    @Override
    public void modifyTransaction(final Transaction t) {
        newTransaction(t); // load the transaction
        modTrans = t; // save a reference to the old transaction
        convertButton.setEnabled(true);
    }

    private void newTransaction(final Transaction t) {
        TransactionEntry entry = t.getTransactionEntries().get(0);

        memoField.setText(entry.getMemo());
        amountField.setDecimal(entry.getCreditAmount());

        datePanel.setDate(t.getLocalDate());
        numberField.setText(t.getNumber());
        payeeField.setText(t.getPayee());

        setReconciledState(t.getReconciled(getAccount()));
    }

    @Override
    public void clearForm() {
        super.clearForm();
        convertButton.setEnabled(false);
    }

    /**
     * Determines if the transaction can be modified with this transaction panel
     * 
     * @param t The transaction to modify
     * @return True if the transaction can be modified in this panel
     */
    @Override
    protected boolean canModifyTransaction(final Transaction t) {
        return t.getTransactionType() == TransactionType.SINGLENTRY;
    }

    /**
     * Helps convert an existing single entry transaction into a double entry transaction
     */
    private void convertAction() {
        AccountListDialog d = new AccountListDialog();
        d.disableAccount(getAccount());
        d.disablePlaceHolders();
        d.setVisible(true);

        if (!d.getReturnStatus()) {
            return;
        }

        Account opp = d.getAccount();
        Transaction t = new Transaction();

        t.setDate(datePanel.getLocalDate());
        t.setNumber(numberField.getText());
        t.setPayee(payeeField.getText());

        TransactionEntry entry = new TransactionEntry();
        entry.setMemo(memoField.getText());

        if (amountField.getDecimal().signum() >= 0) {
            entry.setCreditAccount(getAccount());
            entry.setDebitAccount(opp);
        } else {
            entry.setDebitAccount(getAccount());
            entry.setCreditAccount(opp);
        }

        entry.setCreditAmount(amountField.getDecimal().abs());
        entry.setDebitAmount(amountField.getDecimal().abs().negate());

        ReconcileManager.reconcileTransaction(getAccount(), t, getReconciledState());

        t.addTransactionEntry(entry);

        Transaction tran = TransactionDialog.showDialog(getAccount(), t);
        if (tran != null) {
            if (getEngine().removeTransaction(modTrans)) {
                getEngine().addTransaction(tran);
            }
            clearForm();
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        super.actionPerformed(e);

        if (e.getSource() == convertButton) {
            convertAction();
        }
    }
}
