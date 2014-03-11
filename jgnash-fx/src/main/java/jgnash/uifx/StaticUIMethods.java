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

import jgnash.MainFX;
import jgnash.uifx.controllers.AccountTypeFilter;
import jgnash.uifx.controllers.AccountTypeFilterFormController;
import jgnash.uifx.utils.StageUtils;
import jgnash.util.ResourceUtils;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.controlsfx.dialog.Dialogs;

/**
 * Various static UI support methods
 *
 * @author Craig Cavanaugh
 */
public class StaticUIMethods {

    private StaticUIMethods() {
        // Utility class
    }

    public static void showAccountFilterDialog(final AccountTypeFilter accountTypeFilter) {
        try {
            Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(MainApplication.getPrimaryStage());
            dialog.setTitle(ResourceUtils.getBundle().getString("Title.VisibleAccountTypes"));

            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource("fxml/AccountTypeFilterForm.fxml"), ResourceUtils.getBundle());
            dialog.setScene(new Scene(loader.load()));

            AccountTypeFilterFormController controller = loader.getController();
            controller.setAccountTypeFilter(accountTypeFilter);

            dialog.setResizable(false);

            dialog.getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);
            dialog.getScene().getRoot().getStyleClass().addAll("form", "dialog");

            StageUtils.addBoundsListener(dialog, StaticUIMethods.class);

            dialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void showOpenDialog() {
        try {
            Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(MainApplication.getPrimaryStage());
            dialog.setTitle(ResourceUtils.getBundle().getString("Title.Open"));
            dialog.setScene(new Scene(FXMLLoader.load(MainFX.class.getResource("fxml/OpenDatabaseForm.fxml"), ResourceUtils.getBundle())));

            dialog.setResizable(false);

            dialog.getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);
            dialog.getScene().getRoot().getStyleClass().addAll("form", "dialog");

            StageUtils.addBoundsListener(dialog, StaticUIMethods.class);

            dialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void displayError(final String message) {
        Dialogs.create()
                .owner(MainApplication.getPrimaryStage())
                .title(ResourceUtils.getBundle().getString("Title.Error"))
                .message(message)
                .showError();

    }

    public static void displayException(final Throwable exception) {
        Dialogs.create()
                .owner(MainApplication.getPrimaryStage())
                .title(ResourceUtils.getBundle().getString("Title.Error"))
                .message(exception.getLocalizedMessage())
                .showException(exception);
    }

    public static void displayTaskProgress(final Task task) {
        Dialogs.create()
                .owner(MainApplication.getPrimaryStage())
                .lightweight()
                .title(ResourceUtils.getBundle().getString("Title.PleaseWait"))
                .showWorkerProgress(task);
    }
}
