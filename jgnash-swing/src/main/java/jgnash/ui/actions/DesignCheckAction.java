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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jgnash.ui.UIApplication;
import jgnash.ui.checks.CheckDesignDialog;
import jgnash.ui.util.builder.Action;

/**
 * Action to display help tracker
 *
 * @author Craig Cavanaugh
 *
 */

@Action("designchecks-command")
public final class DesignCheckAction extends AbstractAction {

    /**
     * Invoked when an action occurs.
     *
     * @param e action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        new CheckDesignDialog(UIApplication.getFrame()).setVisible(true);
    }
}