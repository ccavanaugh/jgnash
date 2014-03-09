/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.uifx.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.tasks.CloseFileTask;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

/**
 * Primary Menu Controller
 *
 * @author Craig Cavanaugh
 */
public class MenuBarController implements Initializable, MessageListener {

    @FXML private MenuBar menuBar;

    @FXML private MenuItem closeMenuItem;

    @Override
    public void initialize(final URL url, final ResourceBundle resourceBundle) {
        assert menuBar != null;

        closeMenuItem.setDisable(true);

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM);
    }

    @FXML
    protected void handleExitAction(final ActionEvent actionEvent) {
        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
            CloseFileTask.initiateShutdown();
        } else {
            Platform.exit();
        }
    }

    @FXML
    protected void handleCloseAction(final ActionEvent event) {
        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
            CloseFileTask.initiateClose();
        }
    }

    @FXML
    protected void handleOpenAction(final ActionEvent event) {
        StaticUIMethods.showOpenDialog();
    }

    @Override
    public void messagePosted(final Message event) {
        Platform.runLater(() -> {
            switch (event.getEvent()) {
                case FILE_LOAD_SUCCESS:
                case FILE_NEW_SUCCESS:
                    closeMenuItem.setDisable(false);
                    break;
                case FILE_CLOSING:
                    closeMenuItem.setDisable(true);
                    break;
                case FILE_IO_ERROR:
                case FILE_LOAD_FAILED:
                case FILE_NOT_FOUND:
                    StaticUIMethods.displayError("File system error TBD");  // TODO: need a description
                default:
                    break;
            }
        });
    }
}
