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
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.engine.Account;
import jgnash.engine.TransactionEntry;
import jgnash.uifx.MainApplication;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.util.ResourceUtils;

/**
 * Split Transaction entry dialog
 *
 * @author Craig Cavanaugh
 */
public class SplitTransactionDialog extends Stage implements Initializable {

    @FXML
    private Button newButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button deleteAllButton;

    @FXML
    private Button okButton;

    @FXML
    private Button cancelButton;

    @FXML
    private TableView<TransactionEntry> tableView;

    @FXML
    private TabPane tabPane;

    Tab creditTab;

    Tab debitTab;

    final private ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private ResourceBundle resources;

    public SplitTransactionDialog() {
        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SplitTransactionDialog.fxml"), ResourceUtils.getBundle());
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
        setTitle(ResourceUtils.getBundle().getString("Title.SpitTran"));

        StageUtils.addBoundsListener(this, SplitTransactionDialog.class);
    }

    public ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.resources = resources;

        getAccountProperty().addListener((observable, oldValue, newValue) -> {
            initTabs();
        });

        okButton.setOnAction(event -> okAction());
        cancelButton.setOnAction(event -> cancelAction());
    }

    private void initTabs() {
        final String[] tabNames = RegisterFactory.getCreditDebitTabNames(getAccountProperty().get().getAccountType());

        creditTab = new Tab(tabNames[0]);

        SplitTransactionPaneController creditController = FXMLUtils.loadFXML(o -> {
            creditTab.setContent((Node) o);
        }, "SplitTransactionPane.fxml", resources);

        creditController.setPanelType(PanelType.INCREASE);
        creditController.getAccountProperty().setValue(getAccountProperty().getValue());

        debitTab = new Tab(tabNames[1]);

        SplitTransactionPaneController debitController = FXMLUtils.loadFXML(o -> {
            debitTab.setContent((Node) o);
        }, "SplitTransactionPane.fxml", resources);

        debitController.setPanelType(PanelType.DECREASE);
        debitController.getAccountProperty().setValue(getAccountProperty().getValue());

        tabPane.getTabs().addAll(creditTab, debitTab);
    }

    private void okAction() {
        ((Stage) okButton.getScene().getWindow()).close();
    }

    private void cancelAction() {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }
}
