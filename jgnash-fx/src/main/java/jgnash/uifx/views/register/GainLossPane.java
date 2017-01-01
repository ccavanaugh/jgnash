/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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
public class GainLossPane extends DetailedDecimalTextField {

    @FXML
    private ResourceBundle resources;

    private final SimpleObjectProperty<Account> account = new SimpleObjectProperty<>(null);

    private GainLossDialog gainLossDialog;

    public GainLossPane() {
        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("GainLossPane.fxml"),
                ResourceUtils.getBundle());
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
        gainLossDialog = new GainLossDialog();

        account.addListener((observable, oldValue, newValue)
                -> gainLossDialog.accountProperty().set(accountProperty().get()));
    }

    @Override
    public void show() {
        gainLossDialog.show(() -> {
            setEditable(gainLossDialog.getTransactionEntries().size() == 0);

            if (gainLossDialog.getTransactionEntries().size() != 0) {
                setDecimal(gainLossDialog.getBalance().abs());
            }
        });
    }

    public List<TransactionEntry> getTransactions() {

        final List<TransactionEntry> feeList = gainLossDialog.getTransactionEntries();

        // adjust the cash balance of the investment account
        if (feeList.isEmpty() && getDecimal().compareTo(BigDecimal.ZERO) != 0) {  // ignore zero balance fees
            TransactionEntry fee = new TransactionEntry(accountProperty().get(), getDecimal().abs().negate());
            fee.setTransactionTag(TransactionTag.INVESTMENT_FEE);

            feeList.add(fee);
        }
        return feeList;
    }

    /**
     * Clones a {@code List} of {@code TransactionEntry(s)}.
     *
     * @param fees {@code List} of fees to clone
     */
    void setTransactionEntries(final List<TransactionEntry> fees) {
        final List<TransactionEntry> transactionEntries = gainLossDialog.getTransactionEntries();

        if (fees.size() == 1) {
            TransactionEntry e = fees.get(0);

            if (e.getCreditAccount().equals(e.getDebitAccount())) {
                setDecimal(e.getAmount(accountProperty().get()).abs());
            } else {
                try {
                    transactionEntries.add((TransactionEntry) e.clone()); // copy over the provided set's entry
                } catch (CloneNotSupportedException e1) {
                    Logger.getLogger(GainLossPane.class.getName()).log(Level.SEVERE, e1.getLocalizedMessage(), e1);
                }
                setDecimal(sumGains().abs());
            }
        } else {
            for (final TransactionEntry entry : fees) { // clone the provided set's entries
                try {
                    transactionEntries.add((TransactionEntry) entry.clone());
                } catch (CloneNotSupportedException e) {
                    Logger.getLogger(GainLossPane.class.getName()).log(Level.SEVERE, e.toString(), e);
                }
            }

            setDecimal(sumGains().abs());
        }

        setEditable(transactionEntries.size() < 1);
    }

    private BigDecimal sumGains() {
        BigDecimal sum = BigDecimal.ZERO;

        for (TransactionEntry entry : gainLossDialog.getTransactionEntries()) {
            sum = sum.add(entry.getAmount(accountProperty().get()));
        }

        return sum;
    }

    /**
     * Clear the form and remove all entries.
     */
    void clearForm() {
        gainLossDialog.getTransactionEntries().clear();
        setDecimal(BigDecimal.ZERO);
        setEditable(true);
    }

    public SimpleObjectProperty<Account> accountProperty() {
        return account;
    }
}
