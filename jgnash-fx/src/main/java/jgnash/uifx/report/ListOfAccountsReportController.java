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

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.fxml.FXML;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.report.pdf.Report;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.uifx.report.pdf.ReportController;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Account Register Report Controller
 *
 * @author Craig Cavanaugh
 */
public class ListOfAccountsReportController implements ReportController {

    private Runnable refreshRunnable = null;

    private final Report report = new ListOfAccountsReport();

    public ListOfAccountsReportController() {
        super();
    }

    @FXML
    private void initialize() {
        // boot the report generation
        JavaFXUtils.runLater(this::refreshReport);
    }

    @Override
    public void setRefreshRunnable(final Runnable runnable) {
        refreshRunnable = runnable;
    }

    @Override
    public void getReport(final Consumer<Report> reportConsumer) {
        reportConsumer.accept(report);
    }

    /**
     * Forces a refresh/rebuild of the report
     */
    @Override
    public void refreshReport() {
        // send notification the report has been updated

        report.clearReport();
        try {
            report.addTable(createReportModel());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (refreshRunnable != null) {
            refreshRunnable.run();
        }
    }

    @Override
    public AbstractReportTableModel createReportModel() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        return new ListOfAccountsReport.AccountListModel(engine.getAccountList(), engine.getDefaultCurrency());
    }
}
