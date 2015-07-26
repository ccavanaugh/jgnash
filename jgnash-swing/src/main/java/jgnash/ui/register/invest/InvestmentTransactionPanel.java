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

import java.awt.CardLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.CompoundBorder;

import jgnash.engine.Account;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionType;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.components.ShadowBorder;
import jgnash.ui.register.AbstractEntryFormPanel;
import jgnash.ui.register.PanelType;
import jgnash.ui.register.RegisterEvent;
import jgnash.ui.register.RegisterListener;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Panel that uses a CardLayout to display the investment transaction forms.
 *
 * @author Craig Cavanaugh
 * @author Don Brown
 */
public class InvestmentTransactionPanel extends JPanel implements MessageListener, ActionListener {

    private final Account account;

    private Transaction modTrans;

    private int currentCard;

    private final CardLayout cardLayout;

    private JButton enterButton;

    private JButton cancelButton;

    private JPanel cardPanel;

    private JComboBox<String> actionCombo;

    private AbstractEntryFormPanel[] cards = new AbstractEntryFormPanel[0];

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private static final String[] actions;

    static {
        final ResourceBundle eRb = ResourceUtils.getBundle();

        actions = new String[]{eRb.getString("Transaction.BuyShare"), eRb.getString("Transaction.SellShare"),
                eRb.getString("Transaction.TransferIn"), eRb.getString("Transaction.TransferOut"),
                eRb.getString("Transaction.AddShare"), eRb.getString("Transaction.RemoveShare"),
                eRb.getString("Transaction.ReinvestDiv"), eRb.getString("Transaction.Dividend"),
                eRb.getString("Transaction.SplitShare"), eRb.getString("Transaction.MergeShare"),
                eRb.getString("Transaction.ReturnOfCapital")};
    }

    InvestmentTransactionPanel(final Account account) {
        this.account = account;

        layoutMainPanel();

        cardLayout = (CardLayout) cardPanel.getLayout();
        loadCards();

        registerListeners();

        /* Allows the user to submit the form from the keyboard when the
        enter button is selected */
        enterButton.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    enterAction();
                }
            }
        });
    }

    private void initComponents() {
        actionCombo = new JComboBox<>(new DefaultComboBoxModel<>(actions));

        cardPanel = new JPanel();
        cardPanel.setLayout(new CardLayout());
        enterButton = new JButton(rb.getString("Button.Enter"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));

        cancelButton.addActionListener(this);
        enterButton.addActionListener(this);
        actionCombo.addActionListener(this);
    }

    private void registerListeners() {
        MessageBus.getInstance().registerListener(this, MessageChannel.TRANSACTION);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("d, 4dlu, m:g, 4dlu, m", "f:d, $ugap, f:d");
        CellConstraints cc = new CellConstraints();

        setBorder(new CompoundBorder(new ShadowBorder(), Borders.TABBED_DIALOG));
        setLayout(layout);

        add(cardPanel, cc.xyw(1, 1, 5));
        add(new JSeparator(), cc.xyw(1, 2, 5));
        add(new JLabel(rb.getString("Label.Action")), cc.xy(1, 3));
        add(actionCombo, cc.xy(3, 3));
        add(StaticUIMethods.buildOKCancelBar(enterButton, cancelButton), cc.xy(5, 3));
    }

    void addRegisterListener(final RegisterListener l) {
        listenerList.add(RegisterListener.class, l);
    }

    /**
     * Notify all listeners of a cancel action
     */
    private void fireCancelAction() {
        RegisterEvent e = null;
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == RegisterListener.class) {
                // Lazily create the event:
                if (e == null) {
                    e = new RegisterEvent(this, RegisterEvent.Action.CANCEL);
                }
                ((RegisterListener) listeners[i + 1]).registerEvent(e);
            }
        }
    }

    /**
     * Notify all listeners of an OK action
     */
    private void fireOkAction() {
        RegisterEvent e = null;
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == RegisterListener.class) {
                // Lazily create the event:
                if (e == null) {
                    e = new RegisterEvent(this, RegisterEvent.Action.OK);
                }
                ((RegisterListener) listeners[i + 1]).registerEvent(e);
            }
        }
    }

    void cancelAction() {
        cards[currentCard].clearForm();
        modTrans = null;
        fireCancelAction();
    }

    void enterAction() {
        cards[currentCard].enterAction();
        fireOkAction();
    }

    private void actionAction() {
        activateCard(actionCombo.getSelectedIndex());
    }

    void modifyTransaction(final Transaction t) {

        // check for a locked account and issue a warning if necessary
        if (t.areAccountsLocked()) {
            StaticUIMethods.displayError(rb.getString("Message.TransactionModifyLocked"));
            return;
        }

        modTrans = t;

        if (t instanceof InvestmentTransaction) {
            switch (t.getTransactionType()) {
                case BUYSHARE:
                    actionCombo.setSelectedItem(actions[0]);
                    break;
                case SELLSHARE:
                    actionCombo.setSelectedItem(actions[1]);
                    break;
                case ADDSHARE:
                    actionCombo.setSelectedItem(actions[4]);
                    break;
                case REMOVESHARE:
                    actionCombo.setSelectedItem(actions[5]);
                    break;
                case REINVESTDIV:
                    actionCombo.setSelectedItem(actions[6]);
                    break;
                case DIVIDEND:
                    actionCombo.setSelectedItem(actions[7]);
                    break;
                case SPLITSHARE:
                    actionCombo.setSelectedItem(actions[8]);
                    break;
                case MERGESHARE:
                    actionCombo.setSelectedItem(actions[9]);
                    break;
                case RETURNOFCAPITAL:
                    actionCombo.setSelectedItem(actions[10]);
                    break;
                default:
            }
        } else {
            if (t.getAmount(account).signum() >= 0) {
                actionCombo.setSelectedItem(actions[2]); // transferIn
            } else {
                actionCombo.setSelectedItem(actions[3]); // transferOut
            }
        }

        cards[currentCard].modifyTransaction(t);
    }

    private void loadCards() {
        cards = new AbstractEntryFormPanel[11];

        cards[0] = new BuySharePanel(account);
        cards[1] = new SellSharePanel(account);
        cards[2] = new CashTransactionPanel(account, PanelType.INCREASE);
        cards[3] = new CashTransactionPanel(account, PanelType.DECREASE);
        cards[4] = new AddRemoveSharePanel(account, TransactionType.ADDSHARE);
        cards[5] = new AddRemoveSharePanel(account, TransactionType.REMOVESHARE);
        cards[6] = new ReinvestDividendPanel(account);
        cards[7] = new DividendPanel(account);
        cards[8] = new SplitMergeSharePanel(account, TransactionType.SPLITSHARE);
        cards[9] = new SplitMergeSharePanel(account, TransactionType.MERGESHARE);
        cards[10] = new ReturnOfCapitalPanel(account);

        for (int i = 0; i < actions.length; i++) {
            cardPanel.add(cards[i], actions[i]);
        }
    }

    private void activateCard(final int index) {
        cards[currentCard].clearForm();
        cardLayout.show(cardPanel, actions[index]);
        currentCard = index;
    }

    @Override
    public void messagePosted(final Message event) {
        /* must check modTrans outside the event queue otherwise modTrans is cleared first
         * before check*/
        if (modTrans != null) {
            Transaction t = event.getObject(MessageProperty.TRANSACTION);
            switch (event.getEvent()) {
                case TRANSACTION_REMOVE:
                    if (modTrans.equals(t)) {
                        EventQueue.invokeLater(this::cancelAction);
                    }
                    break;
                default:
                    // ignore any other messages that don't belong to us
                    break;
            }
        }
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == cancelButton) {
            cancelAction();
        } else if (e.getSource() == enterButton) {
            enterAction();
        } else if (e.getSource() == actionCombo) {
            actionAction();
        }
    }
}
