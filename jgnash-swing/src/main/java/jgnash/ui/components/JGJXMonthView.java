/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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

import org.jdesktop.swingx.JXMonthView;
import org.jdesktop.swingx.plaf.LookAndFeelAddons;

/**
 * Extension to the JXMonthView component, so that date from the calendar can be selected by double click.
 * 
 * @author Peter Vida
 *
 */
class JGJXMonthView extends JXMonthView {

    public static final String DATE_ACCEPTED = "dateAccepted";

    public static final String jgUIClassID = "JGMonthViewUI";

    static {
        LookAndFeelAddons.contribute(new JGMonthViewAddon());
    }

    public JGJXMonthView() {
        super();
    }

    public void dateAccepted() {
        fireActionPerformed(DATE_ACCEPTED);
    }

    @Override
    public void updateUI() {
        setUI((JGMonthViewUI) LookAndFeelAddons.getUI(this, JGMonthViewUI.class));
        invalidate();
    }

    @Override
    public String getUIClassID() {
        return jgUIClassID;
    }
}
