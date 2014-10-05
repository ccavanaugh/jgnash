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
package jgnash.plugin;

import javax.swing.JMenuItem;
import javax.swing.JPanel;

/**
 * This is the interface for jGnash application plugins.
 * <p/>
 * <p/>
 * Plugins can install multiple menu items into the primary UI. Any returned {@code JMenuItem}s must have the
 * client property {@code PRECEDINGMENUIDREF} set for the menu item to installed correctly.
 * <p/>
 * <p/>
 * The available preceding id for installation may be found in {@code main-frame-actions.xml} in the
 * {@code jgnash.resource} package.
 * <p/>
 * Example:
 * <p/>
 * {@code
 * JMenuItem item = new JMenuItem("Test Plugin");
 * <p/>
 * item.putClientProperty(Plugin.PRECEDINGMENUIDREF, "paste-command");
 * }
 * <p/>
 * The above example will install a menu item after <tt>Paste</tt> in the <tt>Edit</tt> menu.
 *
 * @author Leif-Erik Dörr
 * @author Craig Cavanaugh
 */
public interface Plugin {

    /**
     * Client property key for the idref of the preceding menu item when adding a plugin specific menu item
     */
    static final String PRECEDINGMENUIDREF = "PrecedingMenuIdref";

    /**
     * Client property key for the name of the options tab to add
     */
    static final String OPTIONSNAME = "OptionsName";

    /**
     * Return a descriptive name for the plugin
     *
     * @return name of the plugin
     */
    String getName();

    /**
     * Called by the PluginFactory to start the plugin
     */
    void start();

    /**
     * Called by the PluginFactory to stop the plugin
     */
    void stop();

    /**
     * This will add a {@code JMenuItem} to the primary application menu. The {@code PRECEDINGMENUIDREF}
     * client property must be set on the returned {@code JMenuItem}.
     *
     * @return a {@code JMenuItem} to perform an action. May be {@code null} if no action is to be performed
     * @see javax.swing.JMenuItem
     */
    JMenuItem[] getMenuItems();

    /**
     * This will add an additional option panel to the standard options dialog. The {@code OPTIONSNAME} client
     * property must be set on the returned {@code JPanel}.
     *
     * @return a {@code JPanel}. May be {@code null} if no panel is to be added
     */
    JPanel getOptionsPanel();

}
