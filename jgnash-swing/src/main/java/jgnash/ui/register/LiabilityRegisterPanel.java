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
package jgnash.ui.register;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.util.Date;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;

import jgnash.engine.Account;
import jgnash.engine.AmortizeObject;
import jgnash.engine.CommodityNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.account.AmortizeDialog;

/**
 * Register panel for liability accounts.
 *
 * @author Craig Cavanaugh
 */
public class LiabilityRegisterPanel extends RegisterPanel {

    private JButton amortizeButton;

    private JButton paymentButton;

    private final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

    /**
     * Creates a new instance of LiabilityRegisterPanel
     *
     * @param account account to create panel for
     */
    LiabilityRegisterPanel(final Account account) {
        super(account);
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        paymentButton = new JButton(rb.getString("Button.NewPayment"));
        amortizeButton = new JButton(rb.getString("Button.Amortize"));

        amortizeButton.addActionListener(this);
        paymentButton.addActionListener(this);
    }

    /**
     * Overrides createButtonPanel in GenericRegisterPanel to add extra buttons
     */
    @Override
    protected JPanel createButtonPanel() {
        FormLayout layout = new FormLayout("d, 4dlu:g, d", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(StaticUIMethods.buildLeftAlignedBar(newButton, duplicateButton, jumpButton, deleteButton));
        builder.append(StaticUIMethods.buildRightAlignedBar(paymentButton, amortizeButton));
        return builder.getPanel();
    }

    /**
     * Displays the Amortize dialog
     */
    private void amortizeAction() {
        AmortizeObject ao = account.getAmortizeObject();

        AmortizeDialog d = new AmortizeDialog(ao);
        d.setVisible(true);

        if (d.getResult()) {
            if (!engine.setAmortizeObject(account, d.getAmortizeObject())) {
                StaticUIMethods.displayError(rb.getString("Message.Error.AmortizationSave"));
            }
        }
    }

    /* creates the payment transaction relative to the debit account */
    private void paymentActionDebit() {
        AmortizeObject ao = account.getAmortizeObject();

        if (ao != null) {

            Transaction tran = null;

            DateChkNumberDialog d = new DateChkNumberDialog(ao.getBankAccount(), rb.getString("Title.NewTrans"));
            d.setVisible(true);

            if (!d.getResult()) {
                return;
            }

            BigDecimal balance = account.getBalance().abs();
            double payment = ao.getPayment();

            double interest;

            if (ao.getUseDailyRate()) {
                Date today = d.getDate();

                Date last;

                if (account.getTransactionCount() > 0) {
                    last = account.getTransactionAt(account.getTransactionCount() - 1).getDate();
                } else {
                    last = today;
                }

                interest = ao.getIPayment(balance, last, today); // get the interest portion

            } else {
                interest = ao.getIPayment(balance); // get the interest portion
            }
                       
            // get debit account
            Account bank = ao.getBankAccount();

            if (bank != null) {
                CommodityNode n = bank.getCurrencyNode();

                Transaction transaction = new Transaction();
                transaction.setDate(d.getDate());
                transaction.setNumber(d.getNumber());
                transaction.setPayee(ao.getPayee());
                
                // transaction is made relative to the debit/checking account              

                TransactionEntry e = new TransactionEntry();

                // this entry is the principal payment              
                e.setCreditAccount(account);
                e.setDebitAccount(bank);
                e.setAmount(n.round(payment - interest));
                e.setMemo(ao.getMemo());

                transaction.addTransactionEntry(e);
            
                // handle interest portion of the payment
                Account i = ao.getInterestAccount();
                if (i != null && interest != 0.0) {
                    e = new TransactionEntry();
                    e.setCreditAccount(i);
                    e.setDebitAccount(bank);
                    e.setAmount(n.round(interest));
                    e.setMemo(rb.getString("Word.Interest"));
                    transaction.addTransactionEntry(e);

                    //System.out.println(e.getAmount());
                }

                // a fee has been assigned
                if (ao.getFees().compareTo(BigDecimal.ZERO) != 0) {
                    Account f = ao.getFeesAccount();
                    if (f != null) {
                        e = new TransactionEntry();
                        e.setCreditAccount(f);
                        e.setDebitAccount(bank);
                        e.setAmount(ao.getFees());
                        e.setMemo(rb.getString("Word.Fees"));
                        transaction.addTransactionEntry(e);

                        //System.out.println(e.getAmount());
                    }
                }

                // the remainder of the balance should be loan principal
                tran = transaction;
            }

            if (tran != null) {// display the transaction in the register
                EditTransactionDialog dlg = new EditTransactionDialog(ao.getBankAccount(), PanelType.DECREASE);
                dlg.newTransaction(tran);
                dlg.setVisible(true);
            } else {
                Logger.getLogger(Engine.class.getName()).warning("Not enough information");
            }
        } else { // could not generate the transaction
            Logger.getLogger(Engine.class.getName()).warning("Please configure amortization");
        }
    }

    /**
     * Creates a payment transaction relative to the liability account
     */
    /*
    private void paymentActionLiability() {

    AmortizeObject ao = ((LiabilityAccount)account).getAmortizeObject();
    Transaction tran = null;

    if (ao != null) {
    DateChkNumberDialog d = new DateChkNumberDialog(null, engine.getAccount(ao.getInterestAccount()));
    d.show();

    if (!d.getResult()) {
    return;
    }

    BigDecimal balance = account.getBalance().abs();
    BigDecimal fees = ao.getFees();
    double payment = ao.getPayment();

    double interest;

    if (ao.getUseDailyRate()) {
    Date today = d.getDate();
    Date last = account.getTransactionAt(account.getTransactionCount() - 1).getDate();
    interest = ao.getIPayment(balance, last, today); // get the interest portion
    } else {
    interest = ao.getIPayment(balance); // get the interest portion
    }

    Account b = engine.getAccount(ao.getBankAccount());
    if (b != null) {
    CommodityNode n = b.getCommodityNode();
    SplitEntryTransaction e;

    SplitTransaction t = new SplitTransaction(b.getCommodityNode());
    t.setAccount(b);
    t.setMemo(ao.getMemo());
    t.setPayee(ao.getPayee());
    t.setNumber(d.getNumber());
    t.setDate(d.getDate());

    // this entry is the complete payment
    e = new SplitEntryTransaction(n);
    e.setCreditAccount(account);
    e.setDebitAccount(b);
    e.setAmount(n.round(payment));
    e.setMemo(ao.getMemo());
    t.addSplit(e);

    try {   // maintain transaction order (stretch time)
    Thread.sleep(2);
    } catch (Exception ie) {}

    // handle interest portion of the payment
    Account i = engine.getAccount(ao.getInterestAccount());
    if (i != null) {
    e = new SplitEntryTransaction(n);
    e.setCreditAccount(i);
    e.setDebitAccount(account);
    e.setAmount(n.round(interest));
    e.setMemo(rb.getString("Word.Interest"));
    t.addSplit(e);
    }

    try {   // maintain transaction order (stretch time)
    Thread.sleep(2);
    } catch (Exception ie) {}

    // a fee has been assigned
    if (ao.getFees().compareTo(new BigDecimal("0")) != 0) {
    Account f = engine.getAccount(ao.getFeesAccount());
    if (f != null) {
    e = new SplitEntryTransaction(n);
    e.setCreditAccount(f);
    e.setDebitAccount(account);
    e.setAmount(ao.getFees());
    e.setMemo(rb.getString("Word.Fees"));
    t.addSplit(e);
    }
    }

    // the total should be the debit to the checking account
    tran = t;
    }
    }

    if (tran != null) {// display the transaction in the register
    newTransaction(tran);
    } else {    // could not generate the transaction
    if (ao == null) {
    Logger.getLogger("jgnashEngine").warning("Please configure amortization");
    } else {
    Logger.getLogger("jgnashEngine").warning("Not enough information");
    }
    }
    }*/
    @Override
    public void actionPerformed(final ActionEvent e) {
        super.actionPerformed(e);

        if (e.getSource() == amortizeButton) {
            amortizeAction();
        } else if (e.getSource() == paymentButton) {
            paymentActionDebit();
        }
    }
}
