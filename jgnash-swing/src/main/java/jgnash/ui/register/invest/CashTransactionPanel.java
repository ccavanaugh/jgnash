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
package jgnash.ui.register.invest;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.JPanel;

import jgnash.engine.Account;
import jgnash.ui.register.PanelType;
import jgnash.ui.register.TransactionPanel;

/**
 * Cash Transaction panel for the investment account register
 *
 * @author Craig Cavanaugh
 * @version $Id: CashTransactionPanel.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public final class CashTransactionPanel extends TransactionPanel {

    CashTransactionPanel(final Account account, final PanelType panelType) {
        super(account, panelType);
        setBorder(Borders.EMPTY_BORDER);    // investment panels do not have a border
    }

    @Override
    protected JPanel createBottomPanel() {
        FormLayout layout = new FormLayout("left:m:g", "f:d:g");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(reconciledButton);

        return builder.getPanel();
    }
}
