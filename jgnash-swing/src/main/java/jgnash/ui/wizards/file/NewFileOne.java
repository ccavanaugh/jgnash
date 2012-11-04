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

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import jgnash.engine.DataStoreType;
import jgnash.engine.EngineFactory;
import jgnash.ui.actions.DatabasePathAction;
import jgnash.ui.components.DataStoreTypeCombo;
import jgnash.ui.components.wizard.WizardPage;
import jgnash.ui.util.TextResource;
import jgnash.util.FileUtils;
import jgnash.util.Resource;

/**
 * New file wizard panel
 *
 * @author Craig Cavanaugh
 */
public class NewFileOne extends JPanel implements WizardPage, ActionListener {

    protected final Resource rb = Resource.get();

    private final JTextField dbNameField = new JTextField();

    private JButton dbNameButton;

    private JLabel overwriteLabel;

    private JEditorPane helpPane;

    private DataStoreTypeCombo typeCombo;

    public NewFileOne() {

        layoutMainPanel();
    }

    private void initComponents() {
        helpPane = new JEditorPane();
        helpPane.setEditable(false);
        helpPane.setEditorKit(new StyledEditorKit());
        helpPane.setBackground(getBackground());
        helpPane.setText(TextResource.getString("NewFileOne.txt"));
        dbNameButton = new JButton("...");
        dbNameButton.addActionListener(this);

        overwriteLabel = new JLabel(rb.getString("Message.Overwritedb"));
        overwriteLabel.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
        overwriteLabel.setFont(overwriteLabel.getFont().deriveFont(Font.ITALIC));

        typeCombo = new DataStoreTypeCombo();
        typeCombo.addActionListener(this);

        dbNameField.addCaretListener(new CaretListener() {

            @Override
            public void caretUpdate(CaretEvent e) {
                checkForOverwrite();
            }
        });

        typeCombo.setSelectedItem(DataStoreType.BINARY_XSTREAM);
    }

    private void layoutMainPanel() {
        initComponents();
        FormLayout layout = new FormLayout("p, 8dlu, f:d:g, 4dlu, p", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.appendSeparator(rb.getString("Title.DatabaseCfg"));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(helpPane, 3);
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(rb.getString("Label.StorageType"), typeCombo);
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(rb.getString("Label.DatabaseName"), dbNameField, dbNameButton);
        builder.nextLine();
        builder.append(overwriteLabel, 5);
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
    }

    @Override
    public boolean isPageValid() {
        return dbNameField.getText().length() > 0;
    }

    /**
     * toString must return a valid description for this page that will
     * appear in the task list of the WizardDialog
     *
     * @return page description
     */
    @Override
    public String toString() {
        return "1. " + rb.getString("Title.DatabaseCfg");
    }

    @Override
    public void getSettings(Map<Enum<?>, Object> map) {
        DataStoreType type = (DataStoreType) map.get(NewFileDialog.Settings.TYPE);

        if (type != null) {
            typeCombo.setSelectedItem(type);
        }

        String fileName = (String) map.get(NewFileDialog.Settings.DATABASE_NAME);

        if (FileUtils.fileHasExtension(fileName)) {
            dbNameField.setText(fileName);
        } else {
            dbNameField.setText(fileName + "." + typeCombo.getSelectedDataStoreType().getDataStore().getFileExt());
        }

        checkForOverwrite();
    }

    @Override
    public void putSettings(Map<Enum<?>, Object> map) {
        map.put(NewFileDialog.Settings.DATABASE_NAME, dbNameField.getText());
        map.put(NewFileDialog.Settings.TYPE, typeCombo.getSelectedDataStoreType());
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == dbNameButton) {
            databaseNameAction();
        } else if (event.getSource() == typeCombo) {
            updateFileExtensionAction();
        }
    }

    private void updateFileExtensionAction() {
        if (dbNameField.getText().length() > 0) {

            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    String fileName = FileUtils.stripFileExtension(dbNameField.getText());
                    dbNameField.setText(fileName + "." + typeCombo.getSelectedDataStoreType().getDataStore().getFileExt());
                }
            });
        }
    }

    private void databaseNameAction() {
        String result = DatabasePathAction.databaseNameAction(this, DatabasePathAction.Type.NEW, typeCombo.getSelectedDataStoreType());

        if (result.length() > 0) {
            dbNameField.setText(result);
            checkForOverwrite();
        }
    }

    private void checkForOverwrite() {
        String database = dbNameField.getText();
        overwriteLabel.setVisible(EngineFactory.doesDatabaseExist(database, typeCombo.getSelectedDataStoreType()));
    }
}