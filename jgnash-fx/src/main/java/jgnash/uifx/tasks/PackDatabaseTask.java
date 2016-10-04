/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import java.io.File;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.concurrent.Task;

import jgnash.engine.DataStore;
import jgnash.engine.EngineFactory;
import jgnash.uifx.StaticUIMethods;
import jgnash.util.FileUtils;
import jgnash.util.ResourceUtils;

/**
 * PackDatabase Task.
 *
 * @author Craig Cavanaugh
 */
public class PackDatabaseTask extends Task<Void> {

    private static final int FORCED_DELAY = 1500;
    private static final int INDETERMINATE = -1;

    private final File file;
    private char[] password = new char[]{};

    private PackDatabaseTask(final File file, final char[] password) {
        this.file = file;
        this.password = password;
    }

    public static void start(final String fileName, final char[] password) {
        if (fileName != null) {
            final PackDatabaseTask saveAsTask = new PackDatabaseTask(new File(fileName), password);
            new Thread(saveAsTask).start();
            StaticUIMethods.displayTaskProgress(saveAsTask);
        }
    }

    @Override
    protected Void call() throws Exception {

        final ResourceBundle resources = ResourceUtils.getBundle();

        boolean fileLoaded = false;

        try {
            updateMessage(resources.getString("Message.PleaseWait"));
            updateProgress(INDETERMINATE, Long.MAX_VALUE);

            // Close an active database
            if (EngineFactory.getEngine(EngineFactory.DEFAULT) != null) {

                // is the file we want to pack already open?
                if (EngineFactory.getActiveDatabase().equals(file.getAbsolutePath())) {
                    fileLoaded = true;
                }

                updateMessage(resources.getString("Message.PleaseWait") + "\n"
                        + resources.getString("Message.ClosingFile"));

                EngineFactory.closeEngine(EngineFactory.DEFAULT);
            }

            updateMessage(resources.getString("Message.PleaseWait") + "\n"
                    + resources.getString("Message.PackingFile"));

            final DataStore dataStore = EngineFactory.getDataStoreByType(file.getAbsolutePath()).getDataStore();

            final String newFile = FileUtils.stripFileExtension(file.getAbsolutePath()) + "-pack" + "."
                    + dataStore.getFileExt();

            final String oldFile = FileUtils.stripFileExtension(file.getAbsolutePath()) + "-old" + "."
                    + dataStore.getFileExt();

            EngineFactory.saveAs(file.getAbsolutePath(), newFile, password);

            dataStore.rename(file.getAbsolutePath(), oldFile);    // rename the old
            dataStore.rename(newFile, file.getAbsolutePath());    // rename the new to the original

            if (fileLoaded) {   // boot the compressed file
                EngineFactory.bootLocalEngine(file.getAbsolutePath(), EngineFactory.DEFAULT, password);
            }

            updateProgress(1, 1);
            updateMessage(resources.getString("Message.PackingFileComplete"));
            Thread.sleep(FORCED_DELAY * 2);
        } catch (final Exception exception) {
            Platform.runLater(() -> StaticUIMethods.displayException(exception));
        }

        return null;
    }
}
