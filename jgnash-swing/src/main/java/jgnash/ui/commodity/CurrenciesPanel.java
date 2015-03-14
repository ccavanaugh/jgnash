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
package jgnash.ui.commodity;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import jgnash.engine.CurrencyNode;
import jgnash.engine.DefaultCurrencies;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.ui.components.GenericCloseDialog;
import jgnash.ui.components.SortedListModel;
import jgnash.ui.util.DialogUtils;
import jgnash.ui.util.ValidationFactory;
import jgnash.util.NotNull;
import jgnash.util.Resource;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Creates a panel for adding and removing currencies. A static method is provided for displaying the panel in a dialog.
 * 
 * @author Craig Cavanaugh
 */
public class CurrenciesPanel extends JPanel implements ActionListener {

    private final Resource rb = Resource.get();

    private SortedListModel<CurrencyNode> aList;

    private SortedListModel<CurrencyElement> cList;

    private JList<CurrencyElement> cJList;

    private JList<CurrencyNode> aJList;

    private JButton customButton;

    private JTextField customField;

    private JButton addButton;

    private JButton removeButton;

    private final Engine engine;

    public static void showDialog(final JFrame parent) {
        final Resource rb = Resource.get();

        EventQueue.invokeLater(() -> {
            GenericCloseDialog d = new GenericCloseDialog(parent, new CurrenciesPanel(), rb.getString("Title.AddRemCurr"));
            d.pack();
            d.setMinimumSize(d.getSize());
            DialogUtils.addBoundsListener(d);
            d.setVisible(true);
        });
    }

    private CurrenciesPanel() {
        engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        layoutMainPanel();
        addButton.addActionListener(this);
        removeButton.addActionListener(this);
        customButton.addActionListener(this);
    }

    private void initComponents() {
        addButton = new JButton(rb.getString("Button.Add"));
        addButton.setIcon(Resource.getIcon("/jgnash/resource/list-add.png"));
        addButton.setHorizontalTextPosition(SwingConstants.LEADING);

        removeButton = new JButton(rb.getString("Button.Remove"));
        removeButton.setIcon(Resource.getIcon("/jgnash/resource/list-remove.png"));

        customButton = new JButton(rb.getString("Button.Add"));
        customField = new JTextField();

        buildLists(); // generate the Jlists
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("d:g(0.5), 8dlu, p, 8dlu, d:g(0.5)", "");
        layout.addGroupedColumn(1);
        layout.addGroupedColumn(5);
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.appendTitle(rb.getString("Title.Available"));
        builder.append("");
        builder.appendTitle(rb.getString("Title.Current"));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.appendRow("fill:80dlu:g");
        builder.append(new JScrollPane(aJList), buildCenterPanel(), new JScrollPane(cJList));
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(layoutCustomPanel(), 5);
        builder.appendSeparator();
    }

    private JPanel layoutCustomPanel() {
        FormLayout layout = new FormLayout("p, 8dlu, 55dlu, 8dlu, max(30dlu;p)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.append(rb.getString("Label.CreateCurr"), ValidationFactory.wrap(customField), customButton);

        return builder.getPanel();
    }

    private JPanel buildCenterPanel() {
        FormLayout layout = new FormLayout("p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(addButton);
        builder.append(removeButton);
        return builder.getPanel();
    }

    private void customAction() {
        if (!customField.getText().isEmpty()) {

            if (engine.getCurrency(customField.getText()) != null) {
                ValidationFactory.showValidationError(rb.getString("Message.Error.Duplicate"), customField);
            } else {
                CurrencyNode node = DefaultCurrencies.buildCustomNode(customField.getText());

                // the add could fail if the commodity symbol is a duplicate
                if (engine.addCurrency(node)) {
                    cList.addElement(new CurrencyElement(node, true));
                    customField.setText(null);
                    return;
                }
            }
        }
        ValidationFactory.showValidationError(rb.getString("Message.Error.MissingSymbol"), customField);
    }

    private void addAction() {
        for (CurrencyNode obj : aJList.getSelectedValuesList()) {
            if (obj != null) {
                aList.removeElement(obj);
                cList.addElement(new CurrencyElement(obj, true));
                engine.addCurrency(obj);
            }
        }
    }

    private void removeAction() {
        for (CurrencyElement element : cJList.getSelectedValuesList()) {
            if (element.isEnabled()) {
                if (engine.removeCommodity(element.getNode())) {
                    cList.removeElement(element);
                    aList.addElement(element.getNode());
                }
            }
        }
    }

    private void buildLists() {
        Set<CurrencyNode> defaultNodes = DefaultCurrencies.generateCurrencies();

        Set<CurrencyNode> activeNodes = engine.getActiveCurrencies();

        List<CurrencyNode> availNodes = engine.getCurrencies();

        availNodes.forEach(defaultNodes::remove);

        aList = new SortedListModel<>(defaultNodes);
        aJList = new JList<>(aList);

        ArrayList<CurrencyElement> list = new ArrayList<>();

        for (CurrencyNode node : availNodes) {
            if (activeNodes.contains(node)) {
                list.add(new CurrencyElement(node, false));
            } else {
                list.add(new CurrencyElement(node, true));
            }
        }

        cList = new SortedListModel<>(list);
        cJList = new JList<>(cList);
        cJList.setCellRenderer(new CurrencyRenderer(cJList.getCellRenderer()));
    }

    /**
     * Invoked when an action occurs
     * 
     * @param e action event
     */

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addButton) {
            addAction();
        } else if (e.getSource() == removeButton) {
            removeAction();
        } else if (e.getSource() == customButton) {
            customAction();
        }
    }

    static class CurrencyElement implements Comparable<CurrencyElement> {

        private final CurrencyNode node;

        private final boolean enabled;

        CurrencyElement(CurrencyNode node, boolean enabled) {
            this.node = node;
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public CurrencyNode getNode() {
            return node;
        }

        @Override
        public final String toString() {
            return node.toString();
        }

        @Override
        public int compareTo(final @NotNull CurrencyElement currencyElement) {
            return node.compareTo(currencyElement.node);
        }

        @Override
        public boolean equals(Object o) {
            boolean result = false;

            if (o instanceof CurrencyElement) {
                result = node.equals(((CurrencyElement) o).node);
            }

            return result;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 13 * hash + (node != null ? node.hashCode() : 0);
            return 13 * hash + (enabled ? 1 : 0);
        }
    }

    private final static class CurrencyRenderer implements ListCellRenderer<CurrencyElement> {

        private final ListCellRenderer<? super CurrencyElement> delegate;

        public CurrencyRenderer(final ListCellRenderer<? super CurrencyElement> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getListCellRendererComponent(final JList<? extends CurrencyElement> list, final CurrencyElement value, final int index, final boolean isSelected, final boolean hasFocus) {
            Component c = delegate.getListCellRendererComponent(list, value, index, isSelected, hasFocus);

            c.setEnabled(value.isEnabled());

            return c;
        }
    }
}
