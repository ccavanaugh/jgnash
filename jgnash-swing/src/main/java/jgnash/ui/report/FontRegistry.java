/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
package jgnash.ui.report;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jgnash.util.OS;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;

/**
 * Utility class map font names to font files
 *
 * @author Craig Cavanaugh
 * @version $Id: FontRegistry.java 3122 2012-01-22 13:18:27Z ccavanaugh $
 */
public class FontRegistry {

    /**
     * Maps the font file to the font name for embedding fonts in PDF files
     */
    private final Map<String, String> registeredFontMap = new HashMap<String, String>();

    private static FontRegistry registry;

    private static final AtomicBoolean registrationComplete = new AtomicBoolean(false);

    private static final AtomicBoolean registrationStarted = new AtomicBoolean(false);

    private FontRegistry() {
    }

    static String getRegisteredFontPath(final String name) {
        if (!registrationStarted.get()) {
            registerFonts();
        }

        if (!registrationComplete.get()) {
            while (!registrationComplete.get()) {
                try {
                    Thread.sleep(500);
                    System.out.println("Waiting for font registration to complete");
                } catch (InterruptedException ignored) {
                }
            }
        }

        return registry.registeredFontMap.get(name.toLowerCase());
    }

    public static void registerFonts() {
        if (!registrationStarted.get()) {
            registrationStarted.set(true);

            Thread thread = new Thread() {

                @Override
                public void run() {
                    registry = new FontRegistry();
                    registry.registerFontDirectories();
                    registrationComplete.set(true);
                    System.out.println("Font registration is complete");
                }
            };

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
    }

    private void registerFont(final String path) {
        try {
            if (path.toLowerCase().endsWith(".ttf") || path.toLowerCase().endsWith(".otf") || path.toLowerCase().indexOf(".ttc,") > 0) {
                Object allNames[] = BaseFont.getAllFontNames(path, BaseFont.WINANSI, null);

                String[][] names = (String[][]) allNames[2]; //full name
                for (String[] name : names) {
                    registeredFontMap.put(name[3].toLowerCase(), path);
                }

            } else if (path.toLowerCase().endsWith(".ttc")) {
                String[] names = BaseFont.enumerateTTCNames(path);
                for (int i = 0; i < names.length; i++) {
                    registerFont(path + "," + i);
                }
            } else if (path.toLowerCase().endsWith(".afm") || path.toLowerCase().endsWith(".pfm")) {
                BaseFont bf = BaseFont.createFont(path, BaseFont.CP1252, false);
                String fullName = bf.getFullFontName()[0][3].toLowerCase();
                registeredFontMap.put(fullName, path);
            }
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Register all the fonts in a directory and its subdirectories.
     *
     * @param dir the directory
     */
    private void registerFontDirectory(final String dir) {

        File directory = new File(dir);

        if (directory.isDirectory()) {
            String files[] = directory.list();
            if (files != null) {

                for (String path : files) {
                    try {
                        File file = new File(dir, path);
                        if (file.isDirectory()) {
                            registerFontDirectory(file.getAbsolutePath());
                        } else {
                            String name = file.getPath();
                            String suffix = name.length() < 4 ? null : name.substring(name.length() - 3).toLowerCase();
                            if ("afm".equals(suffix) || "pfm".equals(suffix)) {
                                File pfb = new File(name.substring(0, name.length() - 3) + "pfb");
                                if (pfb.exists()) {
                                    registerFont(name);
                                }
                            } else if ("ttf".equals(suffix) || "otf".equals(suffix) || "ttc".equals(suffix)) {
                                registerFont(name);
                            }
                        }
                    } catch (Exception ignored) {
                        System.out.println("Could not find path for " + path);
                    }
                }
            }
        }
    }

    /**
     * Register fonts in known directories.
     */
    void registerFontDirectories() {
        if (OS.isSystemWindows()) {
            registerFontDirectory("c:/windows/fonts");
            registerFontDirectory("c:/winnt/fonts");
            registerFontDirectory("d:/windows/fonts");
            registerFontDirectory("d:/winnt/fonts");
        } else if (OS.isSystemOSX()) {
            String userhome = System.getProperty("user.home");
            registerFontDirectory(userhome + "/Library/Fonts");
            registerFontDirectory("/Library/Fonts");
            registerFontDirectory("/Network/Library/Fonts");
            registerFontDirectory("/System/Library/Fonts");
        } else { // Linux, etc.
            registerFontDirectory("/usr/share/X11/fonts");
            registerFontDirectory("/usr/X/lib/X11/fonts");
            registerFontDirectory("/usr/openwin/lib/X11/fonts");
            registerFontDirectory("/usr/share/fonts");
            registerFontDirectory("/usr/X11R6/lib/X11/fonts");
        }
    }
}
