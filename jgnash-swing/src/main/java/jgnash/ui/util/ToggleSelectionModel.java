/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
package jgnash.ui.util;

import javax.swing.DefaultListSelectionModel;

/**
 * Extends the DefaultListSelectionModel to make it easier to select
 * multiple items
 *
 * @author Craig Cavanaugh
 *
 */
public class ToggleSelectionModel extends DefaultListSelectionModel {

    private boolean adjusting = false;

    @Override
    public void setSelectionInterval(int index0, int index1) {
        if (adjusting) {
            return;
        } else if (isSelectedIndex(index1)) {
            super.removeSelectionInterval(index0, index1);
        } else {
            super.addSelectionInterval(index0, index1);
        }
        adjusting = getValueIsAdjusting();
    }

    @Override
    public void setValueIsAdjusting(boolean isAdjusting) {
        if (!isAdjusting) {
            adjusting = false;
        }
        super.setValueIsAdjusting(isAdjusting);
    }
}
