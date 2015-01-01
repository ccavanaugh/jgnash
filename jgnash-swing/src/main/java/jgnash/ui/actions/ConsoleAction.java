/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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

import jgnash.ui.ConsoleDialog;
import jgnash.ui.util.builder.Action;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * UI Action to open the new file dialog
 *
 * @author Craig Cavanaugh
 */
@SuppressWarnings("WeakerAccess")
@Action("console-command")
public class ConsoleAction extends AbstractAction {

    @Override
    public void actionPerformed(final ActionEvent e) {
        ConsoleDialog.show();
    }
}
