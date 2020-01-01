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
package jgnash.uifx.control;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import jgnash.uifx.skin.ThemeManager;
import jgnash.util.Nullable;

/**
 * A busy pane.  Intended to overlay the main ui and blur the display when a long running operation is occurring
 *
 * @author Craig Cavanaugh
 */
public class BusyPane extends StackPane {

    private ImageView imageView;

    private final Label messageLabel;

    private final ProgressIndicator progressIndicator;

    /**
     * Listens for changes to the font scale
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final ChangeListener<Number> fontScaleListener;

    public BusyPane() {

        progressIndicator = new ProgressIndicator();
        messageLabel = new Label();

        final GridPane gridPane = new GridPane();
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setHgap(15);
        gridPane.add(progressIndicator, 0, 0);
        gridPane.add(messageLabel, 1, 0);

        updateFont();

        fontScaleListener = (observable, oldValue, newValue) -> updateFont();
        ThemeManager.fontScaleProperty().addListener(new WeakChangeListener<>(fontScaleListener));

        getChildren().addAll(gridPane);

        setVisible(false);
    }

    private ImageView getImageView() {
        ImageView imageView = new ImageView();
        imageView.setFocusTraversable(false);
        imageView.setEffect(new BoxBlur(4, 4, 2));

        imageView.setImage(getScene().getRoot().snapshot(null, null));

        return imageView;
    }

    private void updateFont() {
        messageLabel.fontProperty().set(Font.font(null, FontWeight.BOLD, null,
                ThemeManager.getBaseTextHeight() * 1.2));
    }

    public void setTask(@Nullable final Task<?> task) {
        if (task == null) {
            setVisible(false);
            progressIndicator.progressProperty().unbind();
            messageLabel.textProperty().unbind();
            getChildren().remove(imageView);

            if (imageView != null) {    // protect against a race condition
                imageView.setImage(null);   // don't retain the image, conserve memory
                imageView = null;
            }
        } else {

            // get the snapshot
            imageView = getImageView();

            messageLabel.textProperty().bind(task.messageProperty());
            progressIndicator.progressProperty().bind(task.progressProperty());

            // Add event handlers to automatically hide the busy pane.
            // These handler will not override the setOnXxx handlers
            task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> setTask(null));
            task.addEventHandler(WorkerStateEvent.WORKER_STATE_CANCELLED, event -> setTask(null));
            task.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, event -> setTask(null));

            setVisible(true);
        }
    }

    @Override protected void layoutChildren() {
        super.layoutChildren();

        if (getParent() != null && isVisible()) {
            setVisible(false);
            getChildren().remove(imageView);
            getChildren().add(imageView);
            imageView.toBack();
            setVisible(true);
        }
    }
}
