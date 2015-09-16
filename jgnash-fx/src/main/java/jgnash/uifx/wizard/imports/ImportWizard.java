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

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import jgnash.uifx.control.wizard.WizardDialogController;
import jgnash.uifx.util.FXMLUtils;
import jgnash.util.ResourceUtils;

/**
 * Import Wizard Dialog
 *
 * @author Craig Cavanaugh
 */
public class ImportWizard {

    public enum Settings {
        BANK,
        ACCOUNT,
        TRANSACTIONS
    }

    private final ObjectProperty<WizardDialogController<Settings>> wizardControllerProperty = new SimpleObjectProperty<>();

    private final Stage stage;

    public ImportWizard() {

        final ResourceBundle resources = ResourceUtils.getBundle();

        final URL fxmlUrl = WizardDialogController.class.getResource("WizardDialog.fxml");

        stage = FXMLUtils.loadFXML(fxmlUrl, wizardControllerProperty(), ResourceUtils.getBundle());
        stage.setTitle(resources.getString("Title.ImportTransactions"));

        final WizardDialogController<Settings> wizardController = wizardControllerProperty().get();

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ImportPageOne.fxml"), resources);
            Pane pane = fxmlLoader.load();
            wizardController.addTaskPane(fxmlLoader.getController(), pane);

        } catch (IOException e) {
            e.printStackTrace();
        }

        stage.setResizable(false);
    }

    public ObjectProperty<WizardDialogController<Settings>> wizardControllerProperty() {
        return wizardControllerProperty;
    }

    public void showAndWait() {
       stage.showAndWait();
    }
}
