/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.net.currency;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jgnash.net.ConnectionFactory;

/**
 * A CurrencyParser for the Yahoo finance site.
 *
 * @author Craig Cavanaugh
 */
public class YahooParser implements CurrencyParser {

    private static final Pattern COMMA_DELIMITER_PATTERN = Pattern.compile(",");

    private BigDecimal result = null;

    @Override
    public synchronized boolean parse(String source, String target) {

        String label = source + target;

        /* Build the URL:  http://finance.yahoo.com/d/quotes.csv?s=USDAUD=X&f=sl1d1t1ba&e=.csv */

        StringBuilder url = new StringBuilder("http://finance.yahoo.com/d/quotes.csv?s=");
        url.append(label);
        url.append("=X&f=sl1d1t1ba&e=.csv");

        BufferedReader in = null;

        try {
            URLConnection connection = ConnectionFactory.openConnection(url.toString());

            if (connection != null) {

                in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));

                /* Result: "USDAUD=X",1.4455,"9/8/2004","5:39am",1.4455,1.4468 */

                String l = in.readLine(); // read the first line
                String[] fields = COMMA_DELIMITER_PATTERN.split(l); // split the line

                if (!"\"N/A\"".equals(fields[2])) { // "N/A"
                    result = new BigDecimal(fields[1]);
                }
            }
        } catch (final SocketTimeoutException | UnknownHostException e) {
            result = null;
            Logger.getLogger(YahooParser.class.getName()).warning(e.getLocalizedMessage());
            return false;
        } catch (final Exception e) {
            Logger.getLogger(YahooParser.class.getName()).severe(e.getLocalizedMessage());
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(YahooParser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return true;
    }

    @Override
    public synchronized BigDecimal getConversion() {
        return result;
    }
}
