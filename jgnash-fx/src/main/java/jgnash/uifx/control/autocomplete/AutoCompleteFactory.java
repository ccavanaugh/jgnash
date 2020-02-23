/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
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
package jgnash.uifx.control.autocomplete;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionType;
import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.uifx.Options;
import jgnash.uifx.control.AutoCompleteTextField;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.MultiHashMap;
import jgnash.resource.util.ResourceUtils;

/**
 * This factory class generates AutoCompleteTextFields that share a common model
 * to reduce the amount of overhead.
 *
 * @author Craig Cavanaugh
 * @author Don Brown
 */
public class AutoCompleteFactory {

    private static MemoModel memoModel;

    private static final Object synchronizationObject = new Object();

    /**
     * Use an ExecutorService to manage the number of running threads.
     */
    private static final ExecutorService pool
            = Executors.newSingleThreadExecutor(
            new DefaultDaemonThreadFactory("Auto Complete Factory Executor"));

    private AutoCompleteFactory() {
        // Factory class
    }

    /**
     * Sets the {@code AutoCompleteModel} for {@code Transaction} memos to
     * an {@code AutoCompleteTextField<Transaction>}.
     *
     * @param autoCompleteTextField text field to bind to
     */
    public static void setMemoModel(final AutoCompleteTextField<Transaction> autoCompleteTextField) {
        if (Options.useAutoCompleteProperty().get()) {
            synchronized (synchronizationObject) {
                if (memoModel == null) {
                    memoModel = new MemoModel();
                }
            }
            autoCompleteTextField.autoCompleteModelObjectProperty().set(memoModel);
        }
    }

    /**
     * Sets the {@code AutoCompleteModel} for {@code Transaction} payees to
     * an {@code AutoCompleteTextField<Transaction>}.
     *
     * @param account               base Account for payee model
     * @param autoCompleteTextField text Field to bind to
     */
    public static void setPayeeModel(final AutoCompleteTextField<Transaction> autoCompleteTextField, final Account account) {
        if (Options.useAutoCompleteProperty().get()) {
            autoCompleteTextField.autoCompleteModelObjectProperty().set(new PayeeAccountModel(account));
        }
    }

    private static abstract class TransactionModel extends DefaultAutoCompleteModel<Transaction> implements MessageListener {

        volatile boolean load = false;

        TransactionModel() {
            init();
        }

        final void init() {
            MessageBus.getInstance().registerListener(this, MessageChannel.TRANSACTION, MessageChannel.SYSTEM);
            load();
        }

        @Override
        public void messagePosted(final Message event) {
            switch (event.getEvent()) {
                case TRANSACTION_ADD:
                    Transaction t = event.getObject(MessageProperty.TRANSACTION);
                    load(t);
                    return;
                case FILE_LOAD_SUCCESS:
                    reload();
                    return;
                case FILE_CLOSING:
                    load = false; // prevent loading
                    purge();
                    return;
                default:
            }
        }

        void load() {
            load = true;

            pool.execute(() -> {
                try {
                    final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                    Objects.requireNonNull(engine);

                    final List<Transaction> transactions = engine.getTransactions();

                    // sort the transactions for consistent order
                    Collections.sort(transactions);

                    for (final Transaction t : transactions) {
                        if (load) {
                            load(t);
                        } else {    // loading has been stopped
                            return;
                        }
                    }

                    super.load();
                } catch (Exception e) {
                    Logger.getLogger(TransactionModel.class.getName()).log(Level.INFO, e.getLocalizedMessage(), e);
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

                // Add both memo versions
                addString(tran.getMemo());
                addString(tran.getTransactionMemo());
            }
        }
    }

    /**
     * This model stores the transaction with the payee field value. A new
     * instance is created for each account and is account specific.
     */
    private static final class PayeeAccountModel extends PayeeModel {

        private final Account account;

        PayeeAccountModel(final Account account) {
            super();
            this.account = account;
        }

        @Override
        void load() {

            // Push the load to the end of the application thread for a lazy init
            JavaFXUtils.runLater(() -> pool.execute(() -> {
                if (account != null) {
                    account.getSortedTransactionList().forEach(this::load);
                }

                // Signal complete at the very end of the application thread
                JavaFXUtils.runLater(() -> pool.execute(() -> loadComplete.set(true)));
            }));
        }

        @Override
        public void messagePosted(final Message event) {
            Account a = event.getObject(MessageProperty.ACCOUNT);
            Transaction t = event.getObject(MessageProperty.TRANSACTION);

            switch (event.getEvent()) {
                case TRANSACTION_ADD:
                    if (a.equals(account)) {
                        load(t);
                    }
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
     * This model stores the transaction with the payee field value.
     * <p>
     * Split entries are excluded because duplicating one in a form would
     * only impact the parent split transaction.
     */
    private static class PayeeModel extends TransactionModel {

        final MultiHashMap<String, Transaction> transactions = new MultiHashMap<>();

        @Override
        void load(final Transaction tran) {
            if (tran != null && tran.getTransactionType() != TransactionType.SPLITENTRY) {

                addString(tran.getPayee());

                if (ignoreCaseEnabled.get()) {
                    transactions.put(tran.getPayee().toLowerCase(Locale.getDefault()), tran);
                } else {
                    transactions.put(tran.getPayee(), tran);
                }
            }
        }

        @Override
        public Collection<Transaction> getAllExtraInfo(final String key) {
            if (ignoreCaseEnabled.get()) {
                return transactions.getAll(key.toLowerCase(Locale.getDefault()));
            }
            return transactions.getAll(key);
        }

        /**
         * Removes the transaction associated with the payee. This is done so
         * that deleted transactions can be garbage collected.
         *
         * @param t transaction to remove
         */
        void removeExtraInfo(final Transaction t) {
            pool.execute(() -> {
                if (ignoreCaseEnabled.get()) {
                    if (!transactions.removeValue(t.getPayee().toLowerCase(Locale.getDefault()), t)) {
                        Logger.getLogger(AutoCompleteFactory.class.getName())
                                .finest(ResourceUtils.getString("Message.Warn.FailedTransInfoRemoval"));
                    }
                } else {
                    if (!transactions.removeValue(t.getPayee(), t)) {
                        Logger.getLogger(AutoCompleteFactory.class.getName())
                                .finest(ResourceUtils.getString("Message.Warn.FailedTransInfoRemoval"));
                    }
                }
            });
        }

        @Override
        public void messagePosted(final Message event) {
            super.messagePosted(event);

            if (event.getEvent() == ChannelEvent.TRANSACTION_REMOVE) {
                removeExtraInfo(event.getObject(MessageProperty.TRANSACTION));
            }
        }
    }
}
