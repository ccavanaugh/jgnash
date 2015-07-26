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
package jgnash.ui.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import jgnash.engine.DataStoreType;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.StoredObject;
import jgnash.engine.xstream.BinaryXStreamDataStore;
import jgnash.ui.UIApplication;
import jgnash.ui.util.builder.Action;
import jgnash.util.FileUtils;
import jgnash.util.ResourceUtils;

/**
 * UI Action to save the current database as a new name and reopen
 *
 * @author Craig Cavanaugh
 */
@Action("saveas-command")
public class SaveFileAsAction extends AbstractEnabledAction {

    private static final String CURRENT_DIR = "cwd";

    /**
     * Opens a Save as Dialog. If the extension of the destination file is different than the file currently open, then
     * an attempt is made to identify the new file format and save accordingly. Otherwise, a copy of the file is made.
     */
    private static void saveFileAs() {

        final ResourceBundle rb = ResourceUtils.getBundle();

        final Preferences pref = Preferences.userNodeForPackage(SaveFileAsAction.class);

        final File current = new File(EngineFactory.getActiveDatabase());

        JFileChooser chooser = new JFileChooser(pref.get(CURRENT_DIR, null));
        chooser.setMultiSelectionEnabled(false);

        chooser.setDialogTitle(rb.getString("Title.SaveAs"));

        final DataStoreType[] types = DataStoreType.values();

        String[] ext = new String[types.length];

        for (int i = 0; i < types.length; i++) {
            ext[i] = types[i].getDataStore().getFileExt();
        }

        StringBuilder description = new StringBuilder(rb.getString("Label.jGnashFiles") + " (");

        for (int i = 0; i < types.length; i++) {
            description.append("*.");
            description.append(types[i].getDataStore().getFileExt());

            if (i < types.length - 1) {
                description.append(", ");
            }
        }

        description.append(')');

        chooser.addChoosableFileFilter(new FileNameExtensionFilter(description.toString(), ext));

        if (chooser.showSaveDialog(UIApplication.getFrame()) == JFileChooser.APPROVE_OPTION) {
            pref.put(CURRENT_DIR, chooser.getCurrentDirectory().getAbsolutePath());

            // get the filename and strip the file extension if added
            final String destination = chooser.getSelectedFile().getAbsolutePath();

            final class SaveAs extends SwingWorker<Void, Void> {

                @Override
                protected Void doInBackground() throws Exception {

                    UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));

                    String fileExtension = FileUtils.getFileExtension(destination);

                    DataStoreType newFileType = DataStoreType.BINARY_XSTREAM;   // default for a new file

                    if (!fileExtension.isEmpty()) {
                        for (DataStoreType type : types) {
                            if (type.getDataStore().getFileExt().equals(fileExtension)) {
                                newFileType = type;
                                break;
                            }
                        }
                    }

                    File newFile = new File(FileUtils.stripFileExtension(destination) + "." + newFileType.getDataStore().getFileExt());

                    // don't perform the save if the destination is going to overwrite the current database
                    if (!current.equals(newFile)) {

                        DataStoreType currentType = EngineFactory.getType(EngineFactory.DEFAULT);

                        if (currentType.supportsRemote && newFileType.supportsRemote) {
                            File tempFile = Files.createTempFile("jgnash", "." + BinaryXStreamDataStore.FILE_EXT).toFile();

                            Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                            if (engine != null) {
                                // Get collection of object to persist
                                Collection<StoredObject> objects = engine.getStoredObjects();

                                // Write everything to a temporary file
                                DataStoreType.BINARY_XSTREAM.getDataStore().saveAs(tempFile, objects);
                                EngineFactory.closeEngine(EngineFactory.DEFAULT);

                                // Boot the engine with the temporary file
                                EngineFactory.bootLocalEngine(tempFile.getAbsolutePath(), EngineFactory.DEFAULT, new char[]{});

                                engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                                if (engine != null) {

                                    // Get collection of object to persist
                                    objects = engine.getStoredObjects();

                                    // Write everything to the new file
                                    newFileType.getDataStore().saveAs(newFile, objects);
                                    EngineFactory.closeEngine(EngineFactory.DEFAULT);

                                    // Boot the engine with the new file
                                    EngineFactory.bootLocalEngine(newFile.getAbsolutePath(), EngineFactory.DEFAULT, new char[]{});
                                }

                                if (!tempFile.delete()) {
                                    Logger.getLogger(SaveAs.class.getName()).info("Unable to remove temporary file");
                                }
                            }
                        } else {
                            Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                            if (engine != null) {
                                Collection<StoredObject> objects = engine.getStoredObjects();
                                newFileType.getDataStore().saveAs(newFile, objects);
                                EngineFactory.closeEngine(EngineFactory.DEFAULT);

                                EngineFactory.bootLocalEngine(newFile.getAbsolutePath(), EngineFactory.DEFAULT, new char[]{});
                            }
                        }
                    }

                    return null;
                }

                @Override
                protected void done() {
                    UIApplication.getFrame().stopWaitMessage();
                }
            }

            new SaveAs().execute();
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        saveFileAs();
    }
}
