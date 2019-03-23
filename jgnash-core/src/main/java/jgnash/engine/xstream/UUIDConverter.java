/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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

import java.util.UUID;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

/**
 * Expanded UUID converter for old file formats
 * 
 * TODO: Remove after release 3.1
 */
public class UUIDConverter extends AbstractSingleValueConverter {

    @SuppressWarnings("rawtypes")
	@Override
    public boolean canConvert(final Class type) {
        return type == UUID.class;
    }

    @Override
    public Object fromString(final String string) {

        if (string.length() > 32) {
            try {
                return UUID.fromString(string);
            } catch (final IllegalArgumentException e) {
                throw new ConversionException("Cannot create UUID instance", e);
            }
        }

        // must be and old format uuid
        try {
            return UUID.fromString(fixUUID(string));
        } catch (final IllegalArgumentException e) {
            return UUID.randomUUID();   // last option, new uuid
        }
    }


    public static String fixUUID(final String string) {
        if (string.length() == 32) {

            final StringBuilder builder = new StringBuilder(string);

            for (int i = 0; i < 4; i++) {
                builder.insert(8 + (i * 5), '-');
            }

            return builder.toString();
        }

        // this will covert any String to a reproducible UUID
        return UUID.nameUUIDFromBytes(string.getBytes()).toString();
    }
}
