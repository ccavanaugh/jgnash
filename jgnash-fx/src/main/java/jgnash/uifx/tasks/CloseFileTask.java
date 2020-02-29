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
package jgnash.uifx.tasks;

import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.concurrent.Task;

import jgnash.engine.EngineFactory;
import jgnash.uifx.StaticUIMethods;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Task to close a file while updating progress.
 *
 * @author Craig Cavanaugh
 */
public class CloseFileTask extends Task<String> {

    private static final int FORCED_DELAY = 1500;

    private static final int INDETERMINATE = -1;

    public static void initiateFileClose() {
        final CloseFileTask closeFileTask = new CloseFileTask();

        final Thread thread = new Thread(closeFileTask);
        thread.setDaemon(true);
        thread.start();

        StaticUIMethods.displayTaskProgress(closeFileTask);
    }

    public static void initiateShutdown() {
        final CloseFileTask closeFileTask = new CloseFileTask();
        closeFileTask.setOnSucceeded(event -> Platform.exit());

        final Thread thread = new Thread(closeFileTask);
        thread.setDaemon(true);
        thread.start();

        StaticUIMethods.displayTaskProgress(closeFileTask);
    }

    @Override
    protected String call() {
        final ResourceBundle resources = ResourceUtils.getBundle();

        try {
            updateMessage(resources.getString("Message.SavingFile"));
            updateProgress(INDETERMINATE, Long.MAX_VALUE);

            final Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(FORCED_DELAY); // lets the UI catch up
                } catch (final InterruptedException exception) {
                    JavaFXUtils.runLater(() -> StaticUIMethods.displayException(exception));
                }
                EngineFactory.closeEngine(EngineFactory.DEFAULT);
            });

            thread.start();
            thread.join();

            updateMessage(resources.getString("Message.FileSaveComplete"));
            Thread.sleep(FORCED_DELAY);
        } catch (final Exception exception) {
            JavaFXUtils.runLater(() -> StaticUIMethods.displayException(exception));
        }

        return resources.getString("Message.FileSaveComplete");
    }
}
