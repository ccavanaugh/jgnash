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
package jgnash.uifx.views.accounts;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jgnash.convert.exportantur.csv.CsvExport;
import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.report.poi.Workbook;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.report.ListOfAccountsReport;
import jgnash.uifx.skin.ThemeManager;
import jgnash.uifx.util.AccountTypeFilter;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.views.main.MainView;
import jgnash.util.FileUtils;
import jgnash.util.Nullable;

/**
 * Static support methods for Account manipulation.
 *
 * @author Craig Cavanaugh
 */
public final class StaticAccountsMethods {

    private static final String EXPORT_DIR = "exportDir";

    private static final String XLS = "xls";

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

    static void exportAccountTree() {
        final ResourceBundle resources = ResourceUtils.getBundle();

        final Preferences pref = Preferences.userNodeForPackage(StaticAccountsMethods.class);
        final FileChooser fileChooser = new FileChooser();

        final File initialDirectory = new File(pref.get(EXPORT_DIR, System.getProperty("user.home")));

        // Protect against an IllegalArgumentException
        if (initialDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(resources.getString("Label.CsvFiles") + " (*.csv)", "*.csv"),
                new FileChooser.ExtensionFilter(resources.getString("Label.SpreadsheetFiles") + " (*.xls)",
                        "*.xls"),
                new FileChooser.ExtensionFilter(resources.getString("Label.SpreadsheetFiles") + " (*.xlsx)",
                        "*.xlsx")
        );

        final File file = fileChooser.showSaveDialog(MainView.getPrimaryStage());
        final File exportFile;

        if (file != null) {
            if (!FileUtils.fileHasExtension(file.getName())) {  // fix up the file name if the user did not specify it
                final String fileExtension = fileChooser.getSelectedExtensionFilter().getExtensions().get(0).substring(1);
                exportFile = new File(FileUtils.stripFileExtension(file.getAbsolutePath()) + fileExtension);
            } else {
                exportFile = file;
            }

            pref.put(EXPORT_DIR, exportFile.getParentFile().getAbsolutePath());

            final Task<Void> exportTask = new Task<>() {
                @Override
                protected Void call() {
                    updateMessage(resources.getString("Message.PleaseWait"));
                    updateProgress(-1, Long.MAX_VALUE);

                    if (FileUtils.getFileExtension(exportFile.getName()).contains(XLS)) {

                        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                        Objects.requireNonNull(engine);

                        final AbstractReportTableModel reportTableModel =
                                new ListOfAccountsReport.AccountListModel(engine.getAccountList(), engine.getDefaultCurrency());

                        Workbook.export(reportTableModel, exportFile);
                    } else {
                        CsvExport.exportAccountTree(EngineFactory.getEngine(EngineFactory.DEFAULT), exportFile.toPath());
                    }
                    return null;
                }
            };

            new Thread(exportTask).start();

            StaticUIMethods.displayTaskProgress(exportTask);
        }

    }
}
