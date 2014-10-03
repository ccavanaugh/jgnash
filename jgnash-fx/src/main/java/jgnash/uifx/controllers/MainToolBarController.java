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

import jgnash.engine.message.Message;
import jgnash.engine.message.MessageListener;
import jgnash.uifx.StaticUIMethods;

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
    Button openButton;

    @FXML
    Button cutButton;

    @FXML
    Button copyButton;

    @FXML
    Button pasteButton;

    @FXML
    protected void handleOpenAction(final ActionEvent event) {
        StaticUIMethods.showOpenDialog();
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        final GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

        cutButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.CUT));
        copyButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.COPY));
        openButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.FOLDER_OPEN));
        pasteButton.setGraphic(fontAwesome.create(FontAwesome.Glyph.PASTE));
    }

    @Override
    public void messagePosted(Message event) {

    }
}
