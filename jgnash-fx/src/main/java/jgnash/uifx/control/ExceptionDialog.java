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
package jgnash.uifx.control;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import jgnash.uifx.util.FXMLUtils;
import jgnash.util.ResourceUtils;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

/**
 * Exception dialog
 *
 * @author Craig Cavanaugh
 */
public class ExceptionDialog {

    @FXML
    private Button clipboardButton;

    @FXML
    private TextArea textArea;

    @FXML
    private Button closeButton;

    @FXML
    private Label message;

    private Stage dialog;

    private final String stackTrace;

    public ExceptionDialog(final Throwable throwable) {
        final ResourceBundle resources = ResourceUtils.getBundle();

        dialog = FXMLUtils.loadFXML(this, "ExceptionDialog.fxml", resources);
        dialog.setTitle(resources.getString("Title.UncaughtException"));

        message.setText(throwable.getLocalizedMessage());

        final StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));

        stackTrace = sw.toString();
        textArea.setText(stackTrace);
    }

    @FXML
    private void initialize() {
        final GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");

        message.setGraphic(fontAwesome.create(FontAwesome.Glyph.EXCLAMATION_TRIANGLE)
                .color(Color.DARKRED).size(Alert.ICON_SIZE));

        closeButton.setOnAction(event -> ((Stage)((Node)event.getSource()).getScene().getWindow()).close());

        clipboardButton.setOnAction(event -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            Map<DataFormat, Object> map = new HashMap<>();
            map.put(DataFormat.PLAIN_TEXT, stackTrace);
            clipboard.setContent(map);
        });
    }

    public void showAndWait() {
        dialog.setResizable(false);
        dialog.showAndWait();
    }
}
