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
import jgnash.uifx.control.DateRangeDialogController;
import jgnash.uifx.report.jasper.JasperViewerDialogController;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.views.AccountBalanceDisplayManager;
import jgnash.uifx.views.main.MainApplication;
import jgnash.util.Nullable;
import jgnash.util.ResourceUtils;

/**
 * Centralize report actions here
 *
 * @author Craig Cavanaugh
 */
public class ReportActions {

    private static final String LAST_DIR = "lastDir";
    private static final String FORCE_CURRENCY = "forceCurrency";

    public static void displayAccountRegisterReport(@Nullable final Account account) {
        final FXMLUtils.Pair<JasperViewerDialogController> reportPair =
                FXMLUtils.load(JasperViewerDialogController.class.getResource("JasperViewerDialog.fxml"),
                        ResourceUtils.getString("Title.AccountRegister"));

        final AccountRegisterReportController controller
                = reportPair.getController().loadReportController("AccountRegisterReport.fxml");

        if (controller != null) {
            controller.setAccount(account);
        }

        reportPair.getStage().show();

        // Preserve size and location   //TODO Bounds listener is enforce too large of a size
        //StageUtils.addBoundsListener(reportPair.getStage(), AccountRegisterReportController.class);
    }

    public static void displayIncomeExpensePieChart() {
        final FXMLUtils.Pair<IncomeExpensePieChartDialogController> pair =
                FXMLUtils.load(IncomeExpensePieChartDialogController.class.getResource("IncomeExpensePieChartDialog.fxml"),
                        ResourceUtils.getString("Title.IncomeExpenseChart"));

        pair.getStage().show();
    }

    public static void displayIncomeExpenseBarChart() {
        final FXMLUtils.Pair<IncomeExpenseBarChartDialogController> pair =
                FXMLUtils.load(IncomeExpenseBarChartDialogController.class.getResource("IncomeExpenseBarChartDialog.fxml"),
                        ResourceUtils.getString("Title.IncomeExpenseBarChart"));

        pair.getStage().show();
    }

    public static void displayPortfolioReport() {
        final FXMLUtils.Pair<JasperViewerDialogController> reportPair =
                FXMLUtils.load(JasperViewerDialogController.class.getResource("JasperViewerDialog.fxml"),
                        ResourceUtils.getString("Title.PortfolioReport"));

        reportPair.getController().loadReportController("PortfolioReport.fxml");
        reportPair.getStage().show();

        // Preserve size and location
        StageUtils.addBoundsListener(reportPair.getStage(), PortfolioReportController.class);
    }

    public static void displayProfitLossReport() {
        final FXMLUtils.Pair<JasperViewerDialogController> reportPair =
                FXMLUtils.load(JasperViewerDialogController.class.getResource("JasperViewerDialog.fxml"),
                        ResourceUtils.getString("Title.ProfitLoss"));

        reportPair.getController().loadReportController("ProfitLossReport.fxml");
        reportPair.getStage().show();

        // Preserve size and location
        StageUtils.addBoundsListener(reportPair.getStage(), ProfitLossReportController.class);
    }

    public static void displayBalanceSheetReport() {
        final FXMLUtils.Pair<JasperViewerDialogController> reportPair =
                FXMLUtils.load(JasperViewerDialogController.class.getResource("JasperViewerDialog.fxml"),
                        ResourceUtils.getString("Title.BalanceSheet"));

        reportPair.getController().loadReportController("BalanceSheetReport.fxml");
        reportPair.getStage().show();

        // Preserve size and location
        StageUtils.addBoundsListener(reportPair.getStage(), BalanceSheetReportController.class);
    }

    public static void displayNetWorthReport() {
        final FXMLUtils.Pair<JasperViewerDialogController> reportPair =
                FXMLUtils.load(JasperViewerDialogController.class.getResource("JasperViewerDialog.fxml"),
                        ResourceUtils.getString("Word.NetWorth"));

        reportPair.getController().loadReportController("NetWorthReport.fxml");
        reportPair.getStage().show();

        // Preserve size and location
        StageUtils.addBoundsListener(reportPair.getStage(), NetWorthReportController.class);
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

        final Optional<LocalDate[]> dates = pair.getController().getDates();

        if (dates.isPresent()) {

            final Preferences preferences = Preferences.userNodeForPackage(ReportActions.class);

            final String lastDir = preferences.get(LAST_DIR, null);

            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(ResourceUtils.getString("Title.SaveFile"));

            if (lastDir != null) {
                fileChooser.setInitialDirectory(new File(lastDir));
            }

            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("TXT", "*.txt")
            );

            final File file = fileChooser.showSaveDialog(MainApplication.getInstance().getPrimaryStage());

            if (file != null) {
                preferences.put(LAST_DIR, file.getParent());

                final ProfitLossTextReport report = new ProfitLossTextReport(file.getAbsolutePath(), dates.get()[0],
                        dates.get()[1], baseCommodity, AccountBalanceDisplayManager::convertToSelectedBalanceMode);

                report.run();
            }
        }
    }

    public static void exportBalanceByMonthCSVReport() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final Preferences preferences = Preferences.userNodeForPackage(ReportActions.class);

        final FXMLUtils.Pair<BalanceByMonthOptionsDialogController> pair
                = FXMLUtils.load(BalanceByMonthOptionsDialogController.class.getResource("BalanceByMonthOptionsDialog.fxml"),
                ResourceUtils.getString("Title.ReportOptions"));

        pair.getController().forceDefaultCurrencyProperty().setValue(preferences.getBoolean(FORCE_CURRENCY, false));
        pair.getStage().setResizable(false);
        pair.getStage().showAndWait();

        final Optional<LocalDate[]> dates = pair.getController().getDates();
        final boolean vertical = pair.getController().isVertical();
        final boolean forceCurrency = pair.getController().forceDefaultCurrencyProperty().get();

        if (dates.isPresent()) {
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

            final File file = fileChooser.showSaveDialog(MainApplication.getInstance().getPrimaryStage());

            if (file != null) {
                preferences.put(LAST_DIR, file.getParent());

                final BalanceByMonthCSVReport report;

                if (forceCurrency) {
                    report = new BalanceByMonthCSVReport(file.getAbsolutePath(), dates.get()[0], dates.get()[1],
                            engine.getDefaultCurrency(), vertical,
                            AccountBalanceDisplayManager::convertToSelectedBalanceMode);

                } else {
                    report = new BalanceByMonthCSVReport(file.getAbsolutePath(), dates.get()[0],
                            dates.get()[1], null, vertical, AccountBalanceDisplayManager::convertToSelectedBalanceMode);
                }

                report.run();
            }
        }
    }
}
