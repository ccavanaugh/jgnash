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

import javafx.fxml.FXML;
import javafx.stage.Stage;
import jgnash.engine.Account;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.TransactionNumberComboBox;
import jgnash.util.NotNull;

import java.time.LocalDate;

/**
 * A controller for getting a date and transaction number.
 *
 * @author Craig Cavanaugh
 */
public class DateTransNumberDialogController {

    @FXML
    private DatePickerEx dateField;

    @FXML
    private TransactionNumberComboBox transactionNumberField;

    private boolean result;

    public void setAccount(@NotNull Account account) {
        transactionNumberField.accountProperty().setValue(account);
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

    public LocalDate getDate() {
        return dateField.getValue();
    }

    public String getNumber() {
        return transactionNumberField.getValue();
    }

    @FXML
    private void okAction() {
        result = true;
        ((Stage) dateField.getScene().getWindow()).close();
    }

    @FXML
    private void cancelAction() {
        ((Stage) dateField.getScene().getWindow()).close();
    }
}
