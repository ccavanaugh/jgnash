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

import java.util.Objects;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.engine.Account;
import jgnash.engine.AmortizeObject;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.views.main.MainView;

public class LiabilityRegisterPaneController extends BankRegisterPaneController {

    // TODO:  CSS lookup
    private final DoubleProperty titledPanePadding = new SimpleDoubleProperty(37);

    @FXML
    private ButtonBar buttonBar;

    @FXML
    @Override
    public void initialize() {
        super.initialize();

        buttonBar.minWidthProperty().bind(titledPane.widthProperty().subtract(titledPanePadding));
    }

    @FXML
    private void handleAmortizeAction() {
        final Stage dialog = new Stage(StageStyle.DECORATED);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(MainView.getPrimaryStage());
        dialog.setTitle(ResourceUtils.getString("Title.AmortizationSetup"));

        final AmortizeSetupDialogController controller = FXMLUtils.loadFXML(o -> dialog.setScene(new Scene((Parent) o)),
                "AmortizeSetupDialog.fxml", ResourceUtils.getBundle());

        if (accountProperty().get().getAmortizeObject() != null) {
            controller.setAmortizeObject(accountProperty().get().getAmortizeObject());
        }

        ThemeManager.applyStyleSheets(dialog.getScene());

        dialog.setResizable(false);

        StageUtils.addBoundsListener(dialog, AmortizeSetupDialogController.class, MainView.getPrimaryStage());

        dialog.showAndWait();

        if (controller.getResult()) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

            Objects.requireNonNull(engine);

            if (!engine.setAmortizeObject(accountProperty().get(), controller.getAmortizeObject())) {
                StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.AmortizationSave"));
            }
        }
    }

    @FXML
    private void handleNewPaymentAction() {
        AmortizeObject amortizeObject = accountProperty().get().getAmortizeObject();

        if (amortizeObject != null) {

            final Account account = accountProperty().get();

            final Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(MainView.getPrimaryStage());
            dialog.setTitle(ResourceUtils.getString("Title.NewTrans"));

            final DateTransNumberDialogController controller = FXMLUtils.loadFXML(o -> dialog.setScene(new Scene((Parent) o)),
                    "DateTransNumberDialog.fxml", ResourceUtils.getBundle());

            controller.setAccount(amortizeObject.getBankAccount());

            ThemeManager.applyStyleSheets(dialog.getScene());

            dialog.setResizable(false);
            StageUtils.addBoundsListener(dialog, DateTransNumberDialogController.class, MainView.getPrimaryStage());

            dialog.showAndWait();

            if (!controller.getResult()) {
                return;
            }

            final Transaction tran = amortizeObject.generateTransaction(account, controller.getDate(),
                    controller.getNumber());

            if (tran != null) {// display the transaction in the register
                TransactionDialog.showAndWait(amortizeObject.getBankAccount(), tran, transaction -> {
                    final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                    Objects.requireNonNull(engine);

                    engine.addTransaction(transaction);
                });

            } else {
                StaticUIMethods.displayWarning(resources.getString("Message.Warn.ConfigAmortization"));
            }
        } else { // could not generate the transaction
            StaticUIMethods.displayWarning(resources.getString("Message.Warn.ConfigAmortization"));
        }

    }
}
