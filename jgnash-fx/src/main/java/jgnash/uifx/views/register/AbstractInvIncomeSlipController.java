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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;

import jgnash.engine.AbstractInvestmentTransactionEntry;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionTag;
import jgnash.engine.TransactionType;
import jgnash.uifx.Options;
import jgnash.uifx.control.AutoCompleteTextField;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.SecurityComboBox;
import jgnash.uifx.control.TransactionNumberComboBox;
import jgnash.uifx.views.main.MainView;
import jgnash.util.NotNull;

/**
 * Abstract investment income entry controller.
 *
 * @author Craig Cavanaugh
 */
public abstract class AbstractInvIncomeSlipController extends AbstractInvSlipController {

    @FXML
    DatePickerEx datePicker;

    @FXML
    AutoCompleteTextField<Transaction> memoTextField;

    @FXML
    TransactionNumberComboBox numberComboBox;

    @FXML
    SecurityComboBox securityComboBox;

    @FXML
    DecimalTextField decimalTextField; // dividend field

    @FXML
    AccountExchangePane accountExchangePane;

    @FXML
    AccountExchangePane incomeExchangePane;

    @FXML
    protected AttachmentPane attachmentPane;

    private static final Logger logger = MainView.getLogger();

    @FXML
    @Override
    public void initialize() {
        super.initialize();

        // Lazy init when account property is set
        account.addListener((observable, oldValue, newValue) -> {
            decimalTextField.scaleProperty().set(newValue.getCurrencyNode().getScale());
            decimalTextField.minScaleProperty().set(newValue.getCurrencyNode().getScale());

            accountExchangePane.baseCurrencyProperty().set(accountProperty().get().getCurrencyNode());
            incomeExchangePane.baseCurrencyProperty().set(accountProperty().get().getCurrencyNode());

            accountExchangePane.amountProperty().bindBidirectional(decimalTextField.decimalProperty());
            incomeExchangePane.amountProperty().bindBidirectional(decimalTextField.decimalProperty());

            clearForm();
        });

        securityComboBox.accountProperty().bind(account);

        validFormProperty.bind(Bindings
                .isNotNull(securityComboBox.valueProperty())
                .and(decimalTextField.textProperty().isNotEmpty())
        );
    }

    @Override
    protected void focusFirstComponent() {
        securityComboBox.requestFocus();
    }

    @Override
    public void modifyTransaction(@NotNull Transaction transaction) {
        if (transaction.getTransactionType() != getTransactionType()) {
            throw new IllegalArgumentException(resources.getString("Message.Error.InvalidTransactionType"));
        }

        clearForm();

        datePicker.setValue(transaction.getLocalDate());
        numberComboBox.setValue(transaction.getNumber());

        final List<TransactionEntry> entries = transaction.getTransactionEntries();

        for (final TransactionEntry e : entries) {
            if (e instanceof AbstractInvestmentTransactionEntry
                    && ((AbstractInvestmentTransactionEntry) e).getTransactionType() == getTransactionType()) {

                memoTextField.setText(e.getMemo());
                securityComboBox.setSecurityNode(((AbstractInvestmentTransactionEntry)e).getSecurityNode());

                incomeExchangePane.setSelectedAccount(e.getDebitAccount());
                incomeExchangePane.setExchangedAmount(e.getDebitAmount().abs());

                decimalTextField.setDecimal(e.getAmount(accountProperty().get()));
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

    @NotNull
    abstract TransactionType getTransactionType();

    @Override
    public void clearForm() {
        super.clearForm();

        if (!Options.rememberLastDateProperty().get()) {
            datePicker.setValue(LocalDate.now());
        }

        numberComboBox.setValue("");
        memoTextField.clear();
        decimalTextField.setDecimal(BigDecimal.ZERO);

        accountExchangePane.setSelectedAccount(accountProperty().get());
        incomeExchangePane.setSelectedAccount(accountProperty().get());
    }
}
