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
package jgnash.ui.wizards.file;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.text.StyledEditorKit;

import jgnash.engine.CurrencyNode;
import jgnash.engine.DefaultCurrencies;
import jgnash.ui.components.SortedComboBoxModel;
import jgnash.ui.components.wizard.WizardPage;
import jgnash.ui.util.TextResource;
import jgnash.util.Resource;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * New file wizard panel
 *
 * @author Craig Cavanaugh
 */
public class NewFileTwo extends JPanel implements WizardPage {

    private final Resource rb = Resource.get();

    // Do not use a CurrencyComboBox at this point or it will boot the engine
    private final JComboBox<CurrencyNode> currencyCombo = new JComboBox<>();

    private JEditorPane helpPane;

    public NewFileTwo() {
        layoutMainPanel();
    }

    private void initComponents() {
        helpPane = new JEditorPane();
        helpPane.setEditable(false);
        helpPane.setEditorKit(new StyledEditorKit());
        helpPane.setBackground(getBackground());
        helpPane.setText(TextResource.getString("NewFileTwo.txt"));
    }

    private void layoutMainPanel() {
        initComponents();
        FormLayout layout = new FormLayout("p, 8dlu, f:d:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.appendSeparator(rb.getString("Title.DefDefCurr"));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(helpPane, 3);

        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(rb.getString("Label.DefaultCurrency"), currencyCombo);
    }

    @Override
    public boolean isPageValid() {
        return currencyCombo.getSelectedItem() != null;
    }

    /**
     * toString must return a valid description for this page that will
     * appear in the task list of the WizardDialog
     *
     * @return Title of this page
     */
    @Override
    public String toString() {
        return "2. " + rb.getString("Title.DefDefCurr");
    }

    @Override
    @SuppressWarnings("unchecked")

    public void getSettings(final Map<Enum<?>, Object> map) {
        Set<CurrencyNode> currencies = (Set<CurrencyNode>) map.get(NewFileDialog.Settings.DEFAULT_CURRENCIES);

        currencyCombo.setModel(new SortedComboBoxModel<>(currencies));

        try {
            String symbol = DefaultCurrencies.getDefault().getSymbol();

            // set the default currency by matching the default locale
            for (CurrencyNode node : currencies) {
                if (symbol.equals(node.getSymbol())) {
                    currencyCombo.setSelectedItem(node);
                }
            }
        } catch (final IllegalArgumentException e) {
            Logger.getLogger(NewFileTwo.class.getName()).warning("Unable to construct the default currency from the default system Locale");
        }
    }

    @Override
    public void putSettings(final Map<Enum<?>, Object> map) {
        map.put(NewFileDialog.Settings.DEFAULT_CURRENCY, currencyCombo.getSelectedItem());
    }
}