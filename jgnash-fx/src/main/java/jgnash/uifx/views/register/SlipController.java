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
package jgnash.uifx.views.register;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import jgnash.engine.Account;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.ReconcileManager;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;
import jgnash.uifx.StaticUIMethods;
import jgnash.util.NotNull;

/**
 * Transaction Entry Controller for Credits and Debits
 *
 * @author Craig Cavanaugh
 */
public class SlipController extends AbstractSlipController {

    @FXML
    protected Button cancelButton;

    @FXML
    protected Button enterButton;

    @FXML
    private Button splitsButton;

    @FXML
    private AccountExchangePane accountExchangePane;

    private SlipType slipType;

    private SplitTransactionDialog splitsDialog;

    private TransactionEntry modEntry = null;

    @FXML
    @Override
    public void initialize() {
        super.initialize();

        // Bind necessary properties to the exchange panel
        accountExchangePane.getBaseAccountProperty().bind(getAccountProperty());
        accountExchangePane.getAmountProperty().bindBidirectional(amountField.decimalProperty());
        accountExchangePane.getAmountEditable().bind(amountField.editableProperty());

        // Lazy init when account property is set
        accountProperty.addListener((observable, oldValue, newValue) -> {
            initializeSplitsDialog(); // initialize the splits dialog
        });
    }

    private void initializeSplitsDialog() {
        splitsDialog = new SplitTransactionDialog();
        splitsDialog.getAccountProperty().setValue(getAccountProperty().get());
    }

    void setSlipType(final SlipType slipType) {
        this.slipType = slipType;
    }

    @Override
    public void modifyTransaction(@NotNull final Transaction transaction) {
        if (transaction.areAccountsLocked()) {
            clearForm();
            StaticUIMethods.displayError(resources.getString("Message.TransactionModifyLocked"));
            return;
        }

        newTransaction(transaction); // load the form

        modTrans = transaction; // save reference to old transaction
        modTrans = attachmentPane.modifyTransaction(modTrans);

        if (!canModifyTransaction(transaction) && transaction.getTransactionType() == TransactionType.SPLITENTRY) {
            for (final TransactionEntry entry : transaction.getTransactionEntries()) {
                if (entry.getCreditAccount().equals(getAccountProperty().get()) || entry.getDebitAccount().equals(getAccountProperty().get())) {
                    modEntry = entry;
                    break;
                }
            }

            if (modEntry == null) {
                Logger logger = Logger.getLogger(SlipController.class.getName());
                logger.warning("Was not able to modify the transaction");
            }
        }
    }

    @Override
    public boolean validateForm() {
        boolean result = super.validateForm();

        if (result) {
            result =  accountExchangePane.getSelectedAccount() != null;
        }

        return result;
    }

    @Override
    Transaction buildTransaction() {

        Transaction transaction;

        final Date date = datePicker.getDate();

        if (splitsDialog.getTransactionEntries().size() > 0) { // build a split transaction
            transaction = new Transaction();

            transaction.setDate(date);
            transaction.setNumber(numberComboBox.getValue());
            transaction.setMemo(memoTextField.getText());
            transaction.setPayee(payeeTextField.getText());

            transaction.addTransactionEntries(splitsDialog.getTransactionEntries());
        } else {
            final int signum = amountField.getDecimal().signum();

            final Account selectedAccount;

            if (modTrans != null && modTrans.areAccountsHidden()) {
                selectedAccount = getOppositeSideAccount(modTrans);
            } else {
                selectedAccount = accountExchangePane.getSelectedAccount();
            }

            if (slipType == SlipType.DECREASE && signum >= 0 || slipType == SlipType.INCREASE && signum == -1) {
                if (hasEqualCurrencies()) {
                    transaction = TransactionFactory.generateDoubleEntryTransaction(selectedAccount,
                            accountProperty.get(), amountField.getDecimal().abs(), date, memoTextField.getText(),
                            payeeTextField.getText(), numberComboBox.getValue());
                } else {
                    transaction = TransactionFactory.generateDoubleEntryTransaction(selectedAccount,
                            accountProperty.get(), accountExchangePane.getExchangeAmountProperty().get().abs(),
                            amountField.getDecimal().abs().negate(), date, memoTextField.getText(),
                            payeeTextField.getText(), numberComboBox.getValue());
                }
            } else {
                if (hasEqualCurrencies()) {
                    transaction = TransactionFactory.generateDoubleEntryTransaction(accountProperty.get(),
                            selectedAccount, amountField.getDecimal().abs(), date, memoTextField.getText(),
                            payeeTextField.getText(), numberComboBox.getValue());
                } else {
                    transaction = TransactionFactory.generateDoubleEntryTransaction(accountProperty.get(),
                            selectedAccount, amountField.getDecimal().abs(),
                            accountExchangePane.getExchangeAmountProperty().get().abs().negate(), date,
                            memoTextField.getText(), payeeTextField.getText(), numberComboBox.getValue());
                }
            }
        }

        ReconcileManager.reconcileTransaction(accountProperty.get(), transaction,
                reconciledButton.isSelected() ? ReconciledState.CLEARED : ReconciledState.NOT_RECONCILED);

        return transaction;
    }

    boolean hasEqualCurrencies() {
        return accountProperty.get().getCurrencyNode().equals(accountExchangePane.getSelectedAccount().getCurrencyNode());
    }

    Account getOppositeSideAccount(final Transaction t) {
        TransactionEntry entry = t.getTransactionEntries().get(0);

        if (entry.getCreditAccount().equals(accountProperty.get())) {
            return entry.getDebitAccount();
        }
        return entry.getCreditAccount();
    }

    void newTransaction(final Transaction t) {
        clearForm();

        amountField.setDecimal(t.getAmount(getAccountProperty().get()).abs());

        memoTextField.setText(t.getMemo());
        payeeTextField.setText(t.getPayee());
        numberComboBox.setValue(t.getNumber());

        datePicker.setDate(t.getDate());
        reconciledButton.setSelected(t.getReconciled(getAccountProperty().get()) != ReconciledState.NOT_RECONCILED);

        if (t.getTransactionType() == TransactionType.SPLITENTRY) {
            accountExchangePane.setSelectedAccount(t.getCommonAccount()); // display common account
            accountExchangePane.setEnabled(false); // disable it

            if (canModifyTransaction(t)) { // split common account is the same as the base account

                //  clone the splits for modification
                splitsDialog.getTransactionEntries().clear();

                for (final TransactionEntry entry : t.getTransactionEntries()) {
                    try {
                        splitsDialog.getTransactionEntries().add((TransactionEntry) entry.clone());
                    } catch (CloneNotSupportedException e) {
                        Logger.getLogger(SlipController.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                    }
                }
                amountField.setEditable(false);
                amountField.setDecimal(t.getAmount(getAccountProperty().get()).abs());
            } else { // not the same common account, can only modify the entry
                splitsButton.setDisable(true);
                payeeTextField.setEditable(false);
                numberComboBox.setDisable(true);
                datePicker.setEditable(false);

                amountField.setEditable(true);
                amountField.setDecimal(t.getAmount(getAccountProperty().get()).abs());

                for (final TransactionEntry entry : t.getTransactionEntries()) {
                    if (entry.getCreditAccount() == getAccountProperty().get()) {
                        accountExchangePane.setExchangedAmount(entry.getDebitAmount().abs());
                        break;
                    } else if (entry.getDebitAccount() == getAccountProperty().get()) {
                        accountExchangePane.setExchangedAmount(entry.getCreditAmount());
                        break;
                    }
                }
            }
        } else if (t instanceof InvestmentTransaction) {
            Logger logger = Logger.getLogger(SlipController.class.getName());
            logger.warning("unsupported transaction type");
        } else { // DoubleEntryTransaction
            accountExchangePane.setEnabled(!t.areAccountsHidden());

            amountField.setDisable(false);
            datePicker.setEditable(true);
        }

        // setup the accountCombo correctly
        if (t.getTransactionType() == TransactionType.DOUBLEENTRY) {
            TransactionEntry entry = t.getTransactionEntries().get(0);

            if (slipType == SlipType.DECREASE) {
                accountExchangePane.setSelectedAccount(entry.getCreditAccount());
                accountExchangePane.setExchangedAmount(entry.getCreditAmount());
            } else {
                accountExchangePane.setSelectedAccount(entry.getDebitAccount());
                accountExchangePane.setExchangedAmount(entry.getDebitAmount().abs());
            }
        }
    }

    @Override
    public void clearForm() {
        super.clearForm();

        splitsDialog.getTransactionEntries().clear();   // clear an old transaction entries

        modEntry = null;

        accountExchangePane.setEnabled(true);
        accountExchangePane.setExchangedAmount(null);

        splitsButton.setDisable(false);
    }

    boolean canModifyTransaction(final Transaction t) {
        boolean result = false;

        switch (t.getTransactionType()) {
            case DOUBLEENTRY:
                result = true;
                break;
            case SPLITENTRY:
                if (t.getCommonAccount().equals(accountProperty.get())) {
                    result = true;
                }
                break;
            default:
                break;
        }

        return result;
    }

    @FXML
    private void splitsAction() {
        splitsDialog.showAndWait();

        amountField.setEditable(splitsDialog.getTransactionEntries().size() == 0);
        amountField.setDecimal(splitsDialog.getBalance());
    }
}
