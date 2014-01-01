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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.text.StyledEditorKit;

import jgnash.engine.CurrencyNode;
import jgnash.ui.components.SortedListModel;
import jgnash.ui.components.wizard.WizardPage;
import jgnash.ui.util.TextResource;
import jgnash.util.Resource;

/**
 * New file wizard panel.
 *
 * @author Craig Cavanaugh
 *
 */
public class NewFileThree extends JPanel implements WizardPage, ActionListener {

    private final Resource rb = Resource.get();
    private JEditorPane helpPane;
    private JList<CurrencyNode> aJList;
    private JButton addButton;
    private JList<CurrencyNode> cJList;
    private JButton removeButton;
    private SortedListModel<CurrencyNode> aList;
    private SortedListModel<CurrencyNode> cList;

    public NewFileThree() {
        layoutMainPanel();
    }

    private void initComponents() {
        addButton = new JButton(rb.getString("Button.Add"));
        addButton.setIcon(Resource.getIcon("/jgnash/resource/list-add.png"));
        addButton.setHorizontalTextPosition(SwingConstants.LEADING);

        removeButton = new JButton(rb.getString("Button.Remove"));
        removeButton.setIcon(Resource.getIcon("/jgnash/resource/list-remove.png"));

        aJList = new JList<>();
        cJList = new JList<>();

        helpPane = new JEditorPane();
        helpPane.setEditable(false);
        helpPane.setEditorKit(new StyledEditorKit());
        helpPane.setBackground(getBackground());
        helpPane.setText(TextResource.getString("NewFileThree.txt"));

        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentHidden(ComponentEvent evt) {
                isPageValid();
            }
        });

        addButton.addActionListener(this);
        removeButton.addActionListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout(
                "min(65dlu;d):g(0.5), 8dlu, d, 8dlu, min(65dlu;d):g(0.5)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.appendSeparator(rb.getString("Title.SelAvailCurr"));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(helpPane, 5);

        builder.appendTitle(rb.getString("Title.Available"));
        builder.append("");
        builder.appendTitle(rb.getString("Title.Selected"));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.appendRow(RowSpec.decode("f:d:g"));
        builder.append(new JScrollPane(aJList), buildCenterPanel(),
                new JScrollPane(cJList));
    }

    private JPanel buildCenterPanel() {
        FormLayout layout = new FormLayout("d", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(addButton);
        builder.append(removeButton);
        return builder.getPanel();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addButton) {
            addAction();
        } else if (e.getSource() == removeButton) {
            removeAction();
        }
    }

    private void removeAction() {
        CurrencyNode obj = cJList.getSelectedValue();
        if (obj != null) {
            cList.removeElement(obj);
            aList.addElement(obj);
        }
    }

    private void addAction() {
        CurrencyNode obj = aJList.getSelectedValue();
        if (obj != null) {
            aList.removeElement(obj);
            cList.addElement(obj);
        }
    }

    /**
     * Checks page validity
     *
     * @return true if valid
     */
    @Override
    public boolean isPageValid() {
        return true;
    }

    /**
     * toString must return a valid description for this page that will appear
     * in the task list of the WizardDialog
     *
     * @return panel name
     */
    @Override
    public String toString() {
        return "3. " + rb.getString("Title.SelAvailCurr");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void getSettings(Map<Enum<?>, Object> map) {
        if (aList == null) { // only load model once, otherwise selections
            // are cleared

            Set<CurrencyNode> currencies = (Set<CurrencyNode>) map.get(NewFileDialog.Settings.DEFAULT_CURRENCIES);

            aList = new SortedListModel<>(currencies);
            cList = new SortedListModel<>();

            aJList.setModel(aList);
            cJList.setModel(cList);
        }
    }

    @Override
    public void putSettings(Map<Enum<?>, Object> map) {
        map.put(NewFileDialog.Settings.CURRENCIES, new TreeSet<>(
                cList.asList()));
    }
}