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
package jgnash.net.security;

/**
 * A CommodityParser for the Yahoo! UK financial site.
 *
 * @author Craig Cavanaugh
 *
 */
public class YahooUKParser extends AbstractYahooParser {

    @Override
    public String getBaseURL() {
        // http://uk.finance.yahoo.com/d/quotes.csv?s=GB00B0HZR397GBP&f=sl1t1c1ohgv&e=.csv          
        
        return "http://uk.finance.yahoo.com/d/quotes.csv?s=";
    }

    @Override
    public boolean useISIN() {
        return false;
    }
}