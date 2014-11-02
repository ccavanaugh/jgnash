/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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

import jgnash.engine.SecurityNode;
import jgnash.net.ConnectionFactory;
import jgnash.util.DateUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * An abstract CommodityParser for the Yahoo! financial web sites.
 * 
 * @author Craig Cavanaugh
 */
public abstract class AbstractYahooParser implements SecurityParser {

    private static final Logger logger = Logger.getLogger(AbstractYahooParser.class.getName());

    private static final Pattern COMMA_DELIMITER_PATTERN = Pattern.compile(",");

    private long volume;

    private BigDecimal price;

    private BigDecimal high;

    private BigDecimal low;

    private Date date = DateUtils.today();

    @Override
    public synchronized long getVolume() {
        return volume;
    }

    private synchronized void setVolume(final long volume) {
        this.volume = volume;
    }

    @Override
    public synchronized BigDecimal getPrice() {
        return price;
    }

    private synchronized void setPrice(final BigDecimal price) {
        this.price = price;
    }

    @Override
    public synchronized BigDecimal getHigh() {
        return high;
    }

    private synchronized void setHigh(final BigDecimal high) {
        this.high = high;
    }

    @Override
    public synchronized BigDecimal getLow() {
        return low;
    }

    private synchronized void setLow(final BigDecimal low) {
        this.low = low;
    }

    /**
     * @return the date
     */
    @Override
    public Date getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    void setDate(Date date) {
        this.date = date;
    }

    // http://uk.old.finance.yahoo.com/d/quotes.csv?s=GB00B0HZR397GBP&f=sl1t1c1ohgv&e=.csv
    // http://download.finance.yahoo.com/d/quotes.csv?s=AMD&f=sl1d1t1c1ohgv&e=.csv
    protected abstract String getBaseURL();

    @Override
    public boolean useISIN() {
        return false;
    }

    @Override
    public synchronized boolean parse(final SecurityNode node) {

        boolean result = false;

        String base = getBaseURL();

        String symbol;

        if (useISIN()) {
            symbol = node.getISIN();
        } else {
            symbol = node.getSymbol();
        }

        // http://finance.yahoo.com/d/quotes.csv?s=SUNW&f=sl1d1t1c1ohgv&e=.csv
        // String u = "http://finance.yahoo.com/d/quotes.csv?s=" + symbol +

        String u = base + symbol + "&f=sl1d1t1c1ohgv&e=.csv";

        String line = null;

        final URLConnection connection = ConnectionFactory.getConnection(u);

        if (connection != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                line = in.readLine();

                if (line != null) {

                    // trim the line.  Yahoo may occasionally include some extra white space
                    line = line.trim();

                    String[] fields = COMMA_DELIMITER_PATTERN.split(line);
                    in.close();

                    if (fields.length >= 7) {

                        // may be returned as a yield percentage... ignore for now
                        if (!fields[1].contains("%")) {
                            setPrice(new BigDecimal(fields[1]));
                        }

                        // try to parse the date "10/12/2012"
                        // the date from Yahoo is the last close date.  It may not reflect the date the parse is performed
                        if (!fields[2].isEmpty()) {
                            try {
                                DateFormat df = new SimpleDateFormat("\"MM/dd/yyyy\"");
                                Date date = df.parse(fields[2]);
                                setDate(date);
                            } catch (ParseException e) {
                                logger.log(Level.SEVERE, null, e);
                            }
                        }

                        if (fields[6].equals("N/A")) {
                            setHigh(BigDecimal.ZERO);
                        } else {
                            setHigh(new BigDecimal(fields[6]));
                        }

                        if (fields[7].equals("N/A")) {
                            setLow(BigDecimal.ZERO);
                        } else {
                            setLow(new BigDecimal(fields[7]));
                        }

                        if (fields[8].equals("N/A")) {
                            setVolume(0);
                        } else {
                            setVolume(Long.parseLong(fields[8]));
                        }

                        result = true;
                    }
                }

            } catch (SocketTimeoutException | UnknownHostException e) {
                price = null;
                logger.log(Level.WARNING, e.getLocalizedMessage(), e);
            } catch (NumberFormatException e) {
                logger.log(Level.SEVERE, line, e);
            } catch (Exception e) {
                logger.log(Level.SEVERE, null, e);
            }
        }

        return result;
    }
}