/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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
package jgnash.ui.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jgnash.engine.Account;
import jgnash.engine.AccountType;
import jgnash.engine.Comparators;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;

/**
 * ComboBox for displaying a list of accounts. Automatically refreshes itself
 * when necessary
 *
 * @author Craig Cavanaugh
 * @author Vijil E C (vijilec at gmail dot com)
 */
public class AccountListComboBox extends JComboBox<Account> implements MessageListener {

    private boolean layingOut = false;

    public AccountListComboBox() {
        this(new DefaultModel(null));
    }

    public AccountListComboBox(final Account baseAccount) {
        this(new DefaultModel(baseAccount));
    }

    AccountListComboBox(final AbstractModel model) {
        super();
        setRenderer(new Renderer(getRenderer()));
        setModel(model);
        registerListeners();
    }

    private void registerListeners() {
        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT,
                MessageChannel.SYSTEM);
    }

    @Override
    public void doLayout() {
        try {
            layingOut = true;
            super.doLayout();
        } finally {
            layingOut = false;
        }
    }

    @Override
    public Dimension getSize() {
        Dimension dim = super.getSize();
        if (!layingOut) {
            dim.width = Math.max(dim.width, getPreferredSize().width);
        }
        return dim;
    }

    /**
     * Static method to create an account list that does not filter holders or
     * locked accounts
     *
     * @return new AccountListComboBox instance
     */
    public static AccountListComboBox getFullInstance() {
        return new AccountListComboBox(new FullModel());
    }

    /**
     * Static method to create an account list that shows only parent accounts
     * of the specified type. Place holders and locked accounts are not
     * filtered.
     *
     * @param base  the account to show the children of
     * @param types Set of AccountTypes to show
     * @return new AccountListComboBox instance
     */
    public static AccountListComboBox getParentTypeInstance(final Account base, final Set<AccountType> types) {
        return new AccountListComboBox(new ParentTypeModel(base, types));
    }

    /**
     * Static method to create an account list based on account type place
     * holders or locked accounts are ignored
     *
     * @param types Set of account types
     * @return new AccountListComboBox instance
     */
    public static AccountListComboBox getInstanceByType(final Set<AccountType> types) {
        return new AccountListComboBox(new AccountTypeModel(types));
    }

    /**
     * Returns the selected account
     *
     * @return the account
     */
    public Account getSelectedAccount() {
        return (Account) getSelectedItem();
    }

    /**
     * Sets the selected accountPath.
     *
     * @param account account to select
     */
    public void setSelectedAccount(final Account account) {
        super.setSelectedItem(account);
    }

    @Override
    public void messagePosted(final Message event) {
        EventQueue.invokeLater(() -> {
            if (event.getEvent() == ChannelEvent.FILE_CLOSING) {
                MessageBus.getInstance().unregisterListener(AccountListComboBox.this, MessageChannel.ACCOUNT, MessageChannel.SYSTEM);
                ((AbstractModel) getModel()).messagePosted(event);
            } else {
                Account account = getSelectedAccount();
                ((AbstractModel) getModel()).messagePosted(event);

                if (account != null && event.getEvent() != ChannelEvent.ACCOUNT_REMOVE
                        && !account.equals(event.getObject(MessageProperty.ACCOUNT))) {
                    setSelectedAccount(account);
                } else {
                    if (getModel().getSize() > 0) {
                        setSelectedIndex(0);
                    }
                }
            }
        });
    }

    static abstract class AbstractModel extends AbstractListModel<Account> implements ComboBoxModel<Account> {

        List<Account> accounts = new ArrayList<>();

        Account baseAccount;

        Object selectedItem;

        Set<AccountType> types;

        final private Object lock = new Object();

        public AbstractModel() {
            this(null);
        }

        public AbstractModel(final Account exclude, final Set<AccountType> types) {
            baseAccount = exclude;
            this.types = types;
        }

        public AbstractModel(final Account exclude) {
            baseAccount = exclude;
        }

        void loadAccounts() {
            synchronized (lock) {

                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);


                final ArrayList<Account> list = new ArrayList<>();
                loadChildren(engine.getRootAccount(), list);
                accounts.clear();
                accounts = list;
                if (!list.isEmpty()) {
                    selectedItem = accounts.get(0); // make the first object the selected item
                }
            }
        }

        abstract protected void loadChildren(final Account acc, final List<Account> array);

        /**
         * Returns the value at the specified index.
         *
         * @param index the requested index
         * @return the value at {@code index}
         */
        @Override
        public Account getElementAt(final int index) {
            synchronized (lock) {
                return accounts.get(index);
            }
        }

        /**
         * Returns the selected item
         *
         * @return The selected item or {@code null} if there is no
         *         selection
         */
        @Override
        public Object getSelectedItem() {
            return selectedItem;
        }

        /**
         * Returns the length of the list.
         *
         * @return the length of the list
         */
        @Override
        public int getSize() {
            synchronized (lock) {
                return accounts.size();
            }
        }

        /**
         * Set the selected item. The implementation of this method should
         * notify all registered {@code ListDataListener}s that the
         * contents have changed.
         *
         * @param anItem the list object to select or {@code null} to clear
         *               the selection
         */
        @Override
        public void setSelectedItem(final Object anItem) {
            if (selectedItem != null && selectedItem != anItem || selectedItem == null && anItem != null) {
                selectedItem = anItem;
                fireContentsChanged(this, -1, -1);
            }
        }

        private void clear() {
            accounts.clear();
            baseAccount = null;
        }

        // Model update must not be pushed to the EDT to maintain synchronous
        // behavior.
        // The view already makes a call to this method from the EDT
        public void messagePosted(final Message event) {
            switch (event.getEvent()) {
                case FILE_CLOSING:
                    clear();
                    break;
                case ACCOUNT_REMOVE:
                    Account account = event.getObject(MessageProperty.ACCOUNT);

                    int index = accounts.indexOf(account);

                    if (index > -1) {
                        accounts.remove(index);
                        fireIntervalRemoved(this, index, index);
                    }
                    break;
                case ACCOUNT_ADD:
                case ACCOUNT_MODIFY:
                    loadAccounts();
                    fireContentsChanged(this, 0, accounts.size());
                    break;
                default: // ignore any other messages that don't belong to us
                    break;

            }
        }
    }

    private final static class FullModel extends AbstractModel {

        public FullModel() {
            super();
            loadAccounts();
        }

        @Override
        protected void loadChildren(final Account acc, final List<Account> array) {
            for (final Account child : acc.getChildren(Comparators.getAccountByCode())) {
                array.add(child);
                if (child.getChildCount() > 0) { // recursively load the account
                    // list
                    loadChildren(child, array);
                }
            }
        }
    }

    private final static class ParentTypeModel extends AbstractModel {

        public ParentTypeModel(final Account base, final Set<AccountType> types) {
            super(base, types);

            Objects.requireNonNull(base);
            loadAccounts();
        }

        @Override
        protected void loadChildren(final Account acc, List<Account> array) {
            for (final Account account : acc.getChildren(Comparators.getAccountByCode())) {
                if (types.contains(account.getAccountType())
                        && account.getChildCount() > 0) {
                    array.add(account);
                }

                if (account.getChildCount() > 0) { // possibly recursive
                    loadChildren(account, array);
                }
            }
        }
    }

    private final static class AccountTypeModel extends AbstractModel {

        public AccountTypeModel(final Set<AccountType> types) {
            super(null, types);
            loadAccounts();
        }

        @Override
        protected void loadChildren(final Account acc, final List<Account> array) {

            for (final Account tAcc : acc.getChildren(Comparators.getAccountByCode())) {
                if (!tAcc.isLocked() && !tAcc.isPlaceHolder()) { // honor the account lock and placeHolder attribute
                    if (types.contains(tAcc.getAccountType())) {
                        array.add(tAcc);
                    }
                }
                if (tAcc.getChildCount() > 0) { // recursively load the account list
                    loadChildren(tAcc, array);
                }
            }
        }
    }

    private final static class DefaultModel extends AbstractModel {

        public DefaultModel(final Account exclude) {
            super(exclude);
            loadAccounts();
        }

        @Override
        protected void loadChildren(final Account acc, final List<Account> array) {

            for (final Account account : acc.getChildren(Comparators.getAccountByCode())) {

                // honor the account locked, placeHolder, and visible attributes
                if (!account.isLocked() && !account.isPlaceHolder() && account.isVisible()) {
                    if (baseAccount != account) {
                        array.add(account);
                    }
                }
                if (account.getChildCount() > 0) { // recursively load the account list
                    loadChildren(account, array);
                }
            }
        }
    }

    /**
     * ComboBox renderer Display a specified text when the ComboBox is disabled
     */
    private class Renderer implements ListCellRenderer<Account> {

        private final ListCellRenderer<? super Account> delegate;

        public Renderer(final ListCellRenderer<? super Account> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getListCellRendererComponent(final JList<? extends Account> list, final Account value,
                                                      final int index, final boolean isSelected, final boolean cellHasFocus) {
            if (value != null) {
                AccountListComboBox.this.setToolTipText(value.getPathName());

                Component c = delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (c instanceof JLabel) {
                    ((JLabel) c).setText(AccountListComboBox.this.getToolTipText());
                }

                return c;
            }
            return delegate.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);
        }
    }
}
