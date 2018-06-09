/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.ReconcileManager;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.util.ValidationFactory;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Transaction Panel Handles the creation and modification of a transaction.
 *
 * @author Craig Cavanaugh
 * @author Don Brown
 * @author axnotizes
 * @author Pranay Kumar
 */
public class TransactionPanel extends AbstractExchangeTransactionPanel {

    private List<TransactionEntry> splits = null;

    private TransactionEntry modEntry = null;

    private final JButton splitsButton;

    public TransactionPanel(final Account account, final PanelType panelType) {
        super(account, panelType);

        splitsButton = new JButton(rb.getString("Button.Splits"));
                
        registerListener();

        layoutMainPanel();       
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("right:d, $lcgap, m:g, 8dlu, right:d, $lcgap, max(48dlu;min)", "f:d, $nlgap, f:d, $nlgap, f:d, $nlgap, f:d");
        layout.setRowGroups(new int[][]{{1, 3, 5, 7}});
        CellConstraints cc = new CellConstraints();

        setLayout(layout);
        setBorder(Borders.DIALOG);

        JPanel sub = buildHorizontalSubPanel("48dlu:g, $lcgap, d", accountPanel, splitsButton);

        add("Label.Payee", cc.xy(1, 1));
        add(payeeField, cc.xy(3, 1));
        add("Label.Number", cc.xy(5, 1));
        add(numberField, cc.xy(7, 1));

        add("Label.Account", cc.xy(1, 3));
        add(sub, cc.xy(3, 3));
        add("Label.Date", cc.xy(5, 3));
        add(datePanel, cc.xy(7, 3));

        add("Label.Memo", cc.xy(1, 5));
        add(memoField, cc.xy(3, 5));
        add("Label.Amount", cc.xy(5, 5));
        add(ValidationFactory.wrap(amountField), cc.xy(7, 5));

        add(createBottomPanel(), cc.xyw(1, 7, 7));
        
        clearForm();
    }
    
    private void registerListener() {
        splitsButton.addActionListener(this);
        splitsButton.addKeyListener(keyListener);        
    }

    @Override
    public void enterAction() {
        if (validateForm()) {
            if (modEntry != null && modTrans != null) {
                try {
                    final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                    Objects.requireNonNull(engine);

                    // clone the transaction
                    Transaction t = (Transaction) modTrans.clone();

                    // remove the modifying entry from the clone
                    t.removeTransactionEntry(modEntry);

                    // generate new TransactionEntry
                    TransactionEntry e = buildTransactionEntry();

                    // add it to the clone
                    t.addTransactionEntry(e);

                    ReconcileManager.reconcileTransaction(getAccount(), t, getReconciledState());

                    engine.removeTransaction(modTrans);
                    engine.addTransaction(t);

                    clearForm();
                    fireOkAction();
                    focusFirstComponent();
                } catch (CloneNotSupportedException e) {                   
                    Logger.getLogger(TransactionPanel.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            } else {
                super.enterAction();
            }

        }
    }

    private void splitsAction() {
        SplitsDialog dlg = SplitsDialog.getSplitsDialog(this, getAccount(), splits, panelType);
        dlg.setVisible(true);

        if (dlg.returnStatus) {
            splits = dlg.getSplits();
            if (!dlg.getSplits().isEmpty()) {
                accountPanel.setEnabled(false);
                amountField.setDecimal(dlg.getBalance().abs());
                amountField.setEditable(false);
            } else {
                accountPanel.setEnabled(true);
                amountField.setEditable(true);
            }
            splits = dlg.getSplits();

            if (splits.isEmpty()) {
                splits = null; // set to null to clear
            }

            if (RegisterFactory.getConcatenateMemos() && splits != null) {
                memoField.setEnabled(false);
                memoField.setText(Transaction.getMemo(splits));
            } else {
                memoField.setEnabled(true);
            }
        }
    }

    private TransactionEntry buildTransactionEntry() {
        TransactionEntry entry = new TransactionEntry();
        entry.setMemo(memoField.getText());

        int signum = amountField.getDecimal().signum();

        if (panelType == PanelType.DECREASE && signum >= 0 || panelType == PanelType.INCREASE && signum < 0) {
            entry.setCreditAccount(accountPanel.getSelectedAccount());
            entry.setDebitAccount(getAccount());

            if (hasEqualCurrencies()) {
                entry.setAmount(amountField.getDecimal().abs());
            } else {
                entry.setDebitAmount(accountPanel.getExchangedAmount().abs().negate());
            }
        } else {
            entry.setCreditAccount(getAccount());
            entry.setDebitAccount(accountPanel.getSelectedAccount());

            if (hasEqualCurrencies()) {
                entry.setAmount(amountField.getDecimal().abs());
            } else {
                entry.setCreditAmount(accountPanel.getExchangedAmount().abs());
            }
        }

        entry.setReconciled(account, getReconciledState());

        return entry;
    }

    @Override
    protected Transaction buildTransaction() {
        Transaction transaction;

        if (splits != null) { // build a split transaction
            transaction = new Transaction();

            transaction.setDate(datePanel.getLocalDate());
            transaction.setNumber(numberField.getText());

            //transaction.setMemo(memoField.getText());
            transaction.setMemo(RegisterFactory.getConcatenateMemos() ? Transaction.CONCATENATE
                    : memoField.getText());

            transaction.setPayee(payeeField.getText());

            transaction.addTransactionEntries(splits);
        } else { // double entry transaction
            final int signum = amountField.getDecimal().signum();

            final Account selectedAccount;

            if (modTrans != null && modTrans.areAccountsHidden()) {
                selectedAccount = getOppositeSideAccount(modTrans);
            } else {
                selectedAccount = accountPanel.getSelectedAccount();
            }

            if (panelType == PanelType.DECREASE && signum >= 0 || panelType == PanelType.INCREASE && signum == -1) {
                if (hasEqualCurrencies()) {
                    transaction = TransactionFactory.generateDoubleEntryTransaction(selectedAccount, getAccount(),
                            amountField.getDecimal().abs(), datePanel.getLocalDate(), memoField.getText(),
                            payeeField.getText(), numberField.getText());
                } else {
                    transaction = TransactionFactory.generateDoubleEntryTransaction(selectedAccount, getAccount(),
                            accountPanel.getExchangedAmount().abs(), amountField.getDecimal().abs().negate(),
                            datePanel.getLocalDate(), memoField.getText(), payeeField.getText(), numberField.getText());
                }
            } else {
                if (hasEqualCurrencies()) {
                    transaction = TransactionFactory.generateDoubleEntryTransaction(getAccount(), selectedAccount,
                            amountField.getDecimal().abs(), datePanel.getLocalDate(), memoField.getText(),
                            payeeField.getText(), numberField.getText());
                } else {
                    transaction = TransactionFactory.generateDoubleEntryTransaction(getAccount(), selectedAccount,
                            amountField.getDecimal().abs(), accountPanel.getExchangedAmount().abs().negate(),
                            datePanel.getLocalDate(), memoField.getText(), payeeField.getText(), numberField.getText());
                }
            }
        }

        ReconcileManager.reconcileTransaction(getAccount(), transaction, getReconciledState());

        return transaction;
    }

    /**
     * Modifies an existing transaction.
     */
    @Override
    public void modifyTransaction(final Transaction t) {

        // check for a locked account and issue a warning if necessary
        if (t.areAccountsLocked()) {
            clearForm();
            StaticUIMethods.displayError(rb.getString("Message.TransactionModifyLocked"));
            return;
        }

        newTransaction(t); // load the form

        modTrans = t; // save reference to old transaction

        modTrans = attachmentPanel.modifyTransaction(modTrans);

        // disable editing if already concatenated
        memoField.setEnabled(!modTrans.isMemoConcatenated());

        if (!canModifyTransaction(t) && t.getTransactionType() == TransactionType.SPLITENTRY) {
            for (TransactionEntry entry : t.getTransactionEntries()) {
                if (entry.getCreditAccount().equals(getAccount()) || entry.getDebitAccount().equals(getAccount())) {
                    modEntry = entry;

                    memoField.setEnabled(true); // override to allow editing the entry
                    break;
                }
            }

            if (modEntry == null) {
                Logger logger = Logger.getLogger(TransactionPanel.class.getName());
                logger.warning("Was not able to modify the transaction");
            }
        }
    }

    void newTransaction(final Transaction t) {
        clearForm();

        splits = null;

        // handles any exchange rate that may exist
        amountField.setDecimal(t.getAmount(getAccount()).abs());

        // Must consider if this is a concatenated memo to set the field correctly
        memoField.setText(t.isMemoConcatenated() ? t.getMemo() : t.getTransactionMemo());

        payeeField.setText(t.getPayee());
        numberField.setText(t.getNumber());
        datePanel.setDate(t.getLocalDate());
        setReconciledState(t.getReconciled(getAccount()));

        if (t.getTransactionType() == TransactionType.SPLITENTRY) {

            accountPanel.setSelectedAccount(t.getCommonAccount()); // display common account
            accountPanel.setEnabled(false);

            if (canModifyTransaction(t)) { // split as the same base account
                //  clone the splits for modification

                splits = new ArrayList<>();
                for (TransactionEntry entry : t.getTransactionEntries()) {
                    try {
                        splits.add((TransactionEntry) entry.clone());
                    } catch (CloneNotSupportedException e) {
                        Logger.getLogger(TransactionPanel.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                    }
                }
                amountField.setEditable(false);
                amountField.setDecimal(t.getAmount(this.getAccount()).abs());
            } else { // not the same common account, can only modify the entry

                splitsButton.setEnabled(false);
                payeeField.setEditable(false);
                numberField.setEnabled(false);
                datePanel.setEditable(false);

                memoField.setText(t.getMemo(getAccount()));   // Override
                //memoField.setEnabled(true);

                amountField.setEditable(true);
                amountField.setDecimal(t.getAmount(this.getAccount()).abs());

                for (TransactionEntry entry : t.getTransactionEntries()) {
                    if (entry.getCreditAccount() == this.getAccount()) {
                        accountPanel.setExchangedAmount(entry.getDebitAmount().abs());
                        break;
                    } else if (entry.getDebitAccount() == this.getAccount()) {
                        accountPanel.setExchangedAmount(entry.getCreditAmount());
                        break;
                    }
                }
            }
        } else if (t instanceof InvestmentTransaction) {
            Logger logger = Logger.getLogger(TransactionPanel.class.getName());
            logger.warning("unsupported transaction type");
        } else { // DoubleEntryTransaction
            accountPanel.setEnabled(!t.areAccountsHidden());

            amountField.setEnabled(true);
            datePanel.setEditable(true);
        }

        // setup the accountCombo correctly
        if (t.getTransactionType() == TransactionType.DOUBLEENTRY) {
            TransactionEntry entry = t.getTransactionEntries().get(0);

            if (panelType == PanelType.DECREASE) {
                accountPanel.setSelectedAccount(entry.getCreditAccount());
                accountPanel.setExchangedAmount(entry.getCreditAmount());
            } else {
                accountPanel.setSelectedAccount(entry.getDebitAccount());
                accountPanel.setExchangedAmount(entry.getDebitAmount().abs());
            }
        }
    }

    private Account getOppositeSideAccount(final Transaction t) {
        TransactionEntry entry = t.getTransactionEntries().get(0);

        if (entry.getCreditAccount().equals(getAccount())) {
            return entry.getDebitAccount();
        }
        return entry.getCreditAccount();
    }

    /**
     * Determines if the transaction can be modified with this transaction panel.
     *
     * @param t The transaction to modify
     * @return True if the transaction can be modified in this panel
     */
    @Override
    protected boolean canModifyTransaction(final Transaction t) {
        boolean result = false; // fail unless proven otherwise

        switch (t.getTransactionType()) {
            case DOUBLEENTRY:
                final TransactionEntry entry = t.getTransactionEntries().get(0);

                // Prevent loading of a single entry scenario into the form.  The user may not detect it
                // and the engine will throw an exception if undetected.
                if (panelType == PanelType.DECREASE) {
                    if (!getAccount().equals(entry.getCreditAccount())) {
                        result = true;
                    }
                } else {
                    if (!getAccount().equals(entry.getDebitAccount())) {
                        result = true;
                    }
                }

                break;
            case SPLITENTRY:
                if (t.getCommonAccount().equals(getAccount())) {
                    result = true;
                }
                break;
            default:
                break;
        }

        return result;
    }

    @Override
    public void clearForm() {
        splits = null;
        modEntry = null;

        amountField.setEditable(true);
        accountPanel.setEnabled(true);
        splitsButton.setEnabled(true);
        datePanel.setEditable(true);

        numberField.setEnabled(true);
        getReconcileCheckBox().setEnabled(true);
        payeeField.setEditable(true);

        // Enable editing
        memoField.setEnabled(true);

        super.clearForm();
    }

    /**
     * Action notification.
     *
     * @param e the action event to process
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        super.actionPerformed(e);
        if (e.getSource() == splitsButton) {
            splitsAction();
        }
    }
}
