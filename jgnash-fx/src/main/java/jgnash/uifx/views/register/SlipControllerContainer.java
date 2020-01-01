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

import javafx.scene.layout.Pane;

/**
 * Utility class to hold the controller, slip, and slip description.
 *
 * @author Craig Cavanaugh
 */
class SlipControllerContainer {
    private final String description;
    private final Slip controller;
    private final Pane pane;

    SlipControllerContainer(final String description, final Slip controller, final Pane pane) {
        this.description = description;
        this.controller = controller;
        this.pane = pane;
    }

    public Slip getController() {
        return controller;
    }

    public Pane getPane() {
        return pane;
    }

    @Override
    public String toString() {
        return description;
    }
}
