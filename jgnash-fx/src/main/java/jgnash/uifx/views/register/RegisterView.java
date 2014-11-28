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

import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import jgnash.engine.Account;
import jgnash.uifx.controllers.AbstractAccountTreeController;
import jgnash.uifx.controllers.AccountTypeFilter;
import jgnash.uifx.views.accounts.StaticAccountsMethods;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

/**
 * Register accountTreeController
 *
 * @author Craig Cavanaugh
 */
public class RegisterView implements Initializable {

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

    private final AccountTypeFilter typeFilter = new AccountTypeFilter(Preferences.userNodeForPackage(RegisterView.class));

    private final AbstractAccountTreeController accountTreeController = new AbstractAccountTreeController() {
        @Override
        protected TreeView<Account> getTreeView() {
            return treeView;
        }

        @Override
        protected boolean isAccountVisible(Account account) {
            return typeFilter.isAccountVisible(account);
        }
    };

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        accountTreeController.initialize(); // must initialize the account controller

        final GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

        reconcileButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.ADJUST));
        filterButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.FILTER));
        zoomButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.EXTERNAL_LINK_SQUARE));

        // Filter changes should force a reload of the tree
        typeFilter.getAccountTypesVisibleProperty().addListener(observable -> accountTreeController.reload());
        typeFilter.getExpenseTypesVisibleProperty().addListener(observable -> accountTreeController.reload());
        typeFilter.getHiddenTypesVisibleProperty().addListener(observable -> accountTreeController.reload());
        typeFilter.getIncomeTypesVisibleProperty().addListener(observable -> accountTreeController.reload());

        accountTreeController.getSelectedAccountProperty().addListener(new ChangeListener<Account>() {
            @Override
            public void changed(ObservableValue<? extends Account> observable, Account oldValue, Account newValue) {
                if (newValue != null) {
                    System.out.println(newValue.getName());   // Display the account
                }
            }
        });
    }

    @FXML
    public void handleFilterAccountAction(final ActionEvent actionEvent) {
        StaticAccountsMethods.showAccountFilterDialog(typeFilter);
    }
}
