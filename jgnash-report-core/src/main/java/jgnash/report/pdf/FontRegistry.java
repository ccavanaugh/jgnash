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
package jgnash.report.pdf;

import jgnash.util.LogUtil;
import org.apache.fontbox.util.autodetect.FontFileFinder;

import java.awt.Font;
import java.awt.FontFormatException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Utility class map font names to font files
 *
 * @author Craig Cavanaugh
 */
public class FontRegistry {

    /**
     * Maps the report file to the report name for embedding fonts in PDF files
     */
    private static final Map<String, String> registeredFontMap = new ConcurrentHashMap<>();

    private static final AtomicBoolean registrationComplete = new AtomicBoolean(false);

    private static final AtomicBoolean registrationStarted = new AtomicBoolean(false);

    private static final Lock lock = new ReentrantLock();

    private static final Condition isComplete = lock.newCondition();

    private FontRegistry() {
    }

    public static List<String> getFontList() {
        blockForFontRegistration();

        final ArrayList<String> fontList = new ArrayList<>(registeredFontMap.keySet());

        Collections.sort(fontList);

        return fontList;
    }

    private static void blockForFontRegistration() {
        if (!registrationStarted.get()) {
            registerFonts();
        }

        if (!registrationComplete.get()) {

            try {
                lock.lock();

                while (!registrationComplete.get()) {
                    isComplete.await();
                }
            } catch (final InterruptedException ex) {
                LogUtil.logSevere(FontRegistry.class, ex);
            } finally {
                lock.unlock();
            }
        }
    }

    static String getRegisteredFontPath(final String name) {
        blockForFontRegistration();

        return registeredFontMap.get(name);
    }

    private static void registerFonts() {

        if (!registrationStarted.get()) {
            registrationStarted.set(true);

            final Thread thread = new Thread(() -> {
                lock.lock();

                try {
                    FontRegistry.registerFontDirectories();

                    registrationComplete.set(true);
                    isComplete.signal();

                    Logger.getLogger(FontRegistry.class.getName()).info("Font registration is complete");
                } finally {
                    lock.unlock();
                }
            });

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
    }

    private static void registerFont(final String path) {
        try {
            if (path.toLowerCase(Locale.ROOT).endsWith(".ttf") || path.toLowerCase(Locale.ROOT).endsWith(".otf")
                    || path.toLowerCase(Locale.ROOT).indexOf(".ttc,") > 0) {

                try (final FileInputStream fileInputStream = new FileInputStream(path)) {
                    final Font font = Font.createFont(Font.TRUETYPE_FONT, fileInputStream);
                    registeredFontMap.put(font.getName(), path);
                }
            } else if (path.toLowerCase(Locale.ROOT).endsWith(".afm") || path.toLowerCase(Locale.ROOT).endsWith(".pfm")) {
                try (final FileInputStream fileInputStream = new FileInputStream(path)) {
                    final Font font = Font.createFont(Font.TYPE1_FONT, fileInputStream);
                    registeredFontMap.put(font.getName(), path);
                }
            }
        } catch (final IOException e) {
            LogUtil.logSevere(FontRegistry.class, e);
        } catch (final FontFormatException ffe) {
            Logger.getLogger(FontRegistry.class.getName()).info("Could not register font: " + path);
            LogUtil.logSevere(FontRegistry.class, ffe);
        }
    }

    /**
     * Register fonts in known directories.
     */
    private static void registerFontDirectories() {
        new FontFileFinder().find().stream().map(uri
                -> new File(uri).getAbsolutePath()).forEach(FontRegistry::registerFont);
    }
}
