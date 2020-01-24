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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;

/**
 * CheckListView control
 *
 * @author Craig Cavanaugh
 */
public class CheckListView<T> extends ListView<T> {

    private final Map<T, BooleanProperty> itemMap = new HashMap<>();

    public CheckListView() {
        super(FXCollections.observableArrayList());

        setCellFactory(listView -> new CheckBoxListCell<>(this::getItemBooleanProperty));
    }

    private BooleanProperty getItemBooleanProperty(final T item) {

        itemMap.putIfAbsent(item, new SimpleBooleanProperty());

        return itemMap.get(item);
    }

    public List<T> getCheckedItems() {
        final List<T> checkedItems = new ArrayList<>();

        for (final T item : getItems()) {
            if (getItemBooleanProperty(item).get()) {
                checkedItems.add(item);
            }
        }

        return checkedItems;
    }

    public void clearChecks() {
        for (final T item : getItems()) {
            getItemBooleanProperty(item).set(false);
        }
    }

    public void checkAll() {
        for (final T item : getItems()) {
            getItemBooleanProperty(item).set(true);
        }
    }

    public void toggleAll() {
        for (final T item : getItems()) {
            getItemBooleanProperty(item).set(!getItemBooleanProperty(item).get());
        }
    }
}
