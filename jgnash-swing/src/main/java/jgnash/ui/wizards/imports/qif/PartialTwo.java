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
package jgnash.ui.wizards.imports.qif;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.StyledEditorKit;

import jgnash.convert.imports.qif.QifAccount;
import jgnash.ui.components.wizard.WizardPage;
import jgnash.util.Resource;
import jgnash.util.TextResource;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * Wizard Page for a partial qif import.
 *
 * @author Craig Cavanaugh
 *
 */
public class PartialTwo extends JPanel implements WizardPage, ActionListener {
    private final Resource rb = Resource.get();

    private JButton deleteButton;

    private JTextPane helpPane;

    private PartialTable table;

    private final QifAccount qAcc;

    public PartialTwo(QifAccount qAcc) {
        this.qAcc = qAcc;
        layoutMainPanel();
    }

    private void initComponents() {
        table = new PartialTable(qAcc);

        deleteButton = new JButton(rb.getString("Button.Delete"));

        helpPane = new JTextPane();
        helpPane.setEditable(false);
        helpPane.setEditorKit(new StyledEditorKit());
        helpPane.setBackground(getBackground());
        helpPane.setText(TextResource.getString("QifTwo.txt"));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent evt) {
                refreshInfo();
            }
        });

        deleteButton.addActionListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p, 8dlu, d:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.appendSeparator(rb.getString("Title.ModQIFTrans"));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.appendRow(RowSpec.decode("p"));
        builder.append(helpPane, 3);

        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();

        builder.appendRow(RowSpec.decode("f:85dlu:g"));
        builder.append(new JScrollPane(table), 3);

        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();

        builder.append(deleteButton);
    }

    void refreshInfo() {
        table.fireTableDataChanged();
    }

    @Override
    public boolean isPageValid() {
        return true;
    }

    /**
     * toString must return a valid description for this page that will appear
     * in the task list of the WizardDialog
     */
    @Override
    public String toString() {
        return "2. " + rb.getString("Title.ModQIFTrans");
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == deleteButton) {
            table.deleteSelected();
        }
    }

    @Override
    public void getSettings(Map<Enum<?>, Object> map) {

    }

    @Override
    public void putSettings(Map<Enum<?>, Object> map) {

    }
}
