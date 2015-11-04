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

import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.uifx.util.FXMLUtils;
import jgnash.util.ResourceUtils;

/**
 * A Dialog for creating and editing new investment transactions
 *
 * @author Craig Cavanaugh
 */
class InvestmentTransactionDialog extends Stage {

    @FXML
    private ComboBox<SlipControllerContainer> actionComboBox;

    @FXML
    private StackPane transactionSlips;

    @FXML
    private ResourceBundle resources;

    @FXML
    private Button enterButton;

    @FXML
    private Button cancelButton;

    private final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private Optional<Transaction> transactionOptional = Optional.empty();

    private InvestmentSlipManager investmentSlipManager;

    private InvestmentTransactionDialog() {
        FXMLUtils.loadFXML(this, "InvestmentTransactionDialog.fxml", ResourceUtils.getBundle());

        setTitle(ResourceUtils.getBundle().getString("Title.NewTrans"));
    }

    private ObjectProperty<Account> accountProperty() {
        return accountProperty;
    }

    @FXML
    private void initialize() {
        enterButton.setOnAction(value -> handleEnterAction());
        cancelButton.setOnAction(value -> handleCancelAction());

        investmentSlipManager = new InvestmentSlipManager(transactionSlips, actionComboBox);
        investmentSlipManager.accountProperty().bind(accountProperty());
    }

    @FXML
    private void handleEnterAction() {
        final Slip controller = actionComboBox.getSelectionModel().getSelectedItem().getController();

        if (controller.validateForm()) {
            transactionOptional = Optional.of(controller.buildTransaction());
            transactionSlips.getScene().getWindow().hide();
        }
    }

    @FXML
    private void handleCancelAction() {
        transactionSlips.getScene().getWindow().hide();
    }

    private Optional<Transaction> getTransactionOptional() {
        return transactionOptional;
    }

    private void setTransaction(final Transaction transaction) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        if (engine.isStored(transaction)) {
            setTitle(ResourceUtils.getBundle().getString("Title.ModifyTransaction"));
        }

        investmentSlipManager.modifyTransaction(transaction);
    }

    public static Optional<Transaction> showAndWait(final Account account, final Transaction transaction) {
        final InvestmentTransactionDialog transactionDialog = new InvestmentTransactionDialog();
        transactionDialog.accountProperty().setValue(account);
        transactionDialog.setTransaction(transaction);

        // Lock the height of the dialog
        transactionDialog.setMinHeight(transactionDialog.getHeight());
        transactionDialog.setMaxHeight(transactionDialog.getHeight());

        transactionDialog.showAndWait();

        return transactionDialog.getTransactionOptional();
    }
}
