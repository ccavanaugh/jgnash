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
package jgnash.ui.register.invest;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.math.BigDecimal;

import javax.swing.JPanel;

import jgnash.engine.Account;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionFactory;
import jgnash.engine.TransactionType;
import jgnash.ui.util.ValidationFactory;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Form for creating Add/Remove share transactions.
 * 
 * @author Craig Cavanaugh
 *
 */
public final class AddRemoveSharePanel extends AbstractPriceQtyInvTransactionPanel {

    private final TransactionType tranType;

    AddRemoveSharePanel(Account account, TransactionType tranType) {
        super(account);

        this.tranType = tranType;

        layoutMainPanel();

        FocusListener focusListener = new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent evt) {
                updateTotalField();
            }
        };

        quantityField.addFocusListener(focusListener);
        priceField.addFocusListener(focusListener);

        securityCombo.addKeyListener(keyListener);
        datePanel.getDateField().addKeyListener(keyListener);
        quantityField.addKeyListener(keyListener);
        memoField.addKeyListener(keyListener);
        priceField.addKeyListener(keyListener);

        clearForm();
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("right:d, $lcgap, 50dlu:g, 8dlu, right:d, $lcgap, max(65dlu;min)", "f:d, $nlgap, f:d, $nlgap, f:d");

        layout.setRowGroups(new int[][] { { 1, 3, 5 } });
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
        add("Label.Total", cc.xy(5, 3));
        add(totalField, cc.xy(7, 3));

        add("Label.Memo", cc.xy(1, 5));
        add(memoField, cc.xy(3, 5));
        add(getReconcileCheckBox(), cc.xyw(5, 5, 3));
    }

    @Override
    public void clearForm() {
        super.clearForm();

        updateTotalField();
    }

    @Override
    public void modifyTransaction(Transaction tran) {

        assert tran instanceof InvestmentTransaction;

        assert tran.getTransactionType() == TransactionType.ADDSHARE || tran.getTransactionType() == TransactionType.REMOVESHARE;

        InvestmentTransaction _tran = (InvestmentTransaction) tran;

        clearForm();

        modTrans = tran;

        datePanel.setDate(tran.getLocalDate());
        memoField.setText(tran.getMemo());
        priceField.setDecimal(_tran.getPrice());
        quantityField.setDecimal(_tran.getQuantity());
        securityCombo.setSelectedNode(_tran.getSecurityNode());

        setReconciledState(tran.getReconciled(getAccount()));

        updateTotalField();
    }

    @Override
    public Transaction buildTransaction() {
        if (tranType == TransactionType.ADDSHARE) {
            return TransactionFactory.generateAddXTransaction(account, securityCombo.getSelectedNode(),
                    priceField.getDecimal(), quantityField.getDecimal(), datePanel.getLocalDate(), memoField.getText());
        }
        return TransactionFactory.generateRemoveXTransaction(account, securityCombo.getSelectedNode(),
                priceField.getDecimal(), quantityField.getDecimal(), datePanel.getLocalDate(), memoField.getText());
    }

    private void updateTotalField() {
        BigDecimal quantity = quantityField.getDecimal();
        BigDecimal price = priceField.getDecimal();
        totalField.setDecimal(quantity.multiply(price));
    }
}
