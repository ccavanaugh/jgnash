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
package jgnash.uifx.dialog.options;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.Options;
import jgnash.uifx.views.AccountBalanceDisplayManager;
import jgnash.uifx.views.AccountBalanceDisplayMode;

/**
 * Controller for Account options.
 *
 * @author Craig Cavanaugh
 */
public class AccountTabController {

    @FXML
    private RadioButton accountOnlyRadioButton;

    @FXML
    private RadioButton allRadioButton;

    @FXML
    private RadioButton noAccountsRadioButton;

    @FXML
    private RadioButton creditAccountsRadioButton;

    @FXML
    private RadioButton incomeExpenseAccountsRadioButton;

    @FXML
    private CheckBox useAccountingTermsCheckBox;

    @FXML
    private TextField accountSeparatorTextField;

    @FXML
    private void initialize() {

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        if (engine != null) {
            accountSeparatorTextField.setText(engine.getAccountSeparator());

            accountSeparatorTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && newValue.length() > 0) {
                    engine.setAccountSeparator(newValue);
                }
            });
        } else {
            accountSeparatorTextField.setDisable(true);
        }

        useAccountingTermsCheckBox.selectedProperty().bindBidirectional(Options.useAccountingTermsProperty());

        switch (AccountBalanceDisplayManager.accountBalanceDisplayMode().get()) {
            case NONE:
                noAccountsRadioButton.setSelected(true);
                break;
            case REVERSE_CREDIT:
                creditAccountsRadioButton.setSelected(true);
                break;
            case REVERSE_INCOME_EXPENSE:
                incomeExpenseAccountsRadioButton.setSelected(true);
                break;
        }

        if (Options.globalBayesProperty().get()) {
            allRadioButton.setSelected(true);
        } else {
            accountOnlyRadioButton.setSelected(true);
        }

        noAccountsRadioButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                AccountBalanceDisplayManager.setDisplayMode(AccountBalanceDisplayMode.NONE);
            }
        });

        creditAccountsRadioButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                AccountBalanceDisplayManager.setDisplayMode(AccountBalanceDisplayMode.REVERSE_CREDIT);
            }
        });

        incomeExpenseAccountsRadioButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                AccountBalanceDisplayManager.setDisplayMode(AccountBalanceDisplayMode.REVERSE_INCOME_EXPENSE);
            }
        });

        allRadioButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                Options.globalBayesProperty().set(true);
            }
        });

        accountOnlyRadioButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                Options.globalBayesProperty().set(false);
            }
        });
    }
}
