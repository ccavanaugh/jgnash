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

import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Support class to cleanly persist BigIntegers as a String
 *
 * @author Craig Cavanaugh
 *
 */
public class TBigInteger implements ObjectConstructor {

    @Override
    public Object onInstantiate(ObjectContainer container, Object storedObject) {

        BigInteger object = BigInteger.ZERO;

        try {
            object = new BigInteger((String) storedObject);
        } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Could not instantiate storedObject", e);
        }
        return object;
    }

    @Override
    public void onActivate(ObjectContainer container, Object applicationObject, Object storedObject) {
        // do nothing
    }

    @Override
    public Object onStore(ObjectContainer container, Object object) {
        return object.toString();
    }

    @Override
    public Class<String> storedClass() {
        return String.class;
    }
}
