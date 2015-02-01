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
package jgnash.uifx.views.accounts;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.MainApplication;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.controllers.AccountTypeFilterFormController;
import jgnash.uifx.util.AccountTypeFilter;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.util.Nullable;
import jgnash.util.ResourceUtils;

/**
 * Static support methods for Account manipulation
 *
 * @author Craig Cavanaugh
 */
public final class StaticAccountsMethods {

    private StaticAccountsMethods() {
        // Utility class
    }

    public static void showAccountFilterDialog(final AccountTypeFilter accountTypeFilter) {
        final Stage dialog = new Stage(StageStyle.DECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(MainApplication.getPrimaryStage());
        dialog.setTitle(ResourceUtils.getBundle().getString("Title.VisibleAccountTypes"));

        AccountTypeFilterFormController controller = FXMLUtils.loadFXML(o -> dialog.setScene(new Scene((Parent) o)),
                "AccountTypeFilterForm.fxml", ResourceUtils.getBundle());

        controller.setAccountTypeFilter(accountTypeFilter);

        dialog.setResizable(false);

        StageUtils.addBoundsListener(dialog, AccountTypeFilterFormController.class);

        dialog.showAndWait();
    }

    public static void showNewAccountPropertiesDialog() {
        final Stage dialog = new Stage(StageStyle.DECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(MainApplication.getPrimaryStage());
        dialog.setTitle(ResourceUtils.getBundle().getString("Title.NewAccount"));

        final AccountPropertiesController controller = FXMLUtils.loadFXML(o -> dialog.setScene(new Scene((Parent) o)),
                "AccountProperties.fxml", ResourceUtils.getBundle());

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        controller.setSelectedCurrency(engine.getDefaultCurrency());
        controller.setParentAccount(engine.getRootAccount());

        dialog.setResizable(false);

        StageUtils.addBoundsListener(dialog, AccountPropertiesController.class);

        dialog.showAndWait();

        if (controller.getResult()) {
            Account account = controller.getTemplate();

            engine.addAccount(account.getParent(), account);
        }
    }

    public static void showModifyAccountProperties(final Account account) {
        final Stage dialog = new Stage(StageStyle.DECORATED);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(MainApplication.getPrimaryStage());
        dialog.setTitle(ResourceUtils.getBundle().getString("Title.ModifyAccount"));

        final AccountPropertiesController controller = FXMLUtils.loadFXML(o -> dialog.setScene(new Scene((Parent) o)),
                "AccountProperties.fxml", ResourceUtils.getBundle());

        controller.loadProperties(account);

        dialog.setResizable(false);

        StageUtils.addBoundsListener(dialog, AccountPropertiesController.class);

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
    }

    public static Optional<Account> selectAccount(@Nullable final Account parentAccount, @Nullable final Account... excluded) {
        try {
            final Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(MainApplication.getPrimaryStage());
            dialog.setTitle(ResourceUtils.getBundle().getString("Title.ParentAccount"));

            final FXMLLoader loader = new FXMLLoader(SelectAccountController.class.getResource("SelectAccountForm.fxml"), ResourceUtils.getBundle());
            dialog.setScene(new Scene(loader.load()));

            final SelectAccountController controller = loader.getController();
            dialog.setResizable(false);

            StageUtils.addBoundsListener(dialog, AccountPropertiesController.class);

            if (parentAccount != null) {
                controller.setSelectedAccount(parentAccount);
            }

            // add excluded accounts if any
            controller.addExcludeAccounts(excluded);

            dialog.showAndWait();

            return controller.getSelectedAccount();
        } catch (final IOException ex) {
            Logger.getLogger(StaticAccountsMethods.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            return Optional.empty();
        }
    }
}
