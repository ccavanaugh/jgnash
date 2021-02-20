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
package jgnash.uifx.wizard.imports;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import jgnash.convert.importat.GenericImport;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.control.wizard.WizardDialogController;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Import Wizard Dialog.
 *
 * @author Craig Cavanaugh
 */
public class ImportWizard {

    public enum Settings {
        BANK,
        ACCOUNT,
        TRANSACTIONS,
        DATE_FORMAT
    }

    private final ObjectProperty<WizardDialogController<Settings>> wizardController = new SimpleObjectProperty<>();

    private final SimpleBooleanProperty dateFormatSelectionEnabled = new SimpleBooleanProperty(false);

    private final Stage stage;

    public ImportWizard() {

        final ResourceBundle resources = ResourceUtils.getBundle();

        final FXMLUtils.Pair<WizardDialogController<Settings>> pair =
                FXMLUtils.load(WizardDialogController.class.getResource("WizardDialog.fxml"),
                        resources.getString("Title.ImportTransactions"));

        stage = pair.getStage();
        wizardControllerProperty().set(pair.getController());

        final WizardDialogController<Settings> wizardController = wizardControllerProperty().get();

        // force a default account before loading tasks to prevent NPE.  The ordered pages will sort out better choices
        wizardController.setSetting(Settings.ACCOUNT, GenericImport.findFirstAvailableAccount());

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ImportPageOne.fxml"), resources);
            Pane pane = fxmlLoader.load();
            ImportPageOneController importPageOneController = fxmlLoader.getController();
            wizardController.addTaskPane(importPageOneController, pane);
            importPageOneController.dateFormatSelectionEnabled().bind(dateFormatSelectionEnabled());

            fxmlLoader = new FXMLLoader(getClass().getResource("ImportPageTwo.fxml"), resources);
            pane = fxmlLoader.load();
            wizardController.addTaskPane(fxmlLoader.getController(), pane);

            fxmlLoader = new FXMLLoader(getClass().getResource("ImportPageThree.fxml"), resources);
            pane = fxmlLoader.load();
            wizardController.addTaskPane(fxmlLoader.getController(), pane);

        } catch (final IOException ioe) {
            Logger.getLogger(ImportWizard.class.getName()).log(Level.SEVERE, ioe.getLocalizedMessage(), ioe);
        }

        JavaFXUtils.runLater(() -> {

            stage.sizeToScene();

            stage.setMinWidth(stage.getWidth());
            stage.setMinHeight(stage.getHeight());
        });
    }

    public ObjectProperty<WizardDialogController<Settings>> wizardControllerProperty() {
        return wizardController;
    }

    public SimpleBooleanProperty dateFormatSelectionEnabled() {
        return dateFormatSelectionEnabled;
    }

    public void showAndWait() {
       stage.showAndWait();
    }
}
