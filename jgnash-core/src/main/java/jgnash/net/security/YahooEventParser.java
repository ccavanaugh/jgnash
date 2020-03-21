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
package jgnash.net.security;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.MathConstants;
import jgnash.engine.SecurityHistoryEvent;
import jgnash.engine.SecurityHistoryEventType;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.net.ConnectionFactory;
import jgnash.net.YahooCrumbManager;
import jgnash.util.LogUtil;
import jgnash.util.NotNull;

import static jgnash.util.EncodeDecode.COMMA_DELIMITER_PATTERN;

/**
 * Retrieves historical stock dividend and split information from Yahoo.
 *
 * @author Craig Cavanaugh
 */
public class YahooEventParser implements SecurityParser {

    private static final String DIV_RESPONSE_HEADER = "Date,Dividends";

    private static final String SPLIT_RESPONSE_HEADER = "Date,Stock Splits";

    private static final String HISTORY_RESPONSE_HEADER = "Date,Open,High,Low,Close,Adj Close,Volume";

    public Set<SecurityHistoryEvent> retrieveHistoricalEvents(@NotNull final SecurityNode securityNode,
                                                              final LocalDate endDate) throws IOException {

        final Set<SecurityHistoryEvent> events = new HashSet<>();

        // Ensure we have a valid cookie and crumb
        if (!YahooCrumbManager.authorize(securityNode.getSymbol())) {
            return events;
        }

        LocalDate startDate = LocalDate.now().minusDays(1);

        final List<SecurityHistoryNode> historyNodeList = securityNode.getHistoryNodes();

        if (!historyNodeList.isEmpty()) {
            startDate = historyNodeList.get(0).getLocalDate();
        }

        events.addAll(retrieveNewDividends(securityNode, startDate, endDate));
        events.addAll(retrieveNewSplits(securityNode, startDate, endDate));

        return events;
    }

    private static List<SecurityHistoryEvent> retrieveNewDividends(@NotNull final SecurityNode securityNode,
                                                                   final LocalDate startDate, final LocalDate endDate) throws IOException {

        /*
        Date,Dividends
        1989-02-02,0.275
        1989-08-03,0.3025
        1964-02-04,0.00167
        1964-08-04,0.00208
        */

        return parseStream(securityNode, startDate, endDate,
                SecurityHistoryEventType.DIVIDEND, DIV_RESPONSE_HEADER::equals,
                line -> {
                    final String[] fields = COMMA_DELIMITER_PATTERN.split(line);

                    if (fields.length == 2) {   // if fields are != 2, then it's not valid data
                        try {
                            return new SecurityHistoryEvent(SecurityHistoryEventType.DIVIDEND,
                                    parseYahooDate(fields[0]), new BigDecimal(fields[1]));

                        } catch (final DateTimeException | NumberFormatException ex) {
                            Logger.getLogger(YahooEventParser.class.getName()).log(Level.INFO, line);
                            LogUtil.logSevere(YahooEventParser.class, ex);
                        }
                    }
                    return null;
                });
    }

    /**
     * Sets a {@code Supplier} that can provide an API token when requested.
     *
     * @param supplier token {@code Supplier}
     */
    @Override
    public void setTokenSupplier(final Supplier<String> supplier) {
        // no nothing, API not required for access to Yahoo
    }

    @Override
    public List<SecurityHistoryNode> retrieveHistoricalPrice(@NotNull final SecurityNode securityNode,
                                                             final LocalDate startDate, final LocalDate endDate) throws IOException {

        /*
         Date,Open,High,Low,Close,Adj Close,Volume
         2016-01-04,128.344070,128.694244,127.056824,135.949997,128.675323,5229400
         2016-01-05,129.441956,129.565018,127.634193,135.850006,128.580673,3924800
         2016-01-06,127.189331,128.325134,126.469986,135.169998,127.937050,4310900
         */

        final List<SecurityHistoryNode> events = parseStream(securityNode, startDate, endDate,
                SecurityHistoryEventType.PRICE, HISTORY_RESPONSE_HEADER::equals,
                line -> {
                    final String[] fields = COMMA_DELIMITER_PATTERN.split(line);

                    if (fields.length == 7) {   // if fields are != 7, then it's not valid data
                        try {
                            // Date,Open,High,Low,Close,Adj Close,Volume
                            final LocalDate date = parseYahooDate(fields[0]);
                            final BigDecimal high = new BigDecimal(fields[2]);
                            final BigDecimal low = new BigDecimal(fields[3]);
                            final BigDecimal close = new BigDecimal(fields[4]);
                            final long volume = Long.parseLong(fields[6]);

                            return new SecurityHistoryNode(date, close, volume, high, low);
                        } catch (final DateTimeException | NumberFormatException ex) {
                            Logger.getLogger(YahooEventParser.class.getName()).log(Level.INFO, line);
                            LogUtil.logSevere(YahooEventParser.class, ex);

                        }
                    }
                    return null;
                });

        Collections.reverse(events);    // reverse for better chronological order

        return events;
    }


    private static List<SecurityHistoryEvent> retrieveNewSplits(@NotNull final SecurityNode securityNode,
                                                                final LocalDate startDate, final LocalDate endDate) throws IOException {

         /*
        Date,Stock Splits
        1973-05-29,5/4
        1964-05-18,5/4
        1997-05-28,2/1
        */

        return parseStream(securityNode, startDate, endDate,
                SecurityHistoryEventType.SPLIT, SPLIT_RESPONSE_HEADER::equals,
                line -> {
                    final String[] fields = COMMA_DELIMITER_PATTERN.split(line);

                    if (fields.length == 2) {   // if fields are != 2, then it's not valid data
                        try {

                            // Yahoo uses : or /
                            final String delimiter = fields[1].contains(":") ? ":" : "/";

                            final String[] fraction = fields[1].split(delimiter);

                            final BigDecimal value = new BigDecimal(fraction[0])
                                                             .divide(new BigDecimal(fraction[1]), MathConstants.mathContext);

                            return new SecurityHistoryEvent(SecurityHistoryEventType.SPLIT, parseYahooDate(fields[0]),
                                    value);

                        } catch (final DateTimeException | NumberFormatException ex) {
                            Logger.getLogger(YahooEventParser.class.getName()).log(Level.INFO, line);
                            LogUtil.logSevere(YahooEventParser.class, ex);
                        }
                    }
                    return null;
                });

    }

    private static <T> List<T> parseStream(@NotNull final SecurityNode securityNode, final LocalDate startDate,
                                           final LocalDate endDate, final SecurityHistoryEventType type,
                                           final Function<String, Boolean> acceptHeaderFunction,
                                           final Function<String, T> processLineFunction) throws IOException, NullPointerException {

        final List<T> events = new ArrayList<>();

        // Ensure we have a valid cookie and crumb
        if (!YahooCrumbManager.authorize(securityNode.getSymbol())) {
            return events;
        }

        final String url = buildYahooQuery(securityNode, startDate, endDate, type);

        URLConnection connection = null;

        try {
            connection = ConnectionFactory.openConnection(url);

            if (connection != null) {

                // required by Yahoo
                connection.setRequestProperty("Cookie", YahooCrumbManager.getCookie());

                int responseCode = ((HttpURLConnection) connection).getResponseCode();

                if (responseCode == 401) {
                    YahooCrumbManager.clearAuthorization();
                } else {
                    try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(),
                            StandardCharsets.UTF_8))) {

                        String line = in.readLine();

                        if (acceptHeaderFunction.apply(line)) {
                            line = in.readLine(); // prime the first read

                            while (line != null) {
                                if (Thread.currentThread().isInterrupted()) {
                                    Thread.currentThread().interrupt();
                                }

                                events.add(processLineFunction.apply(line));

                                line = in.readLine();
                            }
                        }
                    } catch (final FileNotFoundException ignored) {
                        // silently ignored, history may not exist
                    }
                }
            }
        } catch (final NullPointerException | IOException ex) {
            LogUtil.logSevere(YahooEventParser.class, ex);
            YahooCrumbManager.clearAuthorization();
            throw new IOException(ex);
        } finally {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        }

        return events;
    }

    private static LocalDate parseYahooDate(final String string) {
        return LocalDate.of(Integer.parseInt(string.substring(0, 4)),
                Integer.parseInt(string.substring(5, 7)),
                Integer.parseInt(string.substring(8, 10)));
    }


    private static String buildYahooQuery(final SecurityNode securityNode, final LocalDate startDate,
                                          final LocalDate endDate, final SecurityHistoryEventType event) {
        // dividend 1/1/1962 to 8/22/2015
        // https://query1.finance.yahoo.com/v7/finance/download/IBM?period1=-252442800&period2=1440216000&interval=1d&events=div&crumb=oTulTvLJSBg

        // splits
        // https://query1.finance.yahoo.com/v7/finance/download/IBM?period1=-252442800&period2=1440216000&interval=1d&events=split&crumb=oTulTvLJSBg

        // History
        // https://query1.finance.yahoo.com/v7/finance/download/IBM?period1=-252442800&period2=1440216000&interval=1d&events=history&crumb=oTulTvLJSBg

        final LocalDateTime epoch = LocalDateTime.of(1970, Month.JANUARY, 1, 0, 0);

        long period1 = ChronoUnit.SECONDS.between(epoch, LocalDateTime.of(startDate, LocalTime.MIN));
        long period2 = ChronoUnit.SECONDS.between(epoch, LocalDateTime.of(endDate, LocalTime.MAX));


        final StringBuilder builder = new StringBuilder("https://query1.finance.yahoo.com/v7/finance/download/");
        builder.append(securityNode.getSymbol())
                .append("?period1=").append(period1)
                .append("&period2=").append(period2)
                .append("&interval=1d&events=");

        switch (event) {
            case DIVIDEND:
                builder.append("div");
                break;
            case PRICE:
                builder.append("history");
                break;
            case SPLIT:
                builder.append("split");
                break;
        }

        builder.append("&crumb=").append(YahooCrumbManager.getCrumb());

        return builder.toString();
    }

}
