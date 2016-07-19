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
import java.nio.file.Files;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;

import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.StoredObject;
import jgnash.engine.xstream.BinaryXStreamDataStore;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.views.main.MainView;
import jgnash.util.FileUtils;
import jgnash.util.ResourceUtils;

/**
 * Save File As Task.
 *
 * @author Craig Cavanaugh
 */
public class SaveAsTask extends Task<Void> {

    private static final int FORCED_DELAY = 1500;
    private static final int INDETERMINATE = -1;

    private final File file;

    private SaveAsTask(final File file) {
        this.file = file;
    }

    public static void start() {
        final ResourceBundle resources = ResourceUtils.getBundle();

        final File current = new File(EngineFactory.getActiveDatabase());

        final FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(current.getParentFile());
        fileChooser.setTitle(resources.getString("Title.SaveAs"));

        final DataStoreType[] types = DataStoreType.values();

        final String[] ext = new String[types.length];

        final StringBuilder description = new StringBuilder(resources.getString("Label.jGnashFiles") + " (");

        for (int i = 0; i < types.length; i++) {
            ext[i] = "*." + types[i].getDataStore().getFileExt();

            description.append("*.");
            description.append(types[i].getDataStore().getFileExt());

            if (i < types.length - 1) {
                description.append(", ");
            }
        }
        description.append(')');

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description.toString(), ext));

        final File newFile = fileChooser.showSaveDialog(MainView.getInstance().getPrimaryStage());

        if (newFile != null) {
            final SaveAsTask saveAsTask = new SaveAsTask(newFile);
            new Thread(saveAsTask).start();
            StaticUIMethods.displayTaskProgress(saveAsTask);
        }
    }

    @Override
    protected Void call() throws Exception {

        final ResourceBundle resources = ResourceUtils.getBundle();

        final File current = new File(EngineFactory.getActiveDatabase());

        try {
            updateMessage(resources.getString("Message.PleaseWait"));
            updateProgress(INDETERMINATE, Long.MAX_VALUE);

            final String destination = file.getAbsolutePath();

            final String fileExtension = FileUtils.getFileExtension(destination);

            DataStoreType newFileType = DataStoreType.BINARY_XSTREAM;   // default for a new file

            if (!fileExtension.isEmpty()) {
                for (DataStoreType type : DataStoreType.values()) {
                    if (type.getDataStore().getFileExt().equals(fileExtension)) {
                        newFileType = type;
                        break;
                    }
                }
            }

            final File newFile = new File(FileUtils.stripFileExtension(destination)
                    + "." + newFileType.getDataStore().getFileExt());

            // don't perform the save if the destination is going to overwrite the current database
            if (!current.equals(newFile)) {

                DataStoreType currentType = EngineFactory.getType(EngineFactory.DEFAULT);

                if (currentType.supportsRemote && newFileType.supportsRemote) { // Relational database
                    File tempFile = Files.createTempFile("jgnash", "." + BinaryXStreamDataStore.FILE_EXT).toFile();

                    Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                    if (engine != null) {
                        // Get collection of object to persist
                        Collection<StoredObject> objects = engine.getStoredObjects();

                        // Write everything to a temporary file
                        DataStoreType.BINARY_XSTREAM.getDataStore().saveAs(tempFile, objects);
                        EngineFactory.closeEngine(EngineFactory.DEFAULT);

                        // Boot the engine with the temporary file
                        EngineFactory.bootLocalEngine(tempFile.getAbsolutePath(), EngineFactory.DEFAULT,
                                EngineFactory.EMPTY_PASSWORD);

                        engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                        if (engine != null) {

                            // Get collection of object to persist
                            objects = engine.getStoredObjects();

                            // Write everything to the new file
                            newFileType.getDataStore().saveAs(newFile, objects);
                            EngineFactory.closeEngine(EngineFactory.DEFAULT);

                            // Boot the engine with the new file
                            EngineFactory.bootLocalEngine(newFile.getAbsolutePath(),
                                    EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD);
                        }

                        if (!tempFile.delete()) {
                            Logger.getLogger(SaveAsTask.class.getName())
                                    .info(resources.getString("Message.Error.RemoveTempFile"));
                        }
                    }
                } else {    // Simple
                    Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                    if (engine != null) {
                        final Collection<StoredObject> objects = engine.getStoredObjects();
                        newFileType.getDataStore().saveAs(newFile, objects);
                        EngineFactory.closeEngine(EngineFactory.DEFAULT);

                        EngineFactory.bootLocalEngine(newFile.getAbsolutePath(), EngineFactory.DEFAULT,
                                EngineFactory.EMPTY_PASSWORD);
                    }
                }
            }

            updateProgress(1, 1);
            updateMessage(resources.getString("Message.FileSaveComplete"));
            Thread.sleep(FORCED_DELAY * 2);
        } catch (final Exception exception) {
            Platform.runLater(() -> StaticUIMethods.displayException(exception));
        }

        return null;
    }
}
