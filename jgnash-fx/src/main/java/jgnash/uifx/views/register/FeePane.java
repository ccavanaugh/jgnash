/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import jgnash.engine.Account;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionTag;
import jgnash.uifx.control.DetailedDecimalTextField;
import jgnash.util.ResourceUtils;

/**
 * UI Panel for handling investment transaction fees
 * <p>
 * If {@code feeList.size() > 0 }, then a one or more specialized fees exist.  Otherwise, the fee is
 * simple and charged against the account adjusting the cash balance.
 *
 * @author Craig Cavanaugh
 */
@SuppressWarnings("WeakerAccess")
public class FeePane extends DetailedDecimalTextField {
    @FXML
    private ResourceBundle resources;

    private final SimpleObjectProperty<Account> accountProperty = new SimpleObjectProperty<>(null);

    private FeeDialog feeDialog;

    @SuppressWarnings("WeakerAccess")
    public FeePane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FeePane.fxml"), ResourceUtils.getBundle());
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void initialize() {
        feeDialog = new FeeDialog();

        accountProperty.addListener((observable, oldValue, newValue) -> {
            feeDialog.accountProperty().setValue(accountProperty().get());
        });
    }

    @Override
    public void show() {
        feeDialog.show(() -> {
            setEditable(feeDialog.getTransactionEntries().size() == 0);

            if (feeDialog.getTransactionEntries().size() != 0) {
                setDecimal(feeDialog.getBalance().abs());
            }
        });
    }

    public List<TransactionEntry> getTransactions() {

        final List<TransactionEntry> feeList = feeDialog.getTransactionEntries();

        // adjust the cash balance of the investment account
        if (feeList.isEmpty() && getDecimal().compareTo(BigDecimal.ZERO) != 0) {  // ignore zero balance fees
            TransactionEntry fee = new TransactionEntry(accountProperty().get(), getDecimal().abs().negate());
            fee.setTransactionTag(TransactionTag.INVESTMENT_FEE);

            feeList.add(fee);
        }
        return feeList;
    }

    /**
     * Clones a {@code List} of {@code TransactionEntry(s)}
     *
     * @param fees {@code List} of fees to clone
     */
    public void setTransactionEntries(final List<TransactionEntry> fees) {
        final List<TransactionEntry> feeList = feeDialog.getTransactionEntries();

        if (fees.size() == 1) {
            TransactionEntry e = fees.get(0);

            if (e.getCreditAccount().equals(e.getDebitAccount())) {
                setDecimal(e.getAmount(accountProperty().get()).abs());
            } else {
                try {
                    feeList.add((TransactionEntry) e.clone()); // copy over the provided set's entry
                } catch (CloneNotSupportedException e1) {
                    Logger.getLogger(FeePane.class.getName()).log(Level.SEVERE, e1.getLocalizedMessage(), e1);
                }
                setDecimal(sumFees().abs());
            }
        } else {
            for (final TransactionEntry entry : fees) { // clone the provided set's entries
                try {
                    feeList.add((TransactionEntry) entry.clone());
                } catch (CloneNotSupportedException e) {
                    Logger.getLogger(FeePane.class.getName()).log(Level.SEVERE, e.toString(), e);
                }
            }

            setDecimal(sumFees().abs());
        }

        setEditable(feeList.size() < 1);
    }

    private BigDecimal sumFees() {
        BigDecimal sum = BigDecimal.ZERO;

        for (TransactionEntry entry : feeDialog.getTransactionEntries()) {
            sum = sum.add(entry.getAmount(accountProperty().get()));
        }

        return sum;
    }

    /**
     * Clear the form and remove all entries
     */
    void clearForm() {
        feeDialog.getTransactionEntries().clear();
        setDecimal(BigDecimal.ZERO);
    }

    public SimpleObjectProperty<Account> accountProperty() {
        return accountProperty;
    }
}

