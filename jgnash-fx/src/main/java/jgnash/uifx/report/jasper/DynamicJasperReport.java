/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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
package jgnash.uifx.report.jasper;

import javafx.beans.property.SimpleObjectProperty;

import jgnash.report.ui.jasper.BaseDynamicJasperReport;
import jgnash.uifx.StaticUIMethods;

/**
 * Abstract report controller base class that may be extended to create a report.
 * 
 * @author Craig Cavanaugh
 */
public abstract class DynamicJasperReport extends BaseDynamicJasperReport{

    private final SimpleObjectProperty<Runnable> refreshCallBack = new SimpleObjectProperty<>();

    protected SimpleObjectProperty<Runnable> refreshCallBackProperty() {
        return refreshCallBack;
    }

    @Override
    protected void displayError(final String message) {
        StaticUIMethods.displayError(message);
    }
}
