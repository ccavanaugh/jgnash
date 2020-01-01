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
package jgnash.plugin;

/**
 * This is the interface for jGnash application plugins.
 * <p>
 * Plugins can install multiple menu items into the primary UI. Any returned {@code JMenuItem}s must have the
 * client property {@code PRECEDINGMENUIDREF} set for the menu item to installed correctly.
 * <p>
 * The available preceding id for installation may be found in {@code main-frame-actions.xml} in the
 * {@code jgnash.resource} package.
 * <p>
 * Example:
 * <p>
 * {@code
 * JMenuItem item = new JMenuItem("Test Plugin");
 * <p>
 * item.putClientProperty(Plugin.PRECEDINGMENUIDREF, "paste-command");
 * }
 * <p>
 * The above example will install a menu item after <tt>Paste</tt> in the <tt>Edit</tt> menu.
 *
 * @author Leif-Erik Dörr
 * @author Craig Cavanaugh
 */
public interface Plugin {

    /**
     * Client property key for the name of the options tab to add.
     */
    String OPTIONS_NAME = "OptionsName";

    /**
     * Return a descriptive name for the plugin.
     *
     * @return name of the plugin
     */
    String getName();

    /**
     * Called by the PluginFactory to start the plugin.
     */
    @SuppressWarnings("unused")
	default void start(PluginPlatform pluginPlatform) {
    }

    /**
     * Called by the PluginFactory to stop the plugin.
     */
    default void stop() {
    }

    /**
     * Plugin platform identifier.
     */
    enum PluginPlatform {
        Fx
    }
}
