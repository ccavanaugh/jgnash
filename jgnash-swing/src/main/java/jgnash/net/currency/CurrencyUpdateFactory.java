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
package jgnash.net.currency;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.SwingWorker;

import jgnash.engine.CurrencyNode;
import jgnash.engine.EngineFactory;
import jgnash.ui.UIApplication;

/**
 * Fetches latest exchange rates in the background
 *
 * @author Craig Cavanaugh
 *
 */
public class CurrencyUpdateFactory {

    private static final String UPDATE_ON_STARTUP = "updateOnStartup";

    private CurrencyUpdateFactory() {
    }

    public static void setUpdateOnStartup(boolean update) {
        Preferences pref = Preferences.userNodeForPackage(CurrencyUpdateFactory.class);
        pref.putBoolean(UPDATE_ON_STARTUP, update);
    }

    public static boolean getUpdateOnStartup() {
        Preferences pref = Preferences.userNodeForPackage(CurrencyUpdateFactory.class);
        return pref.getBoolean(UPDATE_ON_STARTUP, false);
    }

    public static ExchangeRateUpdateWorker getUpdateWorker() {
        return new ExchangeRateUpdateWorker();
    }

    public static class ExchangeRateUpdateWorker extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() throws Exception {

            if (UIApplication.getFrame() != null) {
                UIApplication.getFrame().setNetworkBusy(true);
            }

            List<CurrencyNode> list = new ArrayList<>(EngineFactory.getEngine(EngineFactory.DEFAULT).getCurrencies());

            int lengthOfTask = (list.size() * list.size() - list.size()) / 2;

            int count = 0;

            for (CurrencyNode i : list) {
                String source = i.getSymbol();
                for (CurrencyNode j : list) {
                    String target = j.getSymbol();

                    if (!source.equals(target) && source.compareToIgnoreCase(target) > 0 && !isCancelled()) {
                        CurrencyParser parser = new YahooParser();

                        if (parser.parse(source, target)) {
                            BigDecimal conv = parser.getConversion();

                            if (conv != null && conv.compareTo(BigDecimal.ZERO) != 0) {
                                EngineFactory.getEngine(EngineFactory.DEFAULT).setExchangeRate(i, j, conv);
                            }
                        }                   

                        count++;

                        setProgress((int) ((float) count / (float) lengthOfTask * 100f));
                    }
                }
            }
            return null;
        }

        @Override
        protected void done() {
            if (UIApplication.getFrame() != null) {
                UIApplication.getFrame().setNetworkBusy(false);
            }
        }
    }
}
