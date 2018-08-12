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
package jgnash.engine.checks;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import jgnash.engine.xstream.XStreamJVM9;
import jgnash.util.OS;

import static jgnash.util.LogUtil.logSevere;

/**
 * Factory methods for serializing CheckLayout objects.
 *
 * @author Craig Cavanaugh
 */
public class CheckLayoutSerializationFactory {

    public static CheckLayout loadLayout(final String file) {
        XStream xstream = getStream();
        CheckLayout layout = null;

        try (final Reader reader = Files.newBufferedReader(Paths.get(file), StandardCharsets.UTF_8)) {
            layout = (CheckLayout) xstream.fromXML(reader);
        } catch (IOException e) {
            logSevere(CheckLayoutSerializationFactory.class, e);
        }
        return layout;
    }

    public static boolean saveLayout(final String file, final CheckLayout layout) {
        boolean result = false;

        XStream xstream = getStream();

        try (final Writer writer = Files.newBufferedWriter(Paths.get(file), StandardCharsets.UTF_8)) {
            xstream.toXML(layout, writer);
            result = true;
        } catch (IOException e) {
            logSevere(CheckLayoutSerializationFactory.class, e);
        }

        return result;
    }

    private static XStream getStream() {
        XStream xstream = new XStreamJVM9(new PureJavaReflectionProvider(), new StaxDriver());
        xstream.alias("CheckLayout", CheckLayout.class);
        xstream.alias("CheckObject", CheckObject.class);       
        
        /* Fix for printing on some Windows Systems.  Value and winID do not always serialize correctly
         * and do not have any apparent impact on restoring printing preferences */
        if (OS.isSystemWindows()) {
            try {
                Class<?> media = Class.forName("sun.print.Win32MediaTray");
                xstream.omitField(media, "value");
                xstream.omitField(media, "winID");
            } catch (ClassNotFoundException e) {
                logSevere(CheckLayoutSerializationFactory.class, e);
            }
        }

        return xstream;
    }

    private CheckLayoutSerializationFactory() {
    }
}
