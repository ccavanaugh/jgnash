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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.MainFX;
import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
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
 * Static support methods for Account manipulation
 *
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

            StageUtils.applyDialogFormCSS(dialog);
            StageUtils.addBoundsListener(dialog, StaticUIMethods.class);

            dialog.show();
        } catch (final IOException e) {
            Logger.getLogger(StaticAccountsMethods.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    public static void showNewAccountPropertiesDialog() {
        try {
            Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(MainApplication.getPrimaryStage());
            dialog.setTitle(ResourceUtils.getBundle().getString("Title.ModifyAccount"));

            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource("fxml/AccountProperties.fxml"), ResourceUtils.getBundle());
            dialog.setScene(new Scene(loader.load()));

            AccountPropertiesController controller = loader.getController();

            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            controller.setSelectedCurrency(engine.getDefaultCurrency());
            controller.setParentAccount(engine.getRootAccount());

            dialog.setResizable(false);

            StageUtils.applyDialogFormCSS(dialog);
            StageUtils.addBoundsListener(dialog, StaticUIMethods.class);

            dialog.showAndWait();

            if (controller.getResult()) {
                Account account = controller.getTemplate();

                engine.addAccount(account.getParent(), account);
            }
        } catch (final IOException e) {
            Logger.getLogger(StaticAccountsMethods.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    public static void showModifyAccountProperties(final Account account) {
        try {
            Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(MainApplication.getPrimaryStage());
            dialog.setTitle(ResourceUtils.getBundle().getString("Title.ModifyAccount"));

            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource("fxml/AccountProperties.fxml"), ResourceUtils.getBundle());
            dialog.setScene(new Scene(loader.load()));

            AccountPropertiesController controller = loader.getController();

            controller.loadProperties(account);

            dialog.setResizable(false);

            StageUtils.applyDialogFormCSS(dialog);
            StageUtils.addBoundsListener(dialog, StaticUIMethods.class);

            dialog.showAndWait();

            if (controller.getResult()) {
                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);

                Account template = controller.getTemplate();

                if (!engine.modifyAccount(template, account)) {
                    StaticUIMethods.displayError(ResourceUtils.getBundle().getString("Message.Error.AccountUpdate"));
                }

                if (account.getAccountType().getAccountGroup() == AccountGroup.INVEST) {
                    if (!engine.updateAccountSecurities(account, controller.getSecurityNodes())) {
                        StaticUIMethods.displayError(ResourceUtils.getBundle().getString("Message.Error.SecurityAccountUpdate"));
                    }
                }
            }

        } catch (final IOException e) {
            Logger.getLogger(StaticAccountsMethods.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
