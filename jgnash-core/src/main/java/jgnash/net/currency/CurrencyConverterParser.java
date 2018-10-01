package jgnash.net.currency;

import jgnash.net.ConnectionFactory;

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

import static jgnash.util.EncodeDecode.COMMA_DELIMITER_PATTERN;

/**
 * A CurrencyParser for the CurrencyConverterAPI site.
 *
 * @author Pranay Kumar
 * @description Use www.currencyconverterapi.com
 */
public class CurrencyConverterParser implements CurrencyParser {

    private BigDecimal result = null;

    @Override
    public synchronized boolean parse(String source, String target) {

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
                String resp = "";
                String l;
                while ( (l = in.readLine()) != null) {
                    resp = resp + l.trim();
                }

                resp = resp.replace("{","");
                resp = resp.replace("}","");

                String[] tokens = resp.split(":");
                if ( ("\""+label+"\"").equals(tokens[0]) ) {
                    result = new BigDecimal(tokens[1]);
                }
            }
        } catch (final SocketTimeoutException | UnknownHostException e) {
            result = null;
            Logger.getLogger(CurrencyConverterParser.class.getName()).warning(e.getLocalizedMessage());
            return false;
        } catch (final Exception e) {
            Logger.getLogger(CurrencyConverterParser.class.getName()).severe(e.getLocalizedMessage());
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(CurrencyConverterParser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return true;
    }

    @Override
    public synchronized BigDecimal getConversion() {
        return result;
    }
}
