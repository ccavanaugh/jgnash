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
package jgnash.ui.register.invest;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import jgnash.engine.Account;
import jgnash.engine.TransactionEntry;
import jgnash.ui.ThemeManager;
import jgnash.ui.components.JFloatField;
import jgnash.ui.plaf.NimbusUtils;
import jgnash.util.Resource;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * UI Panel for handling investment gains and loss.
 * <p/>
 * If gainsList.size() > 0, then a one or more specialized gains exist.  Otherwise, it's
 * assumed the user is not tracking income gains or loss
 *
 * @author Craig Cavanaugh
 * @version $Id: GainsPanel.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
class GainsPanel extends JPanel implements ActionListener {

    private JFloatField gainsField;

    private JButton gainsButton;

    private Account account;

    private List<TransactionEntry> gainsList = new ArrayList<>();

    GainsPanel(final Account account) {
        this.account = account;

        gainsField = new JFloatField(account.getCurrencyNode());
        gainsField.setEditable(false);

        gainsButton = new JButton(Resource.getIcon("/jgnash/resource/document-properties.png"));
        gainsButton.setMargin(new Insets(0, 0, 0, 0));

        gainsButton.addActionListener(this);
        gainsButton.setFocusPainted(false);

        if (ThemeManager.isLookAndFeelNimbus()) {
            NimbusUtils.reduceNimbusButtonMargin(gainsButton);
            gainsButton.setIcon(NimbusUtils.scaleIcon(Resource.getIcon("/jgnash/resource/document-properties.png")));
        }

        layoutPanel();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == gainsButton) {
            IncomeDialog d = IncomeDialog.getIncomeDialog(this, account, gainsList);
            boolean status = d.showIncomeDialog();

            if (status) {
                setTransactionEntries(d.getSplits());
                fireActionPerformed();
            }
        }
    }

    private void layoutPanel() {
        FormLayout layout = new FormLayout("55dlu:g, 1px, min", "f:p");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);
        builder.append(gainsField, gainsButton);
    }

    public List<TransactionEntry> getTransactions() {
        return gainsList;
    }

    /**
     * Clones a <code>List</code> of <code>TransactionEntry(s)</code>
     *
     * @param gains <code>List</code> of gains to clone
     */
    public void setTransactionEntries(final List<TransactionEntry> gains) {
        gainsList = new ArrayList<>();

        for (TransactionEntry entry : gains) { // clone the provided set's entries
            try {
                gainsList.add((TransactionEntry) entry.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        if (sumGains().compareTo(BigDecimal.ZERO) != 0) {
            gainsField.setDecimal(sumGains());
        } else {
            gainsField.setDecimal(null);
        }
    }

    private BigDecimal sumGains() {
        BigDecimal sum = BigDecimal.ZERO;

        for (TransactionEntry entry : gainsList) {
            sum = sum.add(entry.getAmount(account));
        }

        return sum;
    }

    /**
     * Clear the form and remove all entries
     */
    void clearForm() {
        gainsList = new ArrayList<>();
        gainsField.setDecimal(null);
    }

    void addActionListener(final ActionListener l) {
        listenerList.add(ActionListener.class, l);
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
}
