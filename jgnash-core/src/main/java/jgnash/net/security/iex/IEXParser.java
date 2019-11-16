/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
package jgnash.net.security.iex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import jgnash.engine.SecurityHistoryEvent;
import jgnash.engine.SecurityHistoryEventType;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.net.ConnectionFactory;
import jgnash.net.security.SecurityParser;
import jgnash.time.DateUtils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * SecurityParser for querying iexcloud.io for stock price history and events
 *
 * @author Craig Cavanaugh
 */
public class IEXParser implements SecurityParser {

    private static final String BASE_URL = "https://cloud.iexapis.com";
    private static final String SANDBOX_URL = "https://sandbox.iexapis.com/stable";

    private enum IEXChartPeriod {
        OneDay("1d", 5),
        FiveDay("5d", 5),
        OneMonth("1m", 30),
        ThreeMonth("3m", 30 * 3),
        SixMonth("6m", 30 * 6),
        OneYear("1y", 365),
        TwoYear("2y", 365 * 2),
        FiveYear ("5y", 365 * 5),
        max ("max", Integer.MAX_VALUE);

        final private int days;
        final private String path;

        IEXChartPeriod(final String api, final int days) {
            this.days = days;
            this.path = api;
        }

        public static String getPath(final int days) {
            for (final IEXChartPeriod period : IEXChartPeriod.values()) {
                if (period.days > days) {
                    return period.path;
                }
            }

            return IEXChartPeriod.max.path;
        }
    }

    /**
     * Required IEX Token
     */
    private Supplier<String> tokenSupplier = () -> "";

    private String baseURL = BASE_URL;

    public void setUseSandbox() {
        baseURL = SANDBOX_URL;
    }

    /**
     * Sets a {@code Supplier} that can provide an API token when requested.
     *
     * @param supplier token {@code Supplier}
     */
    @Override
    public void setTokenSupplier(final Supplier<String> supplier) {
        Objects.requireNonNull(supplier);
        tokenSupplier = supplier;
    }

    /**
     * Retrieves historical pricing.
     *
     * The IEX API allows query of a period of time starting with the current date.  The startDate is ignored for REST
     * but is used for filter of the returned data
     *
     * @param securityNode SecurityNode to retrieve events for
     * @param startDate    start date
     * @param endDate      end date
     * @return List of SecurityHistoryNode
     * @throws IOException indicates if IO / Network error has occurred
     */
    @Override
    public List<SecurityHistoryNode> retrieveHistoricalPrice(final SecurityNode securityNode, final LocalDate startDate,
                                                             final LocalDate endDate) throws IOException {

        final String range = IEXChartPeriod.getPath((int) DAYS.between(endDate, LocalDate.now()));

        final String restURL = baseURL + "/stock/" + securityNode.getSymbol() + "/chart/" + range +
                                 "?token=" + tokenSupplier.get() + "&format=csv";

        final List<SecurityHistoryNode> historyNodes = new ArrayList<>();

        final URL url = new URL(restURL);
        final URLConnection connection = url.openConnection();
        connection.setConnectTimeout(ConnectionFactory.getConnectionTimeout() * 1000);

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(),
                StandardCharsets.UTF_8))) {

            try (final CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader())) {
                for (final CSVRecord record : csvParser) {
                    final LocalDate date = LocalDate.parse(record.get("date"), DateTimeFormatter.ISO_DATE);

                    if (DateUtils.between(date, startDate, endDate)) {
                        final BigDecimal price = new BigDecimal(record.get("uClose"));
                        final long volume = Long.parseLong(record.get("uVolume"));
                        final BigDecimal high = new BigDecimal(record.get("uHigh"));
                        final BigDecimal low = new BigDecimal(record.get("uLow"));

                        final SecurityHistoryNode historyNode = new SecurityHistoryNode(date, price, volume, high, low);

                        historyNodes.add(historyNode);
                    }
                }
            }
        }

        return historyNodes;
    }

    /**
     * Retrieves historical events
     *
     * @param securityNode SecurityNode to retrieve events for
     * @param endDate      end date
     * @return Set of SecurityHistoryEvent
     * @throws IOException indicates if IO / Network error has occurred
     */
    @Override
    public Set<SecurityHistoryEvent> retrieveHistoricalEvents(final SecurityNode securityNode,
                                                              final LocalDate endDate) throws IOException {

        // GET /stock/{symbol}/splits/{range}

        final String range = IEXChartPeriod.getPath((int) DAYS.between(endDate, LocalDate.now()));

        final String restURL = baseURL + "/stock/" + securityNode.getSymbol() + "/splits/" + range +
                                       "?token=" + tokenSupplier.get() + "&format=csv";

        final Set<SecurityHistoryEvent> historyEvents = new HashSet<>();

        final URL url = new URL(restURL);
        final URLConnection connection = url.openConnection();
        connection.setConnectTimeout(ConnectionFactory.getConnectionTimeout() * 1000);

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(),
                StandardCharsets.UTF_8))) {

            try (final CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader())) {

                LocalDate startDate = LocalDate.now().minusDays(1);

                for (final CSVRecord record : csvParser) {
                    final LocalDate date = LocalDate.parse(record.get("exDate"), DateTimeFormatter.ISO_DATE);

                    if (DateUtils.between(date, startDate, endDate)) {
                        final BigDecimal toFactor = new BigDecimal(record.get("toFactor"));
                        final BigDecimal fromFactor = new BigDecimal(record.get("fromFactor"));
                        final BigDecimal ratio = fromFactor.divide(toFactor, MathContext.DECIMAL64);

                        final SecurityHistoryEvent event = new SecurityHistoryEvent(SecurityHistoryEventType.DIVIDEND, date, ratio);
                        
                        historyEvents.add(event);
                    }
                }
            }
        }

        return historyEvents;
    }
}
