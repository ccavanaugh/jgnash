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
package jgnash.uifx.views.main;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.about.AboutDialog;
import jgnash.uifx.actions.DefaultLocaleAction;
import jgnash.uifx.tasks.CloseFileTask;
import jgnash.uifx.views.register.RegisterStage;
import jgnash.uifx.wizard.file.NewFileWizard;

import java.util.Objects;

/**
 * Primary Menu Controller
 *
 * @author Craig Cavanaugh
 */
public class MenuBarController implements MessageListener {

    @FXML
    private Menu windowMenu;

    @FXML
    private MenuItem updateCurrenciesMenuItem;

    @FXML
    private MenuItem updateSecuritiesMenuItem;

    @FXML
    private MenuBar menuBar;

    @FXML
    private MenuItem openMenuItem;

    @FXML
    private MenuItem closeMenuItem;

    @FXML
    private MenuItem exitMenuItem;

    final private BooleanProperty disabled = new SimpleBooleanProperty(true);

    /**
     * RegisterStage list property
     */
    final private ListProperty<RegisterStage> registerStageListProperty = new SimpleListProperty<>();

    @FXML
    private void initialize() {
        updateSecuritiesMenuItem.disableProperty().bind(disabled);
        updateCurrenciesMenuItem.disableProperty().bind(disabled);
        closeMenuItem.disableProperty().bind(disabled);

        windowMenu.disableProperty().bind(Bindings.or(disabled, registerStageListProperty.emptyProperty()));

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM);
    }

    ListProperty<RegisterStage> registerStageListPropertyProperty() {
        return registerStageListProperty;
    }

    @FXML
    private void handleExitAction() {
        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
            CloseFileTask.initiateShutdown();
        } else {
            Platform.exit();
        }
    }

    @FXML
    private void handleCloseAction() {
        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
            CloseFileTask.initiateFileClose();
        }
    }

    @FXML
    private void handleOpenAction() {
        StaticUIMethods.showOpenDialog();
    }

    @FXML
    private void updateSecurities() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        Objects.requireNonNull(engine);

        engine.startSecuritiesUpdate(0);
    }

    @FXML
    private void updateCurrencies() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        Objects.requireNonNull(engine);

        engine.startExchangeRateUpdate(0);
    }

    @FXML
    private void handleNewAction() {
        NewFileWizard.show();
    }

    @FXML
    private void handleAboutAction() {
        AboutDialog.showAndWait();
    }

    @Override
    public void messagePosted(final Message event) {
        Platform.runLater(() -> {
            switch (event.getEvent()) {
                case FILE_LOAD_SUCCESS:
                case FILE_NEW_SUCCESS:
                    disabled.setValue(false);
                    break;
                case FILE_CLOSING:
                    disabled.setValue(true);
                    break;
                default:
                    break;
            }
        });
    }

    @FXML
    private void changeDefaultLocale() {
        DefaultLocaleAction.showAndWait();
    }

    @FXML
    private void closeAllWindows() {
        // TODO, close all windows
    }
}
