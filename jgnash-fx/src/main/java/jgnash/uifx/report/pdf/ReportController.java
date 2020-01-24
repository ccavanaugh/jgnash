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
package jgnash.uifx.report.pdf;

import jgnash.report.pdf.Report;
import jgnash.report.table.AbstractReportTableModel;

import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Required UI interface for a report controller
 *
 * @author Craig Cavanaugh
 */
public interface ReportController {

    /**
     * Preference key
     */
    String START_DATE_KEY = "startDate";

    /**
     * Preference key
     */
    String END_DATE_KEY = "endDate";

    /**
     * Installs a callback to notify the report viewer that the underlying report has changed itself.
     *
     * @param runnable Runnable / callback that should be executed
     */
    void setRefreshRunnable(final Runnable runnable);

    /**
     * Functional return of the report
     *
     * @param report report consumer
     */
    void getReport(Consumer<Report> report);

    /**
     * Forces a refresh/rebuild of the report
     */
    void refreshReport();

    /**
     * Generated and returns the {@code AbstractReportTableModel} used for report generation
     *
     * @return report model
     */
    AbstractReportTableModel createReportModel();

    /**
     * Returns the default Preference node for the implementing class
     *
     * @return Preference node
     */
    default Preferences getPreferences() {
        return Preferences.userNodeForPackage(getClass()).node(getClass().getSimpleName());
    }

}
