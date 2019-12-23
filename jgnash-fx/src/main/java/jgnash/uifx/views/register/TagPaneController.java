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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Tag;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.resource.font.FontAwesomeLabel;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Controller for managing the Transaction tags pane
 *
 * @author Craig Cavanaugh
 */
public class TagPaneController extends GridPane {

    public static final double ICON_SCALE = 1.2;

    private final Set<Tag> selectedTags = new HashSet<>();

    @FXML
    private Button selectTagsButton;

    @FXML
    private HBox tagBox;

    public TagPaneController() {
        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TransactionTagPane.fxml"),
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
        tagBox.setId("tag-pane");

        selectTagsButton.setOnAction(event -> showPopup());

        // disable the button if there are not any tags
        new Thread(() -> {
            Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            if (engine != null) {
                Set<Tag> tags = engine.getTags();

                if (tags.isEmpty()) {
                    JavaFXUtils.runLater(() -> selectTagsButton.setDisable(true));
                }
            }
        }).start();
    }

    void clearSelectedTags() {
        selectedTags.clear();
        JavaFXUtils.runLater(this::refreshTagView);
    }

    void setSelectedTags(final Collection<Tag> tags) {
        selectedTags.clear();
        selectedTags.addAll(tags);

        JavaFXUtils.runLater(this::refreshTagView);
    }

    Set<Tag> getSelectedTags() {
        return selectedTags;
    }

    private void showPopup() {
        final FXMLUtils.Pair<TransactionTagDialogController> pair =
                FXMLUtils.load(TransactionTagDialogController.class.getResource("TransactionTagDialog.fxml"),
                        ResourceUtils.getString("Title.SelTransTags"));

        final TransactionTagDialogController controller = pair.getController();

        controller.setSelectedTags(selectedTags);

        pair.getStage().showAndWait();

        selectedTags.clear();
        selectedTags.addAll(controller.getSelectedTags());

        JavaFXUtils.runLater(this::refreshTagView);
    }

    void refreshTagView() {
        tagBox.getChildren().clear();

        // the the icons / labels
        for (final Tag tag : selectedTags) {
            tagBox.getChildren().add(new Label(tag.getName(), FontAwesomeLabel.fromInteger(tag.getShape(),
                    FontAwesomeLabel.DEFAULT_SIZE * ICON_SCALE, tag.getColor())));
        }
    }
}
