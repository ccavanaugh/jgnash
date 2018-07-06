/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.prefs.Preferences;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.StyledEditorKit;

import jgnash.convert.imports.DateFormat;
import jgnash.convert.imports.qif.QifAccount;
import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.ui.components.AccountListComboBox;
import jgnash.ui.components.wizard.WizardPage;
import jgnash.util.ResourceUtils;
import jgnash.util.TextResource;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * First part of import wizard for import of QIF files from online sources.
 *
 * @author Craig Cavanaugh
 *
 */
public class PartialOne extends JPanel implements WizardPage, ActionListener {
    private Account destinationAccount;

    private AccountListComboBox accountCombo;

    private JTextPane helpPane;

    private JComboBox<DateFormat> dateFormatCombo;

    private final QifAccount qAcc;

    private final Preferences pref = Preferences.userNodeForPackage(PartialOne.class);

    private static final String LAST_ACCOUNT = "lastAccount";

    private static final String DATE_FORMAT = "dateFormat";

    private final ResourceBundle rb = ResourceUtils.getBundle();

    PartialOne(final QifAccount qAcc) {
        this.qAcc = qAcc;
        layoutMainPanel();
    }

    private void initComponents() {
        accountCombo = new AccountListComboBox();

        helpPane = new JTextPane();
        helpPane.setEditable(false);
        helpPane.setEditorKit(new StyledEditorKit());
        helpPane.setBackground(getBackground());
        helpPane.setText(TextResource.getString("QifOne.txt"));

        /* Create the combo for date format selection */
        dateFormatCombo = new JComboBox<>(DateFormat.values());

        dateFormatCombo.addActionListener(this);
        dateFormatCombo.setSelectedIndex(pref.getInt(DATE_FORMAT, 0));

        accountCombo.addActionListener(this);

        /* Try a set the comboBox to the last selected account */
        String lastAccount = pref.get(LAST_ACCOUNT, "");

        if (!lastAccount.isEmpty()) {

            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            final Account last = engine.getAccountByUuid(UUID.fromString(lastAccount));

            if (last != null) {
                setAccount(last);
            }
        }
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
        builder.nextLine();
        builder.append(rb.getString("Label.DateFormat"), dateFormatCombo);
    }

    private void accountAction() {
        destinationAccount = accountCombo.getSelectedAccount();

        /* Save the id of the selected account */
        if (destinationAccount != null) {
            pref.put(LAST_ACCOUNT, destinationAccount.getUuid().toString());
            pref.putInt(DATE_FORMAT, dateFormatCombo.getSelectedIndex());
        }
    }

    @Override
    public boolean isPageValid() {
        // an account was selected, make sure that the destination is valid
        if (destinationAccount == null) {
            destinationAccount = accountCombo.getSelectedAccount();
        }
        return true;
    }

    public Account getAccount() {
        if (destinationAccount == null) {
            destinationAccount = accountCombo.getSelectedAccount();
        }
        return destinationAccount;
    }

    private void setAccount(final Account account) {
        destinationAccount = account;
        accountCombo.setSelectedAccount(destinationAccount);
    }

    private DateFormat getDateFormat() {
        return (DateFormat) dateFormatCombo.getSelectedItem();
    }

    /**
     * Reparse the dates based on the date format in the combo
     */
    private void reparseDates() {
        qAcc.reparseDates(getDateFormat());
        System.out.println("reparse");
    }

    /**
     * toString must return a valid description for this page that will appear
     * in the task list of the WizardDialog
     */
    @Override
    public String toString() {
        return "1. " + rb.getString("Title.SelDestAccount");
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == accountCombo) {
            accountAction();
        } else if (e.getSource() == dateFormatCombo) {
            reparseDates();
        }
    }

    @Override
    public void getSettings(Map<Enum<?>, Object> map) {

    }

    @Override
    public void putSettings(Map<Enum<?>, Object> map) {

    }
}
