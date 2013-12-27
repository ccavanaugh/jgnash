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
package jgnash.ui.register;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.math.BigDecimal;

import jgnash.engine.Account;
import jgnash.engine.CurrencyNode;
import jgnash.engine.MathConstants;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.text.CommodityFormat;
import jgnash.ui.components.AccountListComboBox;
import jgnash.ui.components.JFloatField;
import jgnash.util.Resource;

/**
 * UI Form for handling the exchange of currencies
 * 
 * @author Craig Cavanaugh
 *
 */
public class AccountExchangePanel extends JPanel implements ActionListener, FocusListener, PopupMenuListener, ItemListener, MessageListener {

    private CurrencyNode baseCurrency;

    private AccountListComboBox accountCombo;

    private JLabel amountLabel;

    private JFloatField exchangeAmountField;

    private JToggleButton expandButton;

    private boolean isLayoutCompact = true;

    /**
     * supplied by constructor
     */
    private JFloatField amountField = null;

    private JPopupMenu detailWindow;

    private JFloatField exchangeRateField;

    private JLabel conversionLabel;

    private long eventTime;

    /**
     * Panel constructor
     * <p>
     * <code>amountField</code> must be fully configured prior to calling the constructor
     * <p>
     * This component cannot be reused after a data set is closed.
     * 
     * @param baseCurrency Base currency
     * @param account Account to filter from available list. May be null
     * @param amountField The field associated with the amount
     */
    public AccountExchangePanel(final CurrencyNode baseCurrency, final Account account, final JFloatField amountField) {
        assert baseCurrency != null && amountField != null;

        Resource rb = Resource.get();

        this.baseCurrency = baseCurrency;
        this.amountField = amountField;

        if (amountField.isEditable()) {
            amountField.addFocusListener(this);
        } else { // amountField is read only and controlled elsewhere

            amountField.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void insertUpdate(DocumentEvent e) {
                    amountFieldAction();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    // do nothing
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    // do nothing
                }
            });
        }

        if (account != null) {
            accountCombo = new AccountListComboBox(account);
        } else {
            accountCombo = new AccountListComboBox();
        }

        accountCombo.addActionListener(this);

        Account selectedAccount = accountCombo.getSelectedAccount();

        if (selectedAccount == null) { // can occur with only one account in the tree
            selectedAccount = account;
        }

        CurrencyNode selectedCurrency;

        if (selectedAccount == null) {
            selectedCurrency = baseCurrency;
        } else {
            selectedCurrency = selectedAccount.getCurrencyNode();
        }

        exchangeAmountField = new JFloatField(selectedCurrency);
        exchangeAmountField.addFocusListener(this);

        expandButton = new JToggleButton(Resource.getIcon("/jgnash/resource/mail-send-receive.png"));
        expandButton.setMargin(new Insets(0, 0, 0, 0));

        expandButton.addItemListener(this);
        expandButton.setFocusPainted(false);

        amountLabel = new JLabel(rb.getString("Label.ExchangeAmount"));

        exchangeRateField = new JFloatField(0, 6, 2);
        exchangeRateField.addFocusListener(this);

        updateExchangeRateField();

        conversionLabel = new JLabel();

        conversionLabel.setText(CommodityFormat.getConversion(baseCurrency, selectedCurrency));

        detailWindow = new JPopupMenu();
        detailWindow.setLayout(new BorderLayout());
        detailWindow.add(layoutRatePanel(), BorderLayout.CENTER);
        detailWindow.addPopupMenuListener(this);

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM);

        layoutPanel();
        updateLayout();
    }

    private void destroy() {
        MessageBus.getInstance().unregisterListener(this, MessageChannel.SYSTEM);
        accountCombo.removeActionListener(this);

        detailWindow.removePopupMenuListener(this);
        exchangeRateField.removeFocusListener(this);
        expandButton.removeItemListener(this);
        exchangeAmountField.removeFocusListener(this);
        amountField.removeFocusListener(this);
    }

    /**
     * Panel constructor
     * <p>
     * <code>amountField</code> must be fully configured prior to calling the constructor.
     * 
     * @param baseCurrency Base currency
     * @param amountField The field associated with the amount
     */
    public AccountExchangePanel(final CurrencyNode baseCurrency, final JFloatField amountField) {
        this(baseCurrency, null, amountField);
    }

    private void updateExchangeRateField() {
        Account selectedAccount = accountCombo.getSelectedAccount();

        if (selectedAccount != null) {
            exchangeRateField.setDecimal(baseCurrency.getExchangeRate(selectedAccount.getCurrencyNode()));
        }
    }

    private JPanel layoutRatePanel() {
        Resource rb = Resource.get();

        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout("d, 6dlu, right:d, $lcgap, max(48dlu;min)", "f:d");
        JPanel panel = new JPanel(layout);

        panel.setBorder(Borders.DLU2);

        panel.add(conversionLabel, cc.xy(1, 1));
        panel.add(new JLabel(rb.getString("Label.ExchangeRate")), cc.xy(3, 1));
        panel.add(exchangeRateField, cc.xy(5, 1));

        return panel;
    }

    private void layoutPanel() {
        FormLayout layout = new FormLayout("50dlu:g", "f:d:g");

        CellConstraints cc = new CellConstraints();
        setLayout(layout);
        add(accountCombo, cc.xy(1, 1));

        isLayoutCompact = true;
    }

    private void addExtraComponents() {
        FormLayout layout = (FormLayout) getLayout();
        layout.appendColumn(ColumnSpec.decode("8dlu")); // col 2

        layout.appendColumn(ColumnSpec.decode("right:d"));
        layout.appendColumn(ColumnSpec.decode("$lcgap"));
        layout.appendColumn(ColumnSpec.decode("max(48dlu;min)"));
        layout.appendColumn(ColumnSpec.decode("1px")); // col 6

        layout.appendColumn(ColumnSpec.decode("min")); // col 7

        CellConstraints cc = new CellConstraints();
        add(amountLabel, cc.xy(3, 1));
        add(exchangeAmountField, cc.xy(5, 1));
        add(expandButton, cc.xy(7, 1));

        invalidate();

        isLayoutCompact = false;
    }

    private void removeExtraComponents() {
        FormLayout layout = (FormLayout) getLayout();

        remove(expandButton);
        remove(exchangeAmountField);
        remove(amountLabel);

        layout.removeColumn(7);
        layout.removeColumn(6);
        layout.removeColumn(5);
        layout.removeColumn(4);
        layout.removeColumn(3);
        layout.removeColumn(2);

        invalidate();

        isLayoutCompact = true;
    }

    private void updateLayout() {
        if (accountCombo.getSelectedAccount() != null) {
            if (accountCombo.getSelectedAccount().getCurrencyNode().equals(baseCurrency) && !isLayoutCompact) {
                removeExtraComponents();
                validate();
            } else if (!accountCombo.getSelectedAccount().getCurrencyNode().equals(baseCurrency) && isLayoutCompact) {
                addExtraComponents();
                validate();
            }
        }
    }

    @Override
    public void itemStateChanged(final ItemEvent e) {

        /* Let the event queue flush itself */
        if (System.currentTimeMillis() - eventTime < 250) {
            expandButton.setSelected(false);
            return;
        }

        if (e.getStateChange() == ItemEvent.SELECTED) {
            Point location = exchangeAmountField.getLocation();
            detailWindow.show(AccountExchangePanel.this, location.x, location.y + exchangeAmountField.getHeight());
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {

        // could be null if user is trying to type in the account
        if (e.getSource() == accountCombo && accountCombo.getSelectedAccount() != null) {
            BigDecimal rate = baseCurrency.getExchangeRate(accountCombo.getSelectedAccount().getCurrencyNode());

            exchangeAmountField.setScale(accountCombo.getSelectedAccount().getCurrencyNode());
            exchangeAmountField.setDecimal(amountField.getDecimal().multiply(rate));

            conversionLabel.setText(CommodityFormat.getConversion(baseCurrency, accountCombo.getSelectedAccount().getCurrencyNode()));

            updateExchangeRateField();
            updateLayout();
        }
    }

    @Override
    public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
        // not used
    }

    @Override
    public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
        eventTime = System.currentTimeMillis();
        expandButton.setSelected(false);
    }

    @Override
    public void popupMenuCanceled(final PopupMenuEvent e) {
        // not used
    }

    public BigDecimal getExchangeRate() {
        if (isLayoutCompact) {
            return BigDecimal.ONE;
        }
        return exchangeRateField.getDecimal();
    }

    public BigDecimal getExchangedAmount() {
        // ensure the exchange amount field is correct.  The focus may have not changed
        if (exchangeAmountField.getDecimal().compareTo(BigDecimal.ZERO) == 0) {
            amountFieldAction();
        }

        return exchangeAmountField.getDecimal();
    }

    public Account getSelectedAccount() {
        return accountCombo.getSelectedAccount();
    }

    public void setSelectedAccount(final Account account) {
        accountCombo.setSelectedAccount(account);
    }

    public void setExchangedAmount(final BigDecimal amount) {
        exchangeAmountField.setDecimal(amount);
    }

    private void amountFieldAction() {
        if (exchangeRateField.getDecimal().compareTo(BigDecimal.ZERO) == 0) {
            if (amountField.getDecimal().compareTo(BigDecimal.ZERO) != 0) {
                exchangeRateField.setDecimal(exchangeAmountField.getDecimal().divide(amountField.getDecimal(), MathConstants.mathContext));
            }
        } else {
            exchangeAmountField.setDecimal(amountField.getDecimal().multiply(exchangeRateField.getDecimal(), MathConstants.mathContext));
        }
    }

    private void exchangeRateFieldAction() {
        if (amountField.getDecimal().compareTo(BigDecimal.ZERO) == 0 && amountField.isEditable()) {
            if (exchangeRateField.getDecimal().compareTo(BigDecimal.ZERO) != 0) {
                amountField.setDecimal(exchangeAmountField.getDecimal().divide(exchangeRateField.getDecimal(), MathConstants.mathContext));
            }
        } else {
            exchangeAmountField.setDecimal(amountField.getDecimal().multiply(exchangeRateField.getDecimal(), MathConstants.mathContext));
        }
    }

    private void exchangeAmountFieldAction() {
        if (amountField.getDecimal().compareTo(BigDecimal.ZERO) == 0 && amountField.isEditable()) {
            if (exchangeRateField.getDecimal().compareTo(BigDecimal.ZERO) != 0) {
                amountField.setDecimal(exchangeAmountField.getDecimal().divide(exchangeRateField.getDecimal(), MathConstants.mathContext));
            }
        } else {
            if (amountField.getDecimal().compareTo(BigDecimal.ZERO) != 0) {
                exchangeRateField.setDecimal(exchangeAmountField.getDecimal().divide(amountField.getDecimal(), MathConstants.mathContext));
            }
        }
    }

    @Override
    public void focusGained(final FocusEvent e) {
        // not used
    }

    @Override
    public void focusLost(final FocusEvent e) {
        if (e.getSource() == exchangeAmountField && exchangeAmountField.getDecimal().compareTo(BigDecimal.ZERO) > 0) {
            exchangeAmountFieldAction();
        } else if (e.getSource() == amountField) {
            amountFieldAction();
        } else if (e.getSource() == exchangeRateField) {
            exchangeRateFieldAction();
        }
    }

    @Override
    public void setEnabled(final boolean enabled) {
        accountCombo.setEnabled(enabled);
        expandButton.setEnabled(enabled);
        exchangeRateField.setEnabled(enabled);
    }

    @Override
    public void messagePosted(final Message event) {
        switch (event.getEvent()) {
            case FILE_CLOSING:
                destroy();
                break;
            default:
                break; // ignore any other messages that don't belong to us
        }
    }
}
