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
package jgnash.uifx.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ComboBox;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.SecurityNode;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.uifx.util.JavaFXUtils;

/**
 * ComboBox that allows selection of a SecurityNode and manages it's own model.
 * <p>
 * The default operation is to load all known {@code SecurityNodes}.  If the
 * {@code accountProperty} is set, then only the account's {@code SecurityNodes}
 * will be available for selection.
 *
 * @author Craig Cavanaugh
 */
public class SecurityComboBox extends ComboBox<SecurityNode> implements MessageListener {

    /**
     * Model for the ComboBox.
     */
    final private ObservableList<SecurityNode> items;

    final private ObjectProperty<Account> account = new SimpleObjectProperty<>();

    public SecurityComboBox() {

        // extract and reuse the default model
        items = getItems();

        // warp in a sorted list
        setItems(new SortedList<>(items, null));

        JavaFXUtils.runLater(this::loadModel); // lazy load to let the ui build happen faster

        account.addListener((observable, oldValue, newValue) -> loadModel());

        MessageBus.getInstance().registerListener(this, MessageChannel.ACCOUNT, MessageChannel.COMMODITY, MessageChannel.SYSTEM);
    }

    public void setSecurityNode(final SecurityNode securityNode) {
        JavaFXUtils.runLater(() -> setValue(securityNode));
    }

    private void loadModel() {
        final Collection<SecurityNode> securityNodes;

        if (account.get() != null) {
            items.clear();
            securityNodes = account.get().getSecurities();
        } else {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);
            securityNodes = engine.getSecurities();
        }

        if (!securityNodes.isEmpty()) {
            final List<SecurityNode> sortedNodeList = new ArrayList<>(securityNodes);
            Collections.sort(sortedNodeList);

            items.addAll(sortedNodeList);
            getSelectionModel().select(0);
        }
    }

    @Override
    public void messagePosted(final Message event) {
        if (event.getObject(MessageProperty.COMMODITY) instanceof SecurityNode) {

            final SecurityNode node = event.getObject(MessageProperty.COMMODITY);
            final Account account = event.getObject(MessageProperty.ACCOUNT);

            JavaFXUtils.runLater(() -> {
                switch (event.getEvent()) {
                    case ACCOUNT_SECURITY_ADD:
                        if (account != null && account.equals(this.account.get())) {
                            final int index = Collections.binarySearch(items, node);

                            if (index < 0) {
                                items.add(-index -1, node);
                            }
                        }
                        break;
                    case ACCOUNT_SECURITY_REMOVE:
                        if (account != null && account.equals(this.account.get())) {
                            items.removeAll(node);
                        }
                        break;
                    case SECURITY_REMOVE:
                        items.removeAll(node);
                        break;
                    case SECURITY_ADD:
                        final int index = Collections.binarySearch(items, node);

                        if (index < 0) {
                            items.add(-index -1, node);
                        }
                        break;
                    case SECURITY_MODIFY:
                        items.removeAll(node);

                        final int i = Collections.binarySearch(items, node);

                        if (i < 0) {
                            items.add(-i - 1, node);
                        }
                        break;
                    case FILE_CLOSING:
                        items.clear();
                    default:
                        break;
                }
            });
        }
    }

    public ObjectProperty<Account> accountProperty() {
        return account;
    }
}
