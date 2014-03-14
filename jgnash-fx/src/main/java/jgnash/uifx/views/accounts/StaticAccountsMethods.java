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
package jgnash.uifx.views.accounts;

import java.io.IOException;

import jgnash.MainFX;
import jgnash.uifx.MainApplication;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.controllers.AccountTypeFilter;
import jgnash.uifx.controllers.AccountTypeFilterFormController;
import jgnash.uifx.utils.StageUtils;
import jgnash.util.ResourceUtils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * @author Craig Cavanaugh
 */
public class StaticAccountsMethods {

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

    public static void showAccountPropertyDialog() {
        try {
            Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(MainApplication.getPrimaryStage());
            dialog.setTitle(ResourceUtils.getBundle().getString("Title.ModifyAccount"));

            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource("fxml/AccountProperties.fxml"), ResourceUtils.getBundle());
            dialog.setScene(new Scene(loader.load()));

            dialog.setResizable(false);

            dialog.getScene().getStylesheets().add(MainApplication.DEFAULT_CSS);
            dialog.getScene().getRoot().getStyleClass().addAll("form", "dialog");

            StageUtils.addBoundsListener(dialog, StaticUIMethods.class);

            dialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
