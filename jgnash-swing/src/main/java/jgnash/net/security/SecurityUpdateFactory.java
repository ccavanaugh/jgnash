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

import java.util.List;

import javax.swing.SwingWorker;

import jgnash.engine.EngineFactory;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityNode;
import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.ui.UIApplication;

/**
 * Fetches latest stock prices in the background
 *
 * @author Craig Cavanaugh
 */
public class SecurityUpdateFactory {

    private static final int ABORT_COUNT = 2;

    private SecurityUpdateFactory() {
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

            for (int i = 0; i < list.size(); i++) {
                if (!isCancelled() && !Thread.currentThread().isInterrupted()) {
                    if (list.get(i).getQuoteSource() != QuoteSource.NONE) { // don't try if source if not defined
                        if (!UpdateFactory.updateOne(list.get(i))) {
                            errorCount++;

                            if (errorCount >= ABORT_COUNT) {
                                setProgress(100);
                                break;
                            }
                        }
                    }
                    setProgress((int) ((i + 1f) / list.size() * 100f));
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
