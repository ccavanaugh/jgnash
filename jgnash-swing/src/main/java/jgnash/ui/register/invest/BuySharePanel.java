/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.math.BigDecimal;
import java.util.List;

import javax.swing.JPanel;

import jgnash.engine.AbstractInvestmentTransactionEntry;
import jgnash.engine.Account;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionEntryBuyX;
import jgnash.engine.TransactionFactory;
import jgnash.ui.register.AccountExchangePanel;
import jgnash.ui.util.ValidationFactory;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Form for buying shares.
 *
 * @author Craig Cavanaugh
 */
public final class BuySharePanel extends AbstractPriceQtyInvTransactionPanel {

    private final FeePanel feePanel;

    private final AccountExchangePanel accountExchangePanel;

    BuySharePanel(final Account account) {
        super(account);

        feePanel = new FeePanel(account);

        accountExchangePanel = new AccountExchangePanel(getAccount().getCurrencyNode(), null, totalField);
        accountExchangePanel.setSelectedAccount(getAccount());

        layoutMainPanel();

        FocusListener focusListener = new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent evt) {
                updateTotalField();
            }
        };

        feePanel.addFocusListener(focusListener);

        feePanel.addActionListener(e -> {
            if (e.getSource() == feePanel) {
                updateTotalField();
            }
        });

        quantityField.addFocusListener(focusListener);
        priceField.addFocusListener(focusListener);

        datePanel.getDateField().addKeyListener(keyListener);
        feePanel.addKeyListener(keyListener);
        memoField.addKeyListener(keyListener);
        priceField.addKeyListener(keyListener);
        quantityField.addKeyListener(keyListener);
        securityCombo.addKeyListener(keyListener);
        getReconcileCheckBox().addKeyListener(keyListener);

        clearForm();
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("right:d, $lcgap, 50dlu:g, 8dlu, right:d, $lcgap, max(65dlu;min)", "f:d, $nlgap, f:d, $nlgap, f:d, $nlgap, f:d");

        layout.setRowGroups(new int[][]{{1, 3, 5, 7}});
        CellConstraints cc = new CellConstraints();

        setLayout(layout);

        /* Create a sub panel to work around a column spanning problem in FormLayout */
        JPanel subPanel = buildHorizontalSubPanel("max(48dlu;min):g(0.5), 8dlu, d, $lcgap, max(48dlu;min):g(0.5)", ValidationFactory.wrap(priceField), "Label.Quantity", ValidationFactory.wrap(quantityField));

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

        add("Label.Account", cc.xy(1, 7));

        add(accountExchangePanel, cc.xy(3, 7));
        add(getReconcileCheckBox(), cc.xyw(5, 7, 3));
    }

    private void updateTotalField() {
        BigDecimal fee = feePanel.getDecimal();
        BigDecimal quantity = quantityField.getDecimal();
        BigDecimal price = priceField.getDecimal();

        BigDecimal value = quantity.multiply(price);

        value = value.add(fee);

        totalField.setDecimal(value);
    }

    @Override
    public void modifyTransaction(final Transaction tran) {
        if (!(tran instanceof InvestmentTransaction)) {
            throw new IllegalArgumentException("bad tranType");
        }
        clearForm();

        datePanel.setDate(tran.getLocalDate());

        List<TransactionEntry> entries = tran.getTransactionEntries();

        feePanel.setTransactionEntries(((InvestmentTransaction) tran).getInvestmentFeeEntries());

        entries.stream().filter(e -> e instanceof TransactionEntryBuyX).forEach(e -> {
            AbstractInvestmentTransactionEntry entry = (AbstractInvestmentTransactionEntry) e;

            memoField.setText(e.getMemo());
            priceField.setDecimal(entry.getPrice());
            quantityField.setDecimal(entry.getQuantity());
            securityCombo.setSelectedNode(entry.getSecurityNode());

            // TODO by default investment account is assigned to debit account.  Should only have
            // look at the credit side of the entry for information
            if (entry.getCreditAccount().equals(account)) {
                accountExchangePanel.setSelectedAccount(entry.getDebitAccount());
                accountExchangePanel.setExchangedAmount(entry.getDebitAmount().abs());
            } else {
                accountExchangePanel.setSelectedAccount(entry.getCreditAccount());
                accountExchangePanel.setExchangedAmount(entry.getCreditAmount());
            }
        });

        updateTotalField();

        modTrans = tran;

        setReconciledState(tran.getReconciled(getAccount()));
    }

    @Override
    public Transaction buildTransaction() {

        BigDecimal exchangeRate = accountExchangePanel.getExchangeRate();

        List<TransactionEntry> fees = feePanel.getTransactions();

        return TransactionFactory.generateBuyXTransaction(accountExchangePanel.getSelectedAccount(), getAccount(),
                securityCombo.getSelectedNode(), priceField.getDecimal(), quantityField.getDecimal(), exchangeRate,
                datePanel.getLocalDate(), memoField.getText(), fees);
    }

    @Override
    public void clearForm() {
        super.clearForm();

        feePanel.clearForm();
        accountExchangePanel.setSelectedAccount(getAccount());
        updateTotalField();
    }
}
