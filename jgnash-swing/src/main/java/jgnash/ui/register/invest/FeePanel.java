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
package jgnash.ui.register.invest;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;

import jgnash.engine.Account;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionTag;
import jgnash.ui.ThemeManager;
import jgnash.ui.components.JFloatField;
import jgnash.ui.plaf.NimbusUtils;
import jgnash.util.Resource;

/**
 * UI Panel for handling investment transaction fees
 * <p/>
 * If feeSet.size() > 0, then a one or more specialized fees exist.  Otherwise, the fee is
 * simple and charged against the account adjusting the cash balance.
 *
 * @author Craig Cavanaugh
 *
 */
class FeePanel extends JPanel implements ActionListener {

    private JFloatField feeField;

    private JButton feeButton;

    private final Account account;

    private List<TransactionEntry> feeList = new ArrayList<>();

    FeePanel(final Account account) {
        this.account = account;

        layoutPanel();
    }

    void addActionListener(final ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    private void initComponents() {
        feeField = new JFloatField(account.getCurrencyNode());

        feeButton = new JButton(Resource.getIcon("/jgnash/resource/document-properties.png"));
        feeButton.setMargin(new Insets(0, 0, 0, 0));

        feeButton.addActionListener(this);
        feeButton.setFocusPainted(false);

        if (ThemeManager.isLookAndFeelNimbus()) {
            NimbusUtils.reduceNimbusButtonMargin(feeButton);
            feeButton.setIcon(NimbusUtils.scaleIcon(Resource.getIcon("/jgnash/resource/document-properties.png")));
        }
    }

    private void fireActionPerformed() {
        ActionEvent event = null;

        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                // Lazily create the event:
                if (event == null) {
                    event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null);
                }
                ((ActionListener) listeners[i + 1]).actionPerformed(event);
            }
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == feeButton) {
            FeesDialog d = FeesDialog.getFeesDialog(this, account, feeList);
            boolean status = d.showFeesDialog();

            if (status) {
                setTransactionEntries(d.getSplits());
                fireActionPerformed();
            }
        }
    }

    private void layoutPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p:g, 1px, min", "f:p");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);
        builder.append(feeField, feeButton);
    }

    public BigDecimal getDecimal() {
        return feeField.getDecimal();
    }

    public List<TransactionEntry> getTransactions() {

        // adjust the cash balance of the investment account
        if (feeList.isEmpty() && feeField.getDecimal().compareTo(BigDecimal.ZERO) != 0) {  // ignore zero balance fees
            TransactionEntry fee = new TransactionEntry(account, feeField.getDecimal().abs().negate());
            fee.setTransactionTag(TransactionTag.INVESTMENT_FEE);

            feeList.add(fee);
        }
        return feeList;
    }

    /**
     * Clones a <code>List</code> of <code>TransactionEntry(s)</code>
     *
     * @param fees <code>List</code> of fees to clone
     */
    public void setTransactionEntries(final List<TransactionEntry> fees) {
        feeList = new ArrayList<>();

        if (fees.size() == 1) {
            TransactionEntry e = fees.get(0);

            if (e.getCreditAccount().equals(e.getDebitAccount())) {
                feeField.setDecimal(e.getAmount(account).abs());
            } else {
                try {
                    feeList.add((TransactionEntry) e.clone()); // copy over the provided set's entry
                } catch (CloneNotSupportedException e1) {
                    Logger.getLogger(FeePanel.class.getName()).log(Level.SEVERE, e1.getLocalizedMessage(), e1);
                }
                feeField.setDecimal(sumFees().abs());
            }
        } else {
            for (TransactionEntry entry : fees) { // clone the provided set's entries
                try {
                    feeList.add((TransactionEntry) entry.clone());
                } catch (CloneNotSupportedException e) {
                    Logger.getLogger(FeePanel.class.getName()).log(Level.SEVERE, e.toString(), e);
                }
            }

            feeField.setDecimal(sumFees().abs());
        }

        feeField.setEditable(feeList.size() < 1);
    }

    private BigDecimal sumFees() {
        BigDecimal sum = BigDecimal.ZERO;

        for (TransactionEntry entry : feeList) {
            sum = sum.add(entry.getAmount(account));
        }

        return sum;
    }

    /**
     * Clear the form and remove all entries
     */
    void clearForm() {
        feeList = new ArrayList<>();
        feeField.setDecimal(null);
    }

    @Override
    public synchronized void addFocusListener(final FocusListener l) {
        feeField.addFocusListener(l);
    }

    @Override
    public synchronized void removeFocusListener(final FocusListener l) {
        feeField.removeFocusListener(l);
    }

    @Override
    public synchronized void addKeyListener(final KeyListener l) {
        feeField.addKeyListener(l);
    }

    @Override
    public synchronized void removeKeyListener(final KeyListener l) {
        feeField.removeKeyListener(l);
    }
}
