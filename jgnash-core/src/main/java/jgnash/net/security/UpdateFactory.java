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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.util.Resource;

/**
 * Fetches latest stock prices in the background
 *
 * @author Craig Cavanaugh
 */
public class UpdateFactory {

    private static final String UPDATE_ON_STARTUP = "updateOnStartup";

    // static reference is kept so LogManager cannot garbage collect the logger
    private static final Logger logger = Logger.getLogger(UpdateFactory.class.getName());

    public static void setUpdateOnStartup(final boolean update) {
        Preferences pref = Preferences.userNodeForPackage(UpdateFactory.class);
        pref.putBoolean(UPDATE_ON_STARTUP, update);
    }

    public static boolean getUpdateOnStartup() {
        Preferences pref = Preferences.userNodeForPackage(UpdateFactory.class);
        return pref.getBoolean(UPDATE_ON_STARTUP, false);
    }

    public static boolean updateOne(final SecurityNode node) {
        boolean result = false;

        final ExecutorService service = Executors.newSingleThreadExecutor();
        final Future<Boolean> future = service.submit(new UpdateOne(node));

        try {
            result = future.get(1, TimeUnit.MINUTES);
            service.shutdown();
        } catch (final InterruptedException | ExecutionException e) { // intentionally interrupted
            logger.log(Level.FINEST, e.getLocalizedMessage(), e);
        } catch (final TimeoutException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        return result;
    }

    public static class UpdateOne implements Callable<Boolean> {

        private final SecurityNode securityNode;

        public UpdateOne(final SecurityNode securityNode) {
            this.securityNode = securityNode;
        }

        @Override
        public Boolean call() throws Exception {
            boolean result = false;

            final Engine e = EngineFactory.getEngine(EngineFactory.DEFAULT);

            if (e != null && securityNode.getQuoteSource() != QuoteSource.NONE) {
                SecurityParser parser = securityNode.getQuoteSource().getParser();

                if (!Thread.currentThread().isInterrupted()) {  // check for thread interruption
                    if (parser.parse(securityNode)) {

                        SecurityHistoryNode history = new SecurityHistoryNode();
                        history.setPrice(parser.getPrice());
                        history.setVolume(parser.getVolume());
                        history.setHigh(parser.getHigh());
                        history.setLow(parser.getLow());
                        history.setDate(parser.getDate());  // returned date from the parser

                        if (!Thread.currentThread().isInterrupted()) { // check for thread interruption
                            if (e.addSecurityHistory(securityNode, history)) {
                                logger.info(Resource.get().getString("Message.UpdatedPrice", securityNode.getSymbol()));
                                result = true;
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
