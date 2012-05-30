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
package jgnash.engine.checks;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory methods for serializing CheckLayout objects
 *
 * @author Craig Cavanaugh
 *
 */
public class CheckLayoutSerializationFactory {

    public static CheckLayout loadLayout(final String file) {
        XStream xstream = getStream();
        CheckLayout layout = null;

        try (FileReader in = new FileReader(file)) {
            layout = (CheckLayout) xstream.fromXML(in);
        } catch (IOException e) {
            Logger.getLogger(CheckLayoutSerializationFactory.class.getName()).log(Level.SEVERE, null, e);
        }
        return layout;
    }

    public static boolean saveLayout(final String file, final CheckLayout layout) {
        boolean result = false;

        XStream xstream = getStream();

        try (FileWriter out = new FileWriter(file)) {
            xstream.toXML(layout, out);
            result = true;
        } catch (IOException e) {
            Logger.getLogger(CheckLayoutSerializationFactory.class.getName()).log(Level.SEVERE, null, e);
        }

        return result;
    }

    private static XStream getStream() {
        XStream xstream = new XStream(new StaxDriver());
        xstream.alias("CheckLayout", CheckLayout.class);
        xstream.alias("CheckObject", CheckObject.class);

        return xstream;
    }

    private CheckLayoutSerializationFactory() {
    }
}
