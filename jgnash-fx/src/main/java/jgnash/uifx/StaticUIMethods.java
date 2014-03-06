/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.uifx;

import jgnash.uifx.tasks.CloseFileTask;
import jgnash.util.ResourceUtils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import org.controlsfx.dialog.Dialogs;

/**
 * Various static UI support methods
 *
 * @author Craig Cavanaugh
 */
public class StaticUIMethods {

    private StaticUIMethods() {
        // Utility class
    }

    public static void displayError(final String message) {
        Dialogs.create()
                .owner(MainApplication.getPrimaryStage())
                .title(ResourceUtils.getBundle().getString("Title.Error"))
                .message(message)
                .showError();

    }

    public static void displayException(final Throwable exception) {
        Dialogs.create()
                .owner(MainApplication.getPrimaryStage())
                .title(ResourceUtils.getBundle().getString("Title.Error"))
                .message(exception.getLocalizedMessage())
                .showException(exception);
    }

    public static void displayTaskProgress(final Task task) {
        Dialogs.create()
                .owner(MainApplication.getPrimaryStage())
                .lightweight()
                .title(ResourceUtils.getBundle().getString("Title.PleaseWait"))
                .showWorkerProgress(task);
    }
}
