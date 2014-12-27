/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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
public class AccountExchangePane extends GridPane implements Initializable {

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

    final private ObjectProperty<CurrencyNode> currencyProperty = new SimpleObjectProperty<>();

    final private ObjectProperty<BigDecimal> exchangeAmountProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);

    final private ObjectProperty<BigDecimal> amountProperty = new SimpleObjectProperty<>(BigDecimal.ZERO);

    final private SimpleBooleanProperty amountEditable = new SimpleBooleanProperty();

    public AccountExchangePane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AccountExchangePane.fxml"), ResourceUtils.getBundle());
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        exchangeRateField.setScale(MathConstants.EXCHANGE_RATE_ACCURACY);

        baseAccountProperty.addListener(new ChangeListener<Account>() {
            @Override
            public void changed(final ObservableValue<? extends Account> observable, final Account oldValue, final Account newValue) {
                accountCombo.filterAccount(newValue);
                getCurrencyProperty().setValue(newValue.getCurrencyNode());
            }
        });

        currencyProperty.addListener((observable, oldValue, newValue) -> {
            exchangeAmountField.setScale(newValue.getScale());
            updateExchangeRateField();
            updateControlVisibility();
        });

        accountCombo.setOnAction(event -> {
            updateExchangeRateField();
            updateControlVisibility();
        });

        expandButton.setOnAction(event -> handleExpandButton());

        exchangeAmountField.decimalProperty().bindBidirectional(exchangeAmountProperty);
        exchangeAmountField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // lost focus
                if (exchangeAmountField.getDecimal().compareTo(BigDecimal.ZERO) > 0) {
                    exchangeAmountFieldAction();
                }
            }
        });

        // Call exchangeRateFieldAction() on entry or loss of focus
        exchangeRateField.setOnAction(event -> exchangeRateFieldAction());
        exchangeRateField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(final ObservableValue<? extends Boolean> observable, final Boolean oldValue, final Boolean newValue) {
                if (!newValue) {
                    exchangeRateFieldAction();
                }
            }
        });

        amountProperty.addListener((observable, oldValue, newValue) -> {
            amountFieldAction();
        });
    }

    private void updateExchangeRateField() {
        final Account selectedAccount = accountCombo.getValue();

        if (selectedAccount != null && currencyProperty.get() != null) {
            exchangeRateField.setDecimal(currencyProperty.get().getExchangeRate(selectedAccount.getCurrencyNode()));
        }
    }

    void amountFieldAction() {
        if (exchangeRateField.getDecimal().compareTo(BigDecimal.ZERO) == 0) {
            if (amountProperty.get().compareTo(BigDecimal.ZERO) != 0) {
                exchangeRateField.setDecimal(exchangeAmountProperty.get().divide(amountProperty.get(), MathConstants.mathContext));
            }
        } else {
            exchangeAmountProperty.set(amountProperty.get().multiply(exchangeRateField.getDecimal(), MathConstants.mathContext));
        }
    }

    private void exchangeRateFieldAction() {
        if (amountProperty.get().compareTo(BigDecimal.ZERO) == 0 && amountEditable.get()) {
            if (exchangeRateField.getDecimal().compareTo(BigDecimal.ZERO) != 0) {
                amountProperty.set(exchangeAmountProperty.get().divide(exchangeRateField.getDecimal(), MathConstants.mathContext));
            }
        } else {
            exchangeAmountProperty.set(amountProperty.get().multiply(exchangeRateField.getDecimal(), MathConstants.mathContext));
        }

        Platform.runLater(popOver::hide);
    }

    private void exchangeAmountFieldAction() {
        if (amountProperty.get().compareTo(BigDecimal.ZERO) == 0 && amountEditable.get()) {
            if (exchangeRateField.getDecimal().compareTo(BigDecimal.ZERO) != 0) {
                amountProperty.set(exchangeAmountProperty.get().divide(exchangeRateField.getDecimal(), MathConstants.mathContext));
            }
        } else {
            if (amountProperty.get().compareTo(BigDecimal.ZERO) != 0) {
                exchangeRateField.setDecimal(exchangeAmountProperty.get().divide(amountProperty.get(), MathConstants.mathContext));
            }
        }
    }

    private void updateControlVisibility() {
        if (getSelectedAccount() != null && getCurrencyProperty() != null) {
            if (getSelectedAccount().getCurrencyNode() == getCurrencyProperty().get()) {
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
            exchangeLabel.setText(CommodityFormat.getConversion(currencyProperty.get(), accountCombo.getValue().getCurrencyNode()));
            popOver.show(expandButton);
        }
    }

    public ObjectProperty<Account> getBaseAccountProperty() {
        return baseAccountProperty;
    }

    public ObjectProperty<CurrencyNode> getCurrencyProperty() {
        return currencyProperty;
    }

    public ObjectProperty<BigDecimal> getExchangeAmountProperty() {
        return exchangeAmountProperty;
    }

    public ObjectProperty<BigDecimal> getAmountProperty() {
        return amountProperty;
    }

    public SimpleBooleanProperty getAmountEditable() {
        return amountEditable;
    }

    public Account getSelectedAccount() {
        return accountCombo.getValue();
    }

    public void setSelectedAccount(final Account account) {
        accountCombo.setValue(account);
    }
}
