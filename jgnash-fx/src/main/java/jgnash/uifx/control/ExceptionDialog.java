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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.uifx.MainApplication;
import jgnash.util.ResourceUtils;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

/**
 * @author Craig Cavanaugh
 */
public class ExceptionDialog implements Initializable {

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

        dialog = new Stage(StageStyle.DECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(MainApplication.getPrimaryStage());
        dialog.setTitle(resources.getString("Title.UncaughtException"));

        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ExceptionDialog.fxml"), resources);
            fxmlLoader.setController(this);
            dialog.setScene(new Scene(fxmlLoader.load()));
        } catch (final IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
        }

        message.setText(throwable.getLocalizedMessage());

        final StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));

        stackTrace = sw.toString();
        textArea.setText(stackTrace);
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
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
