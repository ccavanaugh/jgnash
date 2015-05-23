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
package jgnash;

import javafx.application.Application;

import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.views.main.MainApplication;
import jgnash.util.Version;

/**
 * Main launch point for the JavaFX UI
 *
 * @author Craig Cavanaugh
 */
public class MainFX {

    public static final String VERSION;

    static {
        VERSION = Version.getAppName() + " - " + Version.getAppVersion();
    }

    public static void main(final String[] args) {
        // Register the default exception handler
        Thread.setDefaultUncaughtExceptionHandler(new StaticUIMethods.ExceptionHandler());

        // Boot the application
        Application.launch(MainApplication.class, args);
    }
}
