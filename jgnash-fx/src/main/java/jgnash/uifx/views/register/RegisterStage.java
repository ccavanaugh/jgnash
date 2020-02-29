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
package jgnash.uifx.views.register;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Transaction;
import jgnash.resource.util.ResourceUtils;
import jgnash.resource.util.Version;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.util.NotNull;

/**
 * A Stage that displays a single account register. Size and position is preserved.
 *
 * @author Craig Cavanaugh
 */
public class RegisterStage extends Stage {

    /**
     * Static list of register stages.
     */
    final private static ListProperty<RegisterStage> registerStageList
            = new SimpleListProperty<>(FXCollections.observableArrayList());

    private static final double SCALE_FACTOR = 0.7;

    final private ReadOnlyObjectWrapper<Account> account = new ReadOnlyObjectWrapper<>();

    private final RegisterPaneController controller;

    private RegisterStage(@NotNull final Account account) {
        super(StageStyle.DECORATED);

        this.account.set(account);

        final String formResource;

        if (account.isLocked()) {
            if (account.memberOf(AccountGroup.INVEST)) {
                formResource = "LockedInvestmentRegisterPane.fxml";
            } else {
                formResource = "LockedBasicRegisterPane.fxml";
            }
        } else {
            if (account.memberOf(AccountGroup.INVEST)) {
                formResource = "InvestmentRegisterPane.fxml";
            } else {
                formResource = "BasicRegisterPane.fxml";
            }
        }

        controller = FXMLUtils.loadFXML(scene -> setScene(new Scene((Parent) scene)), formResource,
                ResourceUtils.getBundle());

        ThemeManager.applyStyleSheets(getScene());

        getIcons().add(StaticUIMethods.getApplicationIcon());

        // handle CTRL-F4
        getScene().setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.F4) {
                close();
            }
        });

        double minWidth = Double.MAX_VALUE;
        double minHeight = Double.MAX_VALUE;

        for (final Screen screen : Screen.getScreens()) {
            minWidth = Math.min(minWidth, screen.getVisualBounds().getWidth());
            minHeight = Math.min(minHeight, screen.getVisualBounds().getHeight());
        }

        setWidth(minWidth * SCALE_FACTOR);
        setHeight(minHeight * SCALE_FACTOR);

        // Push the account to the controller at the end of the application thread
        // Must use JavaFXUtils instead of Platform to prevent a race condition
        JavaFXUtils.runLater(() -> controller.accountProperty().set(account));

        updateTitle(account);

        StageUtils.addBoundsListener(this, account.getUuid().toString(), null);

        registerStageList.get().add(this);

        setOnHidden(event -> registerStageList.get().remove(RegisterStage.this));
    }

    public static RegisterStage getRegisterStage(@NotNull final Account account) {

        // look for an existing stage first
        for (final RegisterStage registerStage : registerStageList) {
            if (registerStage.account.get().equals(account)) {
                registerStage.requestFocus();
                return registerStage;
            }
        }

        return new RegisterStage(account);
    }

    public void show(final Transaction transaction) {
        show();
        JavaFXUtils.runLater(() -> controller.selectTransaction(transaction));
    }

    public ReadOnlyObjectProperty<Account> accountProperty() {
        return account.getReadOnlyProperty();
    }

    public static ListProperty<RegisterStage> registerStageList() {
        return registerStageList;
    }

    private void updateTitle(final Account account) {
        setTitle(Version.getAppName() + " - " + account.getPathName());
    }
}
