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

import jgnash.engine.SecurityHistoryEvent;
import jgnash.engine.SecurityHistoryEventType;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.net.ConnectionFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Retrieves historical stock dividend and split information from Yahoo
 *
 * Craig Cavanaugh
 */
public class YahooEventParser {

    private static final String RESPONSE_HEADER = "Date,Dividends";

    private static final Pattern COMMA_DELIMITER_PATTERN = Pattern.compile(",");

    private YahooEventParser() {
        // Utility class
    }

    public static Set<SecurityHistoryEvent> retrieveNew(final SecurityNode securityNode) {
        final Set<SecurityHistoryEvent> events = new HashSet<>();

        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now();

        List<SecurityHistoryNode> historyNodeList = securityNode.getHistoryNodes();

        if (historyNodeList.size() > 0) {
            startDate = historyNodeList.get(0).getLocalDate();
            endDate = historyNodeList.get(historyNodeList.size() - 1).getLocalDate();
        }

        final List<SecurityHistoryEvent> historyEvents = new ArrayList<>(securityNode.getHistoryEvents());
        if (historyEvents.size() > 0) {
            Collections.sort(historyEvents);

            startDate = historyEvents.get(historyEvents.size() - 1).getDate().plusDays(1);
        }

        // s = symbol
        // a = start month -1
        // b = start day
        // c = start year

        // d = end month -1
        // e = end day
        // f = end year

        // g=v&y=0&z=30000 dividends only

        // http://ichart.finance.yahoo.com/x?s=IBM&a=00&b=2&c=1962&d=04&e=25&f=2011&g=v&y=0&z=30000

        /*
        Date,Dividends
        DIVIDEND, 20110506,0.750000
        DIVIDEND, 20110208,0.650000
        DIVIDEND, 20101108,0.650000
        DIVIDEND, 19971106,0.100000
        DIVIDEND, 19970807,0.100000
        SPLIT, 19970528,2:1
        DIVIDEND, 19970206,0.087500
        DIVIDEND, 19961106,0.087500
        STARTDATE, 19620102
        ENDDATE, 20110525
        TOTALSIZE, 195
        STATUS, 0
         */

        final String s = securityNode.getSymbol();

        final String a = Integer.toString(startDate.getMonthValue() - 1);
        final String b = Integer.toString(startDate.getDayOfMonth());
        final String c = Integer.toString(startDate.getYear());

        final String d = Integer.toString(endDate.getMonthValue() - 1);
        final String e = Integer.toString(endDate.getDayOfMonth());
        final String f = Integer.toString(endDate.getYear());

        //http://ichart.finance.yahoo.com/x?s=IBM&a=00&b=2&c=1962&d=04&e=25&f=2011&g=v&y=0&z=30000

        final StringBuilder url = new StringBuilder("http://ichart.finance.yahoo.com/x?s=").append(s);
        url.append("&a=").append(a).append("&b=").append(b).append("&c=").append(c);
        url.append("&d=").append(d).append("&e=").append(e);url.append("&f=").append(f);
        url.append("&g=v&y=0&z=30000");

        URLConnection connection = null;

        try {
            connection = ConnectionFactory.openConnection(url.toString());

            if (connection != null) {
                try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(),
                        StandardCharsets.UTF_8))) {

                    String line = in.readLine();

                    if (RESPONSE_HEADER.equals(line)) {
                        line = in.readLine(); // prime the first read

                        while (line != null) {
                            if (Thread.currentThread().isInterrupted()) {
                                Thread.currentThread().interrupt();
                            }

                            final String[] fields = COMMA_DELIMITER_PATTERN.split(line);

                            if (fields.length == 3) {   // if fields are != 3, then it's not valid data

                                //DIVIDEND, 19970807,0.100000
                                final LocalDate date = LocalDate.of(Integer.parseInt(fields[1].substring(0, 3)),
                                        Integer.parseInt(fields[1].substring(4, 5)) - 1,
                                        Integer.parseInt(fields[1].substring(6, 7)));

                                switch (fields[0]) {
                                    case "DIVIDEND":
                                        BigDecimal dividend = new BigDecimal(fields[2]);
                                        events.add(new SecurityHistoryEvent(SecurityHistoryEventType.DIVIDEND, date, dividend));
                                        break;
                                    case "SPLIT":
                                        final String[] fraction = fields[2].split(":");

                                        final BigDecimal value = new BigDecimal(fraction[0]).divide(new BigDecimal(fraction[1]),
                                                MathContext.DECIMAL32);
                                        events.add(new SecurityHistoryEvent(SecurityHistoryEventType.SPLIT, date, value));
                                    default:
                                        Logger.getLogger(YahooEventParser.class.getName()).log(Level.SEVERE,
                                                "Unknown event: " + fields[0]);
                                        break;
                                }
                            }

                            line = in.readLine();
                        }

                    }
                }
            }
        } catch (NullPointerException | IOException | NumberFormatException ex) {
            Logger.getLogger(YahooEventParser.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (connection != null) {
                if (connection instanceof HttpURLConnection) {
                    ((HttpURLConnection) connection).disconnect();
                }
            }
        }

        return events;
    }
}
