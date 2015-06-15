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
package jgnash.uifx.wizard.file;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import jgnash.engine.CurrencyNode;
import jgnash.engine.DataStoreType;
import jgnash.engine.DefaultCurrencies;
import jgnash.engine.EngineFactory;
import jgnash.engine.RootAccount;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.wizard.WizardDialogController;
import jgnash.uifx.util.FXMLUtils;
import jgnash.util.NewFileUtility;
import jgnash.util.ResourceUtils;

/**
 * Dialog for creating a new file
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
    public NewFileWizard() {

        final ResourceBundle resources = ResourceUtils.getBundle();

        final ObjectProperty<WizardDialogController<Settings>> wizardControllerProperty = new SimpleObjectProperty<>();

        final URL fxmlUrl = WizardDialogController.class.getResource("WizardDialog.fxml");

        final Stage stage = FXMLUtils.loadFXML(fxmlUrl, new ControllerConsumer(wizardControllerProperty), ResourceUtils.getBundle());
        stage.setTitle(resources.getString("Title.NewFile"));

        wizardControllerProperty.get().setSetting(Settings.DATABASE_NAME, EngineFactory.getDefaultDatabase());
        wizardControllerProperty.get().setSetting(Settings.DEFAULT_CURRENCIES, DefaultCurrencies.generateCurrencies());

        final WizardDialogController<Settings> wizardController =  wizardControllerProperty.get();

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

        } catch (IOException e) {
            e.printStackTrace();
        }

        stage.setResizable(false);

        stage.showAndWait();

        if (wizardController.validProperty().get()) {
            final String database = (String) wizardController.getSetting(Settings.DATABASE_NAME);
            final Set<CurrencyNode> nodes = (Set<CurrencyNode>) wizardController.getSetting(Settings.CURRENCIES);
            final CurrencyNode defaultCurrency = (CurrencyNode) wizardController.getSetting(Settings.DEFAULT_CURRENCY);
            final DataStoreType type = (DataStoreType)wizardController.getSetting(Settings.TYPE);
            final String password = (String)wizardController.getSetting(Settings.PASSWORD);
            final List<RootAccount> accountList = (List<RootAccount>) wizardController.getSetting(Settings.ACCOUNT_SET);

            try {
                NewFileUtility.buildNewFile(database, type, password.toCharArray(), defaultCurrency, nodes, accountList);
            } catch (final IOException e) {
                StaticUIMethods.displayError(e.getMessage());
            }
        }
    }

    private static class ControllerConsumer implements Consumer<WizardDialogController<Settings>> {
        private final ObjectProperty<WizardDialogController<Settings>> dialogProperty;

        public ControllerConsumer(ObjectProperty<WizardDialogController<Settings>> dialogProperty) {
            this.dialogProperty = dialogProperty;
        }

        @Override
        public void accept(WizardDialogController<Settings> settingsWizardDialog) {
            dialogProperty.setValue(settingsWizardDialog);
        }
    }
}
