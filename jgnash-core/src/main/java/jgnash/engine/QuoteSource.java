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
package jgnash.engine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.net.security.NullParser;
import jgnash.net.security.SecurityParser;
import jgnash.util.ResourceUtils;

/**
 * Enumeration for quote download source.
 *
 * Not actually used at this time
 *
 * @author Craig Cavanaugh
 */
public enum QuoteSource {

    NONE(ResourceUtils.getString("QuoteSource.None"), NullParser.class),
    YAHOO(ResourceUtils.getString("QuoteSource.Yahoo"), NullParser.class),
    YAHOO_UK(ResourceUtils.getString("QuoteSource.YahooUK"), NullParser.class),
    YAHOO_AUS(ResourceUtils.getString("QuoteSource.YahooAus"), NullParser.class);

    private final transient String description;

    private final transient Class<? extends SecurityParser> parser;

    QuoteSource(String description, final Class<? extends SecurityParser> parser) {
        this.description = description;
        this.parser = parser;
    }

    @Override
    public String toString() {
        return description;
    }

    /**
     * Return a new SecurityParser instance appropriate for the QuoteSource.
     *
     * @return a new SecurityParser instance.  Null if not able to create it
     */
    public SecurityParser getParser() {
        try {
            Constructor<?> accConst = parser.getDeclaredConstructor();
            return (SecurityParser) accConst.newInstance();
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Logger.getLogger(QuoteSource.class.getName()).log(Level.SEVERE, e.toString(), e);
            return null; // unable to create object
        }
    }
}
