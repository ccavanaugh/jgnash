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
package jgnash.uifx;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import jgnash.MainFX;
import jgnash.engine.EngineFactory;
import jgnash.uifx.utils.StageUtils;
import jgnash.util.ResourceUtils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Primary Menu Controller
 *
 * @author Craig Cavanaugh
 */
public class MenuBarController implements Initializable {

    public static final int FORCED_DELAY = 1000;

    public static final int INDETERMINATE = -1;

    private ResourceBundle resources;

    @Override
    public void initialize(final URL url, final ResourceBundle resourceBundle) {
        resources = resourceBundle;
    }

    @FXML protected void handleExitAction(final ActionEvent event) {

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                try {
                    updateMessage(resources.getString("Message.SavingFile"));
                    updateProgress(INDETERMINATE, Long.MAX_VALUE);
                    EngineFactory.closeEngine(EngineFactory.DEFAULT);
                    updateMessage(resources.getString("Message.FileSaveComplete"));
                    Thread.sleep(FORCED_DELAY);
                } catch (final Exception exception) {
                    Platform.runLater(() -> StaticUIMethods.displayException(exception));
                } finally {
                    Platform.exit();
                }
                return resources.getString("Message.FileSaveComplete");
            }
        };

        new Thread(task).start();
        StaticUIMethods.displayTaskProgress(task);
    }

    @FXML protected void handleOpenAction(final ActionEvent event) {
        try {
            Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(MainApplication.getPrimaryStage());
            dialog.setScene(new Scene(FXMLLoader.load(MainFX.class.getResource("fxml/OpenDatabaseForm.fxml"), ResourceUtils.getBundle())));

            dialog.setResizable(false);

            StageUtils.addBoundsListener(dialog, getClass());

            dialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
