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
package jgnash.ui.actions;

import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.SwingWorker;

import jgnash.engine.DataStoreType;
import jgnash.engine.EngineFactory;
import jgnash.ui.UIApplication;
import jgnash.ui.util.builder.Action;
import jgnash.resource.util.ResourceUtils;

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

        JFileChooser chooser = new JFileChooser(pref.get(CURRENT_DIR, null));
        chooser.setMultiSelectionEnabled(false);

        chooser.setDialogTitle(rb.getString("Title.SaveAs"));

        final DataStoreType[] types = DataStoreType.values();

        final String[] ext = new String[types.length];

        for (int i = 0; i < types.length; i++) {
            ext[i] = types[i].getDataStore().getFileExt();
        }

        StringBuilder description = new StringBuilder(rb.getString("Label.jGnashFiles") + " (");

        for (int i = 0; i < types.length; i++) {
            description.append("*");
            description.append(types[i].getDataStore().getFileExt());

            if (i < types.length - 1) {
                description.append(", ");
            }
        }

        description.append(')');

        chooser.addChoosableFileFilter(new DataStoreFilter(description.toString(), ext));

        if (chooser.showSaveDialog(UIApplication.getFrame()) == JFileChooser.APPROVE_OPTION) {
            pref.put(CURRENT_DIR, chooser.getCurrentDirectory().getAbsolutePath());

            final class SaveAs extends SwingWorker<Void, Void> {

                @Override
                protected Void doInBackground() throws Exception {
                    UIApplication.getFrame().displayWaitMessage(rb.getString("Message.PleaseWait"));
                    EngineFactory.saveAs(chooser.getSelectedFile().getAbsolutePath());

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
