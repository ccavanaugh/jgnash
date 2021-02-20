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

import java.io.File;
import java.text.NumberFormat;
import java.util.ResourceBundle;

import javafx.concurrent.Task;
import javafx.stage.FileChooser;

import jgnash.engine.EngineFactory;
import jgnash.resource.util.ResourceUtils;
import jgnash.text.NumericFormats;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.util.FileChooserFactory;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.views.main.MainView;

/**
 * Save File As Task.
 *
 * @author Craig Cavanaugh
 */
public class SaveAsTask extends Task<Void> {

    private static final int FORCED_DELAY = 1500;
    private static final int INDETERMINATE = -1;

    private final File newFile;

    private SaveAsTask(final File file) {
        this.newFile = file;
    }

    public static void start() {
        final ResourceBundle resources = ResourceUtils.getBundle();
        final File current = new File(EngineFactory.getActiveDatabase());

        final FileChooser fileChooser = FileChooserFactory.getDataStoreChooser();
        fileChooser.setInitialDirectory(current.getParentFile());
        fileChooser.setTitle(resources.getString("Title.SaveAs"));

        final File newFile = fileChooser.showSaveDialog(MainView.getPrimaryStage());

        if (newFile != null) {
            final SaveAsTask saveAsTask = new SaveAsTask(newFile);
            new Thread(saveAsTask).start();
            StaticUIMethods.displayTaskProgress(saveAsTask);
        }
    }

    @Override
    protected Void call() {

        final ResourceBundle resources = ResourceUtils.getBundle();

        final NumberFormat percentFormat = NumericFormats.getPercentageFormat();

        try {
            updateMessage(resources.getString("Message.PleaseWait"));
            updateProgress(INDETERMINATE, Long.MAX_VALUE);

            EngineFactory.saveAs(newFile.getAbsolutePath(), value ->
                    updateMessage(resources.getString("Message.PleaseWait") + "\n" + percentFormat.format(value)));

            updateProgress(1, 1);
            updateMessage(resources.getString("Message.FileSaveComplete"));
            Thread.sleep(FORCED_DELAY * 2);
        } catch (final Exception exception) {
            JavaFXUtils.runLater(() -> StaticUIMethods.displayException(exception));
        }

        return null;
    }
}
