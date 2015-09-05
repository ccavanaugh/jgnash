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
package jgnash.uifx.views.main;

import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.about.AboutDialogController;
import jgnash.uifx.actions.DefaultCurrencyAction;
import jgnash.uifx.actions.DefaultLocaleAction;
import jgnash.uifx.dialog.currency.AddRemoveCurrencyController;
import jgnash.uifx.dialog.currency.EditExchangeRatesController;
import jgnash.uifx.dialog.currency.ModifyCurrencyController;
import jgnash.uifx.dialog.options.OptionDialogController;
import jgnash.uifx.dialog.options.TransactionNumberDialogController;
import jgnash.uifx.dialog.security.CreateModifySecuritiesController;
import jgnash.uifx.dialog.security.SecurityHistoryController;
import jgnash.uifx.skin.BaseColorDialogController;
import jgnash.uifx.skin.FontSizeDialogController;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.tasks.CloseFileTask;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.views.register.RegisterStage;
import jgnash.uifx.wizard.file.NewFileWizard;

/**
 * Primary Menu Controller
 *
 * @author Craig Cavanaugh
 */
public class MenuBarController implements MessageListener {

    @FXML
    private MenuItem transNumberListMenuItem;

    @FXML
    private MenuItem optionsMenuItem;

    @FXML
    private Menu themesMenu;

    @FXML
    private Menu currenciesMenu;

    @FXML
    private Menu securitiesMenu;

    @FXML
    private Menu windowMenu;

    @FXML
    private MenuBar menuBar;

    @FXML
    private MenuItem openMenuItem;

    @FXML
    private MenuItem closeMenuItem;

    @FXML
    private MenuItem exitMenuItem;

    private final BooleanProperty disabled = new SimpleBooleanProperty(true);

    @FXML
    private ResourceBundle resources;

    @FXML
    private void initialize() {
        securitiesMenu.disableProperty().bind(disabled);
        currenciesMenu.disableProperty().bind(disabled);
        closeMenuItem.disableProperty().bind(disabled);
        optionsMenuItem.disableProperty().bind(disabled);
        transNumberListMenuItem.disableProperty().bind(disabled);

        windowMenu.disableProperty().bind(Bindings.or(disabled, RegisterStage.registerStageListProperty().emptyProperty()));

        RegisterStage.registerStageListProperty().addListener((ListChangeListener<RegisterStage>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(MenuBarController.this::addWindowMenuItem);
                } else if (c.wasRemoved()) {
                    c.getAddedSubList().forEach(MenuBarController.this::removeWindowMenuItem);
                }
            }
        });

        ThemeManager.addKnownThemes(themesMenu);

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM);
    }

    private void addWindowMenuItem(final RegisterStage registerStage) {
        final MenuItem menuItem = new MenuItem(registerStage.accountProperty().get().getName());
        menuItem.setUserData(registerStage);

        menuItem.setOnAction(event -> {
            final RegisterStage stage = (RegisterStage) menuItem.getUserData();
            stage.requestFocus();
        });

        registerStage.setOnHiding(event -> windowMenu.getItems().removeAll(menuItem));

        windowMenu.getItems().add(0, menuItem);
    }

    private void removeWindowMenuItem(final RegisterStage registerStage) {
        windowMenu.getItems().stream().filter(item -> item.getUserData() == registerStage).
                forEach(item -> windowMenu.getItems().remove(item));
    }

    @FXML
    private void handleExitAction() {
        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
            CloseFileTask.initiateShutdown();
        } else {
            Platform.exit();
        }
    }

    @FXML
    private void handleCloseAction() {
        if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {
            CloseFileTask.initiateFileClose();
        }
    }

    @FXML
    private void handleOpenAction() {
        StaticUIMethods.showOpenDialog();
    }

    @FXML
    private void updateSecurities() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        Objects.requireNonNull(engine);

        engine.startSecuritiesUpdate(0);
    }

    @FXML
    private void updateCurrencies() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        Objects.requireNonNull(engine);

        engine.startExchangeRateUpdate(0);
    }

    @FXML
    private void handleNewAction() {
        NewFileWizard.show();
    }

    @FXML
    private void handleAboutAction() {
        AboutDialogController.showAndWait();
    }

    @Override
    public void messagePosted(final Message event) {
        Platform.runLater(() -> {
            switch (event.getEvent()) {
                case FILE_LOAD_SUCCESS:
                case FILE_NEW_SUCCESS:
                    disabled.setValue(false);
                    break;
                case FILE_CLOSING:
                    disabled.setValue(true);
                    break;
                default:
                    break;
            }
        });
    }

    @FXML
    private void changeDefaultLocale() {
        DefaultLocaleAction.showAndWait();
    }

    @FXML
    private void closeAllWindows() {
        // create a copy to avoid concurrent modification issues
        final ArrayList<RegisterStage> registerStages = new ArrayList<>(RegisterStage.registerStageListProperty().get());

        registerStages.forEach(RegisterStage::close);
    }

    @FXML
    private void handleCreateModifySecuritiesAction() {
        final URL fxmlUrl = CreateModifySecuritiesController.class.getResource("CreateModifySecurities.fxml");
        final Stage stage = FXMLUtils.loadFXML(fxmlUrl, resources);
        stage.setTitle(resources.getString("Title.CreateModifyCommodities"));

        stage.showAndWait();
    }

    @FXML
    private void handleSecuritiesHistoryAction() {
        final URL fxmlUrl = SecurityHistoryController.class.getResource("SecurityHistory.fxml");
        final Stage stage = FXMLUtils.loadFXML(fxmlUrl, resources);
        stage.setTitle(resources.getString("Title.ModifySecHistory"));

        stage.showAndWait();
    }

    @FXML
    private void handleSecurityHistoryImportAction() {
        final URL fxmlUrl = SecurityHistoryController.class.getResource("HistoricalImport.fxml");
        final Stage stage = FXMLUtils.loadFXML(fxmlUrl, resources);
        stage.setTitle(resources.getString("Title.HistoryImport"));

        stage.showAndWait();
    }

    @FXML
    private void handleAddRemoveCurrenciesAction() {
        final URL fxmlUrl = AddRemoveCurrencyController.class.getResource("AddRemoveCurrency.fxml");
        final Stage stage = FXMLUtils.loadFXML(fxmlUrl, resources);
        stage.setTitle(resources.getString("Title.AddRemCurr"));

        stage.showAndWait();
    }

    @FXML
    private void handleSetDefaultCurrencyAction() {
        DefaultCurrencyAction.showAndWait();
    }

    @FXML
    private void handleModifyCurrenciesAction() {
        final URL fxmlUrl = ModifyCurrencyController.class.getResource("ModifyCurrency.fxml");
        final Stage stage = FXMLUtils.loadFXML(fxmlUrl, resources);
        stage.setTitle(resources.getString("Title.ModifyCurrencies"));

        stage.showAndWait();
    }

    @FXML
    private void handleEditExchangeRatesAction() {
        EditExchangeRatesController.showAndWait();
    }

    @FXML
    private void handleFontSizeAction() {
        final URL fxmlUrl = FontSizeDialogController.class.getResource("FontSizeDialog.fxml");
        final Stage stage = FXMLUtils.loadFXML(fxmlUrl, resources);
        stage.setTitle(resources.getString("Title.FontSize"));

        stage.setResizable(false);

        stage.showAndWait();
    }

    @FXML
    private void handleBaseColorAction() {
        final URL fxmlUrl = BaseColorDialogController.class.getResource("BaseColorDialog.fxml");
        final Stage stage = FXMLUtils.loadFXML(fxmlUrl, resources);
        stage.setTitle(resources.getString("Title.BaseColor"));

        stage.setResizable(false);

        stage.showAndWait();
    }

    @FXML
    private void handleShowOptionDialog() {
        final Stage stage = FXMLUtils.loadFXML(OptionDialogController.class.getResource("OptionDialog.fxml"),
               resources);

        stage.setTitle(resources.getString("Title.Options"));
        stage.setResizable(false);
        stage.showAndWait();
    }

    @FXML
    private void handleShowTranNumberListDialog() {
        TransactionNumberDialogController.showAndWait();
    }
}
