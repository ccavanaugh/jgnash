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
package jgnash.ui.wizards.imports.qif;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.StyledEditorKit;

import jgnash.engine.Account;
import jgnash.engine.EngineFactory;
import jgnash.imports.qif.QifAccount;
import jgnash.imports.qif.QifTransaction;
import jgnash.imports.qif.QifUtils;
import jgnash.ui.components.AccountListComboBox;
import jgnash.ui.components.wizard.WizardPage;
import jgnash.ui.util.TextResource;
import jgnash.util.Resource;

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

    private JComboBox<String> dateFormatCombo;

    private final QifAccount qAcc;

    private final Preferences pref = Preferences.userNodeForPackage(PartialOne.class);

    private static final String LAST_ACCOUNT = "lastAccount";

    private static final String DATE_FORMAT = "dateFormat";

    private final Resource rb = Resource.get();

    public PartialOne(QifAccount qAcc) {
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
        String[] formats = {QifUtils.US_FORMAT, QifUtils.EU_FORMAT};
        dateFormatCombo = new JComboBox<>(formats);

        dateFormatCombo.addActionListener(this);
        dateFormatCombo.setSelectedIndex(pref.getInt(DATE_FORMAT, 0));

        accountCombo.addActionListener(this);

        /* Try a set the combobox to the last selected account */
        String lastAccount = pref.get(LAST_ACCOUNT, "");

        Account last = EngineFactory.getEngine(EngineFactory.DEFAULT).getAccountByUuid(lastAccount);

        if (last != null) {
            setAccount(last);
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
            pref.put(LAST_ACCOUNT, destinationAccount.getUuid());
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

    void setAccount(Account account) {
        destinationAccount = account;
        accountCombo.setSelectedAccount(destinationAccount);
    }

    String getDateFormat() {
        return (String) dateFormatCombo.getSelectedItem();
    }

    /**
     * Reparse the dates based on the date format in the combo
     */
    private void reparseDates() {
        String df = getDateFormat();
        int count = qAcc.numItems();

        for (int i = 0; i < count; i++) {
            QifTransaction qt = qAcc.get(i);
            qt.date = QifUtils.parseDate(qt.oDate, df);
        }

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
