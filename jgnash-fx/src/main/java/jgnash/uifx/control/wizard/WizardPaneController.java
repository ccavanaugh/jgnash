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
package jgnash.uifx.control.wizard;

import java.util.Map;

import javafx.beans.property.ObjectProperty;

/**
 * Interface for a task pane in a {@code WizardPane}.
 *
 * @author Craig Cavanaugh
 */
public interface WizardPaneController<K extends Enum<?>> {

    default boolean isPaneValid() {
        return true;
    }

    /**
     * Called after a page has been made active.  The page
     * can load predefined settings/preferences when called.
     *
     * @param map preferences are accessible here
     */
    default void getSettings(final Map<K, Object> map) {

    }

    /**
     * Called on the active prior to switching to the next page.  The page
     * can save settings/preferences to be used later
     *
     * @param map place to put default preferences
     */
   default void putSettings(final Map<K, Object> map) {

   }

    ObjectProperty<WizardDescriptor> descriptorProperty();
}
