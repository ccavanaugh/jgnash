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
package jgnash.ui.account;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;

import jgnash.engine.Account;
import jgnash.engine.AmortizeObject;
import jgnash.ui.components.DatePanel;
import jgnash.ui.components.JFloatField;
import jgnash.ui.components.JIntegerField;
import jgnash.ui.components.JTextFieldEx;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * Amortization setup dialog.
 *
 * @author Craig Cavanaugh
 * @version $Id: AmortizeDialog.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class AmortizeDialog extends JDialog implements ActionListener {
    private Resource rb = Resource.get();

    private JButton cancelButton;

    private JButton okButton;

    private JFloatField interestField;

    private JFloatField loanAmountField;

    private JIntegerField loanTermField;

    private JIntegerField payPeriodsField;

    private JIntegerField intPeriodsField;

    private JFloatField feesField;

    private JButton interestAccButton;

    private JButton bankAccButton;

    private JButton feesAccButton;

    private JTextFieldEx memoField;

    private JTextFieldEx payeeField;

    private DatePanel dateField;

    private JFloatField daysField;

    private JCheckBox useDaysButton;

    private Account bankAccount;

    private Account interestAccount;

    private Account feesAccount;

    private boolean result = false;

    private AmortizeObject ao;

    public AmortizeDialog(AmortizeObject o) {
        super((JFrame) null, true);
        setTitle(rb.getString("Title.AmorSetup"));
        setModal(true);

        layoutMainPanel();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        bankAccButton.addActionListener(this);
        cancelButton.addActionListener(this);
        feesAccButton.addActionListener(this);
        interestAccButton.addActionListener(this);
        okButton.addActionListener(this);
        useDaysButton.addActionListener(this);

        if (o != null) {
            ao = o;
        } else {
            ao = generateDefault();
        }
        fillForm();
        pack();

        setMinimumSize(getSize());

        DialogUtils.addBoundsListener(this);
    }

    private void initComponents() {
        dateField = new DatePanel();
        interestField = new JFloatField(0, 3, 2);
        loanAmountField = new JFloatField();
        loanTermField = new JIntegerField();
        payPeriodsField = new JIntegerField();
        intPeriodsField = new JIntegerField();
        feesField = new JFloatField();
        interestAccButton = new JButton(rb.getString("Word.None"));
        bankAccButton = new JButton(rb.getString("Word.None"));
        feesAccButton = new JButton(rb.getString("Word.None"));
        payeeField = new JTextFieldEx();
        memoField = new JTextFieldEx();
        useDaysButton = new JCheckBox(rb.getString("Button.UseDailyRate"));
        daysField = new JFloatField();
        cancelButton = new JButton(rb.getString("Button.Cancel"));
        okButton = new JButton(rb.getString("Button.Ok"));
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("right:max(40dlu;pref), $lcgap, max(75dlu;pref):grow(1.0)", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.setRowGroupingEnabled(true);

        builder.appendSeparator(rb.getString("Title.AmortizationSetup"));

        builder.append(rb.getString("Label.AnIntRate"), interestField);
        builder.nextLine();
        builder.append(rb.getString("Label.OrigLoanAmt"), loanAmountField);
        builder.nextLine();
        builder.append(rb.getString("Label.LoanTerm"), loanTermField);
        builder.nextLine();
        builder.append(rb.getString("Label.PayPerTerm"), payPeriodsField);
        builder.nextLine();
        builder.append(rb.getString("Label.CompPerTerm"), intPeriodsField);
        builder.nextLine();
        builder.append(rb.getString("Label.FirstPayDate"), dateField);
        builder.nextLine();
        builder.append(rb.getString("Label.EscrowPmi"), feesField);
        builder.nextLine();
        builder.setLeadingColumnOffset(2);
        builder.append(useDaysButton);
        builder.setLeadingColumnOffset(0);
        builder.nextLine();
        builder.append(rb.getString("Label.CompDaysPerYear"), daysField);

        builder.appendSeparator(rb.getString("Title.TransactionSetup"));

        builder.append(rb.getString("Label.InterestAccount"), interestAccButton);
        builder.nextLine();
        builder.append(rb.getString("Label.BankAccount"), bankAccButton);
        builder.nextLine();
        builder.append(rb.getString("Label.FeesAccount"), feesAccButton);
        builder.nextLine();
        builder.append(rb.getString("Label.Payee"), payeeField);
        builder.nextLine();
        builder.append(rb.getString("Label.Memo"), memoField);

        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();

        builder.append(ButtonBarFactory.buildOKCancelBar(okButton, cancelButton), 3);

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
    }

    /**
     * Returns true or false depending on if the user closed the dialog with
     * the OK button or Cancel button.
     *
     * @return the closing state of the dialog
     */
    public boolean getResult() {
        return result;
    }

    /**
     * Returns a new AmortizeObject created with the form data
     *
     * @return a new AmortizeObject
     */
    public AmortizeObject getAmortizeObject() {
        AmortizeObject o = new AmortizeObject();

        o.setDate(dateField.getDate());
        o.setFees(feesField.getDecimal());
        o.setInterestPeriods(intPeriodsField.intValue());
        o.setPaymentPeriods(payPeriodsField.intValue());
        o.setPrincipal(loanAmountField.getDecimal());
        o.setRate(interestField.getDecimal());
        o.setLength(loanTermField.intValue());
        o.setPayee(payeeField.getText());
        o.setMemo(memoField.getText());
        o.setUseDailyRate(useDaysButton.isSelected());
        o.setDaysPerYear(daysField.getDecimal());

        if (bankAccount != null) {
            o.setBankAccount(bankAccount);
        }

        if (interestAccount != null) {
            o.setInterestAccount(interestAccount);
        }

        if (feesAccount != null) {
            o.setFeesAccount(feesAccount);
        }
        return o;
    }

    private static AmortizeObject generateDefault() {
        AmortizeObject o = new AmortizeObject();

        // make up some reasonable numbers
        o.setLength(360);
        o.setPaymentPeriods(12);
        o.setRate(new BigDecimal("5.75"));
        o.setPrincipal(new BigDecimal("80000.00"));
        o.setUseDailyRate(false);
        o.setDaysPerYear(new BigDecimal("365"));

        // Defaults for US and CA are known... not sure about others
        if (Locale.getDefault().getCountry().equals("CA")) {
            o.setInterestPeriods(4);
        } else {
            o.setInterestPeriods(12);
        }
        return o;
    }

    private void fillForm() {
        interestField.setDecimal(ao.getRate());
        loanAmountField.setDecimal(ao.getPrincipal());
        loanTermField.setIntValue(ao.getLength());
        payPeriodsField.setIntValue(ao.getPaymentPeriods());
        intPeriodsField.setIntValue(ao.getInterestPeriods());
        feesField.setDecimal(ao.getFees());
        memoField.setText(ao.getMemo());
        payeeField.setText(ao.getPayee());
        dateField.setDate(ao.getDate());
        daysField.setDecimal(ao.getDaysPerYear());
        useDaysButton.setSelected(ao.getUseDailyRate());
        daysField.setEnabled(ao.getUseDailyRate());

        Account a = ao.getBankAccount();
        if (a != null) {
            bankAccount = a;
            bankAccButton.setText(a.getName());
        }

        a = ao.getInterestAccount();
        if (a != null) {
            interestAccount = a;
            interestAccButton.setText(a.getName());
        }

        a = ao.getFeesAccount();
        if (a != null) {
            feesAccount = a;
            feesAccButton.setText(a.getName());
        }
    }

    private Account showAccountListDialog(Account a) {
        AccountListDialog dlg = new AccountListDialog(a, true);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        if (dlg.getReturnStatus()) {
            return dlg.getAccount();
        }
        return null;
    }

    private void closeDialog() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == cancelButton) {
            closeDialog();
        } else if (e.getSource() == okButton) {
            result = true;
            closeDialog();
        } else if (e.getSource() == bankAccButton) {
            Account a = showAccountListDialog(bankAccount);
            if (a != null) {
                bankAccount = a;
                bankAccButton.setText(a.getName());
            } else {
                bankAccButton.setText(rb.getString("Word.None"));
            }
        } else if (e.getSource() == interestAccButton) {
            Account a = showAccountListDialog(interestAccount);
            if (a != null) {
                interestAccount = a;
                interestAccButton.setText(a.getName());
            } else {
                interestAccButton.setText(rb.getString("Word.None"));
            }
        } else if (e.getSource() == feesAccButton) {
            Account a = showAccountListDialog(feesAccount);
            if (a != null) {
                feesAccount = a;
                feesAccButton.setText(a.getName());
            } else {
                feesAccButton.setText(rb.getString("Word.None"));
            }
        } else if (e.getSource() == useDaysButton) {
            daysField.setEnabled(useDaysButton.isSelected());
        }
    }
}