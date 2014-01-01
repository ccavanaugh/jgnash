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
package jgnash.ui.components;

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JTextField;

import jgnash.engine.Account;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionType;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.ui.components.autocomplete.DefaultAutoCompleteModel;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.MultiHashMap;

/**
 * This factory class generates AutoCompleteTextFields that share a common model
 * to reduce the amount of overhead.
 * 
 * @author Craig Cavanaugh
 * @author Don Brown
 */
public class AutoCompleteFactory {

    // volatile because accessor method is not synchronized
    private static volatile MemoModel memoModel;    
    
    private static boolean autoComplete;

    private static boolean fuzzyMatch;

    private static boolean ignoreCase;

    private static final String AUTO_COMPLETE = "autoComplete";

    private static final String IGNORE_CASE = "ignoreCase";

    private static final String FUZZY_MATCH = "fuzzyMatch";

    private static final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(new Object());

    /**
     * Use an ExecutorService to manage the number of running threads
     */
    private static final ExecutorService pool = Executors.newSingleThreadExecutor(new DefaultDaemonThreadFactory());

    static {
        Preferences p = Preferences.userNodeForPackage(AutoCompleteFactory.class);
        autoComplete = p.getBoolean(AUTO_COMPLETE, true);
        fuzzyMatch = p.getBoolean(FUZZY_MATCH, false);
        ignoreCase = p.getBoolean(IGNORE_CASE, true);
    }

    private AutoCompleteFactory() {
    }

    private static void addPropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    private static void removePropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Sets the availability of the auto-completion ability
     * 
     * @param auto enabled state
     */
    public static void setEnabled(final boolean auto) {

        boolean oldAutoComplete = autoComplete;

        autoComplete = auto;
        Preferences p = Preferences.userNodeForPackage(AutoCompleteFactory.class);
        p.putBoolean(AUTO_COMPLETE, autoComplete);

        propertyChangeSupport.firePropertyChange(AUTO_COMPLETE, oldAutoComplete, autoComplete);
    }

    /**
     * Sets the case sensitivity of auto completion
     * 
     * @param notCaseSensitive case sensitivity state
     */
    public static void setIgnoreCase(final boolean notCaseSensitive) {

        boolean oldIgnoreCase = ignoreCase;

        ignoreCase = notCaseSensitive;
        Preferences p = Preferences.userNodeForPackage(AutoCompleteFactory.class);
        p.putBoolean(IGNORE_CASE, ignoreCase);

        propertyChangeSupport.firePropertyChange(IGNORE_CASE, oldIgnoreCase, ignoreCase);
    }

    /**
     * Sets if fuzzy match is used for auto completion
     * 
     * @param doFuzzyMatch case sensitivity state
     */
    public static void setFuzzyMatch(final boolean doFuzzyMatch) {

        boolean oldfuzzyMatch = fuzzyMatch;

        fuzzyMatch = doFuzzyMatch;
        Preferences p = Preferences.userNodeForPackage(AutoCompleteFactory.class);
        p.putBoolean(FUZZY_MATCH, fuzzyMatch);

        propertyChangeSupport.firePropertyChange(FUZZY_MATCH, oldfuzzyMatch, fuzzyMatch);
    }

    /**
     * Returns the status of auto completion
     * 
     * @return true is auto completion is enabled, false otherwise
     */
    public static boolean isEnabled() {
        return autoComplete;
    }

    /**
     * Returns the case sensitivity of the lookup process
     * 
     * @return true if match is case sensitive
     */
    public static boolean ignoreCase() {
        return ignoreCase;
    }

    /**
     * Returns the state of fuzzy match
     * 
     * @return true if fuzzy match is enabled
     */
    public static boolean fuzzyMatch() {
        return fuzzyMatch;
    }

    /**
     * Returns an auto-complete field that knows about transaction memos.
     * 
     * @return A plain JTextField or an AutoCompleteTextField.
     */
    public static JTextField getMemoField() {
        if (autoComplete) {
            if (memoModel == null) {
                memoModel = new MemoModel();
            }
            return new AutoCompleteTextField(memoModel);
        }
        return new JTextFieldEx();
    }

    /**
     * Returns an auto-complete field that knows about transaction payees.
     * 
     * @param account account this payee field will match
     * @return A plain JTextField or an AutoCompleteTextField.
     */
    public static JTextField getPayeeField(final Account account) {
        if (autoComplete) {
            return new AutoCompleteTextField(new PayeeAccountModel(account));
        }
        return new JTextFieldEx();
    }

    private static abstract class TransactionModel extends DefaultAutoCompleteModel implements MessageListener {

        volatile boolean load = false;

        private PropertyChangeListener listener;

        public TransactionModel() {
            init();
            setIgnoreCase(AutoCompleteFactory.ignoreCase());
            setFuzzyMatch(AutoCompleteFactory.fuzzyMatch());
        }

        final void init() {
            MessageBus.getInstance().registerListener(this, MessageChannel.TRANSACTION, MessageChannel.SYSTEM);

            listener = new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals(IGNORE_CASE) && evt.getNewValue() != evt.getOldValue()) {
                        setIgnoreCase((Boolean) evt.getNewValue());
                        reload();
                    } else if (evt.getPropertyName().equals(AUTO_COMPLETE) && evt.getNewValue() != evt.getOldValue()) {
                        setEnabled((Boolean) evt.getNewValue());
                    } else if (evt.getPropertyName().equals(FUZZY_MATCH) && evt.getNewValue() != evt.getOldValue()) {
                        setFuzzyMatch((Boolean) evt.getNewValue());
                        reload();
                    }
                }
            };

            AutoCompleteFactory.addPropertyChangeListener(listener);

            load();
        }

        @Override
        public void messagePosted(final Message event) {
            switch (event.getEvent()) {
                case TRANSACTION_ADD:
                    Transaction t = (Transaction) event.getObject(MessageProperty.TRANSACTION);
                    load(t);
                    return;
                case FILE_NEW_SUCCESS:
                    purge(); // purge the old
                    return;
                case FILE_LOAD_SUCCESS:
                    reload();
                    return;
                case FILE_CLOSING:
                    load = false; // prevent loading
                    AutoCompleteFactory.removePropertyChangeListener(listener);
                    return;
                default:
            }
        }

        void load() {
            load = true;

            pool.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        List<Transaction> transactions = EngineFactory.getEngine(EngineFactory.DEFAULT)
                                .getTransactions();

                        // sort the transactions for consistent order
                        Collections.sort(transactions);

                        for (Transaction t : transactions) {
                            if (load) {
                                load(t);
                            } else {
                                return;
                            }
                        }
                    } catch (Exception e) {
                        Logger.getLogger(AutoCompleteFactory.class.getName()).log(Level.INFO, e.getLocalizedMessage(),
                                e);
                    }
                }
            });
        }

        final void reload() {
            purge(); // purge the old
            load(); // load the new
        }

        abstract void load(Transaction tran);
    }

    private static final class MemoModel extends PayeeModel {

        @Override
        void load(final Transaction tran) {
            if (tran != null) {
                addString(tran.getMemo());
            }
        }
    }

    /**
     * This model stores the transaction with the payee field value. A new
     * instance is created for each account and is account specific
     */
    private static final class PayeeAccountModel extends PayeeModel {

        private final Account account;

        public PayeeAccountModel(final Account account) {
            super();
            this.account = account;
        }

        @Override
        void load() {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {

                    pool.execute(new Runnable() {

                        @Override
                        public void run() {
                            if (account != null) {
                                for (Transaction t : account.getSortedTransactionList()) {
                                    load(t);
                                }
                            }
                        }
                    });
                }
            });
        }

        @Override
        public void messagePosted(final Message event) {
            Account a = (Account) event.getObject(MessageProperty.ACCOUNT);
            Transaction t = (Transaction) event.getObject(MessageProperty.TRANSACTION);

            switch (event.getEvent()) {
                case TRANSACTION_ADD:
                    if (a.equals(account)) {
                        load(t);
                    }
                    return;
                case FILE_NEW_SUCCESS:
                    purge(); // purge the old
                    return;
                case FILE_LOAD_SUCCESS:
                    reload();
                    return;
                case TRANSACTION_REMOVE:
                    if (a.equals(account)) {
                        removeExtraInfo(t);
                    }
                    return;
                default:
            }
        }
    }

    /**
     * This model stores the transaction with the payee field value. Split
     * entries are filtered for now because duplicating one in a form would
     * produce would only impact the parent split transaction.
     */
    private static class PayeeModel extends TransactionModel {

        final MultiHashMap<String, Transaction> transactions = new MultiHashMap<>();

        @Override
        void load(final Transaction tran) {
            if (tran != null && tran.getTransactionType() != TransactionType.SPLITENTRY) {
                String payee = tran.getPayee();
                if (payee != null) {
                    addString(tran.getPayee());

                    if (ignoreCase) {
                        transactions.put(tran.getPayee().toLowerCase(Locale.getDefault()), tran);
                    } else {
                        transactions.put(tran.getPayee(), tran);
                    }
                }
            }
        }

        @Override
        public Object getExtraInfo(final String key) {
            if (ignoreCase) {
                return transactions.get(key.toLowerCase(Locale.getDefault()));
            }
            return transactions.get(key);
        }

        /**
         * Removes the transaction associated with the payee. This is done so
         * that deleted transactions can be garbage collected.
         * 
         * @param t transaction to remove
         */
        void removeExtraInfo(final Transaction t) {
            if (ignoreCase) {
                transactions.remove(t.getPayee().toLowerCase(Locale.getDefault()), t);
            }
            transactions.remove(t.getPayee(), t);
        }

        @Override
        public void messagePosted(final Message event) {
            super.messagePosted(event);

            switch (event.getEvent()) {
                case TRANSACTION_REMOVE:
                    removeExtraInfo((Transaction) event.getObject(MessageProperty.TRANSACTION));
                    return;
                default:
            }
        }
    }
}
