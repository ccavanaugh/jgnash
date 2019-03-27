/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
import java.util.function.Predicate;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

import jgnash.engine.Account;
import jgnash.engine.Comparators;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.util.NotNull;

/**
 * ComboBox of available accounts.  A Predicate for allowed accounts may be specified.
 *
 * @author Craig Cavanaugh
 *         <p>
 *         <a href="https://bugs.openjdk.java.net/browse/JDK-8129123">Bug JDK-8129123</a>
 */
public class AccountComboBox extends ComboBox<Account> implements MessageListener {

    final private FilteredList<Account> filteredList;

    final private ObservableList<Account> items;

    private ListView<Account> listView;

    public AccountComboBox() {

        items = getItems();

        // By default only visible accounts that are not locked or placeholders are shown
        filteredList = new FilteredList<>(items, getDefaultPredicate());

        setItems(filteredList);

        loadAccounts();

        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT, MessageChannel.SYSTEM);

        setOnMouseClicked(event -> forceScrollSelectionBugWorkAround());

        setOnKeyTyped(new KeyHandler());

        final StringConverter<Account> accountStringConverter = new StringConverter<>() {
            @Override
            public String toString(final Account account) {
                return account == null ? null : account.getPathName();
            }

            @Override
            public Account fromString(final String string) {
                for (final Account account : filteredList) {
                    if (account.getPathName().equals(string)) {
                        return account;
                    }
                }
                return null;
            }
        };

        // cell factory for capturing the ListView needed for addressing JDK Bug JDK-8129123
        setCellFactory(param -> {
            final ComboBoxListCell<Account> comboBoxListCell = new ComboBoxListCell<>();

            comboBoxListCell.listViewProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    listView = newValue;
                }
            });

            comboBoxListCell.setConverter(accountStringConverter);

            return comboBoxListCell;
        });

        // display the full account path instead of the name
        setConverter(accountStringConverter);
    }

    /**
     * Returns the default Predicate used to determine which Accounts are displayed.
     * <p>
     * The default is only visible accounts that are not locked or placeholders are shown.
     *
     * @return default Account Predicate
     */
    public static Predicate<Account> getDefaultPredicate() {
        return account -> account.isVisible() && !account.isLocked() && !account.isPlaceHolder();
    }

    /**
     * Returns a Predicate used to display all accounts.
     *
     * @return Account Predicate
     */
    public static Predicate<Account> getShowAllPredicate() {
        return account -> true;
    }

    // TODO: JDK Bug JDK-8129123 https://bugs.openjdk.java.net/browse/JDK-8129123
    private void forceScrollSelectionBugWorkAround() {
        if (listView != null) {
            listView.scrollTo(getSelectionModel().getSelectedIndex());
        }
    }

    public ObservableList<Account> getUnfilteredItems() {
        return items;
    }

    public void setPredicate(final Predicate<Account> predicate) {
        filteredList.setPredicate(predicate);

        // force an update if the filtered list does not contain the current value
        if (!filteredList.contains(getValue())) {
            selectDefaultAccount();
        }
    }

    private void selectDefaultAccount() {
        // Set a default account, must use the filtered list because that is what is visible
        if (filteredList.size() > 0) {
            if (Platform.isFxApplicationThread()) {
                setValue(filteredList.get(0));
            } else {    // push to the end of the application thread only if needed
                Platform.runLater(() -> setValue(filteredList.get(0)));
            }
        }
    }

    /**
     * A decorator around {@link ComboBox#setValue(Object)} that ensures only Accounts available within this ComboBox
     * are set.
     *
     * @param value Account to set
     */
    public final void setAccountValue(final Account value) {
        if (value == null || filteredList.contains(value)) {
            setValue(value);
        }
    }

    private void loadAccounts(@NotNull final List<Account> accounts) {
        accounts.forEach(account -> {
            getUnfilteredItems().add(account);

            if (account.getChildCount() > 0) {
                loadAccounts(account.getChildren(Comparators.getAccountByCode()));
            }
        });
    }

    private void loadAccounts() {
        getUnfilteredItems().clear();

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        loadAccounts(engine.getRootAccount().getChildren(Comparators.getAccountByCode()));

        selectDefaultAccount();
    }

    @Override
    public void messagePosted(final Message event) {

        // unregister immediately
        if (event.getEvent() == ChannelEvent.FILE_CLOSING) {
            MessageBus.getInstance().unregisterListener(this, MessageChannel.ACCOUNT, MessageChannel.SYSTEM);
        }

        JavaFXUtils.runLater(() -> {
            switch (event.getEvent()) {
                case FILE_CLOSING:
                    items.clear();
                    break;
                case ACCOUNT_REMOVE:
                    items.removeAll((Account) event.getObject(MessageProperty.ACCOUNT));
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

    private class KeyHandler implements EventHandler<KeyEvent> {

        @Override
        public void handle(final KeyEvent event) {
            int current = getSelectionModel().getSelectedIndex();

            if (current < 0 || current > filteredList.size()) {
                current = 0;
            }

            int loop = current;

            do {
                loop++; //move to next item
                if (loop > filteredList.size() - 1) {
                    loop = 0;
                }

                final Account item = filteredList.get(loop);
                if (item.toString().toUpperCase().startsWith(event.getCharacter().toUpperCase())) {
                    getSelectionModel().select(item);
                    break;
                }
            } while (loop != current);

            forceScrollSelectionBugWorkAround();
        }
    }
}
