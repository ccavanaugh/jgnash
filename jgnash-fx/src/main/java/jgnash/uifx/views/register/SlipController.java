/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
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

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.ReconcileManager;
import jgnash.engine.Tag;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;
import jgnash.uifx.Options;
import jgnash.uifx.StaticUIMethods;
import jgnash.util.LogUtil;
import jgnash.util.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Transaction Entry Controller for Credits and Debits.
 *
 * @author Craig Cavanaugh
 */
public class SlipController extends AbstractSlipController {

    private final SimpleListProperty<TransactionEntry> transactionEntries =
            new SimpleListProperty<>(FXCollections.observableArrayList());

    private final SimpleObjectProperty<TransactionEntry> modEntry = new SimpleObjectProperty<>();

    private final BooleanProperty concatenated = new SimpleBooleanProperty();

    @FXML
    protected Button cancelButton;

    @FXML
    protected Button enterButton;

    @FXML
    private Button splitsButton;

    @FXML
    private AccountExchangePane accountExchangePane;

    private SlipType slipType;

    @FXML
    @Override
    public void initialize() {
        super.initialize();

        validFormProperty.bind(amountField.validDecimalProperty()
                .and(Bindings.isNotNull(accountExchangePane.selectedAccountProperty()))
        );

        // Bind necessary properties to the exchange panel
        accountExchangePane.baseAccountProperty().bind(accountProperty());
        accountExchangePane.amountProperty().bindBidirectional(amountField.decimalProperty());
        accountExchangePane.amountEditableProperty().bind(amountField.editableProperty());

        // Bind the enter button, effectively negates the need for validation
        if (enterButton != null) {  // enter button may not have been initialized if used for an investment slip
            enterButton.disableProperty().bind(validFormProperty.not());
        }

        amountField.editableProperty().bind(transactionEntries.emptyProperty());
        accountExchangePane.disableProperty().bind(transactionEntries.emptyProperty().not()
                .or(modEntry.isNotNull()));

        memoTextField.disableProperty().bind(concatenated);
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

        tagPane.setSelectedTags(transaction.getTags(account.get()));

        // Set state of memo concatenation
        concatenated.setValue(modTrans.isMemoConcatenated());

        if (!canModifyTransaction(transaction) && transaction.getTransactionType() == TransactionType.SPLITENTRY) {
            for (final TransactionEntry entry : transaction.getTransactionEntries()) {
                if (entry.getCreditAccount().equals(accountProperty().get()) || entry.getDebitAccount().equals(accountProperty().get())) {
                    modEntry.setValue(entry);

                    concatenated.setValue(false); // override to allow editing the entry
                    break;
                }
            }

            if (modEntry.get() == null) {
                Logger logger = Logger.getLogger(SlipController.class.getName());
                logger.warning("Was not able to modify the transaction");
            }
        }
    }

    @NotNull
    @Override
    public Transaction buildTransaction() {

        Transaction transaction;

        final LocalDate date = datePicker.getValue();

        if (!transactionEntries.isEmpty()) { // build a split transaction
            transaction = new Transaction();

            transaction.setDate(date);
            transaction.setNumber(numberComboBox.getValue());
            transaction.setMemo(Options.concatenateMemosProperty().get() ? Transaction.CONCATENATE
                    : memoTextField.getText());
            transaction.setPayee(payeeTextField.getText());

            transaction.addTransactionEntries(transactionEntries);
        } else {  // double entry transaction
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
                            account.get(), amountField.getDecimal().abs(), date, memoTextField.getText(),
                            payeeTextField.getText(), numberComboBox.getValue());
                } else {
                    transaction = TransactionFactory.generateDoubleEntryTransaction(selectedAccount,
                            account.get(), accountExchangePane.exchangeAmountProperty().get().abs(),
                            amountField.getDecimal().abs().negate(), date, memoTextField.getText(),
                            payeeTextField.getText(), numberComboBox.getValue());
                }
            } else {
                if (hasEqualCurrencies()) {
                    transaction = TransactionFactory.generateDoubleEntryTransaction(account.get(),
                            selectedAccount, amountField.getDecimal().abs(), date, memoTextField.getText(),
                            payeeTextField.getText(), numberComboBox.getValue());
                } else {
                    transaction = TransactionFactory.generateDoubleEntryTransaction(account.get(),
                            selectedAccount, amountField.getDecimal().abs(),
                            accountExchangePane.exchangeAmountProperty().get().abs().negate(), date,
                            memoTextField.getText(), payeeTextField.getText(), numberComboBox.getValue());
                }
            }

            transaction.setTags(tagPane.getSelectedTags());
        }

        ReconcileManager.reconcileTransaction(account.get(), transaction, getReconciledState());

        return transaction;
    }

    private TransactionEntry buildTransactionEntry() {
        final TransactionEntry entry = new TransactionEntry();
        entry.setMemo(memoTextField.getText());

        final int signum = amountField.getDecimal().signum();

        if (slipType == SlipType.DECREASE && signum >= 0 || slipType == SlipType.INCREASE && signum < 0) {
            entry.setCreditAccount(accountExchangePane.getSelectedAccount());
            entry.setDebitAccount(account.get());

            if (hasEqualCurrencies()) {
                entry.setAmount(amountField.getDecimal().abs());
            } else {
                entry.setDebitAmount(accountExchangePane.exchangeAmountProperty().get().abs().negate());
            }
        } else {
            entry.setCreditAccount(account.get());
            entry.setDebitAccount(accountExchangePane.getSelectedAccount());

            if (hasEqualCurrencies()) {
                entry.setAmount(amountField.getDecimal().abs());
            } else {
                entry.setCreditAmount(accountExchangePane.exchangeAmountProperty().get().abs());
            }
        }

        entry.setTags(tagPane.getSelectedTags());

        entry.setReconciled(account.get(), getReconciledState());

        return entry;
    }

    private boolean hasEqualCurrencies() {
        return account.get().getCurrencyNode().equals(accountExchangePane.getSelectedAccount().getCurrencyNode());
    }

    private Account getOppositeSideAccount(final Transaction t) {
        TransactionEntry entry = t.getTransactionEntries().get(0);

        if (entry.getCreditAccount().equals(account.get())) {
            return entry.getDebitAccount();
        }
        return entry.getCreditAccount();
    }

    private void newTransaction(final Transaction t) {
        clearForm();

        amountField.setDecimal(t.getAmount(accountProperty().get()).abs());

        // Must consider if this is a concatenated memo to set the field correctly
        memoTextField.setText(t.isMemoConcatenated() ? t.getMemo() : t.getTransactionMemo());

        payeeTextField.setText(t.getPayee());
        numberComboBox.setValue(t.getNumber());
        datePicker.setValue(t.getLocalDate());
        setReconciledState(t.getReconciled(accountProperty().get()));

        if (t.getTransactionType() == TransactionType.SPLITENTRY) {

            if (canModifyTransaction(t)) { // split common account is the same as the base account

                //  clone the splits for modification
                transactionEntries.clear();

                for (final TransactionEntry entry : t.getTransactionEntries()) {
                    try {
                        transactionEntries.add((TransactionEntry) entry.clone());
                    } catch (final CloneNotSupportedException e) {
                        LogUtil.logSevere(SlipController.class, e);
                    }
                }

                tagPane.setSelectedTags(t.getTags());
                tagPane.setReadOnly(true);

                amountField.setDecimal(t.getAmount(accountProperty().get()).abs());
            } else { // not the same common account, can only modify the entry
                splitsButton.setDisable(true);
                payeeTextField.setEditable(false);
                numberComboBox.setDisable(true);
                datePicker.setEditable(false);

                memoTextField.setText(t.getMemo(account.get()));   // Override

                amountField.setDecimal(t.getAmount(accountProperty().get()).abs());

                for (final TransactionEntry entry : t.getTransactionEntries()) {
                    if (entry.getCreditAccount() == accountProperty().get()) {
                        accountExchangePane.setExchangedAmount(entry.getDebitAmount().abs());
                        tagPane.setSelectedTags(entry.getTags());
                        break;
                    } else if (entry.getDebitAccount() == accountProperty().get()) {
                        accountExchangePane.setExchangedAmount(entry.getCreditAmount());
                        tagPane.setSelectedTags(entry.getTags());
                        break;
                    }
                }

                tagPane.setSelectedTags(t.getTags(account.get()));
                tagPane.setReadOnly(false);
            }
        } else if (t instanceof InvestmentTransaction) {
            Logger logger = Logger.getLogger(SlipController.class.getName());
            logger.warning("unsupported transaction type");

        } else { // DoubleEntryTransaction
            datePicker.setEditable(true);
            tagPane.setSelectedTags(t.getTags(account.get()));
            tagPane.setReadOnly(false);
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

        // Not yet a split transaction
        concatenated.set(false);

        transactionEntries.clear();   // clear an old transaction entries

        modEntry.set(null);

        accountExchangePane.setExchangedAmount(null);

        splitsButton.setDisable(false);
    }

    @Override
    boolean canModifyTransaction(final Transaction t) {
        boolean result = false; // fail unless proven otherwise

        switch (t.getTransactionType()) {
            case DOUBLEENTRY:
                final TransactionEntry entry = t.getTransactionEntries().get(0);

                // Prevent loading of a single entry scenario into the form.  The user may not detect it
                // and the engine will throw an exception if undetected.
                if (slipType == SlipType.DECREASE) {
                    if (!accountProperty().get().equals(entry.getCreditAccount())) {
                        result = true;
                    }
                } else {
                    if (!accountProperty().get().equals(entry.getDebitAccount())) {
                        result = true;
                    }
                }

                break;
            case SPLITENTRY:
                if (t.getCommonAccount().equals(account.get())) {
                    result = true;
                }
                break;
            default:
                break;
        }

        return result;
    }

    @Override
    public void handleEnterAction() {
        if (modEntry.get() != null && modTrans != null) {
            try {
                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);

                // clone the transaction
                final Transaction t = (Transaction) modTrans.clone();

                // remove the modifying entry from the clone
                t.removeTransactionEntry(modEntry.get());

                // generate new TransactionEntry
                final TransactionEntry e = buildTransactionEntry();

                // add it to the clone
                t.addTransactionEntry(e);

                ReconcileManager.reconcileTransaction(account.get(), t, getReconciledState());

                if (engine.removeTransaction(modTrans)) {
                    engine.addTransaction(t);
                }

                clearForm();
                focusFirstComponent();
            } catch (CloneNotSupportedException e) {
                LogUtil.logSevere(SlipController.class, e);
            }
        } else {
            super.handleEnterAction();
        }
    }

    @FXML
    private void splitsAction() {
        final SplitTransactionDialog splitsDialog = new SplitTransactionDialog();
        splitsDialog.accountProperty().setValue(accountProperty().get());
        splitsDialog.getTransactionEntries().setAll(transactionEntries);

        final boolean wasSplit = transactionEntries.get().size() > 0;

        // Show the dialog and process the transactions when it closes
        splitsDialog.show(slipType, () -> {
            transactionEntries.setAll(splitsDialog.getTransactionEntries());

            if (transactionEntries.get().size() > 0) {
                amountField.setDecimal(splitsDialog.getBalance().abs());
            } else if (wasSplit) {
                amountField.setDecimal(BigDecimal.ZERO);    // spits were cleared out
            }

            // If valid splits exist and the user has requested concatenation, show a preview of what will happen
            concatenated.setValue(Options.concatenateMemosProperty().get()
                    && !transactionEntries.isEmpty());

            if (concatenated.get()) {
                memoTextField.setText(Transaction.getMemo(transactionEntries));
            }

            if (transactionEntries.get().size() > 0) {  // process the tags from the split transaction
                final Set<Tag> tags = new HashSet<>();

                for (final TransactionEntry entry : transactionEntries.get()) {
                    tags.addAll(entry.getTags());
                }

                tagPane.setSelectedTags(tags);
                tagPane.refreshTagView();
            } else if (wasSplit) {  // clear out all the prior split entries
                tagPane.clearSelectedTags();
            }
        });
    }
}
