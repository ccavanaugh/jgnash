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

import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ComboBox;

import jgnash.engine.DataStoreType;
import jgnash.uifx.util.JavaFXUtils;

/**
 * ComboBox that allows selection of a CurrencyNode and manages it's own model.
 *
 * @author Craig Cavanaugh
 */
public class DataStoreTypeComboBox extends ComboBox<DataStoreType> {

    public DataStoreTypeComboBox() {
        setEditable(false);

        JavaFXUtils.runLater(this::loadModel); // lazy load to let the ui build happen faster
    }

    private void loadModel() {
        // extract and reuse the default model
        ObservableList<DataStoreType> items = getItems();

        // warp in a sorted list
        setItems(new SortedList<>(items, null));

        items.addAll(DataStoreType.values());

        setValue(DataStoreType.H2_DATABASE);
    }
}
