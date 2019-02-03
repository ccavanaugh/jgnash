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
package jgnash.ui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;

import jgnash.ui.components.ChangeDatabasePasswordDialog;
import jgnash.ui.util.builder.Action;

/**
 * UI Action to change database credentials
 * 
 * @author Craig Cavanaugh
 */
@Action("change-credentials-command")
public class ChangeCredentialsAction extends AbstractEnabledAction {

    @Override
    public void actionPerformed(final ActionEvent e) {
        EventQueue.invokeLater(() -> new ChangeDatabasePasswordDialog().setVisible(true));
    }

    /**
     * Inverts the enabled operation
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(!enabled);
    }
}
