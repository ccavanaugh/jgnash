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
package jgnash.uifx.control;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.resource.font.MaterialDesignLabel;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;

/**
 * Exception dialog.
 *
 * @author Craig Cavanaugh
 */
public class ExceptionDialog {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private Button clipboardButton;

    @FXML
    private TextArea textArea;

    @FXML
    private Button closeButton;

    @FXML
    private Label message;

    private final Stage dialog;

    private String stackTrace;

    private final Throwable throwable;

    public ExceptionDialog(final Throwable throwable) {
        final ResourceBundle resources = ResourceUtils.getBundle();

        this.throwable = throwable;

        dialog = FXMLUtils.loadFXML(this, "ExceptionDialog.fxml", resources);
        dialog.setTitle(resources.getString("Title.UncaughtException"));
    }

    @FXML
    private void initialize() {
        message.setGraphic(new MaterialDesignLabel(MaterialDesignLabel.MDIcon.EXCLAMATION_TRIANGLE,
                ThemeManager.getBaseTextHeight() * Alert.HEIGHT_MULTIPLIER, Color.DARKRED));

        closeButton.setOnAction(event -> ((Stage) parent.get().getWindow()).close());

        clipboardButton.setOnAction(event -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            Map<DataFormat, Object> map = new HashMap<>();
            map.put(DataFormat.PLAIN_TEXT, stackTrace);
            clipboard.setContent(map);
        });
    }

    public void showAndWait() {
        message.setText(throwable.getLocalizedMessage());

        final StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));

        stackTrace = sw.toString();
        textArea.setText(stackTrace);

        dialog.setResizable(false);
        dialog.showAndWait();
    }
}
