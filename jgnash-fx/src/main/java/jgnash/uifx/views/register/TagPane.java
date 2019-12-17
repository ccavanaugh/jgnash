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
package jgnash.uifx.views.register;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import jgnash.engine.Tag;
import jgnash.engine.TransactionEntry;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.control.PopOverButton;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Controller for managing Transaction tags
 */
public class TagPane extends GridPane {

    private final SimpleListProperty<TransactionEntry> transactionEntries =
            new SimpleListProperty<>(FXCollections.observableArrayList());

    private ListChangeListener<TransactionEntry> listChangeListener = c -> refreshTagView();

    @FXML
    private Button selectTagsButton;

    @FXML
    private HBox tagBox;

    public TagPane() {
        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TagPane.fxml"),
                ResourceUtils.getBundle());

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void initialize() {
        selectTagsButton.setOnAction(event -> handleTagSelection());
        transactionEntries.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.removeListener(listChangeListener);
            }

            if (newValue != null) {
                newValue.addListener(listChangeListener);
            }
        });
    }

    void setTransactionEntries(final ObservableList<TransactionEntry> entries) {
        transactionEntries.set(entries);

        JavaFXUtils.runLater(this::refreshTagView);
    }

    void refreshTagView() {
        System.out.println("refresh tag view");

        final Set<Tag> tagSet = new HashSet<>();

        for (final TransactionEntry entry : transactionEntries.get()) {
            tagSet.addAll(entry.getTags());
        }

        tagBox.getChildren().clear();

        for (final Tag tag : tagSet) {
            tagBox.getChildren().add(new Label(tag.getName()));
        }
    }


    private void handleTagSelection() {
        // display a tag selection dialog

    }
}
