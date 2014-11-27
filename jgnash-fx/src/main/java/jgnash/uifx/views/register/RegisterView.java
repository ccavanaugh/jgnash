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
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageListener;
import jgnash.uifx.controllers.AbstractAccountTreeController;
import jgnash.uifx.controllers.AccountTypeFilter;
import jgnash.uifx.views.accounts.StaticAccountsMethods;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TreeView;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

/**
 * Register view
 *
 * @author Craig Cavanaugh
 */
public class RegisterView extends AbstractAccountTreeController implements Initializable, MessageListener {

    @FXML
    public Button reconcileButton;

    @FXML
    public Button filterButton;

    @FXML
    public Button zoomButton;

    @FXML
    public TreeView<Account> treeView;

    private final AccountTypeFilter typeFilter = new AccountTypeFilter(Preferences.userNodeForPackage(RegisterView.class));

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        initialize();

        final GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

        reconcileButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.ADJUST));
        filterButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.FILTER));
        zoomButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.EXTERNAL_LINK_SQUARE));

        // Register invalidation listeners to force a reload
        typeFilter.getAccountTypesVisibleProperty().addListener(observable -> reload());
        typeFilter.getExpenseTypesVisibleProperty().addListener(observable -> reload());
        typeFilter.getHiddenTypesVisibleProperty().addListener(observable -> reload());
        typeFilter.getIncomeTypesVisibleProperty().addListener(observable -> reload());
    }

    @Override
    protected TreeView<Account> getTreeView() {
        return treeView;
    }

    @Override
    protected boolean isAccountVisible(final Account account) {
        return typeFilter.isAccountVisible(account);
    }

    @Override
    public void messagePosted(final Message event) {

    }

    @FXML
    public void handleFilterAccountAction(final ActionEvent actionEvent) {
        StaticAccountsMethods.showAccountFilterDialog(typeFilter);
    }
}
