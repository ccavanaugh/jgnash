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
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
public class CheckComboBox<T> extends ComboBox<CheckComboBox.CheckBoxWrapper<T>> {

    public CheckComboBox() {

        setCellFactory(new Callback<>() {
            @Override
            public ListCell<CheckBoxWrapper<T>> call(ListView<CheckBoxWrapper<T>> param) {

                final ListCell<CheckBoxWrapper<T>> cell = new ListCell<>() {
                    @Override
                    protected void updateItem(final CheckBoxWrapper<T> item, boolean empty) {
                        super.updateItem(item, empty);

                        if (!empty) {
                            final CheckBox checkBox = new CheckBox(item.toString());
                            checkBox.selectedProperty().bind(item.checkProperty());
                            setGraphic(checkBox);
                        }
                    }
                };

                cell.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
                    cell.getItem().checkProperty().set(!cell.getItem().checkProperty().get());
                    JavaFXUtils.runLater(CheckComboBox.this::updatePromptText);
                });

                return cell;
            }
        });

        getItems().addListener((ListChangeListener<CheckBoxWrapper<T>>) c -> JavaFXUtils.runLater(CheckComboBox.this::updatePromptText));
    }

    public void add(T item, boolean check) {
        getItems().add(new CheckBoxWrapper<>(item, check));
    }

    public List<T> getCheckedItems() {
        final List<T> checkedItems = new ArrayList<>();

        for (final CheckBoxWrapper<T> checkBoxWrapper : getItems()) {
            if (checkBoxWrapper.getCheck()) {
                checkedItems.add(checkBoxWrapper.getItem());
            }
        }

        return checkedItems;
    }

    public void setChecked(T item, boolean check) {
        for (final CheckBoxWrapper<T> checkBoxWrapper : getItems()) {
            if (checkBoxWrapper.getItem().equals(item)) {
                checkBoxWrapper.setCheck(check);
            }
        }
    }

    public void setAllChecked() {
        for (final CheckBoxWrapper<T> checkBoxWrapper : getItems()) {
            checkBoxWrapper.setCheck(true);
        }
    }

    private void updatePromptText() {
        final StringBuilder sb = new StringBuilder();
        getItems().filtered(CheckBoxWrapper::getCheck).forEach(p -> sb.append(", ").append(p.getItem()));

        final String string = sb.substring(Integer.min(2, sb.length()));
        setPromptText(string);
        setTooltip(new Tooltip(string));
    }

     public static class CheckBoxWrapper<T> {

        private BooleanProperty check = new SimpleBooleanProperty(false);
        private ObjectProperty<T> item = new SimpleObjectProperty<>();

        CheckBoxWrapper(final T item, final boolean check) {
            this.item.set(item);
            this.check.set(check);
        }

        BooleanProperty checkProperty() {
            return check;
        }

        boolean getCheck() {
            return check.getValue();
        }

        void setCheck(boolean value) {
            check.set(value);
        }

        public T getItem() {
            return item.getValue();
        }

        @Override
        public String toString() {
            return item.getValue().toString();
        }
    }
}
