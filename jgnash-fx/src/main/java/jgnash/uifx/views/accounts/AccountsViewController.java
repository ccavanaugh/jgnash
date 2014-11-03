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
package jgnash.uifx.views.accounts;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.CommodityFormatTreeTableCell;
import jgnash.uifx.controllers.AccountTreeController;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

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

        final GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

        newButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.PLUS));
        modifyButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.EDIT));
        reconcileButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.ADJUST));
        deleteButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.TIMES));
        filterButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.FILTER));
        zoomButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.EXTERNAL_LINK_SQUARE));

        modifyButton.setDisable(true);
        deleteButton.setDisable(true);
        reconcileButton.setDisable(true);
        zoomButton.setDisable(true);

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

        installSelectionListener();
    }

    private void installSelectionListener() {
        treeTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateButtonStates(newValue.getValue());
            } else {
                updateButtonStates(null);
            }
        });
    }

    private void updateButtonStates(final Account account) {
        Platform.runLater(() -> {
            if (account != null) {
                final int count = account.getTransactionCount();

                deleteButton.setDisable(count > 0 || account.getChildCount() > 0);
                reconcileButton.setDisable(count <= 0);
            } else {
                deleteButton.setDisable(true);
                reconcileButton.setDisable(true);
            }

            modifyButton.setDisable(account == null);
            zoomButton.setDisable(account == null);
        });
    }

    @Override
    protected TreeTableView<Account> getTreeTableView() {
        return treeTableView;
    }

    @Override
    public Preferences getPreferences() {
        return preferences;
    }

    @FXML
    public void handleFilterAccountAction(final ActionEvent actionEvent) {
        StaticAccountsMethods.showAccountFilterDialog(this);
    }

    @FXML
    public void handleModifyAccountAction(final ActionEvent actionEvent) {
        final Account account = getSelectedAccount();

        if (account != null) {
            StaticAccountsMethods.showModifyAccountProperties(account);
        }
    }

    @FXML
    public void handleNewAccountAction(final ActionEvent actionEvent) {
        StaticAccountsMethods.showNewAccountPropertiesDialog();
    }

    @FXML
    public void handleDeleteAccountAction(final ActionEvent actionEvent) {
        final Account account = getSelectedAccount();

        if (account != null) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            if (!engine.removeAccount(account)) {
                StaticUIMethods.displayError(resources.getString("Message.Error.AccountRemove"));
            }
        }
    }
}
