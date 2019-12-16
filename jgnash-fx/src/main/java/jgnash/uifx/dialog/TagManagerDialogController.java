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
package jgnash.uifx.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.stage.Stage;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Tag;
import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.TextInputDialog;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Tag Manager dialog controller
 *
 * @author Craig Cavanaugh
 */
public class TagManagerDialogController implements MessageListener {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private Button duplicateButton;

    @FXML
    private Button renameButton;

    @FXML
    private Button deleteButton;

    @FXML
    private ListView<Tag> tagListView;

    @FXML
    private ResourceBundle resources;

    @FXML
    private void initialize() {
        deleteButton.disableProperty().bind(tagListView.getSelectionModel().selectedItemProperty().isNull());
        duplicateButton.disableProperty().bind(tagListView.getSelectionModel().selectedItemProperty().isNull());
        renameButton.disableProperty().bind(tagListView.getSelectionModel().selectedItemProperty().isNull());

        tagListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        MessageBus.getInstance().registerListener(this, MessageChannel.TAG);

        JavaFXUtils.runLater(this::loadTagListView);
    }

    @FXML
    private void handleNewAction() {
        final Tag tag = new Tag();
        tag.setName(resources.getString("Word.NewTag"));

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);
        engine.addTag(tag);
    }

    @FXML
    private void handleDuplicateAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        for (final Tag tag : tagListView.getSelectionModel().getSelectedItems()) {
            try {
                final Tag newTag = (Tag) tag.clone();
                if (!engine.addTag(newTag)) {
                    StaticUIMethods.displayError(resources.getString("Message.Error.TagDuplicate"));
                }
            } catch (final CloneNotSupportedException e) {
                Logger.getLogger(TagManagerDialogController.class.getName()).log(Level.SEVERE, e.toString(), e);
            }
        }
    }

    @FXML
    private void handleRenameAction() {
        for (final Tag tag : tagListView.getSelectionModel().getSelectedItems()) {

            final TextInputDialog textInputDialog = new TextInputDialog(tag.getName());
            textInputDialog.setTitle(resources.getString("Title.RenameTag"));
            textInputDialog.setContentText(resources.getString("Label.RenameTag"));

            final Optional<String> optional = textInputDialog.showAndWait();

            optional.ifPresent(s -> {
                if (!s.isEmpty()) {
                    final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                    Objects.requireNonNull(engine);

                    tag.setName(s);
                    engine.updateTag(tag);
                }
            });
        }
    }

    @FXML
    private void handleDeleteAction() {
        final List<Tag> selected = new ArrayList<>(tagListView.getSelectionModel().getSelectedItems());

        if (selected.size() > 0) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            final Set<Tag> usedTags = engine.getTagsInUse();

            for (final Tag tag : selected) {
                if (!usedTags.contains(tag)) {
                    engine.removeTag(tag);
                }
            }
        }
    }

    @FXML
    private void handleCloseAction() {
        MessageBus.getInstance().unregisterListener(this, MessageChannel.TAG);
        ((Stage)parent.get().getWindow()).close();
    }

    @Override
    public void messagePosted(final Message message) {
        if (message.getEvent() == ChannelEvent.TAG_ADD || message.getEvent() == ChannelEvent.TAG_REMOVE
                    || message.getEvent() == ChannelEvent.TAG_MODIFY) {
            JavaFXUtils.runLater(this::loadTagListView);
        }
    }

    private void loadTagListView() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final List<Tag> tagList = new ArrayList<>(engine.getTags());
        Collections.sort(tagList);

        tagListView.getItems().setAll(tagList);
    }

    public static void showTagManager() {
        final FXMLUtils.Pair<TagManagerDialogController> pair =
                FXMLUtils.load(TagManagerDialogController.class.getResource("TagManagerDialog.fxml"),
                        ResourceUtils.getString("Title.TagManager"));

        pair.getStage().show();
        pair.getStage().setResizable(false);
    }
}
