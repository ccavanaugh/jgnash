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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.MouseEvent;

import jgnash.uifx.util.JavaFXUtils;

/**
 * ComboBox of checked items
 *
 * @author Craig Cavanaugh
 */
public class CheckComboBox<T> extends ComboBox<T> {

    private final Map<T, BooleanProperty> itemBooleanPropertyMap = new HashMap<>();

    private BooleanProperty getItemBooleanProperty(final T item) {
        itemBooleanPropertyMap.putIfAbsent(item, new SimpleBooleanProperty());
        return itemBooleanPropertyMap.get(item);
    }

    public CheckComboBox() {

        setCellFactory(param -> {
            final ListCell<T> cell = new CheckBoxListCell<>(CheckComboBox.this::getItemBooleanProperty);

            // toggle the value
            cell.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
                itemBooleanPropertyMap.get(cell.getItem()).setValue(!itemBooleanPropertyMap.get(cell.getItem()).get());
                JavaFXUtils.runLater(CheckComboBox.this::updatePromptText);
            });

            return cell;
        });

        getItems().addListener((ListChangeListener<T>) c -> JavaFXUtils.runLater(CheckComboBox.this::updatePromptText));
    }

    public void add(T item, boolean check) {
        getItems().add(item);
        getItemBooleanProperty(item).setValue(check);
    }

    public List<T> getCheckedItems() {
        final List<T> checkedItems = new ArrayList<>();

        for (final T item : getItems()) {
            if (itemBooleanPropertyMap.getOrDefault(item, new SimpleBooleanProperty(false)).get()) {
                checkedItems.add(item);
            }
        }

        return checkedItems;
    }

    public void setChecked(T item, boolean check) {
        getItemBooleanProperty(item).setValue(check);
    }

    public void setAllChecked() {
        for (final T item : getItems()) {
            getItemBooleanProperty(item).set(true);
        }
    }

    private void updatePromptText() {
        final StringBuilder sb = new StringBuilder();

        getItems().filtered(t -> itemBooleanPropertyMap.getOrDefault(t, new SimpleBooleanProperty(false)).get())
                .forEach(t -> sb.append(", ").append(t.toString()));

        final String string = sb.substring(Integer.min(2, sb.length()));
        setPromptText(string);
        setTooltip(new Tooltip(string));
    }
}
