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

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.util.Resource;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

/**
 * Register Pane
 *
 * @author Craig Cavanaugh
 */
public class RegisterTablePane extends BorderPane {

    private TableView<Transaction> tableView = new TableView<>();

    private AccountPropertyWrapper accountPropertyWrapper = new AccountPropertyWrapper();

    /**
     * Active account for the pane
     */
    private ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    RegisterTablePane() {
        final Resource resources = Resource.get();

        final GridPane infoPane = new GridPane();

        BorderPane.setAlignment(infoPane, Pos.CENTER);
        BorderPane.setAlignment(tableView, Pos.CENTER);

        setTop(infoPane);
        setCenter(tableView);

        Label accountNameLabel = new Label();
        Label balanceAmountLabel = new Label();
        Label reconciledAmountLabel = new Label();

        infoPane.add(accountNameLabel, 0, 0);
        infoPane.add(new Label(resources.getString("Label.Balance")), 1, 0);

        infoPane.add(balanceAmountLabel, 2, 0);
        infoPane.add(new Label(resources.getString("Label.ReconciledBalance")), 3, 0);

        infoPane.add(reconciledAmountLabel, 4, 0);

        // Update the account
        accountProperty.addListener((observable, oldValue, newValue) -> updateAccount());

        // Bind label test to the account property wrapper
        accountNameLabel.textProperty().bind(accountPropertyWrapper.getAccountNameProperty());
        balanceAmountLabel.textProperty().bind(accountPropertyWrapper.getAccountBalanceProperty());
        reconciledAmountLabel.textProperty().bind(accountPropertyWrapper.getReconciledAmountProperty());
    }

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    private void updateAccount() {
        final Account account = accountProperty.getValue();

        accountPropertyWrapper.setAccount(account);
    }
}
