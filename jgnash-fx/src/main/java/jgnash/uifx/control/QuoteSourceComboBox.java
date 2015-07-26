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

import java.io.IOException;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;

import jgnash.engine.QuoteSource;

/**
 * ComboBox that allows selection of a {@code QuoteSource}
 *
 * @author Craig Cavanaugh
 */
public class QuoteSourceComboBox extends ComboBox<QuoteSource> {

    public QuoteSourceComboBox() {
        final FXMLLoader loader = new FXMLLoader(getClass().getResource("QuoteSourceComboBox.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        setEditable(false);

        Platform.runLater(this::loadModel); // lazy load to let the ui build happen faster
    }

    private void loadModel() {
        // extract and reuse the default model
        ObservableList<QuoteSource> items = getItems();

        // warp in a sorted list
        setItems(new SortedList<>(items, null));

        items.addAll(QuoteSource.values());

        setValue(QuoteSource.YAHOO);
    }
}
