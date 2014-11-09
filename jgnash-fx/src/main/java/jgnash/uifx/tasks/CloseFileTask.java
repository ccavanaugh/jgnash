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
package jgnash.uifx.tasks;

import java.util.ResourceBundle;

import jgnash.engine.EngineFactory;
import jgnash.uifx.StaticUIMethods;
import jgnash.util.ResourceUtils;

import javafx.application.Platform;
import javafx.concurrent.Task;

/**
 * Task to close a file while updating progress
 *
 * @author Craig Cavanaugh
 */
public class CloseFileTask extends Task<String> {

    private static final int FORCED_DELAY = 1500;

    private static final int INDETERMINATE = -1;

    public static void initiateClose() {
        CloseFileTask closeFileTask = new CloseFileTask();

        Thread thread = new Thread(closeFileTask);
        thread.setDaemon(true);
        thread.start();

        StaticUIMethods.displayTaskProgress(closeFileTask);
    }

    public static void initiateShutdown() {
        CloseFileTask closeFileTask = new CloseFileTask();
        closeFileTask.setOnSucceeded(event -> Platform.exit());

        Thread thread = new Thread(closeFileTask);
        thread.setDaemon(true);
        thread.start();

        StaticUIMethods.displayTaskProgress(closeFileTask);
    }

    @Override
    protected String call() throws Exception {
        ResourceBundle resources = ResourceUtils.getBundle();

        try {
            updateMessage(resources.getString("Message.SavingFile"));
            updateProgress(INDETERMINATE, Long.MAX_VALUE);
            EngineFactory.closeEngine(EngineFactory.DEFAULT);
            updateMessage(resources.getString("Message.FileSaveComplete"));
            Thread.sleep(FORCED_DELAY);
        } catch (final Exception exception) {
            Platform.runLater(() -> StaticUIMethods.displayException(exception));
        }

        return resources.getString("Message.FileSaveComplete");
    }
}
