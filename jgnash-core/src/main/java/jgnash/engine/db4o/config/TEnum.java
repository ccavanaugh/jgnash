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
package jgnash.engine.db4o.config;

import com.db4o.ObjectContainer;
import com.db4o.config.ObjectConstructor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Support class to cleanly persist Enums as a String array.
 * <p/>
 * <b>Assumes that enums cannot be altered or created beyond the class definition</b>
 *
 * @author Craig Cavanaugh
 *
 */
public class TEnum implements ObjectConstructor {

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object onInstantiate(ObjectContainer container, Object storedObject) {
        Enum object = null;

        try {
            String[] raw = (String[]) storedObject;
            Class clazz = Class.forName(raw[0]);
            object = Enum.valueOf(clazz, raw[1]);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(TEnum.class.getName()).log(Level.SEVERE, null, ex);
        }
        return object;
    }

    @Override
    public Object onStore(ObjectContainer container, Object applicationObject) {
        Enum<?> e = (Enum<?>) applicationObject;
        return new String[] { e.getClass().getName(), e.name() };
    }

    @Override
    public void onActivate(ObjectContainer container, Object applicationObject, Object storedObject) {
    }

    @Override
    public Class<?> storedClass() {
        return String[].class;
    }
}
