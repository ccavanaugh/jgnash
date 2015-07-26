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
package jgnash.net.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.net.ConnectionFactory;
import jgnash.util.DateUtils;
import jgnash.util.NotNull;
import jgnash.util.ResourceUtils;

/**
 * Fetches latest stock prices in the background
 *
 * @author Craig Cavanaugh
 */
public class UpdateFactory {

    private static final String UPDATE_ON_STARTUP = "updateSecuritiesOnStartup";

    private static final String RESPONSE_HEADER = "Date,Open,High,Low,Close,Volume,Adj Close";

    private static final Pattern COMMA_DELIMITER_PATTERN = Pattern.compile(",");

    // static reference is kept so LogManager cannot garbage collect the logger
    private static final Logger logger = Logger.getLogger(UpdateFactory.class.getName());

    private static final int TIMEOUT = 1;   // default timeout in minutes

    /**
     * Registers a {@code Handler} with the class logger
     * @param handler {@code Handler} to register
     */
    public static void addLogHandler(final Handler handler) {
        logger.addHandler(handler);
    }

    public static void setUpdateOnStartup(final boolean update) {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            engine.putBoolean(UPDATE_ON_STARTUP, update);
        }
    }

    public static boolean getUpdateOnStartup() {
        boolean result = false;

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            result = engine.getBoolean(UPDATE_ON_STARTUP, false);
        }

        return result;
    }

    public static boolean updateOne(final SecurityNode node) {
        boolean result = false;

        final ExecutorService service = Executors.newSingleThreadExecutor();
        final Future<Boolean> future = service.submit(new UpdateSecurityNodeCallable(node));

        try {
            result = future.get(TIMEOUT, TimeUnit.MINUTES);
            service.shutdown();
        } catch (final InterruptedException | ExecutionException e) { // intentionally interrupted
            logger.log(Level.FINEST, e.getLocalizedMessage(), e);
        } catch (final TimeoutException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        return result;
    }

    public static boolean importHistory(final SecurityNode securityNode, final Date startDate, final Date endDate) {
        boolean result = false;

        final ExecutorService service = Executors.newSingleThreadExecutor();
        final Future<Boolean> future = service.submit(new HistoricalImportCallable(securityNode, startDate, endDate));

        try {
            result = future.get(TIMEOUT, TimeUnit.MINUTES);
            service.shutdown();
        } catch (final InterruptedException | ExecutionException e) { // intentionally interrupted
            logger.log(Level.FINEST, e.getLocalizedMessage(), e);
        } catch (final TimeoutException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }


        return result;
    }

    public static List<SecurityHistoryNode> downloadHistory(final SecurityNode securityNode, final Date startDate,
                                                            final Date endDate) {

        final List<SecurityHistoryNode> newSecurityNodes = new ArrayList<>();

        final Calendar cal = Calendar.getInstance();

        final String s = securityNode.getSymbol().toLowerCase();

        cal.setTime(startDate);
        final String a = Integer.toString(cal.get(Calendar.MONTH));
        final String b = Integer.toString(cal.get(Calendar.DATE));
        final String c = Integer.toString(cal.get(Calendar.YEAR));

        cal.setTime(endDate);
        final String d = Integer.toString(cal.get(Calendar.MONTH));
        final String e = Integer.toString(cal.get(Calendar.DATE));
        final String f = Integer.toString(cal.get(Calendar.YEAR));

        // http://ichart.finance.yahoo.com/table.csv?s=AMD&d=1&e=14&f=2007&g=d&a=2&b=21&c=1983&ignore=.csv << new URL 2.14.07

        StringBuilder r = new StringBuilder("http://ichart.finance.yahoo.com/table.csv?a=");
        r.append(a).append("&b=").append(b).append("&c=").append(c);
        r.append("&d=").append(d).append("&e=").append(e);
        r.append("&f=").append(f).append("&s=").append(s);
        r.append("&y=0&g=d&ignore=.csv");

        URLConnection connection = null;

        try {

            /* Yahoo uses the English locale for the date... force the locale */
            final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

            connection = ConnectionFactory.openConnection(r.toString());

            if (connection != null) {

                // Read, parse, and load the new history nodes into a list to be persisted later.  A relational
                // database may stall and cause the network connection to timeout if persisted inline
                try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(),
                        StandardCharsets.UTF_8))) {

                    String line = in.readLine();

                    // make sure that we have valid data format.
                    if (RESPONSE_HEADER.equals(line)) {

                        //Date,Open,High,Low,Close,Volume,Adj Close
                        //2007-02-13,14.75,14.86,14.47,14.60,17824500,14.60

                        line = in.readLine(); // prime the first read

                        while (line != null) {
                            if (Thread.currentThread().isInterrupted()) {
                                Thread.currentThread().interrupt();
                            }

                            if (line.charAt(0) != '<') { // may have comments in file

                                final String[] fields = COMMA_DELIMITER_PATTERN.split(line);

                                final Date date = df.parse(fields[0]);
                                final BigDecimal high = new BigDecimal(fields[2]);
                                final BigDecimal low = new BigDecimal(fields[3]);
                                final BigDecimal close = new BigDecimal(fields[4]);
                                final long volume = Long.parseLong(fields[5]);

                                newSecurityNodes.add(new SecurityHistoryNode(date, close, volume, high, low));
                            }

                            line = in.readLine();
                        }
                    }
                }

                logger.info(ResourceUtils.getString("Message.UpdatedPrice", securityNode.getSymbol()));
            }
        } catch (NullPointerException | IOException | ParseException | NumberFormatException ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            if (connection != null) {
                if (connection instanceof HttpURLConnection) {
                    ((HttpURLConnection) connection).disconnect();
                }
            }
        }

        return newSecurityNodes;
    }

    public static class HistoricalImportCallable implements Callable<Boolean> {

        private final Date startDate;

        private final Date endDate;

        private final SecurityNode securityNode;

        public HistoricalImportCallable(@NotNull final SecurityNode securityNode, @NotNull final Date startDate, @NotNull final Date endDate) {
            this.securityNode = securityNode;

            if (DateUtils.before(startDate, endDate)) {
                this.startDate = startDate;
                this.endDate = endDate;
            } else {
                this.startDate = endDate;
                this.endDate = startDate;
            }
        }

        @Override
        public Boolean call() throws Exception {
            boolean result = true;

            try {
                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);

                final List<SecurityHistoryNode> newSecurityNodes = downloadHistory(securityNode, startDate, endDate);

                for (final SecurityHistoryNode historyNode : newSecurityNodes) {
                    engine.addSecurityHistory(securityNode, historyNode);
                }
            } catch (NullPointerException | NumberFormatException ex) {
                logger.log(Level.SEVERE, null, ex);
                result = false;
            }

            return result;
        }
    }

    public static class UpdateSecurityNodeCallable implements Callable<Boolean> {

        private final SecurityNode securityNode;

        public UpdateSecurityNodeCallable(@NotNull final SecurityNode securityNode) {
            this.securityNode = securityNode;
        }

        @Override
        public Boolean call() throws Exception {
            boolean result = false;

            final Engine e = EngineFactory.getEngine(EngineFactory.DEFAULT);

            if (e != null && securityNode.getQuoteSource() != QuoteSource.NONE) {
                final SecurityParser parser = securityNode.getQuoteSource().getParser();

                if (parser != null && !Thread.currentThread().isInterrupted()) {  // check for thread interruption
                    if (parser.parse(securityNode)) {

                        final SecurityHistoryNode node = new SecurityHistoryNode(parser.getDate(), parser.getPrice(),
                                parser.getVolume(), parser.getHigh(), parser.getLow());

                        if (!Thread.currentThread().isInterrupted()) { // check for thread interruption
                            result = e.addSecurityHistory(securityNode, node);

                            if (result) {
                                logger.info(ResourceUtils.getString("Message.UpdatedPrice", securityNode.getSymbol()));
                            }
                        }
                    }
                }
            }

            return result;
        }
    }

    private UpdateFactory() {
        // Utility class
    }
}
