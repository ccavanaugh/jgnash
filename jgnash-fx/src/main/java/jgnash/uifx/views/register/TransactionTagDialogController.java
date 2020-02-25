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
package jgnash.uifx.views.register;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Tag;
import jgnash.uifx.resource.font.MaterialDesignLabel;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Allow tag selection for a transaction
 *
 * @author Craig Cavanaugh
 */
public class TransactionTagDialogController {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private TilePane tilePane;

    private final AtomicBoolean tagsLoaded = new AtomicBoolean(false);

    private final Set<Tag> originalTagSelection = new HashSet<>();

    private boolean okay = false;

    @FXML
    private void initialize() {
        tilePane.setId("tag-box");
        JavaFXUtils.runLater(this::loadTags);
    }

    @FXML
    private void handleCloseAction() {
        ((Stage)parent.get().getWindow()).close();
    }

    @FXML
    private void handleClearAllAction() {
        for (final Node node : tilePane.getChildren()) {
            ((CheckBox)node).setSelected(false);
        }
    }

    @FXML
    private void handleOkAction() {
        okay = true;
        handleCloseAction();
    }

    private void loadTags() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final List<Tag> tagList = new ArrayList<>(engine.getTags());
        Collections.sort(tagList);

        for (final Tag tag : tagList) {
            final CheckBox checkBox = new CheckBox(tag.getName());

            checkBox.setGraphic(MaterialDesignLabel.fromInteger(tag.getShape(),
                    MaterialDesignLabel.DEFAULT_SIZE * TransactionTagPane.ICON_SCALE, tag.getColor()));

            checkBox.setUserData(tag);
            TilePane.setAlignment(checkBox, Pos.BASELINE_LEFT);
            tilePane.getChildren().add(checkBox);
        }

        tagsLoaded.set(true);
    }

    void setSelectedTags(final Collection<Tag> tags) {

        originalTagSelection.addAll(tags);  // create a copy of the old selection

        JavaFXUtils.runLater(() -> {
            while (!tagsLoaded.get()) {
                Thread.onSpinWait();
            }

            for (final Node node : tilePane.getChildren()) {
                for (final Tag tag : tags) {
                    if (tag.equals(node.getUserData())) {
                        JavaFXUtils.runLater(() -> ((CheckBox)node).setSelected(true));
                    }
                }
            }
        });
    }

    Set<Tag> getSelectedTags() {
        if (!okay) {
            return originalTagSelection;
        }

        final Set<Tag> tagSet = new HashSet<>();

        for (final Node node : tilePane.getChildren()) {
            if ( ((CheckBox)node).isSelected()) {
              tagSet.add((Tag)node.getUserData());
            }
        }

        return tagSet;
    }
}
