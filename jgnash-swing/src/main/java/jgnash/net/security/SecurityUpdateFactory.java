/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.SwingWorker;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.ui.UIApplication;
import jgnash.util.Resource;

/**
 * Fetches latest stock prices in the background
 * 
 * @author Craig Cavanaugh
 */
public class SecurityUpdateFactory {

    private static final int ABORT_COUNT = 2;

    private static final String UPDATE_ON_STARTUP = "updateOnStartup";

    // static reference is kept so LogManager cannot garbage collect the logger
    private static final Logger logger = Logger.getLogger(SecurityUpdateFactory.class.getName());

    private SecurityUpdateFactory() {
    }

    public static void setUpdateOnStartup(final boolean update) {
        Preferences pref = Preferences.userNodeForPackage(SecurityUpdateFactory.class);
        pref.putBoolean(UPDATE_ON_STARTUP, update);
    }

    public static boolean getUpdateOnStartup() {
        Preferences pref = Preferences.userNodeForPackage(SecurityUpdateFactory.class);
        return pref.getBoolean(UPDATE_ON_STARTUP, false);
    }

    public static boolean updateOne(final SecurityNode node) {

        Resource rb = Resource.get();

        // if source is set to none, do not try to parse
        if (node.getQuoteSource() == QuoteSource.NONE) {
            return false;
        }

        SecurityParser parser = node.getQuoteSource().getParser();

        try {
            if (parser.parse(node)) {
                SecurityHistoryNode history = new SecurityHistoryNode();
                history.setPrice(parser.getPrice());
                history.setVolume(parser.getVolume());
                history.setHigh(parser.getHigh());
                history.setLow(parser.getLow());
                history.setDate(parser.getDate());  // returned date from the parser

                Engine e = EngineFactory.getEngine(EngineFactory.DEFAULT);

                if (e != null) {
                    e.addSecurityHistory(node, history);
                }

                String message = MessageFormat.format(rb.getString("Message.UpdatedPrice"), node.getSymbol());

                logger.info(message);

                return true;
            }
        } catch (IOException e) {
            logger.severe(e.toString());
            return false;
        }

        return false;
    }

    public static SecurityUpdateWorker getUpdateWorker() {
        return new SecurityUpdateWorker();
    }

    public static class SecurityUpdateWorker extends SwingWorker<Void, Void> implements MessageListener {

        public SecurityUpdateWorker() {
            super();
            registerListeners();            
        }
        
        private void registerListeners() {
          MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM);  
        }

        @Override
        protected Void doInBackground() throws Exception {

            UIApplication.getFrame().setNetworkBusy(true);

            List<SecurityNode> list = EngineFactory.getEngine(EngineFactory.DEFAULT).getSecurities();

            int errorCount = 0;

            int size = list.size();

            for (int i = 0; i < list.size(); i++) {
                if (!isCancelled()) {
                    if (list.get(i).getQuoteSource() != QuoteSource.NONE) { // don't try if source if not defined
                        if (!updateOne(list.get(i))) {
                            errorCount++;

                            if (errorCount >= ABORT_COUNT) {
                                setProgress(100);
                                break;
                            }
                        }                     
                    }
                    setProgress((int) ((i + 1f) / size * 100f));
                }
            }
            return null;
        }

        @Override
        public void messagePosted(final Message event) {
            if (event.getEvent() == ChannelEvent.FILE_CLOSING) {
                cancel(true);
            }
        }

        @Override
        protected void done() {
            UIApplication.getFrame().setNetworkBusy(false);
            MessageBus.getInstance().unregisterListener(this, MessageChannel.SYSTEM);
        }
    }
}
