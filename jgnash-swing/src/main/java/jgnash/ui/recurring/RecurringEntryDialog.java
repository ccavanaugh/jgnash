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
package jgnash.ui.recurring;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.engine.recurring.DailyReminder;
import jgnash.engine.recurring.MonthlyReminder;
import jgnash.engine.recurring.OneTimeReminder;
import jgnash.engine.recurring.Reminder;
import jgnash.engine.recurring.WeeklyReminder;
import jgnash.engine.recurring.YearlyReminder;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.components.AccountListComboBox;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.JDateField;
import jgnash.ui.components.JIntegerField;
import jgnash.ui.components.JTextFieldEx;
import jgnash.ui.register.TransactionDialog;
import jgnash.ui.util.DialogUtils;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog for entry for recurring transactions.
 *
 * @author Craig Cavanaugh
 */
public class RecurringEntryDialog extends JDialog implements ActionListener {

    private boolean result = false;
    private Reminder reminder = null;
    private DatePanel startDateField;
    private JTextField transactionField;
    private JTextField descriptionField;
    private JButton deleteButton;
    private JButton editButton;
    private JCheckBox enabledCheckBox;
    private JTabbedPane freqTab;
    private JCheckBox autoEnterCheckBox;
    private JIntegerField daysBeforeField;
    private JDateField lastOccurrenceField;
    private JButton cancelButton;
    private JButton okButton;
    private JTextArea notesArea;
    private AccountListComboBox accountCombo;
    private final ResourceBundle rb = ResourceUtils.getBundle();
    private final HashMap<Class<?>, Integer> tabMap = new HashMap<>();
    private Transaction transaction = null;

    /**
     * Creates new form RecurringFormDialog
     *
     * @param reminder reminder
     */
    private RecurringEntryDialog(final Reminder reminder) {
        super(UIApplication.getFrame(), true);

        if (reminder == null) {
            setTitle(rb.getString("Title.NewReminder"));
        } else {
            setTitle(rb.getString("Title.ModifyReminder"));
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        layoutMainPanel();

        // update the form with the selected reminder
        if (reminder != null) {
            displayReminder(reminder);
        }

        okButton.addActionListener(this);
        deleteButton.addActionListener(this);
        editButton.addActionListener(this);
        cancelButton.addActionListener(this);
        autoEnterCheckBox.addActionListener(this);
    }

    static Reminder showDialog() {
        return showDialog(null);
    }

    public static Reminder showDialog(final Reminder reminder) {
        RecurringEntryDialog d = new RecurringEntryDialog(reminder);
        d.setMinimumSize(d.getSize());
        DialogUtils.addBoundsListener(d, "dialogBounds");
        d.setVisible(true);

        if (d.result) {
            return d.reminder;
        }
        return null;
    }

    private JPanel createTransactionPanel() {
        FormLayout layout = new FormLayout("left:p, 4dlu, p:g, 4dlu, p", "f:p, 3dlu, f:p, 3dlu, f:p, 3dlu, f:40dlu:g");
        layout.setRowGroups(new int[][]{{1, 3, 5}});

        CellConstraints cc = new CellConstraints();

        JPanel p = new JPanel(layout);
        descriptionField = new JTextFieldEx();

        accountCombo = new AccountListComboBox();

        notesArea = new JTextArea(5, 20);
        notesArea.setLineWrap(true);
        notesArea.setAutoscrolls(true);

        JScrollPane pane = new JScrollPane(notesArea);
        pane.setAutoscrolls(true);

        transactionField = new JTextFieldEx();
        transactionField.setEditable(false);
        editButton = new JButton(rb.getString("Button.Edit"));

        deleteButton = new JButton(rb.getString("Button.Delete"));

        p.add(new JLabel(rb.getString("Label.Account")), cc.xy(1, 1));
        p.add(accountCombo, cc.xywh(3, 1, 3, 1));

        p.add(new JLabel(rb.getString("Label.Description")), cc.xy(1, 3));
        p.add(descriptionField, cc.xywh(3, 3, 3, 1));


        p.add(new JLabel(rb.getString("Label.Transaction")), cc.xy(1, 5));
        p.add(transactionField, cc.xy(3, 5));
        p.add(new ButtonBarBuilder().addButton(editButton, deleteButton).build(), cc.xy(5, 5));

        p.add(new JLabel(rb.getString("Label.Notes")), cc.xy(1, 7));
        p.add(pane, cc.xywh(3, 7, 3, 1));

        return p;
    }

    private JPanel createFreqPanel() {
        FormLayout layout = new FormLayout("right:p, 4dlu, max(48dlu;min), 6dlu, p, f:p:g", "f:p, 3dlu, min");

        CellConstraints cc = new CellConstraints();

        JPanel p = new JPanel(layout);
        startDateField = new DatePanel();
        enabledCheckBox = new JCheckBox(rb.getString("Button.Enabled"));

        freqTab = new JTabbedPane();

        freqTab.add(rb.getString("Tab.None"), new NoneTab());
        freqTab.add(rb.getString("Tab.Day"), new DayTab());
        freqTab.add(rb.getString("Tab.Week"), new WeekTab());
        freqTab.add(rb.getString("Tab.Month"), new MonthTab());
        freqTab.add(rb.getString("Tab.Year"), new YearTab());

        tabMap.put(OneTimeReminder.class, 0);
        tabMap.put(DailyReminder.class, 1);
        tabMap.put(WeeklyReminder.class, 2);
        tabMap.put(MonthlyReminder.class, 3);
        tabMap.put(YearlyReminder.class, 4);

        p.add(new JLabel(rb.getString("Label.FirstPayDate")), cc.xy(1, 1));
        p.add(startDateField, cc.xy(3, 1));
        p.add(enabledCheckBox, cc.xy(5, 1));
        p.add(freqTab, cc.xywh(1, 3, 6, 1));

        return p;
    }

    private JPanel createEntryPanel() {
        FormLayout layout = new FormLayout("right:p, 4dlu, 45dlu", "f:p, 3dlu, f:p");

        CellConstraints cc = new CellConstraints();

        JPanel p = new JPanel(layout);
        autoEnterCheckBox = new JCheckBox(rb.getString("Button.EnterDaysBefore"));
        daysBeforeField = new JIntegerField();
        lastOccurrenceField = new JDateField();

        p.add(autoEnterCheckBox, cc.xy(1, 1));
        p.add(daysBeforeField, cc.xy(3, 1));

        p.add(new JLabel(rb.getString("Label.LastOccurrence")), cc.xy(1, 3));
        p.add(lastOccurrenceField, cc.xy(3, 3));

        lastOccurrenceField.setValue(null); // clear the date
        lastOccurrenceField.setEditable(false);

        return p;
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("10dlu, p:g", "p, 3dlu, f:p:g, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 6dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();

        okButton = new JButton(rb.getString("Button.Ok"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));

        builder.border(Borders.DIALOG);
        builder.addSeparator(rb.getString("Title.Transaction"), cc.xyw(1, 1, 2));
        builder.add(createTransactionPanel(), cc.xy(2, 3));
        builder.addSeparator(rb.getString("Title.Frequency"), cc.xyw(1, 5, 2));
        builder.add(createFreqPanel(), cc.xy(2, 7));
        builder.addSeparator(rb.getString("Title.Entry"), cc.xyw(1, 9, 2));
        builder.add(createEntryPanel(), cc.xy(2, 11));
        builder.add(StaticUIMethods.buildOKCancelBar(okButton, cancelButton), cc.xy(2, 13));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        pack();
    }

    /**
     * Closes the dialog
     */
    private void closeDialog() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private Reminder generateReminder() {
        RecurringTab tab = (RecurringTab) freqTab.getSelectedComponent();

        Reminder r = tab.getReminder();

        r.setDescription(descriptionField.getText());
        r.setEnabled(enabledCheckBox.isSelected());
        r.setStartDate(startDateField.getLocalDate());
        r.setNotes(notesArea.getText());

        r.setAutoCreate(autoEnterCheckBox.isSelected());

        r.setDaysAdvance(daysBeforeField.intValue());
        r.setAccount(accountCombo.getSelectedAccount());
        r.setTransaction(transaction);

        return r;
    }

    private void displayReminder(final Reminder r) {
        Account a = r.getAccount();

        if (a != null) {
            accountCombo.setSelectedAccount(a);
        } else {
            System.err.println("did not find account");
        }

        transaction = r.getTransaction();
        if (transaction != null) {
            transactionField.setText(transaction.getPayee());
        }

        descriptionField.setText(r.getDescription());
        enabledCheckBox.setSelected(r.isEnabled());
        startDateField.setDate(r.getStartDate());
        notesArea.setText(r.getNotes());

        freqTab.setSelectedIndex(tabMap.get(r.getClass()));
        ((RecurringTab) freqTab.getSelectedComponent()).setReminder(r);

        lastOccurrenceField.setValue(r.getLastDate());

        autoEnterCheckBox.setSelected(r.isAutoCreate());

        int days = r.getDaysAdvance();
        daysBeforeField.setIntValue(days);

        daysBeforeField.setEnabled(autoEnterCheckBox.isSelected());
    }

    private void editTransactionAction() {
        Transaction t = TransactionDialog.showDialog(accountCombo.getSelectedAccount(), transaction);

        if (t != null) {
            transaction = t;
            transactionField.setText(transaction.getPayee());
        }
    }

    private void deleteTransaction() {
        transaction = null;
        transactionField.setText(null);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == cancelButton) {
            closeDialog();
        } else if (e.getSource() == okButton) {
            reminder = generateReminder();
            result = true;
            closeDialog();
        } else if (e.getSource() == autoEnterCheckBox) {
            daysBeforeField.setEnabled(autoEnterCheckBox.isSelected());
        } else if (e.getSource() == editButton) {
            editTransactionAction();
        } else if (e.getSource() == deleteButton) {
            deleteTransaction();
        }
    }
}
