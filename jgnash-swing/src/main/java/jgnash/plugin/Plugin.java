/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
 * <p>
 * 
 * Plugins can install multiple menu items into the primary UI. Any returned <code>JMenuItem</code>s must have the
 * client property <code>PRECEDINGMENUIDREF</code> set for the menu item to installed correctly.
 * <p>
 * 
 * The available preceding id for installation may be found in <code>main-frame-actions.xml</code> in the
 * <code>jgnash.resource</code> package.
 * <p>
 * Example:
 * <p>
 * <code> 
 * JMenuItem item = new JMenuItem("Test Plugin");
 *        
 *  item.putClientProperty(Plugin.PRECEDINGMENUIDREF, "paste-command"); 
 * </code>
 * <p>
 * The above example will install a menu item after <tt>Paste</tt> in the <tt>Edit</tt> menu.
 * 
 * @author Leif-Erik Dörr
 * @author Craig Cavanaugh
 * @version $Id: Plugin.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public interface Plugin {

    /** Client property key for the idref of the preceding menu item when adding a plugin specific menu item */
    static final String PRECEDINGMENUIDREF = "PrecedingMenuIdref";

    /** Client property key for the name of the options tab to add */
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
     * This will add a <code>JMenuItem</code> to the primary application menu. The <code>PRECEDINGMENUIDREF</code>
     * client property must be set on the returned <code>JMenuItem</code>.
     * 
     * @return a <code>JMenuItem</code> to perform an action. May be <code>null</code> if no action is to be performed
     * @see javax.swing.JMenuItem
     */
    JMenuItem[] getMenuItems();

    /**
     * This will add an additional option panel to the standard options dialog. The <code>OPTIONSNAME</code> client
     * property must be set on the returned <code>JPanel</code>.
     * 
     * @return a <code>JPanel</code>. May be <code>null</code> if no panel is to be added
     */
    JPanel getOptionsPanel();

}
