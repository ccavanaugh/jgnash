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
import java.util.prefs.Preferences;

import jgnash.engine.Account;
import jgnash.uifx.controllers.AbstractAccountTreeController;
import jgnash.uifx.controllers.AccountTypeFilter;
import jgnash.uifx.skin.StyleClass;
import jgnash.uifx.views.accounts.StaticAccountsMethods;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

/**
 * Top level view for account registers
 *
 * @author Craig Cavanaugh
 */
public class RegisterViewController implements Initializable {

    private static final String DIVIDER_POSITION = "DividerPosition";

    private static final double DEFAULT_DIVIDER_POSITION = 0.2;

    private final Preferences preferences = Preferences.userNodeForPackage(RegisterViewController.class);

    @FXML
    public SplitPane splitPane;

    @FXML
    private Button reconcileButton;

    @FXML
    private Button filterButton;

    @FXML
    private Button zoomButton;

    @FXML
    private TreeView<Account> treeView;

    @FXML
    private StackPane registerPane;

    private final AccountTypeFilter typeFilter = new AccountTypeFilter(Preferences.userNodeForPackage(getClass()));

    private RegisterPaneController registerPaneController;

    private final AbstractAccountTreeController accountTreeController = new AbstractAccountTreeController() {
        @Override
        protected TreeView<Account> getTreeView() {
            return treeView;
        }

        @Override
        protected boolean isAccountVisible(Account account) {
            return typeFilter.isAccountVisible(account);
        }

        @Override
        protected boolean isAccountSelectable(final Account account) {
            return !account.isPlaceHolder();
        }

        public void initialize() {
            super.initialize();

            // Install a cell that disables selection if the account is a placeholder
            getTreeView().setCellFactory(param -> new DisabledTreeCell());
        }
    };

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        accountTreeController.initialize(); // must initialize the account controller

        final GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

        reconcileButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.ADJUST));
        filterButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.FILTER));
        zoomButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.EXTERNAL_LINK_SQUARE));

        // Load and add the register pane
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("RegisterPane.fxml"), resources);
            registerPane.getChildren().add(fxmlLoader.load());
            registerPaneController = fxmlLoader.getController();
        } catch (final IOException e) {
            Logger.getLogger(RegisterViewController.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        // Filter changes should force a reload of the tree
        typeFilter.getAccountTypesVisibleProperty().addListener(observable -> accountTreeController.reload());
        typeFilter.getExpenseTypesVisibleProperty().addListener(observable -> accountTreeController.reload());
        typeFilter.getHiddenTypesVisibleProperty().addListener(observable -> accountTreeController.reload());
        typeFilter.getIncomeTypesVisibleProperty().addListener(observable -> accountTreeController.reload());

        // Bind the account selection property to the registerPane controller
        registerPaneController.getAccountProperty().bind(accountTreeController.getSelectedAccountProperty());

        // Restore divider location
        splitPane.setDividerPosition(0, preferences.getDouble(DIVIDER_POSITION, DEFAULT_DIVIDER_POSITION));

        // Remember divider location
        splitPane.getDividers().get(0).positionProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(final ObservableValue<? extends Number> observable, final Number oldValue, final Number newValue) {
                preferences.putDouble(DIVIDER_POSITION, (Double)newValue);
            }
        });
    }

    @FXML
    public void handleFilterAccountAction(final ActionEvent actionEvent) {
        StaticAccountsMethods.showAccountFilterDialog(typeFilter);
    }

    private static final class DisabledTreeCell extends TreeCell<Account> {
        @Override
        public void updateItem(final Account account, final boolean empty) {
            super.updateItem(account, empty);   // required

            if (!empty && account != null) {
                setText(account.getName());

                if (account.isPlaceHolder()) {
                    setId(StyleClass.DISABLED_CELL_ID);
                } else {
                    setId(StyleClass.ENABLED_CELL_ID);
                }
            }
        }
    }
}
