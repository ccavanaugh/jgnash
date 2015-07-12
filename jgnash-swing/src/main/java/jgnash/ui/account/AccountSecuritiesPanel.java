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
package jgnash.ui.account;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.SecurityNode;
import jgnash.ui.components.SortedListModel;
import jgnash.ui.util.IconUtils;
import jgnash.util.NotNull;
import jgnash.util.Resource;

import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Craig Cavanaugh
 */
class AccountSecuritiesPanel extends JPanel implements ActionListener {

    private final transient Resource rb = Resource.get();

    private final transient Engine engine;

    private JButton addButton;

    private JButton removeButton;

    private JList<SecurityElement> availJList;

    private JList<SecurityElement> selectedJList;

    private SortedListModel<SecurityElement> availModel;

    private SortedListModel<SecurityElement> selectedModel;

    private final Account account;

    AccountSecuritiesPanel(final Account account) {
        engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        this.account = account;
        layoutMainPanel();
    }

    private void initComponents() {
        addButton = new JButton(rb.getString("Button.Add"), IconUtils.getIcon("/jgnash/resource/list-add.png"));
        addButton.setHorizontalTextPosition(SwingConstants.LEADING);

        removeButton = new JButton(rb.getString("Button.Remove"), IconUtils.getIcon("/jgnash/resource/list-remove.png"));

        availJList = new JList<>();
        selectedJList = new JList<>();

        selectedJList.setCellRenderer(new SecurityRenderer(selectedJList.getCellRenderer()));

        addButton.addActionListener(this);
        removeButton.addActionListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("75dlu:g(0.5), 8dlu, p, 8dlu, 75dlu:g(0.5)", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.appendTitle(rb.getString("Title.Available"));
        builder.append("");
        builder.appendTitle(rb.getString("Title.Current"));

        builder.appendRow("f:p:g(1.0)");

        builder.append(new JScrollPane(availJList));
        builder.append(layoutButtonStack());
        builder.append(new JScrollPane(selectedJList));
    }

    private JPanel layoutButtonStack() {
        return new ButtonStackBuilder().addButton(addButton, removeButton).build();
    }

    private void addAction() {
        SecurityElement obj = availJList.getSelectedValue();
        if (obj != null) {
            availModel.removeElement(obj);
            selectedModel.addElement(obj);
        }
    }

    private void removeAction() {
        SecurityElement obj = selectedJList.getSelectedValue();
        if (obj != null && obj.enabled) {
            selectedModel.removeElement(obj);
            availModel.addElement(obj);
        }
    }

    public void setSecuritiesList(final Set<SecurityNode> list) {

        selectedModel = new SortedListModel<>();

        if (account != null) {
            Set<SecurityNode> used = account.getUsedSecurities();

            for (SecurityNode node : list) {
                if (used.contains(node)) {
                    selectedModel.addElement(new SecurityElement(node, false));
                } else {
                    selectedModel.addElement(new SecurityElement(node, true));
                }
            }
        } else {
            for (SecurityNode node : list) {
                selectedModel.addElement(new SecurityElement(node, true));
            }
        }

        selectedJList.setModel(selectedModel);
        buildAvailableList();
    }

    public Set<SecurityNode> getSecuritiesList() {

        return selectedModel.asList().stream()
                .map(SecurityElement::getNode).collect(Collectors.toCollection(TreeSet::new));
    }

    private void buildAvailableList() {

        List<SecurityElement> list = engine.getSecurities().stream()
                .map(node -> new SecurityElement(node, true)).collect(Collectors.toList());

        List<SecurityElement> tList = list.stream()
                .filter(node -> !selectedModel.contains(node)).collect(Collectors.toList());

        availModel = new SortedListModel<>(tList);
        availJList.setModel(availModel);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == addButton) {
            addAction();
        } else if (e.getSource() == removeButton) {
            removeAction();
        }
    }

    static final class SecurityElement implements Comparable<SecurityElement> {

        private final SecurityNode node;

        private final boolean enabled;

        SecurityElement(final SecurityNode node, final boolean enabled) {
            this.node = node;
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public SecurityNode getNode() {
            return node;
        }

        @Override
        public String toString() {
            return node.toString();
        }

        @Override
        public int compareTo(@NotNull final SecurityElement securityElement) {
            return node.compareTo(securityElement.node);
        }

        @Override
        public int hashCode() {
            return node.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SecurityElement other = (SecurityElement) obj;
            return !(this.node != other.node && (this.node == null || !this.node.equals(other.node)));
        }
    }

    private static final class SecurityRenderer implements ListCellRenderer<SecurityElement> {

        private final ListCellRenderer<? super SecurityElement> delegate;

        public SecurityRenderer(final ListCellRenderer<? super SecurityElement> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getListCellRendererComponent(final JList<? extends SecurityElement> list, final SecurityElement value, final int index, final boolean isSelected, final boolean hasFocus) {
            boolean enabled = value.isEnabled();

            Component c = delegate.getListCellRendererComponent(list, value, index, isSelected, hasFocus);

            c.setEnabled(enabled);
            return c;
        }
    }
}
