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

import java.util.Objects;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.control.ComboBox;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.util.OS;
import jgnash.util.ResourceUtils;

/**
 * Enhanced ComboBox; provides a UI to the user for entering transaction
 * numbers in a consistent manner.
 *
 * @author Craig Cavanaugh
 */
public class TransactionNumberComboBox extends ComboBox<String> {

    private final String nextNumberItem;

    final private ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    /**
     * Reference is needed to prevent premature garbage collection
     */
    @SuppressWarnings("FieldCanBeLocal")
    private ChangeListener<Boolean> focusChangeListener = null;

    public TransactionNumberComboBox() {
        super();

        ResourceBundle rb = ResourceUtils.getBundle();

        nextNumberItem = rb.getString("Item.NextNum");
        String[] defaultItems = new String[]{"", nextNumberItem, rb.getString("Item.Print")};

        getItems().addAll(defaultItems);

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        getItems().addAll(engine.getTransactionNumberList());

        setEditable(true);

        valueProperty().addListener((observable, oldValue, newValue) -> {
            new Thread(() -> {
                if (nextNumberItem.equals(newValue)) {
                    final Account account = accountProperty().getValue();

                    if (account != null) {
                        Platform.runLater(() -> setValue(account.getNextTransactionNumber()));
                    }
                }
            }).start();
        });

        // TODO: This is a workaround for a Java Bug that should be fixed in 8u72
        if (OS.getJavaVersion() < 1.9 && OS.getJavaRelease() < OS.JVM_RELEASE_72) {

            focusChangeListener = (observable, oldValue, isFocused) -> {
                if (!isFocused) {
                    if (getValue() != null && !getValue().equals(getConverter().fromString(getEditor().getText()))) {
                        setValue(getConverter().fromString(getEditor().getText()));
                    }
                }
            };

            getEditor().focusedProperty().addListener(new WeakChangeListener<>(focusChangeListener));
        }
    }

    public ObjectProperty<Account> accountProperty() {
        return accountProperty;
    }
}
