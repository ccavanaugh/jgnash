/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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

import java.util.Objects;
import java.util.Optional;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.util.AccountTypeFilter;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.views.main.MainView;
import jgnash.util.Nullable;

/**
 * Static support methods for Account manipulation.
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
        dialog.initOwner(MainView.getPrimaryStage());
        dialog.setTitle(ResourceUtils.getString("Title.VisibleAccountTypes"));

        final AccountTypeFilterFormController controller = FXMLUtils.loadFXML(o -> dialog.setScene(new Scene((Parent) o)),
                "AccountTypeFilterForm.fxml", ResourceUtils.getBundle());

        ThemeManager.applyStyleSheets(dialog.getScene());

        controller.setAccountTypeFilter(accountTypeFilter);

        dialog.setResizable(false);

        StageUtils.addBoundsListener(dialog, AccountTypeFilterFormController.class, MainView.getPrimaryStage());

        dialog.showAndWait();
    }

    static void showNewAccountPropertiesDialog(@Nullable final Account parentAccount) {
        final Stage dialog = new Stage(StageStyle.DECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(MainView.getPrimaryStage());
        dialog.setTitle(ResourceUtils.getString("Title.NewAccount"));

        final AccountPropertiesController controller = FXMLUtils.loadFXML(o -> dialog.setScene(new Scene((Parent) o)),
                "AccountProperties.fxml", ResourceUtils.getBundle());

        ThemeManager.applyStyleSheets(dialog.getScene());

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        controller.setSelectedCurrency(engine.getDefaultCurrency());

        controller.setParentAccount(parentAccount != null ? parentAccount : engine.getRootAccount());

        dialog.setResizable(false);

        StageUtils.addBoundsListener(dialog, AccountPropertiesController.class, MainView.getPrimaryStage());

        dialog.showAndWait();

        if (controller.getResult()) {
            Account account = controller.getTemplate();

            engine.addAccount(account.getParent(), account);

            if (account.getAccountType().getAccountGroup() == AccountGroup.INVEST) {
                if (!engine.updateAccountSecurities(account, controller.getSecurityNodes())) {
                    StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.SecurityAccountUpdate"));
                }
            }
        }
    }

    static void showModifyAccountProperties(final Account account) {
        final Stage dialog = new Stage(StageStyle.DECORATED);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(MainView.getPrimaryStage());
        dialog.setTitle(ResourceUtils.getString("Title.ModifyAccount"));

        final AccountPropertiesController controller = FXMLUtils.loadFXML(o -> dialog.setScene(new Scene((Parent) o)),
                "AccountProperties.fxml", ResourceUtils.getBundle());

        ThemeManager.applyStyleSheets(dialog.getScene());

        controller.loadProperties(account);

        dialog.setResizable(false);

        StageUtils.addBoundsListener(dialog, AccountPropertiesController.class, MainView.getPrimaryStage());

        dialog.showAndWait();

        if (controller.getResult()) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            Account template = controller.getTemplate();

            if (!engine.modifyAccount(template, account)) {
                StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.AccountUpdate"));
            }

            if (account.getAccountType().getAccountGroup() == AccountGroup.INVEST) {
                if (!engine.updateAccountSecurities(account, controller.getSecurityNodes())) {
                    StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.SecurityAccountUpdate"));
                }
            }
        }
    }

    public static Optional<Account> selectAccount(@Nullable final Account parentAccount, @Nullable final Account... excluded) {
        final FXMLUtils.Pair<SelectAccountController> pair =
                FXMLUtils.load(SelectAccountController.class.getResource("SelectAccountForm.fxml"),
                        ResourceUtils.getString("Title.ParentAccount"));

        if (parentAccount != null) {
            pair.getController().setSelectedAccount(parentAccount);
        }

        // add excluded accounts if any
        pair.getController().addExcludeAccounts(excluded);

        pair.getStage().showAndWait();

        return pair.getController().getSelectedAccount();
    }
}
