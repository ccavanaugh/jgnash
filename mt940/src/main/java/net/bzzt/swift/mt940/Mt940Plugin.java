/*
 * Copyright (C) 2008 Arnout Engelen
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

package net.bzzt.swift.mt940;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JMenuItem;

import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.plugin.FxPlugin;
import jgnash.plugin.SwingPlugin;
import jgnash.uifx.views.main.MainView;
import jgnash.util.ResourceUtils;

public class Mt940Plugin implements SwingPlugin, FxPlugin {

    private static final int MENU_INDEX = 2;

    @Override
    public JMenuItem[] getMenuItems() {

        final JMenuItem menuItem = new JMenuItem();

        menuItem.putClientProperty(SwingPlugin.PRECEDING_MENU_IDREF, "ofximport-command");

        Logger.getLogger(Mt940Plugin.class.getName()).info("Loading mt940 plugin");

        try {
            menuItem.setAction(new ImportMt940Action());

            return new JMenuItem[]{menuItem};
        } catch (NoClassDefFoundError e) {
            Logger.getLogger(Mt940Plugin.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            return null;
        }
    }

    @Override
    public String getName() {
        return "MT940 Import";
    }

    @Override
    public void start(final PluginPlatform pluginPlatform) {
        if (pluginPlatform == PluginPlatform.Fx) {
            installFxMenu();
        }
    }

    private static void installFxMenu() {
        final MenuBar menuBar = MainView.getInstance().getMenuBar();

        menuBar.getMenus().stream().filter(menu -> "fileMenu".equals(menu.getId())).forEach(menu -> menu.getItems()
                .stream().filter(menuItem -> menuItem instanceof Menu)
                .filter(menuItem -> "importMenu".equals(menuItem.getId())).forEach(menuItem -> {

            final MenuItemEx importMenuItem = new MenuItemEx(ResourceUtils.getString("Menu.ImportMt940.Name"));

            importMenuItem.setOnAction(event -> ImportMt940FxAction.showAndWait());

            ((Menu) menuItem).getItems().add(MENU_INDEX, importMenuItem);
        }));
    }

    private static class MenuItemEx extends MenuItem implements MessageListener {

        MenuItemEx(final String text) {
            super(text);
            MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM);
            disableProperty().setValue(true);
        }

        @Override
        public void messagePosted(final Message message) {
            Platform.runLater(() -> {
                switch (message.getEvent()) {
                    case FILE_LOAD_SUCCESS:
                        disableProperty().setValue(false);
                        break;
                    case FILE_CLOSING:
                        disableProperty().setValue(true);
                        break;
                    default:
                        break;
                }
            });
        }
    }
}
