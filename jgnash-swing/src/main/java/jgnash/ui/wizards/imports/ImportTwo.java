/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
package jgnash.ui.wizards.imports;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.StyledEditorKit;

import jgnash.engine.Account;
import jgnash.convert.imports.BayesImportClassifier;
import jgnash.convert.imports.GenericImport;
import jgnash.convert.imports.ImportBank;
import jgnash.convert.imports.ImportTransaction;
import jgnash.ui.components.wizard.WizardPage;
import jgnash.ui.util.JTableUtils;
import jgnash.ui.util.TextResource;
import jgnash.util.Resource;

/**
 * Wizard Page for OFX import.
 * 
 * @author Craig Cavanaugh
 */
public class ImportTwo extends JPanel implements WizardPage, ActionListener {

    private final Resource rb = Resource.get();

    private JButton deleteButton;

    private JTextPane helpPane;

    private ImportTable table;

    public ImportTwo() {
        layoutMainPanel();
    }

    private void initComponents() {

        table = new ImportTable();

        deleteButton = new JButton(rb.getString("Button.Delete"));

        helpPane = new JTextPane();
        helpPane.setEditable(false);
        helpPane.setEditorKit(new StyledEditorKit());
        helpPane.setBackground(getBackground());
        helpPane.setText(TextResource.getString("ImportTwo.txt"));

        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent evt) {
                refreshInfo();
            }
        });

        deleteButton.addActionListener(this);

        JTableUtils.packGenericTable(table);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p, 8dlu, d:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.appendSeparator(rb.getString("Title.ModImportTrans"));
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
        return table.getTransactions() != null;
    }

    /**
     * toString must return a valid description for this page that will appear in the task list of the WizardDialog
     * 
     * @return page description
     */
    @Override
    public String toString() {
        return "2. " + rb.getString("Title.ModImportTrans");
    }

    /**
     * @param e action event
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == deleteButton) {
            table.deleteSelected();
        }
    }

    @Override
    public void getSettings(final Map<Enum<?>, Object> map) {
        ImportBank bank = (ImportBank) map.get(ImportDialog.Settings.BANK);

        if (bank != null) {
            List<ImportTransaction> list = bank.getTransactions();

            Account account = (Account) map.get(ImportDialog.Settings.ACCOUNT);

            // set to sane account assuming it's going to be a single entry
            for (ImportTransaction t : list) {
                t.account = account;
                t.setState(ImportTransaction.ImportState.NEW);
            }

            // match up any pre-existing transactions
            GenericImport.matchTransactions(list, account);

            // classify the transactions
            BayesImportClassifier.classifyTransactions(list, account);

            table.setTransactions(list);

            refreshInfo();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putSettings(final Map<Enum<?>, Object> map) {
        map.put(ImportDialog.Settings.TRANSACTIONS, table.getTransactions());
    }
}
