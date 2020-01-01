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

import java.io.IOException;
import java.math.BigDecimal;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import jgnash.engine.Account;
import jgnash.engine.CurrencyNode;
import jgnash.engine.MathConstants;
import jgnash.text.NumericFormats;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.PopOverButton;
import jgnash.resource.util.ResourceUtils;

/**
 * Controller for handling the exchange of currencies.
 *
 * @author Craig Cavanaugh
 */
public class AccountExchangePane extends GridPane {

    @FXML
    private AccountComboBox accountCombo;

    @FXML
    private DecimalTextField exchangeAmountField;

    @FXML
    private DecimalTextField exchangeRateField;

    @FXML
    private PopOverButton expandButton;

    @FXML
    private Label label;

    @FXML
    private Label exchangeLabel;

    /**
     * Account property may be null.
     */
    final private ObjectProperty<Account> baseAccount = new SimpleObjectProperty<>();

    final private ObjectProperty<CurrencyNode> baseCurrency = new SimpleObjectProperty<>();

    final private ObjectProperty<BigDecimal> exchangeAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);

    final private ObjectProperty<BigDecimal> amount = new SimpleObjectProperty<>(BigDecimal.ZERO);

    final private SimpleBooleanProperty amountEditable = new SimpleBooleanProperty();

    final private ObjectProperty<Account> selectedAccount = new SimpleObjectProperty<>();

    /**
     * Determines if the base account will be visible for selection.
     */
    final private SimpleBooleanProperty filterBaseAccount = new SimpleBooleanProperty(true);

    /**
     * Reference is needed to prevent premature garbage collection.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<Boolean> amountFocusChangeListener;

    /**
     * Reference is needed to prevent premature garbage collection.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<Boolean> rateFocusChangeListener;

    public AccountExchangePane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AccountExchangePane.fxml"),
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

        accountCombo.disableProperty().bind(disableProperty());
        expandButton.disableProperty().bind(disableProperty());
        exchangeRateField.disableProperty().bind(disableProperty());

        exchangeRateField.scaleProperty().set(MathConstants.EXCHANGE_RATE_ACCURACY);

        // base currency should always match the account if the account is set
        baseAccount.addListener((observable, oldValue, newValue) -> {
            if (baseCurrency.get() != null && newValue.getCurrencyNode() != baseCurrency.get()) {
                throw new RuntimeException("baseCurrency does not match baseAccount currency");
            }

            if (filterBaseAccountProperty().get()) {
                // Set predicate to filter the base account
                accountCombo.setPredicate(AccountComboBox.getDefaultPredicate().and(account -> !account.equals(newValue)));
            }
            baseCurrencyProperty().set(newValue.getCurrencyNode());
        });

        baseCurrency.addListener((observable, oldValue, newValue) -> {
            if (baseAccount.get() != null && baseAccount.get().getCurrencyNode() != newValue) {
                throw new RuntimeException("baseCurrency does not match baseAccount currency");
            }

            updateExchangeRateField();
            updateControlVisibility();
        });

        accountCombo.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                exchangeAmountField.scaleProperty().set(newValue.getCurrencyNode().getScale());
                updateExchangeRateField();
                updateControlVisibility();
            }
        });

        selectedAccountProperty().bindBidirectional(accountCombo.valueProperty());

        // update the exchange label text
        expandButton.focusedProperty().addListener((observable, oldValue, newValue) -> {
            // protect against an NPE / race condition at application shutdown should the accountCombo have purged
            // all values prior to a focus change
            if (accountCombo.getValue() != null) {
                exchangeLabel.setText(NumericFormats.getConversion(baseCurrency.get(),
                        accountCombo.getValue().getCurrencyNode()));
            }
        });

        exchangeAmount.bindBidirectional(exchangeAmountField.decimalProperty());

        amountFocusChangeListener = (observable, oldValue, newValue) -> {
            if (!newValue) {
                if (exchangeAmountField.getDecimal().compareTo(BigDecimal.ZERO) > 0) {
                    exchangeAmountFieldAction();
                }
            }
        };

        exchangeAmountField.focusedProperty().addListener(new WeakChangeListener<>(amountFocusChangeListener));

        // Call exchangeRateFieldAction() on entry or loss of focus
        exchangeRateField.setOnAction(event -> exchangeRateFieldAction());

        rateFocusChangeListener = (observable, oldValue, newValue) -> {
            if (!newValue) {
                exchangeRateFieldAction();
            }
        };

        exchangeRateField.focusedProperty().addListener(new WeakChangeListener<>(rateFocusChangeListener));

        amount.addListener((observable, oldValue, newValue) -> amountFieldAction());
    }

    private void updateExchangeRateField() {
        final Account selectedAccount = accountCombo.getValue();

        if (selectedAccount != null && baseCurrency.get() != null) {
            exchangeRateField.setDecimal(baseCurrency.get().getExchangeRate(selectedAccount.getCurrencyNode()));
        }
    }

    private BigDecimal getAmount() {
        if (amount.get() != null) {
            return amount.get();
        }
        
		return BigDecimal.ZERO;
    }

    private void amountFieldAction() {
        if (exchangeRateField.getDecimal().compareTo(BigDecimal.ZERO) == 0) {
            if (getAmount().compareTo(BigDecimal.ZERO) != 0) {
                exchangeRateField.setDecimal(exchangeAmount.get().divide(getAmount(),
                        MathConstants.mathContext));
            }
        } else {
            exchangeAmount.set(getAmount().multiply(exchangeRateField.getDecimal(),
                    MathConstants.mathContext).setScale(baseCurrency.get().getScale(),
                    MathConstants.roundingMode));
        }
    }

    private void exchangeRateFieldAction() {
        if (exchangeAmount.get() != null) { // NPE if exchangeAmount has not been set
            if (getAmount().compareTo(BigDecimal.ZERO) == 0 && amountEditable.get()) {
                if (exchangeRateField.getDecimal().compareTo(BigDecimal.ZERO) != 0) {
                    amount.set(exchangeAmount.get().divide(exchangeRateField.getDecimal(),
                            MathConstants.mathContext).setScale(baseCurrency.get().getScale(),
                            MathConstants.roundingMode));
                }
            } else {
                exchangeAmount.set(getAmount().multiply(exchangeRateField.getDecimal(),
                        MathConstants.mathContext).setScale(baseCurrency.get().getScale(),
                        MathConstants.roundingMode));
            }
        }
    }

    private void exchangeAmountFieldAction() {
        if (getAmount().compareTo(BigDecimal.ZERO) == 0 && amountEditable.get()) {
            if (exchangeRateField.getDecimal().compareTo(BigDecimal.ZERO) != 0) {
                amount.set(exchangeAmount.get().divide(exchangeRateField.getDecimal(),
                        MathConstants.mathContext).setScale(baseCurrency.get().getScale(),
                        MathConstants.roundingMode));
            }
        } else {
            if (getAmount().compareTo(BigDecimal.ZERO) != 0) {
                exchangeRateField.setDecimal(exchangeAmount.get().divide(getAmount(),
                        MathConstants.mathContext));
            }
        }
    }

    private void updateControlVisibility() {
        if (getSelectedAccount() != null && baseCurrencyProperty() != null) {
            if (getSelectedAccount().getCurrencyNode() == baseCurrencyProperty().get()) {
                getChildren().removeAll(label, exchangeAmountField, expandButton);
                GridPane.setColumnSpan(accountCombo, 4);    // span the empty columns
            } else {
                if (!getChildren().contains(label)) {
                    GridPane.setColumnSpan(accountCombo, 1);    // reduce the column span before adding the nodes back
                    getChildren().addAll(label, exchangeAmountField, expandButton);
                }
            }
        }
    }

    ObjectProperty<Account> baseAccountProperty() {
        return baseAccount;
    }

    ObjectProperty<Account> selectedAccountProperty() {
        return selectedAccount;
    }

    ObjectProperty<CurrencyNode> baseCurrencyProperty() {
        return baseCurrency;
    }

    ObjectProperty<BigDecimal> exchangeAmountProperty() {
        return exchangeAmount;
    }

    BigDecimal getExchangeRate() {
        return exchangeRateField.getDecimal();
    }

    ObjectProperty<BigDecimal> amountProperty() {
        return amount;
    }

    SimpleBooleanProperty amountEditableProperty() {
        return amountEditable;
    }

    public Account getSelectedAccount() {
        return accountCombo.getValue();
    }

    void setSelectedAccount(final Account account) {
        accountCombo.setValue(account);
    }

    void setExchangedAmount(final BigDecimal value) {
        exchangeAmount.set(value);
    }

    SimpleBooleanProperty filterBaseAccountProperty() {
        return filterBaseAccount;
    }
}
