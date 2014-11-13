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

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.tasks.CloseFileTask;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

/**
 * @author Craig Cavanaugh
 */
public class MainToolBarController implements Initializable, MessageListener {

    @FXML
    public Button closeButton;

    @FXML
    public Button updateCurrencies;

    @FXML
    Button updateSecurities;

    @FXML
    Button openButton;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        final GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

        closeButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.TIMES));
        openButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.FOLDER_OPEN));
        updateSecurities.setGraphic(fontAwesome.create(FontAwesome.Glyph.CLOUD_DOWNLOAD));
        updateCurrencies.setGraphic(fontAwesome.create(FontAwesome.Glyph.CLOUD_DOWNLOAD));

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM);
    }

    @FXML
    protected void handleOpenAction(final ActionEvent event) {
        StaticUIMethods.showOpenDialog();
    }

    @FXML
    public void handleCloseAction(ActionEvent actionEvent) {
        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
            CloseFileTask.initiateShutdown();
        }
    }

    @Override
    public void messagePosted(final Message event) {
        switch (event.getEvent()) {
            case FILE_NEW_SUCCESS:
            case FILE_LOAD_SUCCESS:
                updateSecurities.setDisable(false);
                updateCurrencies.setDisable(false);
                break;
            case FILE_CLOSING:
            case FILE_LOAD_FAILED:
                updateSecurities.setDisable(true);
                updateCurrencies.setDisable(true);
                break;
            default:
                break;
        }
    }

    @FXML
    void handleSecuritiesUpdateAction(final ActionEvent actionEvent) {
        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        if (engine != null) {
            engine.startSecuritiesUpdate(0);
        }
    }

    @FXML
    public void handleCurrenciesUpdateAction(ActionEvent actionEvent) {
        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        if (engine != null) {
            engine.startExchangeRateUpdate(0);
        }
    }
}
