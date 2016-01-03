/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jgnash.engine.SecurityNode;
import jgnash.net.ConnectionFactory;

/**
 * An abstract CommodityParser for the Yahoo! financial web sites.
 * 
 * @author Craig Cavanaugh
 */
public abstract class YahooParser implements SecurityParser {

    public static final Logger logger = Logger.getLogger(YahooParser.class.getName());

    private static final Pattern COMMA_DELIMITER_PATTERN = Pattern.compile(",");

    private long volume;

    private BigDecimal price;

    private BigDecimal high;

    private BigDecimal low;

    private LocalDate date = LocalDate.now();

    /**
     * Registers a {@code Handler} with the class logger.
     * This also ensures the static logger is initialized.
     * @param handler {@code Handler} to register
     */
    public static void addLogHandler(final Handler handler) {
        logger.addHandler(handler);
    }

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
    public LocalDate getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    private void setDate(LocalDate date) {
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

        final URLConnection connection = ConnectionFactory.openConnection(u);

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
                                final String date =fields[2].replace("\"", "");
                                final DateTimeFormatter df = DateTimeFormatter.ofPattern("M/d/y");
                                setDate(LocalDate.from(df.parse(date)));
                            } catch (final DateTimeParseException e) {
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