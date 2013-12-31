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
package jgnash.ui.actions;

import java.awt.event.ActionEvent;

import javax.swing.SwingWorker;

import jgnash.engine.EngineFactory;
import jgnash.ui.UIApplication;
import jgnash.ui.util.builder.Action;
import jgnash.util.Resource;

/**
 * UI Action to close the current database
 * 
 * @author Craig Cavanaugh
 *
 */
@SuppressWarnings("WeakerAccess")
@Action("close-command")
public class CloseEngineAction extends AbstractEnabledAction {

    private static void closeEngine() {

        final class CloseEngine extends SwingWorker<Void, Void> {

            @Override
            protected Void doInBackground() throws Exception {

                UIApplication.getFrame().closeAllWindows(); // close any open windows first

                UIApplication.getFrame().displayWaitMessage(Resource.get().getString("Message.PleaseWait"));

                // Disk IO is heavy so delay and allow the UI to react before starting the close operation
                Thread.sleep(750);

                EngineFactory.closeEngine(EngineFactory.DEFAULT);

                return null;
            }

            @Override
            protected void done() {
                UIApplication.getFrame().stopWaitMessage();
            }
        }

        new CloseEngine().execute();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        closeEngine();
    }
}
