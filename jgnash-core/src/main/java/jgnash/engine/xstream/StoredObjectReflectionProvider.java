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
package jgnash.engine.xstream;

import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;

import java.util.List;

import jgnash.engine.StoredObject;

/**
 * Expanded XStream reflection provider.
 *
 * This will load all objects that extend {@code StoredObject} into a supplied list as they are created
 *
 * @author Craig Cavanaugh
 */
final class StoredObjectReflectionProvider extends PureJavaReflectionProvider {

    /**
     * Reference to the supplied list to load objects into.
     */
    private final List<StoredObject> objects;

    StoredObjectReflectionProvider(final List<StoredObject> objects) {
        this.objects = objects;
    }

    @Override
    public Object newInstance(final Class type) {
        Object o = super.newInstance(type);

        if (o instanceof StoredObject) {
            objects.add((StoredObject) o);
        }
        return o;
    }
}
