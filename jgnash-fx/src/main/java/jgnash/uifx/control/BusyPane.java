/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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

import jgnash.util.Nullable;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

/**
 * A busy pane.  Intended to overlay the main ui and blur the display when a long running operation is occurring
 *
 * @author Craig Cavanaugh
 */
public class BusyPane extends StackPane {

    private static final String BUSY_PANE_STYLE = "busy-pane";

    private final ImageView imageView;

    private final Label messageLabel;

    private final ProgressIndicator progressIndicator;

    public BusyPane() {

        getStyleClass().add(BUSY_PANE_STYLE);

        imageView = new ImageView();
        imageView.setFocusTraversable(false);
        imageView.setEffect(new BoxBlur(4, 4, 2));

        progressIndicator = new ProgressIndicator();
        messageLabel = new Label();

        final GridPane gridPane = new GridPane();
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setHgap(15);
        gridPane.add(progressIndicator, 0, 0);
        gridPane.add(messageLabel, 1, 0);

        getChildren().addAll(gridPane);

        setVisible(false);
    }

    public void setTask(@Nullable final Task<?> task) {
        if (task == null) {
            setVisible(false);
            progressIndicator.progressProperty().unbind();
            messageLabel.textProperty().unbind();
        } else {
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

            imageView.setImage(getScene().getRoot().snapshot(null, null));

            getChildren().add(imageView);
            imageView.toBack();
            setVisible(true);
        }
    }
}
