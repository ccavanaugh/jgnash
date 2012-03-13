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
package jgnash.ui.budget;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

import java.util.List;

import javax.swing.JPanel;

/**
 * Column footer component for the budget table
 *
 * @author Craig Cavanaugh
 * @version $Id: BudgetColumnFooter.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
class BudgetColumnFooter extends JPanel{
    private static final long serialVersionUID = 1L;

    private List<BudgetPeriodPanel> panels;

    public BudgetColumnFooter(final List<BudgetPeriodPanel> panels) {
        this.panels = panels;

        layoutMainPanel();
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("d", "d, 1dlu");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.add(panels.get(0).getPeriodFooter(), CC.xy(1, 1));

        for (int i = 1; i < panels.size(); i++) {
            builder.appendColumn("d");

            builder.add(panels.get(i).getPeriodFooter(), CC.xy(i + 1, 1));
        }
    }
}
