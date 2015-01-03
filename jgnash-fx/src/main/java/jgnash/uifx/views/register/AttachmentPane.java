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
package jgnash.uifx.views.register;

import java.io.File;
import java.io.IOException;
import java.net.URL;
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

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import jgnash.engine.AttachmentUtils;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.Transaction;
import jgnash.uifx.MainApplication;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.ImageDialog;
import jgnash.util.ResourceUtils;

import javax.imageio.ImageIO;

/**
 * Controller for handling transaction attachments
 *
 * @author Craig Cavanaugh
 */
public class AttachmentPane extends GridPane implements Initializable {

    private static final String LAST_DIR = "LastDir";

    @FXML
    protected Button attachmentButton;

    @FXML
    protected Button viewAttachmentButton;

    @FXML
    protected Button deleteAttachmentButton;

    private SimpleObjectProperty<Path> attachmentProperty = new SimpleObjectProperty<>(null);

    private boolean moveAttachment = false;

    private ResourceBundle resources;

    public AttachmentPane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AttachmentPane.fxml"), ResourceUtils.getBundle());
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.resources = resources;

        attachmentButton.disableProperty().bind(Bindings.isNotNull(attachmentProperty));
        deleteAttachmentButton.disableProperty().bind(Bindings.isNull(attachmentProperty));
        viewAttachmentButton.disableProperty().bind(Bindings.isNull(attachmentProperty));

        attachmentButton.setOnAction(event -> attachmentAction());
        deleteAttachmentButton.setOnAction(event -> handleDeleteAction());
        viewAttachmentButton.setOnAction(event -> showImageAction());
    }

    private void handleDeleteAction() {
        attachmentProperty.setValue(null);
    }

    Transaction modifyTransaction(final Transaction transaction) {
        new Thread(() -> {
            if (transaction.getAttachment() != null && !transaction.getAttachment().isEmpty()) {
                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);

                final Future<Path> pathFuture = engine.getAttachment(transaction.getAttachment());

                Platform.runLater(() -> {
                    try {
                        attachmentProperty.setValue(pathFuture.get());
                    } catch (final InterruptedException | ExecutionException e) {
                        Logger.getLogger(AttachmentPane.class.getName()).log(Level.SEVERE, e.getMessage(), e);
                    }
                });
            }
        }).start();

        return transaction;
    }

    Transaction buildTransaction(final Transaction transaction) {
        if (attachmentProperty.get() != null) {
            if (moveAttachment) {
                if (moveAttachment()) {
                    transaction.setAttachment(attachmentProperty.get().getFileName().toString());
                } else {
                    transaction.setAttachment(null);

                    final String message = ResourceUtils.getString("Message.Error.TransferAttachment",
                            attachmentProperty.get().getFileName().toString());

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

        return engine.addAttachment(attachmentProperty.get(), false);
    }

    void showImageAction() {
        if (attachmentProperty.get() != null) {
            if (Files.exists(attachmentProperty.get())) {
                ImageDialog.showImage(attachmentProperty.get());
            } else {
                StaticUIMethods.displayError(ResourceUtils.getString("Message.Error.MissingAttachment", attachmentProperty.get().toString()));
            }
        }
    }

    void clear() {
        attachmentProperty.set(null);
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

        if (attachmentProperty.get() != null) {
            fileChooser.setInitialFileName(attachmentProperty.get().getFileName().toString());
        }

        final File selectedFile = fileChooser.showOpenDialog(MainApplication.getPrimaryStage());
        if (selectedFile != null) {
            pref.put(LAST_DIR, selectedFile.getParent());   // save last good directory location

            boolean result = true;

            // TODO, add option to copy the file instead of moving it

            if (baseFile.startsWith(EngineFactory.REMOTE_PREFIX)) { // working remotely
                moveAttachment = true;
            } else if (!AttachmentUtils.getAttachmentDirectory(Paths.get(baseFile)).toString().equals(selectedFile.getParent())) {
                String message = ResourceUtils.getString("Message.Warn.MoveFile", selectedFile.toString(),
                        AttachmentUtils.getAttachmentDirectory(Paths.get(baseFile)).toString());

                if (!StaticUIMethods.showConfirmationDialog(resources.getString("Title.MoveFile"), message).getButtonData().isCancelButton()) {
                    moveAttachment = true;

                    final Path newPath = new File(AttachmentUtils.getAttachmentDirectory(Paths.get(baseFile)) +
                            File.separator + selectedFile.getName()).toPath();

                    if (newPath.toFile().exists()) {
                        message = ResourceUtils.getString("Message.Warn.SameFile", selectedFile.toString(),
                                AttachmentUtils.getAttachmentDirectory(Paths.get(baseFile)).toString());

                        StaticUIMethods.displayWarning(message);
                        moveAttachment = false;
                        result = false;
                    }
                } else {
                    result = false;
                }
            }

            if (result) {
                attachmentProperty.setValue(selectedFile.toPath());
            }
        }
    }
}
