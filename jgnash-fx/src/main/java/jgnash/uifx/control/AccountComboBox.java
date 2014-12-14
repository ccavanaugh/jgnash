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
package jgnash.uifx.control;

import java.util.List;
import java.util.Objects;

import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;

/**
 * ComboBox of available accounts
 *
 * @author Craig Cavanaugh
 */
public class AccountComboBox extends ComboBox<Account> implements MessageListener {

    public AccountComboBox() {
        loadAccounts();

        registerListeners();
    }

    private void loadAccounts(final List<Account> accounts) {
        for (Account account : accounts) {
            getItems().add(account);

            if (account.getChildCount() > 0) {
                loadAccounts(account.getChildren(Comparators.getAccountByCode()));
            }
        }
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
}
