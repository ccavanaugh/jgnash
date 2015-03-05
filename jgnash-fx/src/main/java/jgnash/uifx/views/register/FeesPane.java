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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import jgnash.engine.Account;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionTag;
import jgnash.uifx.control.DecimalTextField;
import jgnash.util.ResourceUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * UI Panel for handling investment transaction fees
 * <p/>
 * If {@code feeList.size() > 0 }, then a one or more specialized fees exist.  Otherwise, the fee is
 * simple and charged against the account adjusting the cash balance.
 *
 * @author Craig Cavanaugh
 */
public class FeesPane extends GridPane {

    @FXML
    protected Button detailsButton;

    @FXML
    private DecimalTextField feesField;

    @FXML
    private ResourceBundle resources;

    private final SimpleObjectProperty<Account> accountProperty = new SimpleObjectProperty<>(null);

    private FeesDialog feesDialog;

    public FeesPane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FeesPane.fxml"), ResourceUtils.getBundle());
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
        detailsButton.setOnAction(event -> handleDetailsAction());

        feesDialog = new FeesDialog();

        accountProperty.addListener((observable, oldValue, newValue) -> {
            feesDialog.accountProperty().setValue(accountProperty().get());
        });
    }

    private void handleDetailsAction() {
        feesDialog.showAndWait();

        feesField.setEditable(feesDialog.getTransactionEntries().size() == 0);

        if (feesDialog.getTransactionEntries().size() != 0) {
            feesField.setDecimal(feesDialog.getBalance().abs());
        }
    }

    public List<TransactionEntry> getTransactions() {

        final List<TransactionEntry> feeList = feesDialog.getTransactionEntries();

        // adjust the cash balance of the investment account
        if (feeList.isEmpty() && feesField.getDecimal().compareTo(BigDecimal.ZERO) != 0) {  // ignore zero balance fees
            TransactionEntry fee = new TransactionEntry(accountProperty().get(), feesField.getDecimal().abs().negate());
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
        final List<TransactionEntry> feeList = feesDialog.getTransactionEntries();

        if (fees.size() == 1) {
            TransactionEntry e = fees.get(0);

            if (e.getCreditAccount().equals(e.getDebitAccount())) {
                feesField.setDecimal(e.getAmount(accountProperty().get()).abs());
            } else {
                try {
                    feeList.add((TransactionEntry) e.clone()); // copy over the provided set's entry
                } catch (CloneNotSupportedException e1) {
                    Logger.getLogger(FeesPane.class.getName()).log(Level.SEVERE, e1.getLocalizedMessage(), e1);
                }
                feesField.setDecimal(sumFees().abs());
            }
        } else {
            for (final TransactionEntry entry : fees) { // clone the provided set's entries
                try {
                    feeList.add((TransactionEntry) entry.clone());
                } catch (CloneNotSupportedException e) {
                    Logger.getLogger(FeesPane.class.getName()).log(Level.SEVERE, e.toString(), e);
                }
            }

            feesField.setDecimal(sumFees().abs());
        }

        feesField.setDisable(feeList.size() > 1);
    }

    private BigDecimal sumFees() {
        BigDecimal sum = BigDecimal.ZERO;

        for (TransactionEntry entry : feesDialog.getTransactionEntries()) {
            sum = sum.add(entry.getAmount(accountProperty().get()));
        }

        return sum;
    }

    /**
     * Clear the form and remove all entries
     */
    void clearForm() {
        feesDialog.getTransactionEntries().clear();
        feesField.setDecimal(BigDecimal.ZERO);
    }

    public BigDecimal getDecimal() {
        return feesField.getDecimal();
    }

    public SimpleObjectProperty<Account> accountProperty() {
        return accountProperty;
    }

    public ObjectProperty<BigDecimal> decimalProperty() {
        return feesField.decimalProperty();
    }
}
