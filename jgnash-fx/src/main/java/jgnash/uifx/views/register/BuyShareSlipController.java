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
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.AutoCompleteTextField;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.SecurityComboBox;
import jgnash.uifx.control.TransactionNumberComboBox;
import jgnash.util.NotNull;

/**
 * Transaction Entry Controller for Credits and Debits
 *
 * @author Craig Cavanaugh
 */
public class BuyShareSlipController implements Slip {

    @FXML
    private FeesPane feesPane;

    @FXML
    private DecimalTextField quantityField;

    @FXML
    private DecimalTextField priceField;

    @FXML
    protected TransactionNumberComboBox numberComboBox;

    @FXML
    protected AttachmentPane attachmentPane;

    @FXML
    protected DecimalTextField totalField;

    @FXML
    protected DatePickerEx datePicker;

    @FXML
    private SecurityComboBox securityComboBox;

    @FXML
    protected AutoCompleteTextField<Transaction> memoTextField;

    @FXML
    private AccountExchangePane accountExchangePane;

    @FXML
    protected CheckBox reconciledButton;

    @FXML
    ResourceBundle resources;

    /**
     * Holds a reference to a transaction being modified
     */
    Transaction modTrans = null;

    private TransactionEntry modEntry = null;

    final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    @FXML
    public void initialize() {

        // Bind necessary properties to the exchange panel
        accountExchangePane.getBaseAccountProperty().bind(getAccountProperty());
        accountExchangePane.getAmountProperty().bindBidirectional(totalField.decimalProperty());
        accountExchangePane.getAmountEditable().bind(totalField.editableProperty());

        // Lazy init when account property is set
        accountProperty.addListener((observable, oldValue, newValue) -> {
            //initializeSplitsDialog(); // initialize the splits dialog
        });
    }

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
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
                Logger logger = Logger.getLogger(BuyShareSlipController.class.getName());
                logger.warning("Was not able to modify the transaction");
            }
        }
    }

    void updateTotalField() {
        BigDecimal fee = feesPane.getDecimalProperty().get();
        BigDecimal quantity = quantityField.getDecimal();
        BigDecimal price = priceField.getDecimal();

        BigDecimal value = quantity.multiply(price);

        value = value.add(fee);

        totalField.setDecimal(value);
    }

    @Override
    public boolean validateForm() {
        return true;
    }

    @NotNull
    @Override
    public Transaction buildTransaction() {
        final BigDecimal exchangeRate = accountExchangePane.getExchangeAmountProperty().getValue();
        final List<TransactionEntry> fees = feesPane.getTransactions();

        return TransactionFactory.generateBuyXTransaction(accountExchangePane.getSelectedAccount(),
                getAccountProperty().get(), securityComboBox.getValue(), priceField.getDecimal(),
                quantityField.getDecimal(), exchangeRate, datePicker.getDate(), memoTextField.getText(), fees);
    }

    void newTransaction(final Transaction t) {
        clearForm();
    }

    @Override
    public void clearForm() {
        feesPane.clearForm();

        modEntry = null;

        accountExchangePane.setEnabled(true);
        accountExchangePane.setExchangedAmount(null);
        updateTotalField();
    }

    @Override
    public void handleCancelAction() {

    }

    @Override
    public void handleEnterAction() {

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
}
