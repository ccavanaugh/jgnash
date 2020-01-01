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

import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import jgnash.engine.CurrencyNode;
import jgnash.uifx.control.wizard.AbstractWizardPaneController;
import jgnash.resource.util.ResourceUtils;

/**
 * New file wizard pane, shows summary.
 *
 * @author Craig Cavanaugh
 */
public class NewFileSummaryController extends AbstractWizardPaneController<NewFileWizard.Settings> {

    @FXML
    private TextField fileNameField;

    @FXML
    private TextField defaultCurrencyField;

    @FXML
    private ListView<CurrencyNode> currenciesList;

    @FXML
    private ResourceBundle resources;

    @FXML
    private void initialize() {
        updateDescriptor();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void getSettings(final Map<NewFileWizard.Settings, Object> map) {
        fileNameField.setText((String) map.get(NewFileWizard.Settings.DATABASE_NAME));
        defaultCurrencyField.setText(map.get(NewFileWizard.Settings.DEFAULT_CURRENCY).toString());

        final ObservableList<CurrencyNode> currencyNodes = FXCollections.observableArrayList();
        currencyNodes.addAll((Collection<? extends CurrencyNode>) map.get(NewFileWizard.Settings.CURRENCIES));
        currencyNodes.add((CurrencyNode) map.get(NewFileWizard.Settings.DEFAULT_CURRENCY));
        FXCollections.sort(currencyNodes);

        currenciesList.getItems().setAll(currencyNodes);
    }

    @Override
    public String toString() {
        return "5. " + ResourceUtils.getString("Title.Summary");
    }
}
