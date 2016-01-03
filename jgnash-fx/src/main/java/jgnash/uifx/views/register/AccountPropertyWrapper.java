/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.text.CommodityFormat;
import jgnash.uifx.views.AccountBalanceDisplayManager;
import jgnash.util.DefaultDaemonThreadFactory;

/**
 * Account properties wrapper to making binding of long running processes easier
 *
 * @author Craig Cavanaugh
 */
class AccountPropertyWrapper implements MessageListener {

    private final static ExecutorService executorService = Executors.newSingleThreadExecutor(new DefaultDaemonThreadFactory());

    private final Object numberFormatLock = new Object();

    private final ReadOnlyStringWrapper reconciledAmountProperty = new ReadOnlyStringWrapper();

    private final ReadOnlyStringWrapper accountBalanceProperty = new ReadOnlyStringWrapper();

    private final ReadOnlyStringWrapper cashBalanceProperty = new ReadOnlyStringWrapper();

    private final ReadOnlyStringWrapper marketValueProperty = new ReadOnlyStringWrapper();

    private final ReadOnlyStringWrapper accountNameProperty = new ReadOnlyStringWrapper();

    private final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private NumberFormat numberFormat;  // not thread safe

    public AccountPropertyWrapper() {
        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT, MessageChannel.TRANSACTION);

        accountProperty.addListener(new ChangeListener<Account>() {
            @Override
            public void changed(final ObservableValue<? extends Account> observable, final Account oldValue, final Account newValue) {
                if (newValue != null) {

                    // Account changed, update the number format
                    synchronized (numberFormatLock) {
                        numberFormat = CommodityFormat.getFullNumberFormat(newValue.getCurrencyNode());
                    }

                    // Update account properties
                    updateProperties();
                }
            }
        });
    }

    @Override
    public void messagePosted(final Message event) {
        switch (event.getEvent()) {
            case ACCOUNT_MODIFY:
            case TRANSACTION_ADD:
            case TRANSACTION_REMOVE:
                if (event.getObject(MessageProperty.ACCOUNT).equals(accountProperty.get())) {
                    updateProperties();
                }
                break;
            default:
        }
    }

    private void updateProperties() {
        if (accountProperty.get() != null) {
            Platform.runLater(() -> accountNameProperty.setValue(accountProperty.get().getName()));
        } else {
            Platform.runLater(() -> accountNameProperty.setValue(""));
        }

        executorService.submit(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                synchronized (numberFormatLock) {
                    if (accountProperty.get() != null) {
                        final Account account = accountProperty.get();
                        final BigDecimal balance =
                                AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(),
                                        account.getBalance());

                        Platform.runLater(() -> accountBalanceProperty.setValue(numberFormat.format(balance)));
                    } else {
                        Platform.runLater(() -> accountBalanceProperty.setValue(numberFormat.format(BigDecimal.ZERO)));
                    }
                }
                return null;
            }
        });

        executorService.submit(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                synchronized (numberFormatLock) {
                    if (accountProperty.get() != null) {
                        final Account account = accountProperty.get();
                        final BigDecimal balance =
                                AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(),
                                        account.getReconciledBalance());

                        Platform.runLater(() -> reconciledAmountProperty.setValue(numberFormat.format(balance)));
                    } else {
                        Platform.runLater(() -> reconciledAmountProperty.setValue(numberFormat.format(BigDecimal.ZERO)));
                    }
                }
                return null;
            }
        });

        if (accountProperty.get() != null && accountProperty.get().memberOf(AccountGroup.INVEST)) {
            executorService.submit(new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    synchronized (numberFormatLock) {
                        if (accountProperty.get() != null) {
                            final Account account = accountProperty.get();
                            final BigDecimal balance =
                                    AccountBalanceDisplayManager.convertToSelectedBalanceMode(account.getAccountType(),
                                            account.getCashBalance());

                            Platform.runLater(() -> cashBalanceProperty.setValue(numberFormat.format(balance)));
                        } else {
                            Platform.runLater(() -> cashBalanceProperty.setValue(numberFormat.format(BigDecimal.ZERO)));
                        }
                    }
                    return null;
                }
            });

            executorService.submit(new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    synchronized (numberFormatLock) {
                        if (accountProperty.get() != null) {
                            Platform.runLater(() -> marketValueProperty.setValue(numberFormat.format(accountProperty.get().getMarketValue())));
                        } else {
                            Platform.runLater(() -> marketValueProperty.setValue(numberFormat.format(BigDecimal.ZERO)));
                        }
                    }
                    return null;
                }
            });
        }
    }

    public ReadOnlyStringProperty reconciledAmountProperty() {
        return reconciledAmountProperty.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty accountBalanceProperty() {
        return accountBalanceProperty.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty cashBalanceProperty() {
        return cashBalanceProperty.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty marketValueProperty() {
        return marketValueProperty.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty accountNameProperty() {
        return accountNameProperty.getReadOnlyProperty();
    }

    public ObjectProperty<Account> accountProperty() {
        return accountProperty;
    }
}
