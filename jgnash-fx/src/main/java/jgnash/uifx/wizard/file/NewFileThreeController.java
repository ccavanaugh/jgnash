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
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;

import jgnash.engine.CurrencyNode;
import jgnash.uifx.control.wizard.AbstractWizardPaneController;
import jgnash.resource.util.ResourceUtils;
import jgnash.resource.util.TextResource;

/**
 * New file wizard panel.
 *
 * @author Craig Cavanaugh
 */
public class NewFileThreeController extends AbstractWizardPaneController<NewFileWizard.Settings> {

    @FXML
    private ListView<CurrencyNode> selectedList;

    @FXML
    private ListView<CurrencyNode> availableList;

    @FXML
    private TextArea textArea;

    @FXML
    private ResourceBundle resources;

    @FXML
    private void initialize() {
        textArea.textProperty().set(TextResource.getString("NewFileThree.txt"));

        availableList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        selectedList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        updateDescriptor();
    }

    @Override   
    public void putSettings(final Map<NewFileWizard.Settings, Object> map) {
        map.put(NewFileWizard.Settings.CURRENCIES, new TreeSet<>(selectedList.getItems()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void getSettings(final Map<NewFileWizard.Settings, Object> map) {
        if (availableList.getItems().isEmpty()) {
            Set<CurrencyNode> currencies = (Set<CurrencyNode>) map.get(NewFileWizard.Settings.DEFAULT_CURRENCIES);

            if (currencies != null) {
                availableList.getItems().addAll(currencies);
            }
        }
    }

    @Override
    public String toString() {
        return "3. " + ResourceUtils.getString("Title.SelAvailCurr");
    }

    @FXML
    private void handleAddAction() {
        swap(new ArrayList<>(availableList.getSelectionModel().getSelectedItems()), availableList, selectedList);
    }

    @FXML
    private void handleRemoveAction() {
        swap( new ArrayList<>(selectedList.getSelectionModel().getSelectedItems()), selectedList, availableList);
    }

    private void swap(final List<CurrencyNode> currencyNodes, final ListView<CurrencyNode> sourceListView,
                      final ListView<CurrencyNode> destinationListView) {

        sourceListView.getItems().removeAll(currencyNodes);
        destinationListView.getItems().addAll(currencyNodes);
        FXCollections.sort(destinationListView.getItems());
    }
}
