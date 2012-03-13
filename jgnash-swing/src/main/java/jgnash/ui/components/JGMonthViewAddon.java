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
package jgnash.ui.components;

import org.jdesktop.swingx.plaf.AbstractComponentAddon;
import org.jdesktop.swingx.plaf.LookAndFeelAddons;
import org.jdesktop.swingx.plaf.DefaultsList;

/**
 * Extention to the JXMonthView component, so that date from the calendar
 * can be selected by double click.
 * 
 * @author Peter Vida
 * @version $Id: JGMonthViewAddon.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
class JGMonthViewAddon extends AbstractComponentAddon {
    public JGMonthViewAddon() {
        super("JGJXMonthView");
    }
    @Override
    protected void addBasicDefaults(final LookAndFeelAddons addon, final DefaultsList defaults) {
        super.addBasicDefaults(addon, defaults);

        defaults.add(JGJXMonthView.jgUIClassID, "jgnash.ui.components.JGMonthViewUI");
    }
}
