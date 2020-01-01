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
package jgnash.engine.message;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import jgnash.engine.xstream.XStreamJVM9;

/**
 * Utility class to generate an XStream instance.
 *
 * @author Craig Cavanaugh
 */
class XStreamFactory {
    private XStreamFactory() {}

    static XStream getInstance() {
    	
        final XStream xstream = new XStreamJVM9(new PureJavaReflectionProvider(), new StaxDriver());
        xstream.alias("Message", Message.class);
        xstream.alias("MessageProperty", MessageProperty.class);

        return xstream;
    }
}
