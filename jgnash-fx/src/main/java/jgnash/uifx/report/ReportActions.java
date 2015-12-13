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
package jgnash.uifx.report;

import java.io.File;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.Preferences;

import javafx.stage.FileChooser;

import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.report.ProfitLossTextReport;
import jgnash.uifx.control.DateRangeDialogController;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.views.AccountBalanceDisplayManager;
import jgnash.uifx.views.main.MainApplication;
import jgnash.util.ResourceUtils;

/**
 * Centralize report actions here
 *
 * @author Craig Cavanaugh
 */
public class ReportActions {

    private static final String LAST_DIR = "lastDir";

    public static void displayIncomeExpensePieChart() {
        final FXMLUtils.Pair pair =
                FXMLUtils.load(IncomeExpenseDialogController.class.getResource("IncomeExpenseDialog.fxml"),
                        ResourceUtils.getString("Title.IncomeExpenseChart"));

        pair.getStage().show();
    }

    public static void displayIncomeExpenseBarChart() {
        final FXMLUtils.Pair pair =
                FXMLUtils.load(IncomeExpenseBarChartDialogController.class.getResource("IncomeExpenseBarChartDialog.fxml"),
                        ResourceUtils.getString("Title.IncomeExpenseBarChart"));

        pair.getStage().show();
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

            final Preferences preferences = Preferences.userNodeForPackage(ProfitLossTextReport.class);

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
}
