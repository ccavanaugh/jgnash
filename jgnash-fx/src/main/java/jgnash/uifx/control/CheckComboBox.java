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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

import jgnash.uifx.util.JavaFXUtils;

/**
 * ComboBox of checked items
 *
 * @author Craig Cavanaugh
 */
public class CheckComboBox<T> extends ComboBox<T> {

    private Map<T, BooleanProperty> checkMap = new HashMap<>();

    public CheckComboBox() {

        setCellFactory(new Callback<>() {
            @Override
            public ListCell<T> call(ListView<T> param) {
                final ListCell<T> cell = new ListCell<>() {
                    @Override
                    protected void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);

                        if (!empty) {
                            final CheckBox checkBox = new CheckBox(item.toString());

                            final BooleanProperty booleanProperty =
                                    checkMap.computeIfAbsent(item, t -> new SimpleBooleanProperty(true));

                            checkBox.selectedProperty().bind(booleanProperty);
                            setGraphic(checkBox);
                        }
                    }
                };

                // toggle the value
                cell.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
                    checkMap.get(cell.getItem()).setValue(!checkMap.get(cell.getItem()).get());
                    JavaFXUtils.runLater(CheckComboBox.this::updatePromptText);
                });

                return cell;
            }
        });

        getItems().addListener((ListChangeListener<T>) c -> JavaFXUtils.runLater(CheckComboBox.this::updatePromptText));
    }

    public void add(T item, boolean check) {
        checkMap.put(item, new SimpleBooleanProperty(check));
        getItems().add(item);
    }

    public List<T> getCheckedItems() {
        final List<T> checkedItems = new ArrayList<>();

        for (final T item : getItems()) {
            if (checkMap.getOrDefault(item, new SimpleBooleanProperty(false)).get()) {
                checkedItems.add(item);
            }
        }

        return checkedItems;
    }

    public void setChecked(T item, boolean check) {
        checkMap.getOrDefault(item, new SimpleBooleanProperty(check)).setValue(check);
    }

    public void setAllChecked() {
        checkMap.forEach((t, booleanProperty) -> booleanProperty.setValue(true));
    }

    private void updatePromptText() {
        final StringBuilder sb = new StringBuilder();

        getItems().filtered(t -> checkMap.getOrDefault(t, new SimpleBooleanProperty(false)).get())
                .forEach(t -> sb.append(", ").append(t.toString()));

        final String string = sb.substring(Integer.min(2, sb.length()));
        setPromptText(string);
        setTooltip(new Tooltip(string));
    }
}
