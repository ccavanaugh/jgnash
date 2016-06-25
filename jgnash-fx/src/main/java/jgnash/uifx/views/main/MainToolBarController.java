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
package jgnash.uifx.views.main;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.tasks.CloseFileTask;

/**
 * Primary ToolBar Controller.
 *
 * @author Craig Cavanaugh
 */
public class MainToolBarController implements MessageListener {

    @FXML
    private Button closeButton;

    @FXML
    private Button updateCurrencies;

    @FXML
    private Button updateSecurities;

    final private BooleanProperty disabled = new SimpleBooleanProperty(true);

    @FXML
    private void initialize() {

        closeButton.disableProperty().bind(disabled);
        updateCurrencies.disableProperty().bind(disabled);
        updateSecurities.disableProperty().bind(disabled);

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM);
    }

    @FXML
    private void handleOpenAction() {
        StaticUIMethods.showOpenDialog();
    }

    @FXML
    private void handleCloseAction() {
        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
            CloseFileTask.initiateFileClose();
        }
    }

    @Override
    public void messagePosted(final Message event) {

        Platform.runLater(() -> {
            switch (event.getEvent()) {
                case FILE_LOAD_SUCCESS:
                    disabled.setValue(false);
                    break;
                case FILE_CLOSING:
                case FILE_LOAD_FAILED:
                    disabled.setValue(true);
                    break;
                default:
                    break;
            }
        });
    }

    @FXML
    private void handleSecuritiesUpdateAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        if (engine != null) {
            engine.startSecuritiesUpdate(0);
        }
    }

    @FXML
    private void handleCurrenciesUpdateAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        if (engine != null) {
            engine.startExchangeRateUpdate(0);
        }
    }
}
