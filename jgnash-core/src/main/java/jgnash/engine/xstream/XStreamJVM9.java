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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.basic.BigDecimalConverter;
import com.thoughtworks.xstream.converters.basic.BigIntegerConverter;
import com.thoughtworks.xstream.converters.basic.BooleanConverter;
import com.thoughtworks.xstream.converters.basic.ByteConverter;
import com.thoughtworks.xstream.converters.basic.CharConverter;
import com.thoughtworks.xstream.converters.basic.DateConverter;
import com.thoughtworks.xstream.converters.basic.DoubleConverter;
import com.thoughtworks.xstream.converters.basic.FloatConverter;
import com.thoughtworks.xstream.converters.basic.IntConverter;
import com.thoughtworks.xstream.converters.basic.LongConverter;
import com.thoughtworks.xstream.converters.basic.NullConverter;
import com.thoughtworks.xstream.converters.basic.ShortConverter;
import com.thoughtworks.xstream.converters.basic.StringConverter;
import com.thoughtworks.xstream.converters.collections.ArrayConverter;
import com.thoughtworks.xstream.converters.collections.CharArrayConverter;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.converters.enums.EnumConverter;
import com.thoughtworks.xstream.converters.extended.EncodedByteArrayConverter;
import com.thoughtworks.xstream.converters.reflection.ExternalizableConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.SerializableConverter;
import com.thoughtworks.xstream.converters.time.InstantConverter;
import com.thoughtworks.xstream.converters.time.LocalDateConverter;
import com.thoughtworks.xstream.converters.time.LocalDateTimeConverter;
import com.thoughtworks.xstream.converters.time.LocalTimeConverter;
import com.thoughtworks.xstream.core.util.SelfStreamingInstanceChecker;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.converters.basic.UUIDConverter;

/**
 * This Replaces the default XStream constructor to prevent the loading of converters that trigger an illegal reflective 
 * access operation when operating with JVM 9 or newer.
 * 
 * @author Craig Cavanaugh
 */
public class XStreamJVM9 extends XStream {

	public XStreamJVM9(final ReflectionProvider reflectionProvider, final HierarchicalStreamDriver hierarchicalStreamDriver) {
		super(reflectionProvider, hierarchicalStreamDriver);
	}	

	@Override
	protected void setupConverters() {
				
		registerConverter(new ReflectionConverter(getMapper(), getReflectionProvider()), PRIORITY_VERY_LOW);
		registerConverter(new SerializableConverter(getMapper(), getReflectionProvider(), getClassLoaderReference()), PRIORITY_LOW);
		registerConverter(new ExternalizableConverter(getMapper(), getClassLoaderReference()), PRIORITY_LOW);

		registerConverter(new NullConverter(), PRIORITY_VERY_HIGH);
		registerConverter(new IntConverter(), PRIORITY_NORMAL);
		registerConverter(new FloatConverter(), PRIORITY_NORMAL);
		registerConverter(new DoubleConverter(), PRIORITY_NORMAL);
		registerConverter(new LongConverter(), PRIORITY_NORMAL);
		registerConverter(new ShortConverter(), PRIORITY_NORMAL);
		registerConverter((Converter) new CharConverter(), PRIORITY_NORMAL);
		registerConverter(new BooleanConverter(), PRIORITY_NORMAL);
		registerConverter(new ByteConverter(), PRIORITY_NORMAL);
		registerConverter(new StringConverter(), PRIORITY_NORMAL);
		registerConverter(new DateConverter(), PRIORITY_NORMAL);
		registerConverter(new BigIntegerConverter(), PRIORITY_NORMAL);
		registerConverter(new BigDecimalConverter(), PRIORITY_NORMAL);
		registerConverter(new ArrayConverter(getMapper()), PRIORITY_NORMAL);
		registerConverter(new CharArrayConverter(), PRIORITY_NORMAL);
		registerConverter(new CollectionConverter(getMapper()), PRIORITY_NORMAL);
		registerConverter(new MapConverter(getMapper()), PRIORITY_NORMAL);
		registerConverter((Converter) new EncodedByteArrayConverter(), PRIORITY_NORMAL);
		registerConverter(new EnumConverter(), PRIORITY_NORMAL);
		registerConverter(new InstantConverter(), PRIORITY_NORMAL);
		registerConverter(new LocalDateConverter(), PRIORITY_NORMAL);
		registerConverter(new LocalDateTimeConverter(), PRIORITY_NORMAL);
		registerConverter(new LocalTimeConverter(), PRIORITY_NORMAL);
		registerConverter(new UUIDConverter(), PRIORITY_NORMAL);

		registerConverter(new SelfStreamingInstanceChecker(getConverterLookup(), this), PRIORITY_NORMAL);				
	}

}
