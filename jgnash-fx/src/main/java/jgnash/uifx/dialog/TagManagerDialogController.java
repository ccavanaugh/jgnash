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
package jgnash.uifx.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;

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
import jgnash.uifx.control.TextFieldEx;
import jgnash.uifx.resource.font.MaterialDesignLabel;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.util.EncodeDecode;

import static jgnash.uifx.resource.font.MaterialDesignLabel.MDIcon;
import static jgnash.uifx.views.register.TransactionTagPane.ICON_SCALE;

/**
 * Tag Manager dialog controller
 *
 * @author Craig Cavanaugh
 */
public class TagManagerDialogController implements MessageListener {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private ColorPicker colorPicker;

    @FXML
    private Button saveButton;

    @FXML
    private TextArea descriptionTextArea;

    @FXML
    private TextFieldEx nameField;

    @FXML
    private ComboBox<MDIcon> iconCombo;

    @FXML
    private Button duplicateButton;

    @FXML
    private Button deleteButton;

    @FXML
    private ListView<Tag> tagListView;

    @FXML
    private ResourceBundle resources;

    private final Map<Tag, Boolean> lockedMap = new HashMap<>();
    private final BooleanProperty tagLocked = new SimpleBooleanProperty();

    @FXML
    private void initialize() {
        deleteButton.disableProperty().bind(tagListView.getSelectionModel().selectedItemProperty().isNull().or(tagLocked));
        duplicateButton.disableProperty().bind(tagListView.getSelectionModel().selectedItemProperty().isNull());
        nameField.disableProperty().bind(tagListView.getSelectionModel().selectedItemProperty().isNull());
        saveButton.disableProperty().bind(tagListView.getSelectionModel().selectedItemProperty().isNull());
        tagListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        iconCombo.setEditable(false);

        MessageBus.getInstance().registerListener(this, MessageChannel.TAG);

        JavaFXUtils.runLater(this::loadTagListView);

        JavaFXUtils.runLater(() -> tagListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                JavaFXUtils.runLater(() -> loadForm(newValue));
            }
        }));

        iconCombo.setCellFactory(new ListViewListCellCallback());
        iconCombo.setButtonCell(new FAIconListCell());

        // load the icons while filtering for only icons intended for use as Tags
        JavaFXUtils.runLater(() -> iconCombo.getItems().addAll(Arrays.stream(MDIcon.values())
                .filter(MDIcon::isTag).collect(Collectors.toList())));

        tagListView.setCellFactory(new TagListViewListCellCallback());

        // reset to default values
        handleResetAction();
    }

    private void loadForm(final Tag tag) {
        nameField.setText(tag.getName());
        descriptionTextArea.setText(tag.getDescription());
        colorPicker.setValue(Color.valueOf(EncodeDecode.longToColorString(tag.getColor())));

        // updates delete lock
        tagLocked.setValue(lockedMap.getOrDefault(tag, Boolean.FALSE));

        new Thread(() -> {
            final int unicode = tag.getShape();
            for (final MDIcon faIcon : MDIcon.values()) {
                if (unicode == faIcon.getUnicode()) {
                    JavaFXUtils.runLater(() -> iconCombo.setValue(faIcon));
                }
            }
        }).start();
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
    private void handleDeleteAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        engine.removeTag(tagListView.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void handleCloseAction() {
        MessageBus.getInstance().unregisterListener(this, MessageChannel.TAG);
        ((Stage) parent.get().getWindow()).close();
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

        lockedMap.clear();

        final List<Tag> tagList = new ArrayList<>(engine.getTags());
        Collections.sort(tagList);

        tagListView.getItems().setAll(tagList);

        final Set<Tag> usedTags = engine.getTagsInUse();
        for (final Tag tag : tagList) {
            lockedMap.put(tag, usedTags.contains(tag));
        }
    }

    public static void showTagManager() {
        final FXMLUtils.Pair<TagManagerDialogController> pair =
                FXMLUtils.load(TagManagerDialogController.class.getResource("TagManagerDialog.fxml"),
                        ResourceUtils.getString("Title.TagManager"));

        pair.getStage().show();
    }

    @FXML
    private void handleSaveAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final Tag tag = tagListView.getSelectionModel().getSelectedItem();

        if (tag != null) {
            tag.setName(nameField.getText());
            tag.setDescription(descriptionTextArea.getText());
            tag.setShape(iconCombo.getValue().getUnicode());
            tag.setColor(EncodeDecode.colorStringToLong(colorPicker.getValue().toString()));

            engine.updateTag(tag);
        }
    }

    @FXML
    private void handleResetAction() {
        JavaFXUtils.runLater(() -> {
            nameField.setText("");
            descriptionTextArea.setText("");
            colorPicker.setValue(Color.BLACK);
            iconCombo.setValue(MDIcon.CIRCLE);
        });
    }

    private static class ListViewListCellCallback implements Callback<ListView<MDIcon>, ListCell<MDIcon>> {
        @Override
        public ListCell<MaterialDesignLabel.MDIcon> call(ListView<MDIcon> param) {
            return new FAIconListCell();
        }
    }

    private static class FAIconListCell extends ListCell<MDIcon> {
        private final Label label = new Label();

        {
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(final MDIcon item, final boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) {
                setGraphic(null);
            } else {
                label.setGraphic(new MaterialDesignLabel(item, MaterialDesignLabel.DEFAULT_SIZE));
                setGraphic(label);
            }
        }
    }

    private static class TagListViewListCellCallback implements Callback<ListView<Tag>, ListCell<Tag>> {
        @Override
        public ListCell<Tag> call(ListView<Tag> param) {
            return new TagListCell();
        }

        private static class TagListCell extends ListCell<Tag> {
            private final Label label = new Label();

            @Override
            protected void updateItem(final Tag item, final boolean empty) {
                super.updateItem(item, empty);  // required

                if (item == null || empty) {
                    setGraphic(null);
                    setText("");
                } else {
                    label.setGraphic(MaterialDesignLabel.fromInteger(item.getShape(),
                            MaterialDesignLabel.DEFAULT_SIZE * ICON_SCALE, item.getColor()));
                    label.setText(item.toString());
                    setGraphic(label);
                }
            }
        }
    }
}
