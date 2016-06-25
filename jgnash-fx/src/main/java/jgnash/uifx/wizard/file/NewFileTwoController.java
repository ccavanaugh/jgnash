/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;

import jgnash.engine.CurrencyNode;
import jgnash.engine.DefaultCurrencies;
import jgnash.uifx.control.wizard.AbstractWizardPaneController;
import jgnash.util.ResourceUtils;
import jgnash.util.TextResource;

/**
 * New file wizard panel.
 *
 * @author Craig Cavanaugh
 */
public class NewFileTwoController extends AbstractWizardPaneController<NewFileWizard.Settings> {

    // Do not use a CurrencyComboBox at this point or it will boot the engine
    @FXML
    private ComboBox<CurrencyNode> defaultCurrencyComboBox;

    @FXML
    private TextArea textArea;

    @FXML
    private ResourceBundle resources;

    @FXML
    private void initialize() {
        textArea.setText(TextResource.getString("NewFileTwo.txt"));

        initDefaultCurrencies();
    }

    @Override
    public void putSettings(final Map<NewFileWizard.Settings, Object> map) {
        map.put(NewFileWizard.Settings.DEFAULT_CURRENCY, defaultCurrencyComboBox.getValue());
    }

    private void initDefaultCurrencies() {
        final Set<CurrencyNode> currencyNodes = DefaultCurrencies.generateCurrencies();
        final ObservableList<CurrencyNode> items = defaultCurrencyComboBox.getItems();

        defaultCurrencyComboBox.setItems(new SortedList<>(items));
        items.addAll(currencyNodes);

        try {
            final String symbol = DefaultCurrencies.getDefault().getSymbol();

            // set the default currency by matching the default locale
            currencyNodes.stream()
                    .filter(node -> symbol.equals(node.getSymbol()))
                    .forEach(defaultCurrencyComboBox::setValue);
        } catch (final IllegalArgumentException e) {
            Logger.getLogger(NewFileTwoController.class.getName()).warning("Unable to construct the default currency from the default system Locale");
        }

        updateDescriptor();
    }

    @Override
    public boolean isPaneValid() {
        return defaultCurrencyComboBox.getValue() != null;
    }

    @Override
    public String toString() {
        return "2. " + ResourceUtils.getString("Title.DefDefCurr");
    }

    @FXML
    private void handleDefaultCurrencyAction() {
        updateDescriptor();
    }
}
