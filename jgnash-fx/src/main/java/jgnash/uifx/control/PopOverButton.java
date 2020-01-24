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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;

import jgnash.uifx.skin.StyleClass;

/**
 * Pop Over Button control
 *
 * @author Craig Cavanaugh
 */
public class PopOverButton extends MenuButton {

    @SuppressWarnings("unused")
    public PopOverButton() {
        this(null);
    }

    public PopOverButton(final Node graphic) {
        super(null, graphic, (MenuItem[])null);

        getStyleClass().add(StyleClass.POP_OVER_BUTTON);
    }

    private final ObjectProperty<Node> contentNode = new SimpleObjectProperty<>(this, "contentNode");

    /**
     * Returns the value of the content property
     *
     * @return the content node
     */
    @SuppressWarnings("unused")
    public final Node getContentNode() {
        return contentNode.get();
    }

    /**
     * Sets the value of the content property.
     *
     * @param content the new content node value
     */
    public final void setContentNode(final Node content) {
        contentNode.set(content);

        final VBox vBox = new VBox(5);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().setAll(content);

        final MenuItem menuItem = new MenuItem();
        menuItem.setGraphic(vBox);

        getItems().setAll(menuItem);
    }
}
