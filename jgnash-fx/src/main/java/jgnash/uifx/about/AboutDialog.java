/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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
package jgnash.uifx.about;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.web.WebView;
import jgnash.uifx.util.TableViewManager;
import jgnash.uifx.views.main.MainApplication;
import jgnash.util.HTMLResource;
import jgnash.util.NotNull;
import jgnash.util.ResourceUtils;

import java.util.Properties;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * About Dialog
 *
 * @author Craig Cavanaugh
 */
public class AboutDialog {

    private static final double FONT_SCALE = 0.8333;

    private static final String PREF_NODE_USER_ROOT = "/jgnash/uifx/about";

    private static final int MAX_HEIGHT = 600;

    private AboutDialog() {
        // utility class
    }

    public static void showAndWait() {
        final ResourceBundle rb = ResourceUtils.getBundle();

        final Dialog<Void> dialog = new Dialog<>();

        dialog.getDialogPane().getStylesheets().addAll(MainApplication.DEFAULT_CSS);
        dialog.getDialogPane().getStyleClass().addAll("form", "dialog");

        dialog.setTitle(rb.getString("Title.About"));
        dialog.getDialogPane().getButtonTypes().addAll(new ButtonType(rb.getString("Button.Close"), ButtonBar.ButtonData.CANCEL_CLOSE));

        final TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        dialog.getDialogPane().setContent(tabPane);

        tabPane.getTabs().addAll(addHTMLTab(rb.getString("Menu.About.Name"), "notice.html"),
                addHTMLTab(rb.getString("Tab.Credits"), "credits.html"),
                addHTMLTab(rb.getString("Tab.AppLicense"), "jgnash-license.html"),
                addHTMLTab(rb.getString("Tab.GPLLicense"), "gpl-license.html"),
                addHTMLTab(rb.getString("Tab.LGPLLicense"), "lgpl.html"),
                addHTMLTab("Apache License", "apache-license.html"),
                addHTMLTab("XStream License", "xstream-license.html"),
                getSystemPropertiesTab());

        dialog.getDialogPane().setMaxHeight(MAX_HEIGHT);

        dialog.showAndWait();
    }

    private static Tab addHTMLTab(final String name, final String resource) {
        final WebView webView = new WebView();
        webView.getEngine().load(HTMLResource.getURL(resource).toExternalForm());
        webView.setFontScale(FONT_SCALE);

        return new Tab(name, webView);
    }

    private static Tab getSystemPropertiesTab() {

        final ResourceBundle rb = ResourceUtils.getBundle();

        final ObservableList<SystemProperty> propertiesList = FXCollections.observableArrayList();

        final Properties properties = System.getProperties();

        propertiesList.addAll(properties.stringPropertyNames().stream().map(prop ->
                new SystemProperty(prop, properties.getProperty(prop))).collect(Collectors.toList()));

        FXCollections.sort(propertiesList);

        final TableColumn<SystemProperty, String> keyCol = new TableColumn<>(rb.getString("Column.PropName"));
        keyCol.setCellValueFactory(param -> param.getValue().keyProperty());

        final TableColumn<SystemProperty, String> valueCol = new TableColumn<>(rb.getString("Column.PropVal"));
        valueCol.setCellValueFactory(param -> param.getValue().valueProperty());

        final TableView<SystemProperty> tableView = new TableView<>(propertiesList);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        final ObservableList<TableColumn<SystemProperty, ?>>tableViewColumns = tableView.getColumns();

        tableViewColumns.add(keyCol);
        tableViewColumns.add(valueCol);

        final TableViewManager<SystemProperty> tableViewManager= new TableViewManager<>(tableView, PREF_NODE_USER_ROOT);

        tableViewManager.setColumnWeightFactory(param -> {
            switch (param) {
                case 0:
                    return 0d;
                default:
                    return 1d;
            }
        });

        tableView.getStylesheets().addAll(MainApplication.DEFAULT_CSS);

        final ContextMenu menu = new ContextMenu();
        final MenuItem copyMenuItem = new MenuItem(rb.getString("Menu.Copy.Name"));

        copyMenuItem.setOnAction(event -> dumpPropertiesToClipboard(tableView));

        menu.getItems().add(copyMenuItem);
        tableView.setContextMenu(menu);

        Platform.runLater(tableViewManager::packTable);

        return new Tab(ResourceUtils.getString("Tab.SysInfo"), tableView);
    }

    private static void dumpPropertiesToClipboard(final TableView<SystemProperty> tableView) {
        final StringBuilder buffer = new StringBuilder();

        tableView.getSelectionModel().getSelectedItems().stream().filter(systemProperty ->
                systemProperty.keyProperty().get() != null).forEach(systemProperty -> {
            buffer.append(systemProperty.keyProperty().get());
            buffer.append("\t");
            if (systemProperty.valueProperty().get() != null) {
                buffer.append(systemProperty.valueProperty().get());
            }
            buffer.append("\n");
        });

        final ClipboardContent content = new ClipboardContent();
        content.putString(buffer.toString());

        Clipboard.getSystemClipboard().setContent(content);
    }

    private static class SystemProperty implements Comparable<SystemProperty> {
        private final StringProperty key;
        private final StringProperty value;

        private SystemProperty(final String name, final String value) {
            this.key = new SimpleStringProperty(name);
            this.value = new SimpleStringProperty(value);
        }

        public StringProperty keyProperty() {
            return key;
        }

        public StringProperty valueProperty() {
            return value;
        }

        @Override
        public int compareTo(@NotNull final SystemProperty o) {
            return keyProperty().get().compareTo(o.keyProperty().get());
        }
    }
}

