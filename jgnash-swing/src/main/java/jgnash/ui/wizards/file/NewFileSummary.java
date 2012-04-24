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
package jgnash.ui.wizards.file;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Color;
import java.util.Map;
import java.util.Set;

import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

import jgnash.engine.CurrencyNode;
import jgnash.ui.components.SortedListModel;
import jgnash.ui.components.wizard.WizardPage;
import jgnash.util.Resource;

/**
 * New file wizard panel.
 *
 * @author Craig Cavanaugh
 *
 */
public class NewFileSummary extends javax.swing.JPanel implements WizardPage {
    private final Resource rb = Resource.get();

    private JTextField fileField;

    private JTextField baseCurrencyField;

    private JList<CurrencyNode> currenciesList;

    public NewFileSummary() {
        layoutMainPanel();
    }

    private void initComponents() {
        fileField = new JTextField();
        fileField.setEditable(false);

        baseCurrencyField = new JTextField();
        baseCurrencyField.setEditable(false);

        currenciesList = new JList<>();
        currenciesList.setBackground((Color) UIManager.getDefaults().get("TextField.inactiveBackground"));
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p, 8dlu, d:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.appendSeparator(rb.getString("Title.Summary"));
        builder.append(rb.getString("Label.FileName"), fileField);
        builder.append(rb.getString("Label.DefaultCurrency"), baseCurrencyField);

        JScrollPane scrollPane = new JScrollPane(currenciesList);
        scrollPane.setBorder(new LineBorder((Color) UIManager.getDefaults().get("TextArea.inactiveForeground")));
        builder.append(rb.getString("Label.Currencies"), scrollPane);
    }

    @Override
    public boolean isPageValid() {
        return true;
    }

    @Override
    public String toString() {
        return "5. " + rb.getString("Title.Summary");
    }

    @Override
    @SuppressWarnings("unchecked")

    public void getSettings(Map<Enum<?>, Object> map) {
        fileField.setText((String) map.get(NewFileDialog.Settings.DATABASE_NAME));
        baseCurrencyField.setText(map.get(NewFileDialog.Settings.DEFAULT_CURRENCY).toString());

        SortedListModel<CurrencyNode> model = new SortedListModel<>((Set<CurrencyNode>) map.get(NewFileDialog.Settings.CURRENCIES));
        model.addElement(((CurrencyNode) (map.get(NewFileDialog.Settings.DEFAULT_CURRENCY))));
        currenciesList.setModel(model);
    }

    @Override
    public void putSettings(Map<Enum<?>, Object> map) {
    }
}
