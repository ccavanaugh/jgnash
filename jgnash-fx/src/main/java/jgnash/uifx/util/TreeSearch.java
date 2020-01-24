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
package jgnash.uifx.util;

import javafx.scene.control.TreeItem;

/**
 * Utility class to search through a JavaFX tree.
 *
 * @author Craig Cavanaugh
 */
public class TreeSearch {

    private TreeSearch() {
        // Utility class
    }

    public static <T> TreeItem<T> findTreeItem(final TreeItem<T> treeItem, final T value) {

        if (treeItem == null) {
            return null;
        }

        if (treeItem.getValue().equals(value)) {
            return treeItem;
        }
        
		TreeItem<T> childItem;
		
		for (TreeItem<T> child : treeItem.getChildren()) {
		    if ((childItem = findTreeItem(child, value)) != null) {
		        return childItem;
		    }
		}
		
        return null;
    }
}
