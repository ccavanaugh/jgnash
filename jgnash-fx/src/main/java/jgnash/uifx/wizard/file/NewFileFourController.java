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
package jgnash.uifx.wizard.file;

import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import jgnash.engine.Account;
import jgnash.engine.AccountTreeXMLFactory;
import jgnash.engine.Comparators;
import jgnash.engine.RootAccount;
import jgnash.uifx.control.CheckListView;
import jgnash.uifx.control.wizard.AbstractWizardPaneController;
import jgnash.resource.util.ResourceUtils;
import jgnash.resource.util.TextResource;

/**
 * New file wizard pane, handles selection of account sets.
 *
 * @author Craig Cavanaugh
 */
public class NewFileFourController extends AbstractWizardPaneController<NewFileWizard.Settings> {

    @FXML
    private TreeView<Account> accountTreeView;

    @FXML
    private CheckListView<RootAccount> accountSetsList;

    @FXML
    private TextArea textArea;

    @FXML
    private ResourceBundle resources;

    @FXML
    private void initialize() {
        textArea.textProperty().set(TextResource.getString("NewFileFour.txt"));
        accountSetsList.getItems().addAll(AccountTreeXMLFactory.getLocalizedAccountSet());

        accountSetsList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue)
                -> showAccountSet(newValue));

        updateDescriptor();
    }

    private void showAccountSet(final RootAccount rootAccount) {
        final TreeItem<Account> root = new TreeItem<>(rootAccount);
        root.setExpanded(true);

        accountTreeView.setRoot(root);
        loadChildren(root);
    }

    private void loadChildren(final TreeItem<Account> parentItem) {
        parentItem.getValue().getChildren(Comparators.getAccountByCode()).forEach(child ->
        {
            final TreeItem<Account> childItem = new TreeItem<>(child);
            childItem.setExpanded(true);
            parentItem.getChildren().add(childItem);

            if (child.getChildCount() > 0) {
                loadChildren(childItem);
            }
        });
    }

    @Override   
    public void putSettings(final Map<NewFileWizard.Settings, Object> map) {
        map.put(NewFileWizard.Settings.ACCOUNT_SET,  new ArrayList<Account>(accountSetsList.getCheckedItems()));
    }

    @Override
    public String toString() {
        return "4. " + ResourceUtils.getString("Title.ChooseAccounts");
    }
}
