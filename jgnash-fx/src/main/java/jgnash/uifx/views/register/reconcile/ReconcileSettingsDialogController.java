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
package jgnash.uifx.views.register.reconcile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;
import java.util.prefs.Preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ReconcileManager;
import jgnash.resource.util.ResourceUtils;
import jgnash.time.DateUtils;
import jgnash.uifx.Options;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.views.AccountBalanceDisplayManager;

/**
 * Account reconcile settings dialog.
 *
 * @author Craig Cavanaugh
 */
public class ReconcileSettingsDialogController {

    private static final String CALC_BAL = "calcClosingBalance";

    private static final int FUZZY_DATE_RANGE = 2;

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private ButtonBar buttonBar;

    @FXML
    private DecimalTextField openingBalanceTextField;

    @FXML
    private DecimalTextField closingBalanceTextField;

    @FXML
    private CheckBox autoFillBalanceCheckBox;

    @FXML
    private DatePickerEx datePicker;

    private final ObjectProperty<Account> account = new SimpleObjectProperty<>();

    private final Preferences preferences = Preferences.userNodeForPackage(ReconcileSettingsDialogController.class);

    @FXML
    private void initialize() {
        buttonBar.buttonOrderProperty().bind(Options.buttonOrderProperty());

        autoFillBalanceCheckBox.selectedProperty().set(preferences.getBoolean(CALC_BAL, false));

        accountProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                determineBalances();
            }
        });
    }

    public ObjectProperty<Account> accountProperty() {
        return account;
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

        final LocalDate lastSuccessDate = ReconcileManager.getAccountDateAttribute(account,
                Account.RECONCILE_LAST_SUCCESS_DATE).orElse(null);

        final LocalDate lastAttemptDate = ReconcileManager.getAccountDateAttribute(account,
                Account.RECONCILE_LAST_ATTEMPT_DATE).orElse(null);

        final LocalDate lastStatementDate = ReconcileManager.getAccountDateAttribute(account,
                Account.RECONCILE_LAST_STATEMENT_DATE).orElse(LocalDate.now());

        final BigDecimal lastClosingBalance = ReconcileManager.getAccountBigDecimalAttribute(account,
                Account.RECONCILE_LAST_CLOSING_BALANCE).orElse(null);

        final BigDecimal lastOpeningBalance = ReconcileManager.getAccountBigDecimalAttribute(account,
                Account.RECONCILE_LAST_OPENING_BALANCE).orElse(null);

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
            statementDate = lastStatementDate; // set the new statement date + 1 month

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
    private void handleUpdateBalance() {
        if (autoFillBalanceCheckBox.isSelected()) {
            final Account account = accountProperty().get();

            // Balance at the statement date
            final BigDecimal closingBalance = AccountBalanceDisplayManager
                    .convertToSelectedBalanceMode(account.getAccountType(), account.getBalance(datePicker.getValue()));
            closingBalanceTextField.setDecimal(closingBalance);

            // Balance at the 1st unreconciled transaction
            final BigDecimal openingBalance = AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(),
                    account.getOpeningBalanceForReconcile());
            openingBalanceTextField.setDecimal(openingBalance);
        }

        preferences.putBoolean(CALC_BAL, autoFillBalanceCheckBox.isSelected());
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parent.get().getWindow()).close();
    }

    @FXML
    private void handleOkayAction() {
        final LocalDate statementDate = datePicker.getValue();
        final BigDecimal openingBalance = openingBalanceTextField.getDecimal();
        final BigDecimal closingBalance = closingBalanceTextField.getDecimal();

        final FXMLUtils.Pair<ReconcileDialogController> pair =
                FXMLUtils.load(ReconcileDialogController.class.getResource("ReconcileDialog.fxml"),
                        ResourceUtils.getString("Button.Reconcile") + " - " + account.get().getPathName());

        pair.getController().initialize(account.get(), statementDate, openingBalance, closingBalance);

        // Override the defaults set by FXMLUtils
        pair.getStage().initModality(Modality.NONE);
        pair.getStage().initOwner(null);

        JavaFXUtils.runLater(() -> {
            pair.getStage().show();
            pair.getStage().setMinWidth(pair.getStage().getWidth());
            pair.getStage().setMinHeight(pair.getStage().getHeight());
        });

        // push account updates outside the UI thread to improve performance
        new Thread(() -> {
            ReconcileManager.setAccountDateAttribute(accountProperty().get(),
                    Account.RECONCILE_LAST_ATTEMPT_DATE, LocalDate.now());

            ReconcileManager.setAccountDateAttribute(accountProperty().get(),
                    Account.RECONCILE_LAST_STATEMENT_DATE, statementDate);

            ReconcileManager.setAccountBigDecimalAttribute(accountProperty().get(),
                    Account.RECONCILE_LAST_OPENING_BALANCE, openingBalance);

            ReconcileManager.setAccountBigDecimalAttribute(accountProperty().get(),
                    Account.RECONCILE_LAST_CLOSING_BALANCE, closingBalance);
        }).start();

        handleCloseAction(); // close the dialog
    }
}
