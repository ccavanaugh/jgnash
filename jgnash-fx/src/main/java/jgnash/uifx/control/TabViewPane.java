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

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;

/**
 * Extended TabPane for consistent view creation.
 *
 * @author Craig Cavanaugh
 */
public class TabViewPane extends TabPane {

    private static final String VIEW_TITLE = "view-title";

    public TabViewPane() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("TabViewPane.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        setSide(Side.LEFT);
        setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
    }

    public void addTab(final Node node, final String description) {
        BorderPane borderPane = new BorderPane();
        TitledPane titledPane = new TitledPane(description, null);
        titledPane.setCollapsible(false);
        titledPane.setExpanded(false);
        titledPane.setFocusTraversable(false);

        titledPane.getStyleClass().add(VIEW_TITLE);

        borderPane.setTop(titledPane);
        borderPane.setCenter(node);

        Tab tab = new Tab();
        tab.setText(description);
        tab.setContent(borderPane);

        getTabs().add(tab);
    }
}
