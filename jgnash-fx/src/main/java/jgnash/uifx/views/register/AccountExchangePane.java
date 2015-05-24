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

import java.io.IOException;
import java.math.BigDecimal;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import jgnash.engine.Account;
import jgnash.engine.CurrencyNode;
import jgnash.engine.MathConstants;
import jgnash.text.CommodityFormat;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DecimalTextField;
import jgnash.util.ResourceUtils;

import org.controlsfx.control.PopOver;

/**
 * Controller for handling the exchange of currencies
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
    private Button expandButton;

    @FXML
    private Label label;

    @FXML
    private PopOver popOver;

    @FXML
    private Label exchangeLabel;

    /**
     * Account property may be null
     */
    final private ObjectProperty<Account> baseAccountProperty = new SimpleObjectProperty<>();

    final private ObjectProperty<CurrencyNode> baseCurrencyProperty = new SimpleObjectProperty<>();

    final private ObjectProperty<BigDecimal> exchangeAmountProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);

    final private ObjectProperty<BigDecimal> amountProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);

    final private SimpleBooleanProperty amountEditableProperty = new SimpleBooleanProperty();

    /**
     * Determines if the base account will be visible for selection
     */
    final private SimpleBooleanProperty filterBaseAccountProperty = new SimpleBooleanProperty(true);

    final private SimpleBooleanProperty disableProperty = new SimpleBooleanProperty(false);

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

        popOver.detachableProperty().setValue(false);

        accountCombo.disableProperty().bind(disableProperty);
        expandButton.disableProperty().bind(disableProperty);
        exchangeRateField.disableProperty().bind(disableProperty);

        exchangeRateField.scaleProperty().set(MathConstants.EXCHANGE_RATE_ACCURACY);

        // base currency should always match the account if the account is set
        baseAccountProperty.addListener((observable, oldValue, newValue) -> {
            if (baseCurrencyProperty.get() != null && newValue.getCurrencyNode() != baseCurrencyProperty.get()) {
                throw new RuntimeException("baseCurrency does not match baseAccount currency");
            }

            if (filterBaseAccountProperty().get()){
                accountCombo.filterAccount(newValue);
            }
            baseCurrencyProperty().setValue(newValue.getCurrencyNode());
        });

        baseCurrencyProperty.addListener((observable, oldValue, newValue) -> {
            if (baseAccountProperty.get() != null && baseAccountProperty.get().getCurrencyNode() != newValue) {
                throw new RuntimeException("baseCurrency does not match baseAccount currency");
            }

            updateExchangeRateField();
            updateControlVisibility();
        });

        accountCombo.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                exchangeAmountField.scaleProperty().setValue(newValue.getCurrencyNode().getScale());
                updateExchangeRateField();
                updateControlVisibility();
            }
        });

        expandButton.setOnAction(event -> handleExpandButton());

        exchangeAmountProperty.bindBidirectional(exchangeAmountField.decimalProperty());

        exchangeAmountField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // lost focus
                if (exchangeAmountField.getDecimal().compareTo(BigDecimal.ZERO) > 0) {
                    exchangeAmountFieldAction();
                }
            }
        });

        // Call exchangeRateFieldAction() on entry or loss of focus
        exchangeRateField.setOnAction(event -> exchangeRateFieldAction());
        exchangeRateField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                exchangeRateFieldAction();
            }
        });

        amountProperty.addListener((observable, oldValue, newValue) -> {
            amountFieldAction();
        });
    }

    private void updateExchangeRateField() {
        final Account selectedAccount = accountCombo.getValue();

        if (selectedAccount != null && baseCurrencyProperty.get() != null) {
            exchangeRateField.setDecimal(baseCurrencyProperty.get().getExchangeRate(selectedAccount.getCurrencyNode()));
        }
    }

    private BigDecimal getAmount() {
        if (amountProperty.get() != null) {
            return amountProperty.get();
        } else {
            return BigDecimal.ZERO;
        }
    }

    private void amountFieldAction() {
        if (exchangeRateField.getDecimal().compareTo(BigDecimal.ZERO) == 0) {
            if (getAmount().compareTo(BigDecimal.ZERO) != 0) {
                exchangeRateField.setDecimal(exchangeAmountProperty.get().divide(getAmount(),
                        MathConstants.mathContext));
            }
        } else {
            exchangeAmountProperty.set(getAmount().multiply(exchangeRateField.getDecimal(),
                    MathConstants.mathContext).setScale(baseCurrencyProperty.get().getScale(),
                    MathConstants.roundingMode));
        }
    }

    private void exchangeRateFieldAction() {
        if (exchangeAmountProperty.get() != null) { // NPE if exchangeAmount has not been set
            if (getAmount().compareTo(BigDecimal.ZERO) == 0 && amountEditableProperty.get()) {
                if (exchangeRateField.getDecimal().compareTo(BigDecimal.ZERO) != 0) {
                    amountProperty.set(exchangeAmountProperty.get().divide(exchangeRateField.getDecimal(),
                            MathConstants.mathContext).setScale(baseCurrencyProperty.get().getScale(),
                            MathConstants.roundingMode));
                }
            } else {
                exchangeAmountProperty.set(getAmount().multiply(exchangeRateField.getDecimal(),
                        MathConstants.mathContext).setScale(baseCurrencyProperty.get().getScale(),
                        MathConstants.roundingMode));
            }
        }

        Platform.runLater(popOver::hide);
    }

    private void exchangeAmountFieldAction() {
        if (getAmount().compareTo(BigDecimal.ZERO) == 0 && amountEditableProperty.get()) {
            if (exchangeRateField.getDecimal().compareTo(BigDecimal.ZERO) != 0) {
                amountProperty.set(exchangeAmountProperty.get().divide(exchangeRateField.getDecimal(),
                        MathConstants.mathContext).setScale(baseCurrencyProperty.get().getScale(),
                        MathConstants.roundingMode));
            }
        } else {
            if (getAmount().compareTo(BigDecimal.ZERO) != 0) {
                exchangeRateField.setDecimal(exchangeAmountProperty.get().divide(getAmount(),
                        MathConstants.mathContext));
            }
        }
    }

    private void updateControlVisibility() {
        if (getSelectedAccount() != null && baseCurrencyProperty() != null) {
            if (getSelectedAccount().getCurrencyNode() == baseCurrencyProperty().get()) {
                getChildren().removeAll(label, exchangeAmountField, expandButton);
            } else {
                if (!getChildren().contains(label)) {
                    getChildren().addAll(label, exchangeAmountField, expandButton);
                }
            }
        }
    }

    private void handleExpandButton() {
        if (popOver.isShowing()) {
            popOver.hide();
        } else {
            // update the label before the popover is shown
            exchangeLabel.setText(CommodityFormat.getConversion(baseCurrencyProperty.get(),
                    accountCombo.getValue().getCurrencyNode()));
            popOver.show(expandButton);
        }
    }

    public ObjectProperty<Account> baseAccountProperty() {
        return baseAccountProperty;
    }

    @SuppressWarnings("WeakerAccess")
    public ObjectProperty<CurrencyNode> baseCurrencyProperty() {
        return baseCurrencyProperty;
    }

    public ObjectProperty<BigDecimal> exchangeAmountProperty() {
        return exchangeAmountProperty;
    }

    public ObjectProperty<BigDecimal> amountProperty() {
        return amountProperty;
    }

    public SimpleBooleanProperty amountEditableProperty() {
        return amountEditableProperty;
    }

    public Account getSelectedAccount() {
        return accountCombo.getValue();
    }

    void setSelectedAccount(final Account account) {
        accountCombo.setValue(account);
    }

    void setExchangedAmount(final BigDecimal value) {
        exchangeAmountProperty.setValue(value);
    }

    void setEnabled(final boolean enabled) {
        disableProperty().setValue(!enabled);
    }

    public SimpleBooleanProperty filterBaseAccountProperty() {
        return filterBaseAccountProperty;
    }
}
