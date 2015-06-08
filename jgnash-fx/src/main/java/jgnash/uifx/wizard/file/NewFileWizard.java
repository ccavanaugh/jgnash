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
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.uifx.control.wizard.WizardDialog;
import jgnash.util.ResourceUtils;

/**
 * Dialog for creating a new file
 *
 * @author Craig Cavanaugh
 */
public class NewFileWizard {

    private ResourceBundle resources = ResourceUtils.getBundle();

    public enum Settings {
        CURRENCIES,
        DEFAULT_CURRENCIES,
        DEFAULT_CURRENCY,
        DATABASE_NAME,
        ACCOUNT_SET,
        TYPE,
        PASSWORD
    }

    public NewFileWizard() {

        URL fxmlUrl = WizardDialog.class.getResource("WizardDialog.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl, ResourceUtils.getBundle());

        final Stage stage = new Stage(StageStyle.DECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(resources.getString("Title.NewFile"));

        try {

            Pane pane = fxmlLoader.load();
            WizardDialog<NewFileWizard.Settings> wizardController = fxmlLoader.getController();

            stage.setScene(new Scene(pane));

            fxmlUrl = NewFileOneController.class.getResource("NewFileOne.fxml");
            fxmlLoader = new FXMLLoader(fxmlUrl, ResourceUtils.getBundle());

            wizardController.addTaskPane(fxmlLoader.getRoot());

            //wizardController.setSetting(Settings.CURRENCIES, DefaultCurrencies.generateCurrencies());
            //wizardController.setSetting(Settings.DATABASE_NAME, EngineFactory.getDefaultDatabase());


        } catch (IOException e) {
            e.printStackTrace();
        }

        stage.showAndWait();
    }
}
