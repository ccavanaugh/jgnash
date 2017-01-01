/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.StyledEditorKit;

import jgnash.engine.Account;
import jgnash.ui.components.AccountListComboBox;
import jgnash.ui.components.wizard.WizardPage;
import jgnash.util.ResourceUtils;
import jgnash.util.TextResource;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * First part of import wizard for import of OFX or Mt940 files from online
 * sources.
 *
 * @author Craig Cavanaugh
 *
 */
public class ImportOne extends JPanel implements WizardPage {

    private AccountListComboBox accountCombo;

    private JTextPane helpPane;

    private final ResourceBundle rb = ResourceUtils.getBundle();

    ImportOne() {
        layoutMainPanel();
    }

    private void initComponents() {
        accountCombo = new AccountListComboBox();

        helpPane = new JTextPane();
        helpPane.setEditable(false);
        helpPane.setEditorKit(new StyledEditorKit());
        helpPane.setBackground(getBackground());
        helpPane.setText(TextResource.getString("ImportOne.txt"));
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p, 8dlu, 85dlu:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.appendSeparator(rb.getString("Title.SelDestAccount"));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.appendRow(RowSpec.decode("p"));
        builder.append(helpPane, 3);

        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(rb.getString("Label.DestAccount"), accountCombo);
    }

    @Override
    public boolean isPageValid() {
        return accountCombo.getSelectedAccount() != null;
    }

    /**
     * toString must return a valid description for this page that will appear
     * in the task list of the WizardDialog
     *
     * @return page description
     */
    @Override
    public String toString() {
        return "1. " + rb.getString("Title.SelDestAccount");
    }

    @Override
    public void getSettings(final Map<Enum<?>, Object> map) {
        Account a = (Account) map.get(ImportDialog.Settings.ACCOUNT);

        if (a != null) {
            accountCombo.setSelectedAccount(a);
        }
    }

    @Override
    public void putSettings(final Map<Enum<?>, Object> map) {
        map.put(ImportDialog.Settings.ACCOUNT, accountCombo.getSelectedAccount());
    }
}
