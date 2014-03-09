/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;

/**
 * Extended TabPane for consistent view creation
 *
 * @author Craig Cavanaugh
 */
public class TabViewPane extends TabPane {

    public TabViewPane() {
        setSide(Side.LEFT);
        setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
    }

    public void addTab(final Node node, final String description) {
        BorderPane borderPane = new BorderPane();
        TitledPane titledPane = new TitledPane(description, null);
        titledPane.setCollapsible(false);
        titledPane.setExpanded(false);
        titledPane.setFocusTraversable(false);
        titledPane.setStyle("-fx-font-size: 1.2em;");

        borderPane.setTop(titledPane);
        borderPane.setCenter(node);

        Tab tab = new Tab();
        tab.setText(description);
        tab.setContent(borderPane);

        getTabs().add(tab);
    }
}
