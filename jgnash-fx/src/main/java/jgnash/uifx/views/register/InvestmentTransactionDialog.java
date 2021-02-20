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

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.Options;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.views.main.MainView;

/**
 * A Dialog for creating and editing new investment transactions.
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

    @FXML
    private ButtonBar buttonBar;

    private final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private final ObjectProperty<Consumer<Transaction>> transactionConsumer = new SimpleObjectProperty<>();

    private InvestmentSlipManager investmentSlipManager;

    private InvestmentTransactionDialog() {
        FXMLUtils.loadFXML(this, "InvestmentTransactionDialog.fxml", ResourceUtils.getBundle());

        setTitle(ResourceUtils.getString("Title.NewTrans"));
    }

    private ObjectProperty<Account> accountProperty() {
        return accountProperty;
    }

    @FXML
    private void initialize() {
        buttonBar.buttonOrderProperty().bind(Options.buttonOrderProperty());

        enterButton.setOnAction(value -> handleEnterAction());
        cancelButton.setOnAction(value -> handleCancelAction());

        investmentSlipManager = new InvestmentSlipManager(transactionSlips, actionComboBox);
        investmentSlipManager.accountProperty().bind(accountProperty());

        actionComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                enterButton.disableProperty().bind(newValue.getController().validFormProperty().not());
            }
        });
    }

    private void setTransactionConsumer(final Consumer<Transaction> consumer) {
        transactionConsumer.set(consumer);
    }

    @FXML
    private void handleEnterAction() {
        final Slip controller = actionComboBox.getSelectionModel().getSelectedItem().getController();

        transactionConsumer.get().accept(controller.buildTransaction());
        transactionSlips.getScene().getWindow().hide();
    }

    @FXML
    private void handleCancelAction() {
        transactionSlips.getScene().getWindow().hide();
    }

    private void setTransaction(final Transaction transaction) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        if (engine.isStored(transaction)) {
            setTitle(ResourceUtils.getString("Title.ModifyTransaction"));
        }

        investmentSlipManager.modifyTransaction(transaction);
    }

    public static void show(final Account account, final Transaction transaction,
                            final Consumer<Transaction> consumer) {

        final InvestmentTransactionDialog invTransDialog = new InvestmentTransactionDialog();

        invTransDialog.accountProperty().set(account);
        invTransDialog.setTransactionConsumer(consumer);

        invTransDialog.setTransaction(transaction);

        JavaFXUtils.runLater(() -> {
            invTransDialog.show();

            // Size and lock the height of the dialog after it has been shown
            JavaFXUtils.runLater(() -> {
                invTransDialog.sizeToScene();

                invTransDialog.setMinHeight(invTransDialog.getHeight());
                invTransDialog.setMaxHeight(invTransDialog.getHeight());
            });

            JavaFXUtils.runLater(() -> StageUtils.addBoundsListener(invTransDialog, InvestmentTransactionDialog.class,
                    MainView.getPrimaryStage()));
        });
    }
}
