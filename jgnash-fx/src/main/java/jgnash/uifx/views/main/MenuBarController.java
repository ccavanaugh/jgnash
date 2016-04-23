/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.about.AboutDialogController;
import jgnash.uifx.actions.DefaultCurrencyAction;
import jgnash.uifx.actions.DefaultDateFormatAction;
import jgnash.uifx.actions.DefaultLocaleAction;
import jgnash.uifx.actions.ExecuteJavaScriptAction;
import jgnash.uifx.actions.ImportOfxAction;
import jgnash.uifx.actions.ImportQifAction;
import jgnash.uifx.dialog.currency.AddRemoveCurrencyController;
import jgnash.uifx.dialog.currency.EditExchangeRatesController;
import jgnash.uifx.dialog.currency.ModifyCurrencyController;
import jgnash.uifx.dialog.options.OptionDialogController;
import jgnash.uifx.dialog.options.TransactionNumberDialogController;
import jgnash.uifx.dialog.security.CreateModifySecuritiesController;
import jgnash.uifx.dialog.security.SecurityHistoryController;
import jgnash.uifx.report.ReportActions;
import jgnash.uifx.skin.BaseColorDialogController;
import jgnash.uifx.skin.FontSizeDialogController;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.tasks.CloseFileTask;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.views.register.RegisterStage;
import jgnash.uifx.wizard.file.NewFileWizard;

/**
 * Primary Menu Controller
 *
 * @author Craig Cavanaugh
 */
public class MenuBarController implements MessageListener {

    @FXML
    private Menu reportMenu;

    @FXML
    private MenuItem importQifMenuItem;

    @FXML
    private MenuItem importOfxMenuItem;

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
        reportMenu.disableProperty().bind(disabled);
        transNumberListMenuItem.disableProperty().bind(disabled);
        importOfxMenuItem.disableProperty().bind(disabled);
        importQifMenuItem.disableProperty().bind(disabled);

        windowMenu.disableProperty().bind(Bindings.or(disabled, RegisterStage.registerStageList().emptyProperty()));

        RegisterStage.registerStageList().addListener((ListChangeListener<RegisterStage>) c -> {
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
        NewFileWizard.showAndWait();
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
                    disabled.setValue(false);
                    break;
                case FILE_CLOSING:
                    closeAllWindows();
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
        final RegisterStage[] stages = RegisterStage.registerStageList()
                .toArray(new RegisterStage[RegisterStage.registerStageList().size()]);

        for (final RegisterStage stage : stages) {
            stage.close();
        }
    }

    @FXML
    private void handleCreateModifySecuritiesAction() {
        final FXMLUtils.Pair<CreateModifySecuritiesController> pair =
                FXMLUtils.load(CreateModifySecuritiesController.class.getResource("CreateModifySecurities.fxml"),
                        resources.getString("Title.CreateModifyCommodities"));

        pair.getStage().show();
    }

    @FXML
    private void handleSecuritiesHistoryAction() {
        final FXMLUtils.Pair<SecurityHistoryController> pair =
                FXMLUtils.load(SecurityHistoryController.class.getResource("SecurityHistory.fxml"),
                        resources.getString("Title.ModifySecHistory"));

        pair.getStage().show();
    }

    @FXML
    private void handleSecurityHistoryImportAction() {
        final FXMLUtils.Pair<SecurityHistoryController> pair =
                FXMLUtils.load(SecurityHistoryController.class.getResource("HistoricalImport.fxml"),
                        resources.getString("Title.HistoryImport"));

        pair.getStage().show();
    }

    @FXML
    private void handleAddRemoveCurrenciesAction() {
        final FXMLUtils.Pair<AddRemoveCurrencyController> pair =
                FXMLUtils.load(AddRemoveCurrencyController.class.getResource("AddRemoveCurrency.fxml"),
                        resources.getString("Title.AddRemCurr"));

        pair.getStage().show();
    }

    @FXML
    private void handleSetDefaultCurrencyAction() {
        DefaultCurrencyAction.showAndWait();
    }

    @FXML
    private void handleModifyCurrenciesAction() {
        final FXMLUtils.Pair<ModifyCurrencyController> pair = FXMLUtils.load(ModifyCurrencyController.class.getResource("ModifyCurrency.fxml"),
                resources.getString("Title.ModifyCurrencies"));

        pair.getStage().show();
    }

    @FXML
    private void handleEditExchangeRatesAction() {
        EditExchangeRatesController.show();
    }

    @FXML
    private void handleFontSizeAction() {
        final FXMLUtils.Pair<FontSizeDialogController> pair = FXMLUtils.load(FontSizeDialogController.class.getResource("FontSizeDialog.fxml"),
                resources.getString("Title.FontSize"));

        pair.getStage().setResizable(false);

        pair.getStage().show();
    }

    @FXML
    private void handleBaseColorAction() {
        final FXMLUtils.Pair<BaseColorDialogController> pair =
                FXMLUtils.load(BaseColorDialogController.class.getResource("BaseColorDialog.fxml"),
                        resources.getString("Title.BaseColor"));

        pair.getStage().setResizable(false);

        pair.getStage().show();
    }

    @FXML
    private void handleShowOptionDialog() {
        final FXMLUtils.Pair<OptionDialogController> pair = FXMLUtils.load(OptionDialogController.class.getResource("OptionDialog.fxml"),
                resources.getString("Title.Options"));

        pair.getStage().setResizable(false);
        pair.getStage().show();
    }

    @FXML
    private void handleShowTranNumberListDialog() {
        TransactionNumberDialogController.showAndWait();
    }

    @FXML
    private void handleShowConsoleDialog() {
        ConsoleDialogController.show();
    }

    @FXML
    private void handleExecuteJavaScriptFile() {
        ExecuteJavaScriptAction.showAndWait();
    }

    @FXML
    private void handleImportOFXAction() {
        ImportOfxAction.showAndWait();
    }

    @FXML
    private void handleImportQIFAction() {
        ImportQifAction.showAndWait();
    }

    @FXML
    private void handleChangeDateFormat() {
        DefaultDateFormatAction.showAndWait();
    }

    @FXML
    private void handleIncomeExpensePieChart() {
        ReportActions.displayIncomeExpensePieChart();
    }

    @FXML
    private void handleIncomeExpenseBarChart() {
        ReportActions.displayIncomeExpenseBarChart();
    }

    @FXML
    private void handleExportProfitLoss() {
        ReportActions.exportProfitLossReport();
    }

    @FXML
    private void handleExportBalanceByMonthCSVReport() {
        ReportActions.exportBalanceByMonthCSVReport();
    }

    @FXML
    private void handleDisplayPortfolioReport() {
        ReportActions.displayPortfolioReport();
    }

    @FXML
    private void handleDisplayAccountRegisterReport() {
        ReportActions.displayAccountRegisterReport(null);
    }

    @FXML
    private void handleDisplayProfitLossReport() {
        ReportActions.displayProfitLossReport();
    }

    @FXML
    private void handleDisplayBalanceSheetReport() {
        ReportActions.displayBalanceSheetReport();
    }

    @FXML
    private void handleDisplayNetWorthReport() {
        ReportActions.displayNetWorthReport();
    }
}
