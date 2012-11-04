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

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;

import jgnash.engine.AbstractInvestmentTransactionEntry;
import jgnash.engine.Account;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionEntryReinvestDivX;
import jgnash.engine.TransactionFactory;
import jgnash.ui.util.ValidationFactory;

/**
 * Form for buying and selling shares.
 *
 * @author Craig Cavanaugh
 *
 */
public final class ReinvestDividendPanel extends AbstractPriceQtyInvTransactionPanel {

    private final FeePanel feePanel;

    private final GainsPanel gainsPanel;

    protected ReinvestDividendPanel(Account account) {
        super(account);


        feePanel = new FeePanel(account);
        gainsPanel = new GainsPanel(account);


        layoutMainPanel();

        FocusListener focusListener = new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent evt) {
                updateTotalField();
            }
        };

        feePanel.addFocusListener(focusListener);
        quantityField.addFocusListener(focusListener);
        priceField.addFocusListener(focusListener);

        datePanel.getDateField().addKeyListener(keyListener);
        feePanel.addKeyListener(keyListener);
        memoField.addKeyListener(keyListener);
        priceField.addKeyListener(keyListener);
        quantityField.addKeyListener(keyListener);
        securityCombo.addKeyListener(keyListener);
        reconciledButton.addKeyListener(keyListener);

        clearForm();
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("right:d, $lcgap, 50dlu:g, 8dlu, right:d, $lcgap, max(65dlu;min)", "f:d, $nlgap, f:d, $nlgap, f:d, $nlgap, f:d");

        layout.setRowGroups(new int[][]{{1, 3, 5, 7}});
        CellConstraints cc = new CellConstraints();

        setLayout(layout);

        /* Create a sub panel to work around a column spanning problem in FormLayout */
        JPanel subPanel = buildHorizontalSubPanel("max(48dlu;min):g(0.5), 8dlu, d, $lcgap, max(48dlu;min):g(0.5), 8dlu, d, 4dlu, max(48dlu;min)", ValidationFactory.wrap(priceField), "Label.Quantity", ValidationFactory.wrap(quantityField),
                "Label.Gains", gainsPanel);

        add("Label.Security", cc.xy(1, 1));
        add(ValidationFactory.wrap(securityCombo), cc.xy(3, 1));
        add("Label.Date", cc.xy(5, 1));
        add(datePanel, cc.xy(7, 1));

        add("Label.Price", cc.xy(1, 3));
        add(subPanel, cc.xy(3, 3));
        add("Label.Fees", cc.xy(5, 3));
        add(feePanel, cc.xy(7, 3));

        add("Label.Memo", cc.xy(1, 5));
        add(memoField, cc.xy(3, 5));
        add("Label.Total", cc.xy(5, 5));
        add(totalField, cc.xy(7, 5));

        add(reconciledButton, cc.xyw(1, 7, 5));
    }

    void updateTotalField() {
        BigDecimal fee = feePanel.getDecimal();
        BigDecimal quantity = quantityField.getDecimal();
        BigDecimal price = priceField.getDecimal();

        BigDecimal value = quantity.multiply(price);

        value = value.subtract(fee);

        totalField.setDecimal(value);
    }

    @Override
    public void modifyTransaction(Transaction tran) {
        if (!(tran instanceof InvestmentTransaction)) {
            throw new IllegalArgumentException("bad tranType");
        }
        clearForm();

        datePanel.setDate(tran.getDate());

        List<TransactionEntry> entries = tran.getTransactionEntries();

        feePanel.setTransactionEntries(((InvestmentTransaction) tran).getInvestmentFeeEntries());

        gainsPanel.setTransactionEntries(((InvestmentTransaction) tran).getInvestmentGainLossEntries());

        for (TransactionEntry e : entries) {
            if (e instanceof TransactionEntryReinvestDivX) {
                AbstractInvestmentTransactionEntry entry = (AbstractInvestmentTransactionEntry) e;

                memoField.setText(e.getMemo());
                priceField.setDecimal(entry.getPrice());
                quantityField.setDecimal(entry.getQuantity());
                securityCombo.setSelectedNode(entry.getSecurityNode());
            }
        }

        updateTotalField();

        modTrans = tran;

        reconciledButton.setSelected(tran.getReconciled(getAccount()) == ReconciledState.RECONCILED);
    }

    @Override
    public Transaction buildTransaction() {

        Collection<TransactionEntry> fees = feePanel.getTransactions();

        Collection<TransactionEntry> gains = gainsPanel.getTransactions();

        return TransactionFactory.generateReinvDividendXTransaction(account, securityCombo.getSelectedNode(), priceField.getDecimal(), quantityField.getDecimal(), datePanel.getDate(), memoField.getText(), reconciledButton.isSelected(), fees, gains);
    }

    @Override
    public void clearForm() {
        super.clearForm();

        feePanel.clearForm();
        gainsPanel.clearForm();
        updateTotalField();
    }
}
