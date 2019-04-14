/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

import jgnash.engine.AccountType;
import jgnash.engine.EngineFactory;
import jgnash.report.pdf.Report;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.report.pdf.ReportController;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Portfolio report controller.
 *
 * @author Craig Cavanaugh
 */
public class PortfolioReportController implements ReportController {

    private static final String RECURSIVE = "recursive";

    private static final String VERBOSE = "verbose";

    @FXML
    private AccountComboBox accountComboBox;

    @FXML
    private CheckBox subAccountCheckBox;

    @FXML
    private CheckBox longNameCheckBox;

    @FXML
    private ResourceBundle resources;

    private Runnable refreshRunnable = null;

    private final Report report = new PortfolioReport();

    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<Object> changeListener;

    @FXML
    private void initialize() {
        // Only show visible investment accounts
        accountComboBox.setPredicate(account -> account.instanceOf(AccountType.INVEST) && account.isVisible());

        final Preferences preferences = getPreferences();

        subAccountCheckBox.setSelected(preferences.getBoolean(RECURSIVE, true));
        longNameCheckBox.setSelected(preferences.getBoolean(VERBOSE, false));

        changeListener = (observable, oldValue, newValue) -> {
            if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {   // could be null if GC is slow
                handleReportRefresh();
            }
        };

        subAccountCheckBox.selectedProperty().addListener(new WeakChangeListener<>(changeListener));
        longNameCheckBox.selectedProperty().addListener(new WeakChangeListener<>(changeListener));
        accountComboBox.valueProperty().addListener(new WeakChangeListener<>(changeListener));

        // boot the report generation
        JavaFXUtils.runLater(this::refreshReport);
    }

    @Override
    public void setRefreshRunnable(final Runnable runnable) {
        refreshRunnable = runnable;
    }


    @Override
    public void getReport(Consumer<Report> reportConsumer) {
        reportConsumer.accept(report);
    }

    @Override
    public void refreshReport() {
        handleReportRefresh();
    }

    private void handleReportRefresh() {

        final Preferences preferences = getPreferences();

        preferences.putBoolean(RECURSIVE, subAccountCheckBox.isSelected());
        preferences.putBoolean(VERBOSE, longNameCheckBox.isSelected());

        if (!accountComboBox.isDisabled()) {   // make sure an account is available
            addTable();

            // send notification the report has been updated
            if (refreshRunnable != null) {
                refreshRunnable.run();
            }
        }
    }

    private void addTable() {
        AbstractReportTableModel model = createReportModel();

        report.clearReport();

        try {
            report.addTable(model, accountComboBox.getValue().getName()
                    + " - " + resources.getString("Title.PortfolioReport"));
            report.addFooter();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private AbstractReportTableModel createReportModel() {
        return PortfolioReport.createReportModel(accountComboBox.getValue(), subAccountCheckBox.isSelected(),
                longNameCheckBox.isSelected());
    }
}
