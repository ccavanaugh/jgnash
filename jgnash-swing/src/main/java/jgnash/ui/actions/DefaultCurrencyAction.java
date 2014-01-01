/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.ui.UIApplication;
import jgnash.ui.components.CurrencyComboBox;
import jgnash.ui.util.builder.Action;
import jgnash.util.Resource;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * UI Action to open the new file dialog
 *
 * @author Craig Cavanaugh
 *
 */
@Action("currency-default-command")
public class DefaultCurrencyAction extends AbstractEnabledAction {

    @Override
    public void actionPerformed(final ActionEvent e) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                Resource rb = Resource.get();

                CurrencyComboBox combo = new CurrencyComboBox();

                Object[] options = {rb.getString("Button.Ok"), rb.getString("Button.Cancel")};
                int result = JOptionPane.showOptionDialog(UIApplication.getFrame(), combo, rb.getString("Title.SelDefCurr"), JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);

                if (result == JOptionPane.YES_OPTION) {

                    Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                    engine.setDefaultCurrency(combo.getSelectedNode());
                    JOptionPane.showMessageDialog(UIApplication.getFrame(), rb.getString("Message.CurrChange") + " " + engine.getDefaultCurrency().getSymbol());
                }
            }
        });
    }
}
