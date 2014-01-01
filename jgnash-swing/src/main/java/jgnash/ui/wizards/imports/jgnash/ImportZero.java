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
package jgnash.ui.wizards.imports.jgnash;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.StyledEditorKit;

import jgnash.ui.actions.ImportPathAction;
import jgnash.ui.components.wizard.WizardPage;
import jgnash.ui.util.TextResource;
import jgnash.util.FileMagic;
import jgnash.util.Resource;

/**
 * New file wizard panel
 *
 * @author Craig Cavanaugh
 */
public class ImportZero extends JPanel implements WizardPage, ActionListener {

    private final Resource rb = Resource.get();

    private final JTextField fileImportField = new JTextField();

    private JButton fileImportButton;

    private JLabel validFileLabel;

    private JEditorPane helpPane;

    private boolean valid = false;

    public ImportZero() {
        layoutMainPanel();
    }

    private void initComponents() {
        helpPane = new JEditorPane();
        helpPane.setEditable(false);
        helpPane.setEditorKit(new StyledEditorKit());
        helpPane.setBackground(getBackground());
        helpPane.setText(TextResource.getString("ImportFileZero.txt"));

        fileImportButton = new JButton("...");
        fileImportButton.addActionListener(this);

        validFileLabel = new JLabel(rb.getString("Message.FileNotValid"));
        validFileLabel.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
        validFileLabel.setFont(validFileLabel.getFont().deriveFont(Font.ITALIC));

        fileImportField.addCaretListener(new CaretListener() {

            @Override
            public void caretUpdate(CaretEvent e) {
                validateFile();
            }
        });
    }

    private void layoutMainPanel() {
        initComponents();
        FormLayout layout = new FormLayout("p, 8dlu, f:d:g, 4dlu, p", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.appendSeparator(rb.getString("Title.FileImport"));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(helpPane, 3);
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(rb.getString("Label.FileName"), fileImportField, fileImportButton);
        builder.nextLine();
        builder.append(validFileLabel, 5);
    }

    @Override
    public boolean isPageValid() {
        return valid;
    }

    /**
     * toString must return a valid description for this page that will
     * appear in the task list of the WizardDialog
     *
     * @return task description
     */
    @Override
    public String toString() {
        return "1. " + rb.getString("Title.FileImport");
    }

    @Override
    public void getSettings(Map<Enum<?>, Object> map) {
        fileImportField.setText((String) map.get(ImportDialog.Settings.IMPORT_FILE));
        validateFile();
    }

    @Override
    public void putSettings(Map<Enum<?>, Object> map) {
        map.put(ImportDialog.Settings.IMPORT_FILE, fileImportField.getText());
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == fileImportButton) {
            databaseNameAction();
        }
    }

    private void databaseNameAction() {
        String result = ImportPathAction.databaseNameAction(this);

        if (!result.isEmpty()) {
            fileImportField.setText(result);
            validateFile();
        }
    }

    private void validateFile() {
        File file = new File(fileImportField.getText());

        if (file.exists()) {
            valid = FileMagic.isValidjGnash1File(file);
            validFileLabel.setVisible(!valid);

        } else {
            validFileLabel.setVisible(true);
            valid = false;
        }
    }
}