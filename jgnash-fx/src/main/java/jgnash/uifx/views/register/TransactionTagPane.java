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

import java.io.IOException;
import java.util.Collection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Tag;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.resource.font.MaterialDesignLabel;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Controller for managing the Transaction tags pane
 *
 * @author Craig Cavanaugh
 */
public class TransactionTagPane extends GridPane implements MessageListener {

    public static final double ICON_SCALE = 1.2;

    private final ObservableSet<Tag> selectedTags = FXCollections.observableSet();

    @FXML
    private Button selectTagsButton;

    @FXML
    private HBox tagBox;

    public TransactionTagPane() {
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

        updateVisibility();

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM, MessageChannel.TAG);
    }

    void setReadOnly(boolean readOnly) {
        selectTagsButton.setDisable(readOnly);
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

    ObservableSet<Tag> getSelectedTags() {
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
            final Label label  = new Label("", MaterialDesignLabel.fromInteger(tag.getShape(),
                    MaterialDesignLabel.DEFAULT_SIZE * ICON_SCALE, tag.getColor()));
            label.setTooltip(new Tooltip(tag.getName()));
            tagBox.getChildren().add(label);
        }
    }

    private void updateVisibility() {
        new Thread(() -> {
            Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            if (engine != null) {
                JavaFXUtils.runLater(() -> selectTagsButton.setVisible(!engine.getTags().isEmpty()));
            }
        }).start();
    }

    @Override
    public void messagePosted(final Message message) {
        switch (message.getEvent()) {
            case TAG_ADD:
            case TAG_REMOVE:
                updateVisibility();
                break;
            case TAG_MODIFY:
                JavaFXUtils.runLater(this::refreshTagView);
                break;
            case FILE_CLOSING:
                MessageBus.getInstance().unregisterListener(this, MessageChannel.SYSTEM, MessageChannel.TAG);
            default:
        }
    }
}
