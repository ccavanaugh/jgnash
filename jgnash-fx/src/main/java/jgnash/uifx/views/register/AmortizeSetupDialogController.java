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
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;
import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.AmortizeObject;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.IntegerTextField;
import jgnash.uifx.control.TextFieldEx;
import jgnash.uifx.util.JavaFXUtils;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Amortization setup controller
 *
 * @author Craig Cavanaugh
 */
public class AmortizeSetupDialogController {

    @FXML
    private AccountComboBox interestAccountCombo;

    @FXML
    private AccountComboBox bankAccountCombo;

    @FXML
    private AccountComboBox feesAccountCombo;

    @FXML
    private TextFieldEx payeeField;

    @FXML
    private TextFieldEx memoField;

    @FXML
    private DecimalTextField daysField;

    @FXML
    private DatePickerEx dateField;

    @FXML
    private DecimalTextField feesField;

    @FXML
    private CheckBox useDaysButton;

    @FXML
    private IntegerTextField payPeriodsField;

    @FXML
    private IntegerTextField intPeriodsField;

    @FXML
    private IntegerTextField loanTermField;

    @FXML
    private DecimalTextField loanAmountField;

    @FXML
    private DecimalTextField interestField;

    private final Account NOP_ACCOUNT = new Account();

    private boolean result;

    @FXML
    public void initialize() {

        feesAccountCombo.setPredicate(AccountComboBox.getDefaultPredicate().and(account -> account == NOP_ACCOUNT
                || account.memberOf(AccountGroup.EXPENSE)));

        feesAccountCombo.getUnfilteredItems().add(0, NOP_ACCOUNT);

        daysField.disableProperty().bind(useDaysButton.selectedProperty().not());

        feesField.disableProperty().bind(Bindings.isNull(feesAccountCombo.valueProperty())
                .or(Bindings.equal(NOP_ACCOUNT, feesAccountCombo.valueProperty())));

        JavaFXUtils.runLater(() -> fillForm(generateDefault()));
    }

    AmortizeObject getAmortizeObject() {
        AmortizeObject o = new AmortizeObject();

        o.setDate(dateField.getValue());
        o.setFees(feesField.getDecimal());
        o.setInterestPeriods(intPeriodsField.getInteger());
        o.setPaymentPeriods(payPeriodsField.getInteger());
        o.setPrincipal(loanAmountField.getDecimal());
        o.setRate(interestField.getDecimal());
        o.setLength(loanTermField.getInteger());
        o.setPayee(payeeField.getText());
        o.setMemo(memoField.getText());
        o.setUseDailyRate(useDaysButton.isSelected());
        o.setDaysPerYear(daysField.getDecimal());


        o.setBankAccount(bankAccountCombo.getValue());
        o.setInterestAccount(interestAccountCombo.getValue());
        o.setFeesAccount(feesAccountCombo.getValue() != NOP_ACCOUNT ? feesAccountCombo.getValue() : null);

        return o;
    }

    void setAmortizeObject(final AmortizeObject amortizeObject) {
        JavaFXUtils.runLater(() -> fillForm(amortizeObject));
    }

    private static AmortizeObject generateDefault() {
        AmortizeObject o = new AmortizeObject();

        // make up some reasonable numbers
        o.setLength(360);
        o.setPaymentPeriods(12);
        o.setRate(new BigDecimal("4.75"));
        o.setPrincipal(new BigDecimal("80000.00"));
        o.setUseDailyRate(false);
        o.setDaysPerYear(new BigDecimal("365"));

        // Defaults for US and CA are known... not sure about others
        if (Locale.getDefault().getCountry().equals("CA")) {
            o.setInterestPeriods(4);
        } else {
            o.setInterestPeriods(12);
        }
        return o;
    }

    private void fillForm(final AmortizeObject ao) {
        interestField.setDecimal(ao.getRate());
        loanAmountField.setDecimal(ao.getPrincipal());
        loanTermField.setInteger(ao.getLength());
        payPeriodsField.setInteger(ao.getPaymentPeriods());
        intPeriodsField.setInteger(ao.getInterestPeriods());
        feesField.setDecimal(ao.getFees());
        memoField.setText(ao.getMemo());
        payeeField.setText(ao.getPayee());
        dateField.setValue(ao.getDate());
        daysField.setDecimal(ao.getDaysPerYear());
        useDaysButton.setSelected(ao.getUseDailyRate());

        Account a = ao.getBankAccount();
        if (a != null) {
            bankAccountCombo.setAccountValue(ao.getBankAccount());
        }

        a = ao.getInterestAccount();
        if (a != null) {
            interestAccountCombo.setAccountValue(ao.getInterestAccount());
        }

        feesAccountCombo.setAccountValue(feesAccountCombo.getValue() == null ? NOP_ACCOUNT : ao.getFeesAccount());
    }

    /**
     * Returns true or false depending on if the user closed the dialog with
     * the OK button or Cancel button.
     *
     * @return the closing state of the dialog
     */
    public boolean getResult() {
        return result;
    }

    @FXML
    private void okAction() {
        result = true;
        ((Stage) memoField.getScene().getWindow()).close();
    }

    @FXML
    private void cancelAction() {
        ((Stage) memoField.getScene().getWindow()).close();
    }
}
