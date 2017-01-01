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
package jgnash.ui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;

import jgnash.engine.message.MessageBus;
import jgnash.ui.UIApplication;
import jgnash.ui.components.RemoteConnectionDialog;
import jgnash.ui.util.builder.Action;

/**
 * UI Action to gracefully shutdown a remote server.
 * 
 * @author Craig Cavanaugh
 */
@Action("shutdown-server-command")
public class ShutdownServerAction extends AbstractEnabledAction {

    @Override
    public void actionPerformed(final ActionEvent e) {
        EventQueue.invokeLater(() -> {
            RemoteConnectionDialog dialog = new RemoteConnectionDialog(UIApplication.getFrame());

            dialog.setVisible(true);

            if (dialog.getResult()) {
                final int port = dialog.getPort() + 1; // message server is base + 1;
                final String host = dialog.getHost();
                final char[] password = dialog.getPassword();

                MessageBus.getInstance().shutDownRemoteServer(host, port, password);
            }
        });
    }


    /**
     * Inverts the enabled operation.
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(!enabled);
    }
}
