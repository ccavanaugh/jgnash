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

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import jgnash.uifx.skin.ThemeManager;
import jgnash.resource.util.ResourceUtils;

/**
 * Status bar.
 *
 * @author Craig Cavanaugh
 */
public class StatusBar extends StackPane {

    private final StringProperty text = new SimpleStringProperty(this, "text");

    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<>(this, "graphic");

    private final DoubleProperty progress = new SimpleDoubleProperty(this, "progress");

    public StatusBar() {
        getStyleClass().add("status-bar");

        final Label label = new Label();
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        label.textProperty().bind(textProperty());
        label.graphicProperty().bind(graphicProperty());
        label.getStyleClass().add("status-label");
        label.textFillProperty().bind(ThemeManager.controlTextFillProperty());

        textProperty().set(ResourceUtils.getString("Button.Ok"));

        final ProgressIndicator progressBar = new ProgressIndicator();
        progressBar.progressProperty().bind(progressProperty());
        progressBar.visibleProperty().bind(Bindings.notEqual(0, progressProperty()));
        progressBar.maxHeightProperty().bind(heightProperty());

        final BorderPane borderPane = new BorderPane();
        borderPane.setCenter(label);
        borderPane.setRight(progressBar);

        getChildren().add(borderPane);
    }

    /**
     * The property used for storing the text message shown by the status bar.
     *
     * @return the text message property
     */
    public final StringProperty textProperty() {
        return text;
    }

    /**
     * The property used to store a graphic node that can be displayed by the
     * status label inside the status bar control.
     *
     * @return the property used for storing a graphic node
     */
    public final ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    /**
     * The property used to store the progress, a value between 0 and 1. A negative
     * value causes the progress indicator to show an indeterminate state.
     *
     * @return the property used to store the progress of a task
     */
    public final DoubleProperty progressProperty() {
        return progress;
    }
}
