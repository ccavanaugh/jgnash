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

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import jgnash.engine.AccountGroup;
import jgnash.engine.AccountType;

/**
 * Register pane controller
 *
 * @author Craig Cavanaugh
 */
public class InvestmentRegisterPaneController extends RegisterPaneController {

    @FXML
    public ComboBox<InvestmentTransactionFormController> actionComboBox;

    @FXML
    protected StackPane register;

    @FXML
    protected TabPane transactionForms;

    private ResourceBundle resources;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.resources = resources;

        transactionForms.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Load the register table
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("InvestmentRegisterTable.fxml"), resources);
            register.getChildren().add(fxmlLoader.load());
            registerTableController = fxmlLoader.getController();

            // Bind  the register pane to this account property
            registerTableController.getAccountProperty().bind(getAccountProperty());
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        getAccountProperty().addListener((observable, oldValue, newValue) -> {
            buildTabs();
        });
    }

    private void buildTabs() {
        final AccountType accountType = getAccountProperty().get().getAccountType();

        final String[] tabNames = RegisterFactory.getCreditDebitTabNames(accountType);

        final Tab creditTab = buildTab(tabNames[0], PanelType.INCREASE);
        final Tab debitTab = buildTab(tabNames[1], PanelType.DECREASE);

        transactionForms.getTabs().addAll(creditTab, debitTab);

        if (accountType == AccountType.CHECKING || accountType == AccountType.CREDIT) {
            transactionForms.getSelectionModel().select(debitTab);
        } else if (accountType.getAccountGroup() == AccountGroup.INCOME) {
            transactionForms.getSelectionModel().select(debitTab);
        }
    }

    private Tab buildTab(final String tabName, final PanelType panelType) {

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TransactionPane.fxml"), resources);
            final Pane pane = fxmlLoader.load();

            final TransactionPaneController transactionPaneController = fxmlLoader.getController();

            transactionPaneController.setPanelType(panelType);
            transactionPaneController.getAccountProperty().bind(getAccountProperty());

            final Tab tab = new Tab(tabName);
            tab.setContent(pane);

            return tab;
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
        return new Tab();
    }

    @FXML
    private void okAction() {
    }

    @FXML
    private void cancelAction() {
    }

    @FXML
    private void handleSecuritiesAction() {
    }
}
