/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

import jgnash.engine.CurrencyNode;
import jgnash.engine.DataStoreType;
import jgnash.engine.DefaultCurrencies;
import jgnash.engine.EngineFactory;
import jgnash.engine.RootAccount;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.wizard.WizardDialogController;
import jgnash.uifx.util.FXMLUtils;
import jgnash.util.FileUtils;
import jgnash.util.NewFileUtility;
import jgnash.util.ResourceUtils;

/**
 * Dialog for creating a new file.
 *
 * @author Craig Cavanaugh
 */
public class NewFileWizard {

    public enum Settings {
        CURRENCIES,
        DEFAULT_CURRENCIES,
        DEFAULT_CURRENCY,
        DATABASE_NAME,
        ACCOUNT_SET,
        TYPE,
        PASSWORD
    }

    @SuppressWarnings("unchecked")
    private NewFileWizard() {

        final ResourceBundle resources = ResourceUtils.getBundle();

        final FXMLUtils.Pair<WizardDialogController<Settings>> pair =
                FXMLUtils.load(WizardDialogController.class.getResource("WizardDialog.fxml"),
                        resources.getString("Title.NewFile"));

        final WizardDialogController<Settings> wizardController = pair.getController();

        wizardController.setSetting(Settings.DATABASE_NAME, EngineFactory.getDefaultDatabase());
        wizardController.setSetting(Settings.DEFAULT_CURRENCIES, DefaultCurrencies.generateCurrencies());

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("NewFileOne.fxml"), resources);
            Pane pane = fxmlLoader.load();
            wizardController.addTaskPane(fxmlLoader.getController(), pane);

            fxmlLoader = new FXMLLoader(getClass().getResource("NewFileTwo.fxml"), resources);
            pane = fxmlLoader.load();
            wizardController.addTaskPane(fxmlLoader.getController(), pane);

            fxmlLoader = new FXMLLoader(getClass().getResource("NewFileThree.fxml"), resources);
            pane = fxmlLoader.load();
            wizardController.addTaskPane(fxmlLoader.getController(), pane);

            fxmlLoader = new FXMLLoader(getClass().getResource("NewFileFour.fxml"), resources);
            pane = fxmlLoader.load();
            wizardController.addTaskPane(fxmlLoader.getController(), pane);

            fxmlLoader = new FXMLLoader(getClass().getResource("NewFileSummary.fxml"), resources);
            pane = fxmlLoader.load();
            wizardController.addTaskPane(fxmlLoader.getController(), pane);

        } catch (final IOException e) {
            StaticUIMethods.displayError(e.getLocalizedMessage());
        }

        pair.getStage().setResizable(false);
        pair.getStage().showAndWait();

        if (wizardController.validProperty().get()) {
            final String database = (String) wizardController.getSetting(Settings.DATABASE_NAME);
            final Set<CurrencyNode> nodes = (Set<CurrencyNode>) wizardController.getSetting(Settings.CURRENCIES);
            final CurrencyNode defaultCurrency = (CurrencyNode) wizardController.getSetting(Settings.DEFAULT_CURRENCY);
            final DataStoreType type = (DataStoreType) wizardController.getSetting(Settings.TYPE);
            final String password = (String) wizardController.getSetting(Settings.PASSWORD);
            final List<RootAccount> accountList = (List<RootAccount>) wizardController.getSetting(Settings.ACCOUNT_SET);

            // Ensure file extension matches data store type
            final String fileName = FileUtils.stripFileExtension(database) + type.getDataStore().getFileExt();

            try {
                NewFileUtility.buildNewFile(fileName, type, password.toCharArray(), defaultCurrency, nodes, accountList);
            } catch (final IOException e) {
                StaticUIMethods.displayError(e.getLocalizedMessage());
            }
        }
    }

    public static void showAndWait() {
        new NewFileWizard();
    }
}
