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
package jgnash.engine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import jgnash.net.security.NullParser;
import jgnash.net.security.SecurityParser;
import jgnash.net.security.YahooAusParser;
import jgnash.net.security.YahooParser;
import jgnash.net.security.YahooUKParser;
import jgnash.util.Resource;

/**
 * Enumeration for quote download source
 *
 * @author Craig Cavanaugh
 *
 */
@SuppressWarnings({"UnusedDeclaration"})
public enum QuoteSource {

    NONE(Resource.get().getString("QuoteSource.None"), NullParser.class),
    YAHOO(Resource.get().getString("QuoteSource.Yahoo"), YahooParser.class),
    YAHOO_UK(Resource.get().getString("QuoteSource.YahooUK"), YahooUKParser.class),
    YAHOO_AUS(Resource.get().getString("QuoteSource.YahooAus"), YahooAusParser.class);

    private final transient String description;

    private final transient Class<? extends SecurityParser> parser;

    private QuoteSource(String description, final Class<? extends SecurityParser> parser) {
        this.description = description;
        this.parser = parser;
    }

    @Override
    public String toString() {
        return description;
    }

    /**
     * Return a new SecurityParser instance appropriate for the QuoteSource
     *
     * @return a new SecurityParser instance.  Null if not able to create it
     */
    public SecurityParser getParser() {
        try {
            Constructor<?> accConst = parser.getDeclaredConstructor();
            return (SecurityParser) accConst.newInstance();
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            return null; // unable to create object
        }
    }
}
