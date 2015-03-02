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
package jgnash.uifx.control;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.util.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * ComboBox of available accounts
 *
 * @author Craig Cavanaugh
 */
public class AccountComboBox extends ComboBox<Account> implements MessageListener {

    final private ObservableList<Account> filteredAccountList = FXCollections.observableArrayList();

    final private SimpleBooleanProperty showHiddenAccountsProperty = new SimpleBooleanProperty(false);

    final private SimpleBooleanProperty showLockedAccountsProperty = new SimpleBooleanProperty(false);

    final private SimpleBooleanProperty showPlaceHoldersProperty = new SimpleBooleanProperty(false);

    public AccountComboBox() {

        setButtonCell(new AccountPathListCell());
        setCellFactory(param -> new AccountPathListCell());

        loadAccounts();
        registerListeners();
    }

    public void filterAccount(@NotNull final Account... account) {
        filteredAccountList.addAll(account);
    }

    public SimpleBooleanProperty getShowHiddenAccountsProperty() {
        return showHiddenAccountsProperty;
    }

    public SimpleBooleanProperty getShowLockedAccountsProperty() {
        return showLockedAccountsProperty;
    }

    public SimpleBooleanProperty getShowPlaceHoldersProperty() {
        return showPlaceHoldersProperty;
    }

    private void loadAccounts(@NotNull final List<Account> accounts) {
        accounts.stream().filter(account -> !filteredAccountList.contains(account)).forEach(account -> {

            if (account.isVisible() && !account.isPlaceHolder() && !account.isLocked()) {
                getItems().add(account);
            } else if (account.isPlaceHolder() && getShowPlaceHoldersProperty().get()) {
                getItems().add(account);
            } else if (!account.isVisible() && getShowHiddenAccountsProperty().get()) {
                getItems().add(account);
            } else if (account.isLocked() && getShowLockedAccountsProperty().get()) {
                getItems().add(account);
            }

            if (account.getChildCount() > 0) {
                loadAccounts(account.getChildren(Comparators.getAccountByCode()));
            }
        });
    }

    private void loadAccounts() {
        getItems().clear();

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        loadAccounts(engine.getRootAccount().getChildren(Comparators.getAccountByCode()));

        // Set a default account
        if (getItems().size() > 0) {
            setValue(getItems().get(0));
        }
    }

    private void registerListeners() {
        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT, MessageChannel.SYSTEM);

        filteredAccountList.addListener((ListChangeListener<Account>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    loadAccounts();
                }
            }
        });

        showPlaceHoldersProperty.addListener((observable, oldValue, newValue) -> {
            loadAccounts();
        });

        showHiddenAccountsProperty.addListener((observable, oldValue, newValue) -> {
            loadAccounts();
        });
    }

    @Override
    public void messagePosted(Message event) {
        Platform.runLater(() -> {
            switch (event.getEvent()) {
                case FILE_CLOSING:
                    getItems().clear();
                    break;
                case ACCOUNT_REMOVE:
                    getItems().removeAll((Account) event.getObject(MessageProperty.ACCOUNT));
                    filteredAccountList.removeAll((Account) event.getObject(MessageProperty.ACCOUNT));
                    break;
                case ACCOUNT_ADD:
                case ACCOUNT_MODIFY:
                    loadAccounts();
                    break;
                default:
                    break;
            }
        });
    }

    /**
     * Display the full pathname of the account and not just the name
     */
    private static class AccountPathListCell extends ListCell<Account> {
        @Override
        protected void updateItem(final Account item, final boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(item.getPathName());
            }
        }
    }
}
