/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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
package jgnash.ui.report.jasper;

import javax.swing.JPanel;

import jgnash.ui.StaticUIMethods;

/**
 * Abstract report class that must be extended to create a report
 * 
 * @author Craig Cavanaugh
 */
public abstract class DynamicJasperReport extends BaseDynamicJasperReport {

    private DynamicJasperReportPanel viewer;

    void setViewer(final DynamicJasperReportPanel viewer) {
        this.viewer = viewer;
    }

    protected void refreshReport() {
        viewer.refreshReport();
    }

    final public void showReport() {
        DynamicJasperReportFrame.viewReport(this); //finally display the report report
    }

    /**
     * Creates a report control panel. May return null if a panel is not used The ReportController is responsible for
     * dynamic report options with the exception of page format options
     *
     * @return control panel
     */
    public abstract JPanel getReportController();

    @Override
    protected void displayError(final String message) {
        StaticUIMethods.displayError(message);
    }
}
