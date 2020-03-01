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
package jgnash.uifx.views.register;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.concurrent.Task;

import jgnash.engine.Account;
import jgnash.engine.AccountGroup;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.text.NumericFormats;
import jgnash.uifx.Options;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.views.AccountBalanceDisplayManager;
import jgnash.util.DefaultDaemonThreadFactory;

/**
 * Account properties wrapper to making binding of long-running processes easier.
 *
 * @author Craig Cavanaugh
 */
class AccountPropertyWrapper implements MessageListener {

    private static final ExecutorService executorService =
            Executors.newSingleThreadExecutor(new DefaultDaemonThreadFactory("Account Property Wrapper Executor"));

    private final Object numberFormatLock = new Object();

    private final ReadOnlyStringWrapper reconciledAmount = new ReadOnlyStringWrapper();

    private final ReadOnlyStringWrapper accountBalance = new ReadOnlyStringWrapper();

    private final ReadOnlyStringWrapper cashBalance = new ReadOnlyStringWrapper();

    private final ReadOnlyStringWrapper marketValue = new ReadOnlyStringWrapper();

    private final ReadOnlyStringWrapper accountName = new ReadOnlyStringWrapper();

    private final ObjectProperty<Account> account = new SimpleObjectProperty<>();

    private NumberFormat numberFormat;  // not thread safe

    @SuppressWarnings("FieldCanBeLocal")
    private final ChangeListener<String> formatListener;

    AccountPropertyWrapper() {
        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT, MessageChannel.TRANSACTION);

        account.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {

                // Account changed, update the number format
                synchronized (numberFormatLock) {
                    numberFormat = NumericFormats.getFullCommodityFormat(newValue.getCurrencyNode());
                }

                // Update account properties
                updateProperties();
            }
        });

        // Listen for changes to formatting preferences and force an update
        formatListener = (observable, oldValue, newValue) -> {
            synchronized (numberFormatLock) {
                numberFormat = NumericFormats.getFullCommodityFormat(account.get().getCurrencyNode());
                updateProperties();
            }
        };

        Options.fullNumericFormatProperty().addListener(new WeakChangeListener<>(formatListener));
        Options.shortNumericFormatProperty().addListener(new WeakChangeListener<>(formatListener));
        Options.shortDateFormatProperty().addListener(new WeakChangeListener<>(formatListener));
    }

    @Override
    public void messagePosted(final Message event) {
        switch (event.getEvent()) {
            case ACCOUNT_MODIFY:
            case TRANSACTION_ADD:
            case TRANSACTION_REMOVE:
                if (event.getObject(MessageProperty.ACCOUNT).equals(account.get())) {
                    updateProperties();
                }
                break;
            default:
        }
    }

    private void updateProperties() {
        if (account.get() != null) {
            JavaFXUtils.runLater(() -> accountName.set(account.get().getName()));
        } else {
            JavaFXUtils.runLater(() -> accountName.set(""));
        }

        executorService.submit(new Task<Void>() {
            @Override
            protected Void call() {
                synchronized (numberFormatLock) {
                    if (account.get() != null) {
                        final Account acc = AccountPropertyWrapper.this.account.get();
                        final BigDecimal balance =
                                AccountBalanceDisplayManager.convertToSelectedBalanceMode(acc.getAccountType(),
                                        acc.getBalance());

                        JavaFXUtils.runLater(() -> accountBalance.set(numberFormat.format(balance)));
                    } else {
                        JavaFXUtils.runLater(() -> accountBalance.set(numberFormat.format(BigDecimal.ZERO)));
                    }
                }
                return null;
            }
        });

        executorService.submit(new Task<Void>() {
            @Override
            protected Void call() {
                synchronized (numberFormatLock) {
                    if (account.get() != null) {
                        final Account acc = AccountPropertyWrapper.this.account.get();
                        final BigDecimal balance =
                                AccountBalanceDisplayManager.convertToSelectedBalanceMode(acc.getAccountType(),
                                        acc.getReconciledBalance());

                        JavaFXUtils.runLater(() -> reconciledAmount.set(numberFormat.format(balance)));
                    } else {
                        JavaFXUtils.runLater(() -> reconciledAmount.set(numberFormat.format(BigDecimal.ZERO)));
                    }
                }
                return null;
            }
        });

        if (account.get() != null && account.get().memberOf(AccountGroup.INVEST)) {
            executorService.submit(new Task<Void>() {
                @Override
                protected Void call() {
                    synchronized (numberFormatLock) {
                        if (account.get() != null) {
                            final Account acc = AccountPropertyWrapper.this.account.get();
                            final BigDecimal balance =
                                    AccountBalanceDisplayManager.convertToSelectedBalanceMode(acc.getAccountType(),
                                            acc.getCashBalance());

                            JavaFXUtils.runLater(() -> cashBalance.set(numberFormat.format(balance)));
                        } else {
                            JavaFXUtils.runLater(() -> cashBalance.set(numberFormat.format(BigDecimal.ZERO)));
                        }
                    }
                    return null;
                }
            });

            executorService.submit(new Task<Void>() {
                @Override
                protected Void call() {
                    synchronized (numberFormatLock) {
                        if (account.get() != null) {
                            JavaFXUtils.runLater(() -> marketValue.set(numberFormat.format(account.get().getMarketValue())));
                        } else {
                            JavaFXUtils.runLater(() -> marketValue.set(numberFormat.format(BigDecimal.ZERO)));
                        }
                    }
                    return null;
                }
            });
        }
    }

    ReadOnlyStringProperty reconciledAmountProperty() {
        return reconciledAmount.getReadOnlyProperty();
    }

    ReadOnlyStringProperty accountBalanceProperty() {
        return accountBalance.getReadOnlyProperty();
    }

    ReadOnlyStringProperty cashBalanceProperty() {
        return cashBalance.getReadOnlyProperty();
    }

    ReadOnlyStringProperty marketValueProperty() {
        return marketValue.getReadOnlyProperty();
    }

    ReadOnlyStringProperty accountNameProperty() {
        return accountName.getReadOnlyProperty();
    }

    public ObjectProperty<Account> accountProperty() {
        return account;
    }
}
