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
package jgnash.uifx.views.register;

import java.text.NumberFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jgnash.engine.Account;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.text.CommodityFormat;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;

/**
 * Account properties wrapper to making binding of long running processes easier
 *
 * @author Craig Cavanaugh
 */
class AccountPropertyWrapper implements MessageListener {

    final static ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final Object lock = new Object();

    @Nullable private Account account = null;

    private ReadOnlyStringWrapper reconciledAmountProperty = new ReadOnlyStringWrapper();
    private ReadOnlyStringWrapper accountBalanceProperty = new ReadOnlyStringWrapper();
    private ReadOnlyStringWrapper accountNameProperty = new ReadOnlyStringWrapper();

    private NumberFormat numberFormat;  // not thread safe

    public AccountPropertyWrapper() {
        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT);
    }

    void setAccount(@NotNull final Account account) {
        this.account = account;
        numberFormat = CommodityFormat.getFullNumberFormat(account.getCurrencyNode());

        updateProperties();
    }

    @Override
    public void messagePosted(final Message event) {
        switch (event.getEvent()) {
            case ACCOUNT_MODIFY:
                if (event.getObject(MessageProperty.ACCOUNT).equals(account)) {
                    updateProperties();
                }
            default:
        }
    }

    private void updateProperties() {

        if (account != null) {
            Platform.runLater(() -> accountNameProperty.setValue(account.getName()));
        }

        final Task<Void> balanceTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (account != null) {
                    synchronized (lock) {
                        Platform.runLater(() -> accountBalanceProperty.setValue(numberFormat.format(account.getBalance())));
                    }
                }
                return null;
            }
        };

        final Task<Void> reconciledBalanceTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (account != null) {
                    synchronized (lock) {
                        Platform.runLater(() -> reconciledAmountProperty.setValue(numberFormat.format(account.getReconciledBalance())));

                    }
                }
                return null;
            }
        };

        executorService.submit(balanceTask);
        executorService.submit(reconciledBalanceTask);
    }

    public ReadOnlyStringProperty getReconciledAmountProperty() {
        return reconciledAmountProperty.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty getAccountBalanceProperty() {
        return accountBalanceProperty.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty getAccountNameProperty() {
        return accountNameProperty.getReadOnlyProperty();
    }
}
