/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
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

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityHistoryEvent;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.time.DateUtils;
import jgnash.util.NotNull;
import jgnash.util.ResourceUtils;

/**
 * Fetches latest stock prices in the background.
 *
 * @author Craig Cavanaugh
 */
public class UpdateFactory {

    private static final String UPDATE_ON_STARTUP = "updateSecuritiesOnStartup";

    // static reference is kept so LogManager cannot garbage collect the logger
    private static final Logger logger = Logger.getLogger(UpdateFactory.class.getName());

    private static final int TIMEOUT = 1;   // default timeout in minutes

    /**
     * Registers a {@code Handler} with the class logger.
     *
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

    /**
     * Determines if an automatic update is recommended.
     * <p>
     * The current approach is to avoid multiple updates on Saturday or Sunday if one has already occurred.
     * This could be expanded to understand locale rules.
     *
     * @param lastUpdate the last known timestamp for an update to have occurred
     * @return true if an update is recommended
     */
    public static boolean shouldAutomaticUpdateOccur(final LocalDateTime lastUpdate) {
        boolean result = true;

        final LocalDate lastDate = LocalDate.from(lastUpdate);
        final DayOfWeek lastDayOfWeek = lastDate.getDayOfWeek();

        if (lastDayOfWeek == DayOfWeek.SATURDAY || lastDayOfWeek == DayOfWeek.SUNDAY) {
            if (LocalDate.now().equals(lastDate) ||
                    (LocalDate.now().minusDays(1).equals(lastDate)) && lastDayOfWeek == DayOfWeek.SATURDAY) {
                result = false;
            }
        }

        if (result && LocalDate.now().equals(lastDate)) {   // check for an after hours update
            switch (Locale.getDefault().getCountry()) {
                case "AU":
                case "CA":
                case "HK":
                case "US":
                    final ZonedDateTime zdtUS = lastUpdate.atZone(ZoneId.of("UTC").normalized());
                    if (zdtUS.getHour() >= 21 && zdtUS.getMinute() > 25) {  // 4:25 EST for delayed online sources
                        result = false;
                    }
                    break;
                case "GB":  // UK
                    final ZonedDateTime zdtUK = lastUpdate.atZone(ZoneId.of("UTC").normalized());
                    if (zdtUK.getHour() >= 21 && zdtUK.getMinute() > 55) {  // 4:55 EST for delayed online sources
                        result = false;
                    }
                    break;
                case "IN":  // India
                    final ZonedDateTime zdtIN = lastUpdate.atZone(ZoneId.of("UTC").normalized());
                    if (zdtIN.getHour() >= 20 && zdtIN.getMinute() > 55) {  // 3:55 EST for delayed online sources
                        result = false;
                    }
                    break;
                default:
                    break;
            }
        }

        return result;
    }

    public static boolean getUpdateOnStartup() {
        boolean result = false;

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        if (engine != null) {
            result = engine.getBoolean(UPDATE_ON_STARTUP, false);
        }

        return result;
    }

    public static void updateSecurityEvents(final SecurityNode node) {
        waitForCallable(new UpdateSecurityNodeEventsCallable(node));
    }

    public static boolean updateOne(final SecurityNode node) {
        return waitForCallable(new UpdateSecurityNodeCallable(node));
    }

    public static boolean importHistory(final SecurityNode securityNode, final LocalDate startDate, final LocalDate endDate) {
        return waitForCallable(new HistoricalImportCallable(securityNode, startDate, endDate));
    }

    private static boolean waitForCallable(final Callable<Boolean> callable) {
        boolean result = false;

        final ExecutorService service = Executors.newSingleThreadExecutor();
        final Future<Boolean> future = service.submit(callable);

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

    public static List<SecurityHistoryNode> downloadHistory(final SecurityNode securityNode, final LocalDate startDate,
                                                            final LocalDate endDate) {

        final List<SecurityHistoryNode> newSecurityNodes = YahooEventParser.retrieveHistoricalPrice(securityNode,
                startDate, endDate);

        if (newSecurityNodes.size() > 0) {
            logger.info(ResourceUtils.getString("Message.UpdatedPrice", securityNode.getSymbol()));
        }

        return newSecurityNodes;
    }

    private static class HistoricalImportCallable implements Callable<Boolean> {

        private final LocalDate startDate;

        private final LocalDate endDate;

        private final SecurityNode securityNode;

        HistoricalImportCallable(@NotNull final SecurityNode securityNode, @NotNull final LocalDate startDate,
                                 @NotNull final LocalDate endDate) {
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

    /**
     * Updates historical information for one day
     */
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

                if (!Thread.currentThread().isInterrupted()) {  // check for thread interruption

                    final List<SecurityHistoryNode> nodes = YahooEventParser.retrieveHistoricalPrice(securityNode,
                            LocalDate.now().minusDays(1), LocalDate.now());

                    for (final SecurityHistoryNode node : nodes) {
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

    public static class UpdateSecurityNodeEventsCallable implements Callable<Boolean> {

        private final SecurityNode securityNode;

        public UpdateSecurityNodeEventsCallable(@NotNull final SecurityNode securityNode) {
            this.securityNode = securityNode;
        }

        @Override
        public Boolean call() throws Exception {

            boolean result = true;

            final Engine e = EngineFactory.getEngine(EngineFactory.DEFAULT);

            final LocalDate oldest = securityNode.getHistoryNodes().get(0).getLocalDate();

            if (e != null && securityNode.getQuoteSource() != QuoteSource.NONE) {

                final Set<SecurityHistoryEvent> oldHistoryEvents = new HashSet<>(securityNode.getHistoryEvents());

                for (final SecurityHistoryEvent securityHistoryEvent : YahooEventParser.retrieveNew(securityNode)) {
                    if (!Thread.currentThread().isInterrupted()) { // check for thread interruption

                        if (securityHistoryEvent.getDate().isAfter(oldest) || securityHistoryEvent.getDate().isEqual(oldest)) {
                            if (!oldHistoryEvents.contains(securityHistoryEvent)) {
                                result = e.addSecurityHistoryEvent(securityNode, securityHistoryEvent);

                                if (result) {
                                    logger.info(ResourceUtils.getString("Message.UpdatedSecurityEvent", securityNode.getSymbol()));
                                }
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
