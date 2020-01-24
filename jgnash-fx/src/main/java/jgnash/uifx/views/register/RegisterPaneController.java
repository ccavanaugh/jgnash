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

import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.StackPane;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.uifx.Options;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.views.main.MainView;
import jgnash.util.NotNull;

/**
 * Register pane controller.
 *
 * @author Craig Cavanaugh
 */
abstract class RegisterPaneController {

    @FXML
    protected Button newButton;

    @FXML
    protected Button duplicateButton;

    @FXML
    protected Button deleteButton;

    /**
     * The register table and labels should be loaded into this pane.
     */
    @FXML
    protected StackPane registerTablePane;

    @FXML
    protected ResourceBundle resources;

    @FXML
    protected TitledPane titledPane;

    /**
     * Active account for the pane.
     */
    private final ObjectProperty<Account> account = new SimpleObjectProperty<>();

    /**
     * This will be bound to the register table selection.
     */
    final ObjectProperty<Transaction> selectedTransaction = new SimpleObjectProperty<>();

    final ObjectProperty<RegisterTableController> registerTableController = new SimpleObjectProperty<>();

    ObjectProperty<Account> accountProperty() {
        return account;
    }

    @FXML
    void initialize() {

        /*
         * The Buttons may be null depending on the form tha was loaded with this controller
         */

        // Buttons should not be enabled if a transaction is not selected
        if (deleteButton != null) {
            deleteButton.disableProperty().bind(selectedTransaction.isNull());
        }

        if (duplicateButton != null) {
            duplicateButton.disableProperty().bind(selectedTransaction.isNull());
        }

        // Clear the table selection
        if (newButton != null) {
            newButton.setOnAction(event -> registerTableController.get().clearTableSelection());

            // disable if the titledPane is collapsed
            newButton.disableProperty().bind(titledPane.expandedProperty().not());
        }

        // When changed, bind the selected transaction and account properties
        registerTableController.addListener((observable, oldValue, newValue) -> {

            // Bind transaction selection to the register table controller
            selectedTransaction.bind(newValue.selectedTransactionProperty());

            // Bind the register pane to this account property
            newValue.accountProperty().bind(accountProperty());
        });

        // When changed, call for transaction modification if not null
        selectedTransaction.addListener((observable, oldValue, newValue) -> {

            /* Push to the end of the application thread to allow other UI controls to update before
            * updating many transaction form controls */
            JavaFXUtils.runLater(() -> {
                if (newValue != null) {
                    modifyTransaction(newValue);
                } else {
                    clearForm(); // selection was forcibly cleared, better clear the form
                }
            });
        });

        if (titledPane != null) {   // make sure we don't have a locked instance
            titledPane.animatedProperty().bind(Options.animationsEnabledProperty());
        }
    }

    @FXML
    private void handleDeleteAction() {
        registerTableController.get().deleteTransactions();
    }

    /**
     * Default empty implementation to modify a transaction when selected.
     *
     * @param transaction {@code Transaction} to be modified
     */
    void modifyTransaction(@NotNull final Transaction transaction) {

    }

    void selectTransaction(@NotNull final Transaction transaction) {
        registerTableController.get().selectTransaction(transaction);
    }

    /**
     * Default empty implementation.
     */
    void clearForm() {

    }

    @FXML
    void handleJumpAction() {
        registerTableController.get().handleJumpAction();
    }

    @FXML
    private void handleDuplicateAction() {
        clearForm();

        RegisterActions.duplicateTransaction(account.get(), registerTableController.get().getSelectedTransactions());

        // Request focus as it may have been lost
        MainView.requestFocus();
    }
}
