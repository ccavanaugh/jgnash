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

import java.awt.EventQueue;

import javax.swing.AbstractAction;

import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;

/**
 * Abstract action to automatically enables/disabled itself as files are loaded and unloaded
 *
 * @author Craig Cavanaugh
 *
 */
public abstract class AbstractEnabledAction extends AbstractAction implements MessageListener {

    private static final long serialVersionUID = -7931923279511599388L;

    public AbstractEnabledAction() {
        registerListener();
        setEnabled(false);
    }

    private void registerListener() {
        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM);
    }

    @Override
    public void messagePosted(final Message event) {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                switch (event.getEvent()) {
                    case FILE_CLOSING:
                        setEnabled(false);
                        break;
                    case FILE_NEW_SUCCESS:
                    case FILE_LOAD_SUCCESS:
                    case UI_RESTARTED:
                        setEnabled(true);
                        break;
                    default:
                        break;
                }
            }
        });
    }

}
