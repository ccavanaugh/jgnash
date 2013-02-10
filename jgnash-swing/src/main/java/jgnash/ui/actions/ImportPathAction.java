/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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

import java.awt.Component;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;

import jgnash.util.Resource;

/**
 * UI Action to request database path from the user
 *
 * @author Craig Cavanaugh
 *
 */
public class ImportPathAction {

    private static final String LAST_DIR = "LastDir";

    private ImportPathAction() {
    }

    public static String databaseNameAction(Component parent) {
        Resource rb = Resource.get();

        Preferences pref = Preferences.userNodeForPackage(ImportPathAction.class);

        JFileChooser chooser = new JFileChooser(pref.get(LAST_DIR, null));
        chooser.setApproveButtonText(rb.getString("Button.Ok"));
        chooser.setDialogTitle(rb.getString("Title.NewFile"));

        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            pref.put(LAST_DIR, chooser.getCurrentDirectory().getAbsolutePath());

            return chooser.getSelectedFile().getAbsolutePath();
        }
        return "";
    }
}
