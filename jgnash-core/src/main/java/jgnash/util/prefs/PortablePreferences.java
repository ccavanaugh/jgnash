/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2021 Craig Cavanaugh
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

import jgnash.resource.util.ResourceUtils;
import jgnash.util.FileUtils;
import jgnash.util.LogUtil;
import jgnash.util.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

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
        final Path importFile = getPreferenceFile(file);

        if (Files.isReadable(importFile)) {
            Logger.getLogger(PortablePreferences.class.getName()).info("Importing preferences");

            try (final InputStream is = Files.newInputStream(importFile)) {
                Preferences.importPreferences(is);
            } catch (final InvalidPreferencesFormatException | IOException e) {
                System.err.println("Preferences file " + importFile + " could not be read");
                LogUtil.logSevere(PortablePreferences.class, e);
            }
        } else {
            System.err.println("Preferences file " + importFile + " was not found");
        }
    }

    public static void deleteUserPreferences() {
        try {
            final Preferences p = Preferences.userRoot();
            if (p.nodeExists("/jgnash")) {
                Preferences jgnash = p.node("/jgnash");
                jgnash.removeNode();
                p.flush();
                System.out.println(ResourceUtils.getString("Message.UninstallGood"));
            } else {
                System.err.println(ResourceUtils.getString("Message.PrefFail"));
            }
        } catch (final BackingStoreException bse) {
            LogUtil.logSevere(PortablePreferences.class, bse);
            System.err.println(ResourceUtils.getString("Message.UninstallBad"));
        }
    }

    private static Path getPreferenceFile(final String portableFile) {
        final Path exportFile;

        if (portableFile != null && !portableFile.isEmpty()) {
            exportFile = Paths.get(portableFile);
        } else {
            exportFile = Paths.get(System.getProperty("user.dir") + FileUtils.SEPARATOR + "pref.xml");
        }
        Logger.getLogger(PortablePreferences.class.getName()).log(Level.INFO, "Preference file: {0}", exportFile);
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

            final Path exportFile = getPreferenceFile(file);

            final Preferences preferences = Preferences.userRoot();

            try (final OutputStream os = Files.newOutputStream(exportFile)) {
                try {
                    if (preferences.nodeExists("/jgnash")) {
                        final Preferences p = preferences.node("/jgnash");
                        p.exportSubtree(os);
                    }
                    deleteUserPreferences();
                } catch (final BackingStoreException e) {
                    LogUtil.logSevere(PortablePreferences.class, e);
                }
            } catch (final IOException e) {
                LogUtil.logSevere(ExportPreferencesThread.class, e);
            }
        }
    }
}
