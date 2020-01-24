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
package jgnash.uifx.views.main;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import jgnash.engine.Engine;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.resource.util.ResourceUtils;

/**
 * Controller for the console dialog.
 *
 * @author Craig Cavanaugh
 */
public class ConsoleDialogController {

    private static final long BYTES_PER_MB = 1024000;

    private  static final int REFRESH_PERIOD = 500;

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private ProgressBar memoryUsageProgressBar;

    @FXML
    private Text memoryUsageText;

    @FXML
    private TextArea consoleArea;

    private PrintStream oldOutStream;

    private PrintStream oldErrStream;

    private PrintStream outStream;

    private PrintStream errStream;

    private final LogHandler logHandler = new LogHandler();

    private Timeline timeline;

    private final Runtime runtime = Runtime.getRuntime();

    private static final AtomicBoolean visible = new AtomicBoolean(false);

    @FXML
    void initialize() {
        oldErrStream = System.err;
        oldOutStream = System.out;

        // Force a monospaced font
        consoleArea.setFont(Font.font("Monospaced", consoleArea.getFont().getSize()));

        Engine.getLogger().addHandler(logHandler);

        try {
            outStream = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                    oldOutStream.write(b);
                    JavaFXUtils.runLater(() -> consoleArea.appendText(String.valueOf((char) b)));
                }
            }, false, Charset.defaultCharset().name());
        } catch (final UnsupportedEncodingException ex) {
            Logger.getLogger(ConsoleDialogController.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            errStream = new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                    oldErrStream.write(b);
                    JavaFXUtils.runLater(() -> consoleArea.appendText(String.valueOf((char) b)));
                }
            }, false, Charset.defaultCharset().name());
        } catch (final UnsupportedEncodingException ex) {
            Logger.getLogger(ConsoleDialogController.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Plug in the new streams
        System.setOut(outStream);
        System.setErr(errStream);

        timeline = new Timeline(new KeyFrame(Duration.millis(REFRESH_PERIOD), new EventHandler<>() {
            private long total;

            private long used;

            private long oldUsed;

            private static final int diff = 1;

            @Override
            public void handle(ActionEvent event) {
                total = runtime.totalMemory() / BYTES_PER_MB;
                used = total - runtime.freeMemory() / BYTES_PER_MB;
                if (used < oldUsed - diff || used > oldUsed + diff) {
                    JavaFXUtils.runLater(() -> {
                        memoryUsageProgressBar.setProgress((double) used / (double) total);
                        memoryUsageText.setText(used + "/" + total + " MB");
                    });
                    oldUsed = used;
                }
            }
        }));

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        // Close with the main application
        MainView.getPrimaryStage()
                .addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, event -> handleCloseAction());
    }

    @FXML
    private void handleCloseAction() {
        System.setErr(oldErrStream);
        System.setOut(oldOutStream);

        Engine.getLogger().removeHandler(logHandler);

        timeline.stop();

        ((Stage) parent.get().getWindow()).close();
        visible.set(false);
    }

    @FXML
    private void handleForceGarbageCollection() {
        System.gc();
    }

    @FXML
    private void handleCopyToClipboard() {
        final ClipboardContent content = new ClipboardContent();
        content.putString(consoleArea.getText());

        Clipboard.getSystemClipboard().setContent(content);
    }

    public static void show() {
        if (!visible.get()) {
            visible.set(true);

            final FXMLUtils.Pair<ConsoleDialogController> pair =
                    FXMLUtils.load(ConsoleDialogController.class.getResource("ConsoleDialog.fxml"),
                            ResourceUtils.getString("Title.Console"));

            // Override the defaults set by FXMLUtils
            pair.getStage().initModality(Modality.NONE);
            pair.getStage().initOwner(null);

            pair.getStage().show();
        }
    }

    private class LogHandler extends Handler {
        @Override
        public void publish(LogRecord record) {
            JavaFXUtils.runLater(() -> consoleArea.appendText(record.getMessage() + System.lineSeparator()));
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }
}
