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
package jgnash.uifx.controllers;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import jgnash.engine.Account;
import jgnash.uifx.StaticUIMethods;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import org.controlsfx.glyphfont.FontAwesome;

/**
 * Accounts view controller
 *
 * @author Craig Cavanaugh
 */
public class AccountsViewController extends AccountTreeController {

    private final Preferences preferences = Preferences.userNodeForPackage(AccountsViewController.class);

    @FXML
    TreeTableView<Account> treeTableView;

    @FXML
    Button newButton;

    @FXML
    Button modifyButton;

    @FXML
    Button reconcileButton;

    @FXML
    Button deleteButton;

    @FXML
    Button filterButton;

    @FXML
    Button zoomButton;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        super.initialize(location, resources);

        newButton.setGraphic(FontAwesome.Glyph.PLUS.create());
        modifyButton.setGraphic(FontAwesome.Glyph.EDIT.create());
        reconcileButton.setGraphic(FontAwesome.Glyph.ADJUST.create());
        deleteButton.setGraphic(FontAwesome.Glyph.REMOVE_SIGN.create());
        filterButton.setGraphic(FontAwesome.Glyph.FILTER.create());
        zoomButton.setGraphic(FontAwesome.Glyph.ZOOM_IN.create());

        initializeTreeTableView();

        Platform.runLater(this::loadAccountTree);
    }

    @SuppressWarnings("unchecked")
    protected void initializeTreeTableView() {
        treeTableView.setShowRoot(false);   // don't show the root

        TreeTableColumn<Account, Integer> entriesColumn = new TreeTableColumn<>(resources.getString("Column.Entries"));
        entriesColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper(param.getValue().getValue().getTransactionCount()));

        TreeTableColumn<Account, BigDecimal> balanceColumn = new TreeTableColumn<>(resources.getString("Column.Balance"));
        balanceColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper(param.getValue().getValue().getTreeBalance()));
        balanceColumn.setCellFactory(cell -> new CommodityFormatTreeTableCell());

        TreeTableColumn<Account, BigDecimal> reconciledBalanceColumn = new TreeTableColumn<>(resources.getString("Column.ReconciledBalance"));
        reconciledBalanceColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper(param.getValue().getValue().getReconciledTreeBalance()));
        reconciledBalanceColumn.setCellFactory(cell -> new CommodityFormatTreeTableCell());

        TreeTableColumn<Account, String> currencyColumn = new TreeTableColumn<>(resources.getString("Column.Currency"));
        currencyColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getCurrencyNode().getSymbol()));

        TreeTableColumn<Account, String> typeColumn = new TreeTableColumn<>(resources.getString("Column.Type"));
        typeColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getAccountType().toString()));

        treeTableView.getColumns().addAll(entriesColumn, balanceColumn, reconciledBalanceColumn, currencyColumn, typeColumn);
    }

    @Override
    protected TreeTableView<Account> getTreeTableView() {
        return treeTableView;
    }

    @Override
    public Preferences getPreferences() {
        return preferences;
    }

    @SuppressWarnings("unused")
    @FXML
    public void handleFilterAccountAction(final ActionEvent actionEvent) {
        StaticUIMethods.showAccountFilterDialog(this);
    }
}
