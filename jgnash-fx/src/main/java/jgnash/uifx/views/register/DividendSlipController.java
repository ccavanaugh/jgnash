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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

import javafx.fxml.FXML;

import jgnash.engine.AbstractInvestmentTransactionEntry;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionEntryDividendX;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionTag;
import jgnash.engine.TransactionType;
import jgnash.uifx.Options;
import jgnash.uifx.control.AutoCompleteTextField;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.SecurityComboBox;
import jgnash.uifx.control.TransactionNumberComboBox;
import jgnash.uifx.util.ValidationFactory;
import jgnash.uifx.views.main.MainApplication;
import jgnash.util.NotNull;

/**
 * Dividend entry controller
 *
 * @author Craig Cavanaugh
 */
public class DividendSlipController extends AbstractInvSlipController {

    @FXML
    DatePickerEx datePicker;

    @FXML
    AutoCompleteTextField<Transaction> memoTextField;

    @FXML
    TransactionNumberComboBox numberComboBox;

    @FXML
    SecurityComboBox securityComboBox;

    @FXML
    DecimalTextField dividendField;

    @FXML
    private AccountExchangePane accountExchangePane;

    @FXML
    private AccountExchangePane incomeExchangePane;

    @FXML
    protected AttachmentPane attachmentPane;

    private static final Logger logger = MainApplication.getLogger();

    @FXML
    public void initialize() {
        super.initialize();

        // Lazy init when account property is set
        accountProperty.addListener((observable, oldValue, newValue) -> {
            dividendField.scaleProperty().setValue(newValue.getCurrencyNode().getScale());
            dividendField.minScaleProperty().setValue(newValue.getCurrencyNode().getScale());

            accountExchangePane.baseCurrencyProperty().setValue(accountProperty().get().getCurrencyNode());
            incomeExchangePane.baseCurrencyProperty().setValue(accountProperty().get().getCurrencyNode());

            accountExchangePane.amountProperty().bindBidirectional(dividendField.decimalProperty());
            incomeExchangePane.amountProperty().bindBidirectional(dividendField.decimalProperty());

            clearForm();
        });

        securityComboBox.accountProperty().bind(accountProperty);
    }

    @Override
    protected void focusFirstComponent() {
        securityComboBox.requestFocus();
    }

    @NotNull
    @Override
    public Transaction buildTransaction() {
        BigDecimal incomeExchangedAmount = dividendField.getDecimal().negate();

        BigDecimal accountExchangedAmount = dividendField.getDecimal();

        if (!incomeExchangePane.getSelectedAccount().getCurrencyNode().equals(accountProperty().get().getCurrencyNode())) {
            incomeExchangedAmount = incomeExchangePane.exchangeAmountProperty().get().negate();
        }

        if (!accountExchangePane.getSelectedAccount().getCurrencyNode().equals(accountProperty().get().getCurrencyNode())) {
            accountExchangedAmount = accountExchangePane.exchangeAmountProperty().get();
        }

        final Transaction transaction = TransactionFactory.generateDividendXTransaction(incomeExchangePane.getSelectedAccount(),
                accountProperty().get(), accountExchangePane.getSelectedAccount(), securityComboBox.getValue(),
                dividendField.getDecimal(), incomeExchangedAmount, accountExchangedAmount, datePicker.getValue(),
                memoTextField.getText());

        transaction.setNumber(numberComboBox.getValue());

        return transaction;
    }

    @Override
    public void modifyTransaction(@NotNull Transaction transaction) {
        if (transaction.getTransactionType() != TransactionType.DIVIDEND) {
            throw new IllegalArgumentException(resources.getString("Message.Error.InvalidTransactionType"));
        }

        clearForm();

        datePicker.setValue(transaction.getLocalDate());
        numberComboBox.setValue(transaction.getNumber());

        final List<TransactionEntry> entries = transaction.getTransactionEntries();

        for (TransactionEntry e : entries) {
            if (e instanceof TransactionEntryDividendX) {
                AbstractInvestmentTransactionEntry entry = (AbstractInvestmentTransactionEntry) e;

                memoTextField.setText(e.getMemo());
                securityComboBox.setSecurityNode(entry.getSecurityNode());

                incomeExchangePane.setSelectedAccount(entry.getDebitAccount());
                incomeExchangePane.setExchangedAmount(entry.getDebitAmount().abs());

                dividendField.setDecimal(entry.getAmount(accountProperty().get()));
            } else if (e.getTransactionTag() == TransactionTag.INVESTMENT_CASH_TRANSFER) {
                accountExchangePane.setSelectedAccount(e.getCreditAccount());
                accountExchangePane.setExchangedAmount(e.getCreditAmount());
            } else {
                logger.warning("Invalid transaction");
            }
        }

        modTrans = transaction;
        modTrans = attachmentPane.modifyTransaction(modTrans);

        setReconciledState(transaction.getReconciled(accountProperty().get()));
    }

    @Override
    public void clearForm() {
        super.clearForm();

        if (!Options.rememberLastDateProperty().get()) {
            datePicker.setValue(LocalDate.now());
        }

        numberComboBox.setValue("");
        memoTextField.clear();
        dividendField.setDecimal(BigDecimal.ZERO);

        accountExchangePane.setSelectedAccount(accountProperty().get());
        incomeExchangePane.setSelectedAccount(accountProperty().get());
    }

    @Override
    public boolean validateForm() {
        if (securityComboBox.getValue() == null) {
            logger.warning(resources.getString("Message.Error.SecuritySelection"));
            ValidationFactory.showValidationError(securityComboBox,
                    resources.getString("Message.Error.SecuritySelection"));
            return false;
        }

        if (dividendField.getLength() == 0) {
            logger.warning(resources.getString("Message.Error.DividendValue"));
            ValidationFactory.showValidationError(dividendField,
                    resources.getString("Message.Error.DividendValue"));
            return false;
        }

        return true;
    }
}
