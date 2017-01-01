/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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
package jgnash.util.prefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import jgnash.util.NotNull;
import jgnash.util.ResourceUtils;

/**
 * Portable preferences utility class.
 *
 * @author Craig Cavanaugh
 */
public class PortablePreferences {

    private PortablePreferences() {
        // Utility class
    }

    public static void initPortablePreferences(final String portableFile) {
        System.setProperty("java.util.prefs.PreferencesFactory", "jgnash.util.prefs.MapPreferencesFactory");

        importPreferences(portableFile);

        // add a shutdown hook to export user preferences
        Runtime.getRuntime().addShutdownHook(new ExportPreferencesThread(portableFile));
    }

    private static void importPreferences(final String file) {
        final File importFile = getPreferenceFile(file);

        if (importFile.canRead()) {
            Logger.getLogger(PortablePreferences.class.getName()).info("Importing preferences");

            try (FileInputStream is = new FileInputStream(importFile)) {
                Preferences.importPreferences(is);
            } catch (InvalidPreferencesFormatException | IOException e) {
                System.err.println("Preferences file " + importFile.getAbsolutePath() + " could not be read");
                Logger.getLogger(PortablePreferences.class.getName()).log(Level.SEVERE, e.toString(), e);
            }
        } else {
            System.err.println("Preferences file " + importFile.getAbsolutePath() + " was not found");
        }
    }

    public static void deleteUserPreferences() {
        try {
            Preferences prefs = Preferences.userRoot();
            if (prefs.nodeExists("/jgnash")) {
                Preferences jgnash = prefs.node("/jgnash");
                jgnash.removeNode();
                prefs.flush();
            } else {
                System.err.println(ResourceUtils.getString("Message.PrefFail"));
            }
        } catch (BackingStoreException bse) {
            Logger.getLogger(PortablePreferences.class.getName()).log(Level.SEVERE, bse.toString(), bse);
            System.err.println(ResourceUtils.getString("Message.UninstallBad"));
        }
    }

    private static File getPreferenceFile(final String portableFile) {
        final File exportFile;

        if (portableFile != null && !portableFile.isEmpty()) {
            exportFile = new File(portableFile);
        } else {
            exportFile = new File(System.getProperty("user.dir") + System.getProperty("file.separator") + "pref.xml");
        }
        return exportFile;
    }

    private static class ExportPreferencesThread extends Thread {
        final String file;

        ExportPreferencesThread(@NotNull final String file) {
            this.file = file;
        }

        @Override
        public void run() {
            Logger.getLogger(ExportPreferencesThread.class.getName()).info("Exporting preferences");

            final File exportFile = getPreferenceFile(file);

            final Preferences preferences = Preferences.userRoot();

            try (final FileOutputStream os = new FileOutputStream(exportFile)) {
                try {
                    if (preferences.nodeExists("/jgnash")) {
                        final Preferences p = preferences.node("/jgnash");
                        p.exportSubtree(os);
                    }
                    deleteUserPreferences();
                } catch (final BackingStoreException e) {
                    Logger.getLogger(ExportPreferencesThread.class.getName()).log(Level.SEVERE, e.toString(), e);
                }
            } catch (final IOException e) {
                Logger.getLogger(ExportPreferencesThread.class.getName()).log(Level.SEVERE, e.toString(), e);
            }
        }
    }
}
