/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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

import jgnash.ui.UIApplication;
import jgnash.ui.commodity.CurrenciesPanel;
import jgnash.ui.util.builder.Action;

import java.awt.event.ActionEvent;

/**
 * UI Action to open the currencies dialog
 *
 * @author Craig Cavanaugh
 *
 */
@Action("currency-addremove-command")
public class CurrenciesAction extends AbstractEnabledAction {

    private static final long serialVersionUID = 0L;

    @Override
    public void actionPerformed(ActionEvent e) {
        CurrenciesPanel.showDialog(UIApplication.getFrame());
    }
}
