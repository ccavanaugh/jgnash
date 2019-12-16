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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import jgnash.engine.Tag;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;

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
    private void initialize() {
        MessageBus.getInstance().registerListener(this, MessageChannel.TAG);
    }

    @FXML
    private void handleNewAction() {
    }

    @FXML
    private void handleDuplicateAction() {
    }

    @FXML
    private void handleRenameAction() {
    }

    @FXML
    private void handleDeleteAction() {
    }

    @FXML
    private void handleCloseAction() {
        MessageBus.getInstance().unregisterListener(this, MessageChannel.TAG);
        ((Stage)parent.get().getWindow()).close();
    }

    public static void showTagManager() {
        final FXMLUtils.Pair<TagManagerDialogController> pair =
                FXMLUtils.load(TagManagerDialogController.class.getResource("TagManagerDialog.fxml"),
                        ResourceUtils.getString("Title.TagManager"));

        pair.getStage().show();
        pair.getStage().setResizable(false);
    }

    @Override
    public void messagePosted(Message message) {

    }
}
