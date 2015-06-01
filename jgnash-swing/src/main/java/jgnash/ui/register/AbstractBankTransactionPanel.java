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
package jgnash.ui.register;

import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ReconcileManager;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.ui.components.AutoCompleteFactory;
import jgnash.ui.components.AutoCompleteTextField;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.IndeterminateCheckBox;
import jgnash.ui.components.JFloatField;
import jgnash.ui.components.TransactionNumberComboBox;
import jgnash.ui.util.ValidationFactory;

/**
 * Abstract JPanel that implements common code used in all bank TransactionPanels. This class does not perform any
 * layout or container assignment.
 * <p/>
 * Any extending class may assign the KeyListener member to additional components to improve focus traversal.
 *
 * @author Craig Cavanaugh
 * @author axnotizes
 */
public abstract class AbstractBankTransactionPanel extends AbstractTransactionPanel implements ActionListener, MessageListener {

    final JButton enterButton;

    final JButton cancelButton;

    private final IndeterminateCheckBox reconciledButton;

    final JFloatField amountField;

    final DatePanel datePanel;

    final JTextField memoField;

    final JTextField payeeField;

    final TransactionNumberComboBox numberField;

    final Account account;

    Transaction modTrans;

    private boolean autoComplete = true;

    final AttachmentPanel attachmentPanel = new AttachmentPanel();

    /**
     * Abstract transaction panel
     *
     * @param account The account that this transaction panel will be used for
     */
    AbstractBankTransactionPanel(final Account account) {
        this.account = account;

        enterButton = new JButton(rb.getString("Button.Enter"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));

        amountField = new JFloatField(account.getCurrencyNode());
        datePanel = new DatePanel();
        memoField = AutoCompleteFactory.getMemoField();
        numberField = new TransactionNumberComboBox(account);
        payeeField = AutoCompleteFactory.getPayeeField(account);

        reconciledButton = new IndeterminateCheckBox(rb.getString("Button.Cleared"));
        reconciledButton.setHorizontalTextPosition(SwingConstants.LEADING);
        reconciledButton.setMargin(new Insets(0, 0, 0, 0));

        /* Connect the buttons to the form */
        registerListeners();

        /* Listen to the engine for change events */
        registerMessageBusListeners();

        /* Install the validating KeyListener */
        amountField.addKeyListener(keyListener);
        datePanel.getDateField().addKeyListener(keyListener);
        memoField.addKeyListener(keyListener);
        numberField.getEditor().getEditorComponent().addKeyListener(keyListener);
        payeeField.addKeyListener(keyListener);

        /* Allows the user to submit the form from the keyboard when the
        enter button is selected */
        enterButton.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    enterAction();
                }
            }
        });

        /* Only install the focus listener if the payee field is the correct
         * type; it could be a JTextField instead.
         */
        if (payeeField instanceof AutoCompleteTextField) {
            payeeField.addFocusListener(new PayeeFocusListener());
        }

        setFocusCycleRoot(true); // focus will return to the first field
    }

    private void registerListeners() {
        cancelButton.addActionListener(this);
        enterButton.addActionListener(this);
    }

    private void registerMessageBusListeners() {
        MessageBus.getInstance().registerListener(this, MessageChannel.TRANSACTION);
    }

    @Override
    protected IndeterminateCheckBox getReconcileCheckBox() {
        return reconciledButton;
    }

    Account getAccount() {
        return account;
    }

    /**
     * Clears the fields and modifying transaction
     */
    @Override
    public void clearForm() {
        amountField.setDecimal(null);

        if (!getRememberLastDate()) {
            datePanel.setDate(new Date());
        }

        memoField.setText(null);
        numberField.setText(null);
        payeeField.setText(null);
        reconciledButton.setSelected(false);

        attachmentPanel.clear();

        modTrans = null;
    }

    /**
     * Enables / Disables auto fill
     *
     * @param complete true to allow auto fill to happen
     */
    public void setAutoComplete(final boolean complete) {
        autoComplete = complete;
    }

    /**
     * Validates the form
     *
     * @return True if the form is valid
     */
    @Override
    protected boolean validateForm() {
        if (amountField.getText() != null && amountField.getText().isEmpty()) {
            ValidationFactory.showValidationError(rb.getString("Message.Error.Value"), amountField);
            return false;
        }

        return true;
    }

    /**
     * Method that is called when the cancel button is used
     */
    private void cancelAction() {
        clearForm();
        fireCancelAction();
        focusFirstComponent();
    }

    /**
     * Method that is called when the enter button is used
     */
    @Override
    public void enterAction() {
        if (validateForm()) {
            if (modTrans == null) {
                Transaction newTrans = buildTransaction();

                ReconcileManager.reconcileTransaction(getAccount(), newTrans, getReconciledState());

                newTrans = attachmentPanel.buildTransaction(newTrans);  // chain the transaction build

                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                if (engine != null) {
                    engine.addTransaction(newTrans);
                }
            } else {
                Transaction newTrans = buildTransaction();

                newTrans.setDateEntered(modTrans.getDateEntered());

                // restore the reconciled state of the previous old transaction
                for (Account a : modTrans.getAccounts()) {
                    if (!a.equals(getAccount())) {
                        ReconcileManager.reconcileTransaction(a, newTrans, modTrans.getReconciled(a));
                    }
                }

                /*
                 * Reconcile the modified transaction for this account.
                 * This must be performed last to ensure consistent results per the ReconcileManager rules
                 */
                ReconcileManager.reconcileTransaction(getAccount(), newTrans, getReconciledState());

                newTrans = attachmentPanel.buildTransaction(newTrans);  // chain the transaction build

                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                if (engine != null && engine.removeTransaction(modTrans)) {
                    engine.addTransaction(newTrans);
                }
            }
            clearForm();
            fireOkAction();
            focusFirstComponent();
        }
    }

    @Override
    public void messagePosted(final Message event) {
        EventQueue.invokeLater(() -> {
            if (event.getEvent() == ChannelEvent.TRANSACTION_REMOVE) {
                if (event.getObject(MessageProperty.TRANSACTION).equals(modTrans)) {
                    clearForm();
                }
            }
        });
    }

    /**
     * Action notification
     *
     * @param e the action event to process
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == cancelButton) {
            cancelAction();
        } else if (e.getSource() == enterButton) {
            enterAction();
        }
    }

    /**
     * Determines if the transaction can be modified with this transaction panel
     *
     * @param t The transaction to modify
     * @return True if the transaction can be modified in this panel
     */
    protected abstract boolean canModifyTransaction(Transaction t);

    /**
     * Modify a transaction before it is used to complete the panel for auto fill. The supplied transaction must be a
     * new or cloned transaction. It can't be a transaction that lives in the map. The returned transaction can be the
     * supplied reference or may be a new instance
     *
     * @param t The transaction to modify
     * @return the modified transaction
     */
    private Transaction modifyTransactionForAutoComplete(final Transaction t) {

        // tweak the transaction
        t.setNumber(null);
        t.setReconciled(ReconciledState.NOT_RECONCILED); // clear both sides        

        // set the last date as required
        if (!getRememberLastDate()) {
            t.setDate(new Date());
        } else {
            t.setDate(datePanel.getDate());
        }

        // preserve any transaction entries that may have been entered first
        if (!amountField.isEmpty() && !amountField.getText().isEmpty()) {
            Transaction newTrans = buildTransaction();
            t.clearTransactionEntries();
            t.addTransactionEntries(newTrans.getTransactionEntries());
        }

        // preserve any preexisting memo field info
        if (memoField.getText() != null && !memoField.getText().isEmpty()) {
            t.setMemo(memoField.getText());
        }

        // Do not copy over attachments
        t.setAttachment(null);

        return t;
    }

    /**
     * This class handles the auto fill of a Transaction. If it's a new transaction and the string in the "payee" field
     * can be found in a previous transaction, the rest of the transaction fields are populated with the values of the
     * previous transaction. The date and transaction fields are left alone.
     */
    private class PayeeFocusListener extends FocusAdapter {

        /**
         * @see FocusAdapter#focusLost(java.awt.event.FocusEvent)
         */
        @Override
        public void focusLost(final FocusEvent e) {
            if (modTrans == null && autoComplete) {
                AutoCompleteTextField f = (AutoCompleteTextField) payeeField;

                if (f.getText() != null && !f.getText().isEmpty()) {
                    final List transactions = f.getModel().getAllExtraInfo(f.getText());

                    for (final Object t : transactions) {
                        if (canModifyTransaction((Transaction) t)) {
                            try {
                                modifyTransaction(modifyTransactionForAutoComplete((Transaction) ((Transaction)t).clone()));
                            } catch (CloneNotSupportedException ex) {
                                Logger.getLogger(PayeeFocusListener.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
                            }
                            modTrans = null;
                            break;
                        }
                    }
                }
            }
        }
    }
}
