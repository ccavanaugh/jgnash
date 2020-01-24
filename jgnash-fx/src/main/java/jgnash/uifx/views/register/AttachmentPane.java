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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import jgnash.engine.AttachmentUtils;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.ImageDialog;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.views.main.MainView;
import jgnash.util.FileUtils;
import jgnash.resource.util.ResourceUtils;

/**
 * Controller for handling transaction attachments.
 *
 * @author Craig Cavanaugh
 */
public class AttachmentPane extends GridPane {

    private static final String LAST_DIR = "LastDir";

    @FXML
    protected Button attachmentButton;

    @FXML
    protected Button viewAttachmentButton;

    @FXML
    protected Button deleteAttachmentButton;

    private final SimpleObjectProperty<Path> attachment = new SimpleObjectProperty<>(null);

    private boolean moveAttachment = false;

    @FXML
    private ResourceBundle resources;

    public AttachmentPane() {
        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AttachmentPane.fxml"),
                ResourceUtils.getBundle());

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void initialize() {
        attachmentButton.disableProperty().bind(Bindings.isNotNull(attachment));
        deleteAttachmentButton.disableProperty().bind(Bindings.isNull(attachment));
        viewAttachmentButton.disableProperty().bind(Bindings.isNull(attachment));

        attachmentButton.setOnAction(event -> attachmentAction());
        deleteAttachmentButton.setOnAction(event -> handleDeleteAction());
        viewAttachmentButton.setOnAction(event -> showImageAction());
    }

    private void handleDeleteAction() {
        attachment.set(null);
    }

    /**
     * Builder method for extracting an attachment from an existing {@code Transaction}.
     *
     * @param transaction {@code Transaction} to extract attachment information from
     * @return the provided {@code Transaction}
     */
    Transaction modifyTransaction(final Transaction transaction) {
        new Thread(() -> {
            if (transaction.getAttachment() != null && !transaction.getAttachment().isEmpty()) {
                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);

                final Future<Path> pathFuture = engine.getAttachment(transaction.getAttachment());

                JavaFXUtils.runLater(() -> {
                    try {
                        attachment.set(pathFuture.get());
                    } catch (final InterruptedException | ExecutionException e) {
                        Logger.getLogger(AttachmentPane.class.getName()).log(Level.SEVERE, e.getMessage(), e);
                    }
                });
            }
        }).start();

        return transaction;
    }

    /**
     * Builder method for binding an attachment to a {@code Transaction}.
     *
     * @param transaction {@code Transaction} to update
     * @return the provided {@code Transaction}
     */
    Transaction buildTransaction(final Transaction transaction) {
        if (attachment.get() != null) {

            final Path path = attachment.get().getFileName();

            if (moveAttachment) {
                if (moveAttachment() && path != null) {
                    transaction.setAttachment(path.toString());
                } else if (path!= null) {
                    transaction.setAttachment(null);

                    final String message = ResourceUtils.getString("Message.Error.TransferAttachment", path.toString());

                    StaticUIMethods.displayError(message);
                }
            }
        } else {
            transaction.setAttachment(null);
        }

        return transaction;
    }

    private boolean moveAttachment() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        return engine.addAttachment(attachment.get(), false);
    }

    private void showImageAction() {
        if (attachment.get() != null) {
            if (Files.exists(attachment.get())) {
                ImageDialog.showImage(attachment.get());
            } else {
                StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.MissingAttachment", attachment.get().toString()));
            }
        }
    }

    void clear() {
        attachment.set(null);
    }

    private void attachmentAction() {
        final Preferences pref = Preferences.userNodeForPackage(this.getClass());
        final String baseFile = EngineFactory.getActiveDatabase();

        final List<String> extensions = new ArrayList<>();

        for (final String suffix : ImageIO.getReaderFileSuffixes()) {
            extensions.add("*." + suffix);
        }

        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(resources.getString("Title.SelFile"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(resources.getString("Title.ImageFiles"), extensions)
        );

        final String lastDirectory = pref.get(LAST_DIR, null);
        if (lastDirectory != null) {
            fileChooser.setInitialDirectory(new File(lastDirectory));
        }

        if (attachment.get() != null) {
            final Path path = attachment.get().getFileName();
            if (path != null) {
                fileChooser.setInitialFileName(path.toString());
            }
        }

        final File selectedFile = fileChooser.showOpenDialog(MainView.getPrimaryStage());
        if (selectedFile != null) {
            pref.put(LAST_DIR, selectedFile.getParent());   // save last good directory location

            boolean result = true;

            // TODO, add option to copy the file instead of moving it

            final Path attachmentDirectory = AttachmentUtils.getAttachmentDirectory(Paths.get(baseFile));

            if (baseFile.startsWith(EngineFactory.REMOTE_PREFIX)) { // working remotely
                moveAttachment = true;
            } else if (attachmentDirectory != null && !attachmentDirectory.toString().equals(selectedFile.getParent())) {
                String message = ResourceUtils.getString("Message.Warn.MoveFile", selectedFile.toString(),
                        attachmentDirectory.toString());

                if (!StaticUIMethods.showConfirmationDialog(resources.getString("Title.MoveFile"), message).getButtonData().isCancelButton()) {
                    moveAttachment = true;

                    final Path newPath = Paths.get(AttachmentUtils.getAttachmentDirectory(Paths.get(baseFile)) +
                            FileUtils.SEPARATOR + selectedFile.getName());

                    if (Files.exists(newPath)) {
                        message = ResourceUtils.getString("Message.Warn.SameFile", selectedFile.toString(),
                                attachmentDirectory.toString());

                        StaticUIMethods.displayWarning(message);
                        moveAttachment = false;
                        result = false;
                    }
                } else {
                    result = false;
                }
            }

            if (result) {
                attachment.set(selectedFile.toPath());
            }
        }
    }
}
