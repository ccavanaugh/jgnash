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
package jgnash.uifx.report;

import java.io.File;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.Preferences;

import javafx.stage.FileChooser;

import jgnash.engine.Account;
import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.report.BalanceByMonthCSVReport;
import jgnash.report.ProfitLossTextReport;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.control.DateRangeDialogController;
import jgnash.uifx.report.pdf.ReportViewerDialogController;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.views.AccountBalanceDisplayManager;
import jgnash.uifx.views.main.MainView;
import jgnash.util.Nullable;

/**
 * Utility class for loading and displaying reports.
 *
 * @author Craig Cavanaugh
 */
public class ReportActions {

    private static final String LAST_DIR = "lastDir";
    private static final String FORCE_CURRENCY = "forceCurrency";

    public static void displayAccountBalanceChart() {
        final FXMLUtils.Pair<AccountBalanceChartController> pair =
                FXMLUtils.load(IncomeExpenseBarChartDialogController.class.getResource("AccountBalanceChart.fxml"),
                        ResourceUtils.getString("Title.AccountBalance"));

        pair.getStage().show();
    }

    public static void displayListOfAccountsReport() {
        final FXMLUtils.Pair<ReportViewerDialogController> reportPair =
                FXMLUtils.load(ReportViewerDialogController.class.getResource("ReportViewerDialog.fxml"),
                        ResourceUtils.getString("Title.ListOfAccounts"));

        reportPair.getController().loadReportController("ListOfAccountsReport.fxml");

        // Preserve size and location
        StageUtils.addBoundsListener(reportPair.getStage(), ListOfAccountsReportController.class, MainView.getPrimaryStage());

        reportPair.getStage().show();
    }

    public static void displayAccountRegisterReport(@Nullable final Account account) {
        final FXMLUtils.Pair<ReportViewerDialogController> reportPair =
                FXMLUtils.load(ReportViewerDialogController.class.getResource("ReportViewerDialog.fxml"),
                        ResourceUtils.getString("Title.AccountRegister"));

        final AccountRegisterReportController controller
                = reportPair.getController().loadReportController("AccountRegisterReport.fxml");

        if (controller != null) {
            controller.setAccount(account);
        }

        // Preserve size and location
        StageUtils.addBoundsListener(reportPair.getStage(), AccountRegisterReportController.class, MainView.getPrimaryStage());

        reportPair.getStage().show();
    }

    public static void displayIncomeExpensePieChart() {
        final FXMLUtils.Pair<IncomeExpensePieChartDialogController> pair =
                FXMLUtils.load(IncomeExpensePieChartDialogController.class.getResource("IncomeExpensePieChartDialog.fxml"),
                        ResourceUtils.getString("Title.IncomeExpenseChart"));

        pair.getStage().show();
    }

    public static void displayIncomeExpensePayeePieChart() {
        final FXMLUtils.Pair<IncomeExpensePayeePieChartDialogController> pair =
                FXMLUtils.load(IncomeExpensePayeePieChartDialogController.class.getResource("IncomeExpensePayeePieChartDialog.fxml"),
                        ResourceUtils.getString("Title.IncomeExpenseChart"));

        pair.getStage().show();
    }

    public static void displayIncomeExpenseBarChart() {
        final FXMLUtils.Pair<IncomeExpenseBarChartDialogController> pair =
                FXMLUtils.load(IncomeExpenseBarChartDialogController.class.getResource("IncomeExpenseBarChartDialog.fxml"),
                        ResourceUtils.getString("Title.IncomeExpenseBarChart"));

        pair.getStage().show();
    }

    public static void displayTransactionTagPieChart() {
        final FXMLUtils.Pair<TransactionTagPieChartDialogController> pair =
                FXMLUtils.load(TransactionTagPieChartDialogController.class.getResource("TransactionTagPieChartDialog.fxml"),
                        ResourceUtils.getString("Title.TransactionTagPieChart"));

        pair.getStage().show();
    }

    public static void displayPortfolioReport() {
        final FXMLUtils.Pair<ReportViewerDialogController> reportPair =
                FXMLUtils.load(ReportViewerDialogController.class.getResource("ReportViewerDialog.fxml"),
                        ResourceUtils.getString("Title.PortfolioReport"));

        reportPair.getController().loadReportController("PortfolioReport.fxml");

        // Preserve size and location
        StageUtils.addBoundsListener(reportPair.getStage(), PortfolioReportController.class, MainView.getPrimaryStage());

        reportPair.getStage().show();
    }

    public static void displayProfitLossReport() {
        final FXMLUtils.Pair<ReportViewerDialogController> reportPair =
                FXMLUtils.load(ReportViewerDialogController.class.getResource("ReportViewerDialog.fxml"),
                        ResourceUtils.getString("Title.ProfitLoss"));

        reportPair.getController().loadReportController("ProfitLossReport.fxml");

        // Preserve size and location
        StageUtils.addBoundsListener(reportPair.getStage(), ProfitLossReportController.class, MainView.getPrimaryStage());

        reportPair.getStage().show();
    }

    public static void displayBalanceSheetReport() {
        final FXMLUtils.Pair<ReportViewerDialogController> reportPair =
                FXMLUtils.load(ReportViewerDialogController.class.getResource("ReportViewerDialog.fxml"),
                        ResourceUtils.getString("Title.BalanceSheet"));

        reportPair.getController().loadReportController("BalanceSheetReport.fxml");

        // Preserve size and location
        StageUtils.addBoundsListener(reportPair.getStage(), BalanceSheetReportController.class, MainView.getPrimaryStage());

        reportPair.getStage().show();
    }

    public static void displayNetWorthReport() {
        final FXMLUtils.Pair<ReportViewerDialogController> reportPair =
                FXMLUtils.load(ReportViewerDialogController.class.getResource("ReportViewerDialog.fxml"),
                        ResourceUtils.getString("Word.NetWorth"));

        reportPair.getController().loadReportController("NetWorthReport.fxml");

        // Preserve size and location
        StageUtils.addBoundsListener(reportPair.getStage(), NetWorthReportController.class, MainView.getPrimaryStage());

        reportPair.getStage().show();
    }

    public static void exportProfitLossReport() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final CurrencyNode baseCommodity = engine.getDefaultCurrency();

        final FXMLUtils.Pair<DateRangeDialogController> pair
                = FXMLUtils.load(DateRangeDialogController.class.getResource("DateRangeDialog.fxml"),
                ResourceUtils.getString("Title.ReportOptions"));

        pair.getStage().setResizable(false);
        pair.getStage().showAndWait();

        final Optional<LocalDate[]> optional = pair.getController().getDates();

        optional.ifPresent(localDates -> {

            final Preferences pref = Preferences.userNodeForPackage(ReportActions.class);

            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(ResourceUtils.getString("Title.SaveFile"));

            final File initialDirectory = new File(pref.get(LAST_DIR, System.getProperty("user.home")));

            // Protect against an IllegalArgumentException
            if (initialDirectory.isDirectory()) {
                fileChooser.setInitialDirectory(initialDirectory);
            }

            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("TXT", "*.txt")
            );

            final File file = fileChooser.showSaveDialog(MainView.getPrimaryStage());

            if (file != null) {
                pref.put(LAST_DIR, file.getParent());

                final ProfitLossTextReport report = new ProfitLossTextReport(file.getAbsolutePath(), localDates[0],
                        localDates[1], baseCommodity, AccountBalanceDisplayManager::convertToSelectedBalanceMode);

                report.run();
            }
        });
    }

    public static void exportBalanceByMonthCSVReport() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final Preferences preferences = Preferences.userNodeForPackage(ReportActions.class);

        final FXMLUtils.Pair<BalanceByMonthOptionsDialogController> pair
                = FXMLUtils.load(BalanceByMonthOptionsDialogController.class.getResource("BalanceByMonthOptionsDialog.fxml"),
                ResourceUtils.getString("Title.ReportOptions"));

        pair.getController().forceDefaultCurrencyProperty().set(preferences.getBoolean(FORCE_CURRENCY, false));
        pair.getStage().setResizable(false);
        pair.getStage().showAndWait();

        final boolean vertical = pair.getController().isVertical();
        final boolean forceCurrency = pair.getController().forceDefaultCurrencyProperty().get();

        final Optional<LocalDate[]> optional = pair.getController().getDates();

        optional.ifPresent(localDates -> {
            final String lastDir = preferences.get(LAST_DIR, null);
            preferences.putBoolean(FORCE_CURRENCY, forceCurrency);

            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(ResourceUtils.getString("Title.SaveFile"));

            if (lastDir != null) {
                fileChooser.setInitialDirectory(new File(lastDir));
            }

            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("CSV", "*.csv")
            );

            final File file = fileChooser.showSaveDialog(MainView.getPrimaryStage());

            if (file != null) {
                preferences.put(LAST_DIR, file.getParent());

                final BalanceByMonthCSVReport report;

                if (forceCurrency) {
                    report = new BalanceByMonthCSVReport(file.getAbsolutePath(), localDates[0], localDates[1],
                            engine.getDefaultCurrency(), vertical,
                            AccountBalanceDisplayManager::convertToSelectedBalanceMode);

                } else {
                    report = new BalanceByMonthCSVReport(file.getAbsolutePath(), localDates[0],
                            localDates[1], null, vertical, AccountBalanceDisplayManager::convertToSelectedBalanceMode);
                }

                report.run();
            }
        });
    }
}
