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
package jgnash.uifx.wizard.imports;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import jgnash.convert.imports.BayesImportClassifier;
import jgnash.convert.imports.GenericImport;
import jgnash.convert.imports.ImportBank;
import jgnash.convert.imports.ImportTransaction;
import jgnash.engine.Account;
import jgnash.uifx.control.wizard.AbstractWizardPaneController;
import jgnash.util.TextResource;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Import Wizard, imported transaction wizard
 *
 * @author Craig Cavanaugh
 */
public class ImportPageTwoController extends AbstractWizardPaneController<ImportWizard.Settings> {

    @FXML
    private TableView<ImportTransaction> tableView;

    @FXML
    private Button deleteButton;

    @FXML
    private ResourceBundle resources;

    @FXML
    private TextArea textArea;

    private final SimpleBooleanProperty valid = new SimpleBooleanProperty(false);

    @FXML
    private void initialize() {
        textArea.setText(TextResource.getString("ImportTwo.txt"));

        deleteButton.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull());

        tableView.getItems().addListener((ListChangeListener<ImportTransaction>) c ->
                valid.setValue(tableView.getItems().size() > 0));

        updateDescriptor();
    }

    @Override
    public void putSettings(final Map<ImportWizard.Settings, Object> map) {
       // map.put(ImportWizard.Settings.ACCOUNT, accountComboBox.getValue());
    }

    @Override
    public void getSettings(final Map<ImportWizard.Settings, Object> map) {
        ImportBank bank = (ImportBank) map.get(ImportWizard.Settings.BANK);

        if (bank != null) {
            List<ImportTransaction> list = bank.getTransactions();

            Account account = (Account) map.get(ImportWizard.Settings.ACCOUNT);

            // set to sane account assuming it's going to be a single entry
            for (final ImportTransaction t : list) {
                t.account = account;
                t.setState(ImportTransaction.ImportState.NEW);
            }

            // match up any pre-existing transactions
            GenericImport.matchTransactions(list, account);

            // classify the transactions
            BayesImportClassifier.classifyTransactions(list, account);

            tableView.getItems().addAll(list);
        }

        updateDescriptor();
    }

    @Override
    public boolean isPaneValid() {
        return valid.getValue();
    }

    @Override
    public String toString() {
        return "2. " + resources.getString("Title.ModImportTrans");
    }

    @FXML
    private void handleDeleteAction() {

    }
}
