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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.uifx.MainApplication;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.util.StageUtils;
import jgnash.util.ResourceUtils;

/**
 * A Dialog for creating and editing new investment transactions
 *
 * @author Craig Cavanaugh
 */
public class InvestmentTransactionDialog extends Stage {

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

    private InvestmentTransactionDialog() {
        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("InvestmentTransactionDialog.fxml"), ResourceUtils.getBundle());
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
        }

        initOwner(MainApplication.getPrimaryStage());
        initStyle(StageStyle.DECORATED);
        initModality(Modality.APPLICATION_MODAL);
        setTitle(ResourceUtils.getBundle().getString("Title.NewTrans"));

        StageUtils.addBoundsListener(this, InvestmentTransactionDialog.class);
    }

    private ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    @FXML
    private void initialize() {
        enterButton.setOnAction(value -> handleEnterAction());
        cancelButton.setOnAction(value -> handleCancelAction());

        actionComboBox.setEditable(false);

        getAccountProperty().addListener((observable, oldValue, newValue) -> {
            buildTabs();
        });

        actionComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.getController().clearForm();
            }
            transactionSlips.getChildren().clear();
            transactionSlips.getChildren().addAll(newValue.getPane());
        });
    }

    private void buildTabs() {
        final String[] actions = new String[]{resources.getString("Transaction.BuyShare"),
                resources.getString("Transaction.SellShare"), resources.getString("Transaction.TransferIn"),
                resources.getString("Transaction.TransferOut"), resources.getString("Transaction.AddShare"),
                resources.getString("Transaction.RemoveShare"), resources.getString("Transaction.ReinvestDiv"),
                resources.getString("Transaction.Dividend"), resources.getString("Transaction.SplitShare"),
                resources.getString("Transaction.MergeShare"), resources.getString("Transaction.ReturnOfCapital")};

        final List<SlipControllerContainer> transactionPanes = new ArrayList<>();

        // TODO: more investment slips
        transactionPanes.add(buildCashTransferTab(actions[2], SlipType.INCREASE));
        transactionPanes.add(buildCashTransferTab(actions[3], SlipType.DECREASE));

        actionComboBox.getItems().addAll(transactionPanes);

        actionComboBox.getSelectionModel().select(0);    // force selection
    }

    private SlipControllerContainer buildCashTransferTab(final String name, final SlipType slipType) {

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("InvestmentTransactionPane.fxml"), resources);
            final Pane pane = fxmlLoader.load();

            final SlipController slipController = fxmlLoader.getController();

            slipController.setSlipType(slipType);
            slipController.getAccountProperty().bind(getAccountProperty());

            return new SlipControllerContainer(name, slipController, pane);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void handleEnterAction() {
        final Slip controller = actionComboBox.getSelectionModel().getSelectedItem().getController();

        if (controller.validateForm()) {
            transactionOptional = Optional.ofNullable(controller.buildTransaction());
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

        if (!engine.isStored(transaction)) { // must not be a persisted transaction
            if (transaction instanceof InvestmentTransaction) {
                // TODO more forms
                StaticUIMethods.displayWarning("Not implemented yet");
            } else {
                if (transaction.getAmount(accountProperty.get()).signum() >= 0) {
                    actionComboBox.getSelectionModel().select(2); // transferIn
                } else {
                    actionComboBox.getSelectionModel().select(3); // transferOut
                }
            }

            actionComboBox.getSelectionModel().getSelectedItem().getController().modifyTransaction(transaction);
        }
    }

    public static Optional<Transaction> showAndWait(final Account account, final Transaction transaction) {
        InvestmentTransactionDialog transactionDialog = new InvestmentTransactionDialog();
        transactionDialog.getAccountProperty().setValue(account);
        transactionDialog.setTransaction(transaction);

        // Lock the height of the dialog
        transactionDialog.setMinHeight(transactionDialog.getHeight());
        transactionDialog.setMaxHeight(transactionDialog.getHeight());

        transactionDialog.showAndWait();

        return transactionDialog.getTransactionOptional();
    }
}
