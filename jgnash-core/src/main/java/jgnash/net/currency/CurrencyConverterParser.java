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
package jgnash.net.currency;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import jgnash.net.ConnectionFactory;
import jgnash.util.LogUtil;

/**
 * A CurrencyParser for the CurrencyConverterAPI site.
 *
 * Use www.currencyconverterapi.com
 *
 * @author Pranay Kumar
 */
public class CurrencyConverterParser implements CurrencyParser {

    private BigDecimal result = null;

    @Override
    public synchronized boolean parse(final String source, final String target) {

        String label = source + "_" + target;

        /* Build the URL:  https://free.currencyconverterapi.com/api/v6/convert?q=HKD_INR&compact=ultra */

        StringBuilder url = new StringBuilder("https://free.currencyconverterapi.com/api/v6/convert?q=");
        url.append(label);
        url.append("&compact=ultra");

        BufferedReader in = null;

        try {
            URLConnection connection = ConnectionFactory.openConnection(url.toString());

            if (connection != null) {

                in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));

                /* Result: {"HKD_INR":9.263368} */
                StringBuilder resp = new StringBuilder();
                String l;
                while ( (l = in.readLine()) != null) {
                    resp.append(l.trim());
                }

                resp = new StringBuilder(resp.toString().replace("{", ""));
                resp = new StringBuilder(resp.toString().replace("}", ""));

                String[] tokens = resp.toString().split(":");
                if ( ("\""+label+"\"").equals(tokens[0]) ) {
                    result = new BigDecimal(tokens[1]);
                }
            }
        } catch (final SocketTimeoutException | UnknownHostException e) {
            result = null;
            Logger.getLogger(CurrencyConverterParser.class.getName()).warning(e.getLocalizedMessage());
            return false;
        } catch (final Exception e) {
            LogUtil.logSevere(CurrencyConverterParser.class, e);
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException ex) {
                LogUtil.logSevere(CurrencyConverterParser.class, ex);
            }
        }

        return true;
    }

    @Override
    public synchronized BigDecimal getConversion() {
        return result;
    }
}
