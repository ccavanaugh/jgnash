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

import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.util.AccountTypeFilter;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.views.main.MainApplication;
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
        dialog.initOwner(MainApplication.getInstance().getPrimaryStage());
        dialog.setTitle(ResourceUtils.getBundle().getString("Title.VisibleAccountTypes"));

        final AccountTypeFilterFormController controller = FXMLUtils.loadFXML(o -> dialog.setScene(new Scene((Parent) o)),
                "AccountTypeFilterForm.fxml", ResourceUtils.getBundle());

        dialog.getScene().getStylesheets().addAll(MainApplication.DEFAULT_CSS);

        controller.setAccountTypeFilter(accountTypeFilter);

        dialog.setResizable(false);

        StageUtils.addBoundsListener(dialog, AccountTypeFilterFormController.class);

        dialog.showAndWait();
    }

    public static void showNewAccountPropertiesDialog(@Nullable final Account parentAccount) {
        final Stage dialog = new Stage(StageStyle.DECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(MainApplication.getInstance().getPrimaryStage());
        dialog.setTitle(ResourceUtils.getBundle().getString("Title.NewAccount"));

        final AccountPropertiesController controller = FXMLUtils.loadFXML(o -> dialog.setScene(new Scene((Parent) o)),
                "AccountProperties.fxml", ResourceUtils.getBundle());

        dialog.getScene().getStylesheets().addAll(MainApplication.DEFAULT_CSS);

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        controller.setSelectedCurrency(engine.getDefaultCurrency());

        controller.setParentAccount(parentAccount != null ? parentAccount : engine.getRootAccount());

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
        dialog.initOwner(MainApplication.getInstance().getPrimaryStage());
        dialog.setTitle(ResourceUtils.getBundle().getString("Title.ModifyAccount"));

        final AccountPropertiesController controller = FXMLUtils.loadFXML(o -> dialog.setScene(new Scene((Parent) o)),
                "AccountProperties.fxml", ResourceUtils.getBundle());

        dialog.getScene().getStylesheets().addAll(MainApplication.DEFAULT_CSS);

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
        final ObjectProperty<SelectAccountController> controllerObjectProperty = new SimpleObjectProperty<>();

        final URL fxmlUrl = SelectAccountController.class.getResource("SelectAccountForm.fxml");
        final Stage stage = FXMLUtils.loadFXML(fxmlUrl, controllerObjectProperty, ResourceUtils.getBundle());
        stage.setTitle(ResourceUtils.getString("Title.ParentAccount"));

        if (parentAccount != null) {
            controllerObjectProperty.get().setSelectedAccount(parentAccount);
        }

        // add excluded accounts if any
        controllerObjectProperty.get().addExcludeAccounts(excluded);

        stage.showAndWait();

        return controllerObjectProperty.get().getSelectedAccount();
    }
}
