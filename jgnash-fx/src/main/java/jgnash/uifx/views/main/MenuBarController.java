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
package jgnash.uifx.views.main;

import java.io.File;
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
import javafx.stage.FileChooser;

import jgnash.convert.importat.ImportFilter;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.plugin.PluginFactory;
import jgnash.report.pdf.FontRegistry;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.about.AboutDialogController;
import jgnash.uifx.actions.DefaultCurrencyAction;
import jgnash.uifx.actions.DefaultLocaleAction;
import jgnash.uifx.actions.ExecuteJavaScriptAction;
import jgnash.uifx.actions.ExportAccountsAction;
import jgnash.uifx.actions.ImportAccountsAction;
import jgnash.uifx.actions.ImportOfxAction;
import jgnash.uifx.actions.ImportQifAction;
import jgnash.uifx.dialog.ChangeDatabasePasswordDialogController;
import jgnash.uifx.dialog.ImportScriptsDialogController;
import jgnash.uifx.dialog.PackDatabaseDialogController;
import jgnash.uifx.dialog.RemoteConnectionDialogController;
import jgnash.uifx.dialog.TagManagerDialogController;
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
import jgnash.uifx.tasks.SaveAsTask;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.views.budget.BudgetManagerDialogController;
import jgnash.uifx.views.recurring.RecurringDialogController;
import jgnash.uifx.views.register.RegisterStage;
import jgnash.uifx.wizard.file.NewFileWizard;
import jgnash.util.FileUtils;

/**
 * Primary Menu Controller.
 *
 * @author Craig Cavanaugh
 */
public class MenuBarController implements MessageListener {

    private final BooleanProperty disabled = new SimpleBooleanProperty(true);
    
    private final BooleanProperty investDisabled = new SimpleBooleanProperty(false);

    @FXML
    private MenuItem tagManagerMenuItem;

    @FXML
    private MenuItem configureTranImportFiltersMenuItem;

    @FXML
    private MenuItem packDatabaseMenuItem;

    @FXML
    private MenuItem recurringTransactionsMenuItem;

    @FXML
    private MenuItem budgetManagerMenuItem;

    @FXML
    private MenuItem shutdownServerMenuItem;

    @FXML
    private MenuItem changePasswordMenuItem;

    @FXML
    private MenuItem saveAsMenuItem;

    @FXML
    private MenuItem importAccountsMenuItem;

    @FXML
    private MenuItem exportAccountsMenuItem;

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
    private MenuItem portfolioReportMenuItem;

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

    @FXML
    private ResourceBundle resources;

    @FXML
    private void initialize() {
        budgetManagerMenuItem.disableProperty().bind(disabled);
        changePasswordMenuItem.disableProperty().bind(disabled.not());
        closeMenuItem.disableProperty().bind(disabled);
        configureTranImportFiltersMenuItem.disableProperty().bind(disabled);
        currenciesMenu.disableProperty().bind(disabled);
        exportAccountsMenuItem.disableProperty().bind(disabled);
        importAccountsMenuItem.disableProperty().bind(disabled);
        importOfxMenuItem.disableProperty().bind(disabled);
        importQifMenuItem.disableProperty().bind(disabled);
        recurringTransactionsMenuItem.disableProperty().bind(disabled);
        reportMenu.disableProperty().bind(disabled);
        saveAsMenuItem.disableProperty().bind(disabled);
        securitiesMenu.disableProperty().bind(disabled);
        shutdownServerMenuItem.disableProperty().bind(disabled.not());
        tagManagerMenuItem.disableProperty().bind(disabled);
        transNumberListMenuItem.disableProperty().bind(disabled);
        packDatabaseMenuItem.disableProperty().bind(disabled.not());
        portfolioReportMenuItem.disableProperty().bind(Bindings.or(disabled, investDisabled));

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
        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT);
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
        PluginFactory.stopPlugins();    // Stop plugins

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

    @FXML
    private void changeDefaultLocale() {
        DefaultLocaleAction.showAndWait();
    }

    @FXML
    private void closeAllWindows() {
        // create a copy to avoid concurrent modification issues
        final RegisterStage[] stages = RegisterStage.registerStageList()
                .toArray(new RegisterStage[0]);

        for (final RegisterStage stage : stages) {
            stage.close();
        }
    }

    @FXML
    private void handleCreateModifySecuritiesAction() {
        final FXMLUtils.Pair<CreateModifySecuritiesController> pair =
                FXMLUtils.load(CreateModifySecuritiesController.class.getResource("CreateModifySecurities.fxml"),
                        resources.getString("Title.CreateModifyCommodities"));

        StageUtils.addBoundsListener(pair.getStage(), CreateModifySecuritiesController.class);

        pair.getStage().show();
    }

    @FXML
    private void handleSecuritiesHistoryAction() {
        final FXMLUtils.Pair<SecurityHistoryController> pair =
                FXMLUtils.load(SecurityHistoryController.class.getResource("SecurityHistory.fxml"),
                        resources.getString("Title.ModifySecHistory"));

        StageUtils.addBoundsListener(pair.getStage(), SecurityHistoryController.class);

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
    private void handleChangeDatabasePasswordAction() {
        final FXMLUtils.Pair<ChangeDatabasePasswordDialogController> pair =
                FXMLUtils.load(ChangeDatabasePasswordDialogController.class.getResource("ChangePasswordDialog.fxml"),
                        resources.getString("Title.ChangePassword"));

        pair.getStage().show();
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
    private void handleApplyStyleAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(resources.getString("Title.Open"));

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(resources.getString("Label.CssFiles"), "*.css"));

        final File file = fileChooser.showOpenDialog(MainView.getPrimaryStage());

        if (file != null && FileUtils.getFileExtension(file.toString()).equalsIgnoreCase("css")) {
            if (ThemeManager.setUserStyle(file.toPath())) {
                JavaFXUtils.runLater(() -> StaticUIMethods.displayMessage(resources.getString("Message.Info.RestartToApply")));
            }
        }
    }

    @FXML
    private void handleClearStyleAction() {
        if (ThemeManager.setUserStyle(null)) {
            JavaFXUtils.runLater(() -> StaticUIMethods.displayMessage(resources.getString("Message.Info.RestartToApply")));
        }
    }

    @FXML
    private void handleShowOptionDialog() {

        // tickle the font registry to load the dialog faster
        new Thread(FontRegistry::getFontList).start();

        final FXMLUtils.Pair<OptionDialogController> pair = FXMLUtils.load(OptionDialogController.class.getResource("OptionDialog.fxml"),
                resources.getString("Title.Options"));

        StageUtils.addBoundsListener(pair.getStage(), OptionDialogController.class);

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
    private void handleIncomeExpensePieChart() {
        ReportActions.displayIncomeExpensePieChart();
    }

    @FXML
    private void handleIncomeExpensePayeePieChart() {
        ReportActions.displayIncomeExpensePayeePieChart();
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
    private void handleDisplayTransactionTagPieChart() {
        ReportActions.displayTransactionTagPieChart();
    }

    @FXML
    private void handleDisplayAccountBalanceChart() {
        ReportActions.displayAccountBalanceChart();
    }

    @FXML
    private void handleDisplayListOfAccountsReport() {
        ReportActions.displayListOfAccountsReport();
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

    @FXML
    private void handleImportAccountsAction() {
        ImportAccountsAction.showAndWait();
    }

    @FXML
    private void handleExportAccountsAction() {
        ExportAccountsAction.showAndWait();
    }

    @FXML
    private void handleSaveAsAction() {
        SaveAsTask.start();
    }

    @FXML
    private void handlePackDatabaseAction() {
        final FXMLUtils.Pair<PackDatabaseDialogController> pair =
                FXMLUtils.load(PackDatabaseDialogController.class.getResource("PackDatabaseDialog.fxml"),
                        resources.getString("Title.PackDatabase"));

        pair.getStage().show();
    }

    @FXML
    private void handleShutDownServerAction() {
        final FXMLUtils.Pair<RemoteConnectionDialogController> pair =
                FXMLUtils.load(RemoteConnectionDialogController.class.getResource("RemoteConnectionDialog.fxml"),
                        resources.getString("Title.ConnectServer"));

        final RemoteConnectionDialogController controller = pair.getController();

        pair.getStage().showAndWait();

        if (controller.getResult()) {
            // Message buss is on port + 1
            JavaFXUtils.runLater(() -> MessageBus.getInstance().shutDownRemoteServer(controller.getHost(),
                    controller.getPort() + 1, controller.getPassword()));
        }
    }

    @FXML
    private void handleBudgetManagerAction() {
        BudgetManagerDialogController.showBudgetManager();
    }

    @FXML
    private void handleTagManagerAction() {
        TagManagerDialogController.showTagManager();
    }

    @FXML
    private void handleShowRecurringTransactionsAction() {
        final FXMLUtils.Pair<RecurringDialogController> pair =
                FXMLUtils.load(RecurringDialogController.class.getResource("RecurringDialog.fxml"),
                        resources.getString("Title.Reminders"));

        pair.getStage().show();
    }

    @FXML
    private void handleShowTranImportFilterDialog() {
        final FXMLUtils.Pair<ImportScriptsDialogController> pair =
                FXMLUtils.load(ImportScriptsDialogController.class.getResource("ImportScriptsDialog.fxml"),
                        resources.getString("Title.ConfigTransImportFilters"));

        pair.getController().setEnabledScripts(ImportFilter.getEnabledImportFilters());

        pair.getController().setAcceptanceConsumer(ImportFilter::saveEnabledImportFilters);

        pair.getStage().show();

        StageUtils.addBoundsListener(pair.getStage(), ImportScriptsDialogController.class, MainView.getPrimaryStage());
    }

    private void checkAccountTypes() {
        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            investDisabled.setValue(engine.getInvestmentAccountList().isEmpty());
        }
    }

    @Override
    public void messagePosted(final Message event) {
        switch (event.getEvent()) {
            case FILE_LOAD_SUCCESS:
                JavaFXUtils.runLater(() -> {
                    disabled.set(false);
                    checkAccountTypes();
                });
                break;
            case FILE_CLOSING:
                JavaFXUtils.runLater(() -> {
                    closeAllWindows();
                    disabled.set(true);
                });
                break;
            case ACCOUNT_ADD:
            case ACCOUNT_REMOVE:
                JavaFXUtils.runLater(this::checkAccountTypes);
                break;
            default:
                break;
        }
    }
}
