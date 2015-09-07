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
package jgnash.uifx.views.register.reconcile;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.views.AccountBalanceDisplayManager;
import jgnash.util.DateUtils;
import jgnash.util.ResourceUtils;

/**
 * Account reconcile settings dialog.
 *
 * @author Craig Cavanaugh
 */
public class ReconcileSettingsDialogController {

    private static final int FUZZY_DATE_RANGE = 2;

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private DecimalTextField openingBalanceTextField;

    @FXML
    private DecimalTextField closingBalanceTextField;

    @FXML
    private DatePickerEx datePicker;

    private final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    @FXML
    private void initialize() {
        accountProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                determineBalances();
            }
        });
    }

    public ObjectProperty<Account> accountProperty() {
        return accountProperty;
    }

    private void determineBalances() {
        final Account account = accountProperty().get();

        // Last date of the month for the 1st unreconciled transaction
        LocalDate statementDate = account.getFirstUnreconciledTransactionDate().with(TemporalAdjusters.lastDayOfMonth());

        // Balance at the 1st unreconciled transaction
        BigDecimal openingBalance = AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(),
                account.getOpeningBalanceForReconcile());

        // Balance at the statement date
        BigDecimal closingBalance = AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(),
                account.getBalance(statementDate));

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        Objects.requireNonNull(engine);

        LocalDate lastSuccessDate = null;
        LocalDate lastAttemptDate = null;
        LocalDate lastStatementDate = LocalDate.now();

        BigDecimal lastOpeningBalance = null;
        BigDecimal lastClosingBalance = null;

        String value = account.getAttribute(Account.RECONCILE_LAST_SUCCESS_DATE);
        if (value != null) {
            lastSuccessDate = DateUtils.asLocalDate(Long.parseLong(value));
        }

        value = account.getAttribute(Account.RECONCILE_LAST_ATTEMPT_DATE);
        if (value != null) {
            lastAttemptDate = DateUtils.asLocalDate(Long.parseLong(value));
        }

        value = account.getAttribute(Account.RECONCILE_LAST_STATEMENT_DATE);
        if (value != null) {
            lastStatementDate = DateUtils.asLocalDate(Long.parseLong(value));
        }

        value = account.getAttribute(Account.RECONCILE_LAST_CLOSING_BALANCE);
        if (value != null) {
            lastClosingBalance = new BigDecimal(value);
        }

        value = account.getAttribute(Account.RECONCILE_LAST_OPENING_BALANCE);
        if (value != null) {
            lastOpeningBalance = new BigDecimal(value);
        }

        if (lastSuccessDate != null) { // we had prior success, use a new date one month out if the date is earlier than today
            if (DateUtils.before(lastStatementDate, LocalDate.now())) {

                // set the new statement date
                statementDate = lastStatementDate.plusMonths(1);

                // use the account balance of the estimated statement date
                closingBalance = AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(), account.getBalance(statementDate));
            }
        }

        // an recent attempt has been made before, override defaults
        if (lastAttemptDate != null && Math.abs(ChronoUnit.DAYS.between(lastAttemptDate, LocalDate.now())) <= FUZZY_DATE_RANGE) {
            if (lastStatementDate != null) {
                statementDate = lastStatementDate; // set the new statement date + 1 month
            }

            if (lastOpeningBalance != null) {
                openingBalance = lastOpeningBalance;
            }

            if (lastClosingBalance != null) {
                closingBalance = lastClosingBalance;
            }
        }

        datePicker.setValue(statementDate);
        openingBalanceTextField.setDecimal(openingBalance);
        closingBalanceTextField.setDecimal(closingBalance);
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parentProperty.get().getWindow()).close();
    }

    @FXML
    private void handleOkayAction() {
        final ObjectProperty<ReconcileDialogController> controllerObjectProperty = new SimpleObjectProperty<>();

        final URL fxmlUrl = ReconcileDialogController.class.getResource("ReconcileDialog.fxml");
        final Stage stage = FXMLUtils.loadFXML(fxmlUrl, controllerObjectProperty, ResourceUtils.getBundle());
        stage.setTitle(ResourceUtils.getString("Button.Reconcile") + " - " + accountProperty.get().getPathName());

        Objects.requireNonNull(controllerObjectProperty.get());

        controllerObjectProperty.get().initialize(accountProperty.get(), datePicker.getValue(),
                openingBalanceTextField.getDecimal(), closingBalanceTextField.getDecimal());

        // Override the defaults set by FXMLUtils
        stage.initModality(Modality.NONE);
        stage.initOwner(null);

        Platform.runLater(() -> {
            stage.show();
            stage.setMinWidth(stage.getWidth());
            stage.setMinHeight(stage.getHeight());
        });

        handleCloseAction(); // close the dialog
    }
}
