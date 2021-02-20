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
package jgnash.uifx.about;

import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import jgnash.resource.util.HTMLResource;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.FXMLUtils.Pair;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.util.TableViewManager;
import jgnash.util.NotNull;

/**
 * About Dialog.
 *
 * @author Craig Cavanaugh
 */
public class AboutDialogController {

    private static final double FONT_SCALE = 0.8333;

    private static final int MAX_HEIGHT = 490;

    private static final String PREF_NODE = "/jgnash/uifx/about/AboutDialogController";

    private static final double[] PREF_COLUMN_WEIGHTS = {0, 100};

    private static final String DEFAULT = "default";

    @FXML
    private ResourceBundle resources;

    @FXML
    private TabPane tabbedPane;

    @SuppressWarnings("FieldCanBeLocal")
    private TableViewManager<SystemProperty> tableViewManager;

    @FXML
    void initialize() {
        tabbedPane.getTabs().addAll(
                addHTMLTab(resources.getString("Tab.About"), "notice.html"),
                addHTMLTab(resources.getString("Tab.Credits"), "credits.html"),
                addHTMLTab(resources.getString("Tab.AppLicense"), "jgnash-license.html"),
                addHTMLTab(resources.getString("Tab.GPLLicense"), "gpl-license.html"),
                addHTMLTab(resources.getString("Tab.LGPLLicense"), "lgpl.html"),
                addHTMLTab("Apache License", "apache-license.html"),
                addHTMLTab("XStream License", "xstream-license.html"),
                getSystemPropertiesTab());

    }

    private static Tab addHTMLTab(final String name, final String resource) {
        final WebView webView = new WebView();

        webView.setFontScale(FONT_SCALE);
        webView.setMaxHeight(MAX_HEIGHT);

        // be paranoid, protect against external scripts
        webView.getEngine().setJavaScriptEnabled(false);
        webView.getEngine().load(HTMLResource.getURL(resource).toExternalForm());

        return new Tab(name, webView);
    }

    private Tab getSystemPropertiesTab() {

        final ObservableList<SystemProperty> propertiesList = FXCollections.observableArrayList();

        final Properties properties = System.getProperties();

        propertiesList.addAll(properties.stringPropertyNames().stream().map(prop ->
                new SystemProperty(prop, properties.getProperty(prop))).collect(Collectors.toList()));

        FXCollections.sort(propertiesList);



        final TableColumn<SystemProperty, String> keyCol = new TableColumn<>(resources.getString("Column.PropName"));
        keyCol.setCellValueFactory(param -> param.getValue().keyProperty());

        final TableColumn<SystemProperty, String> valueCol = new TableColumn<>(resources.getString("Column.PropVal"));
        valueCol.setCellValueFactory(param -> param.getValue().valueProperty());

        final TableView<SystemProperty> tableView = new TableView<>(propertiesList);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        final ObservableList<TableColumn<SystemProperty, ?>>tableViewColumns = tableView.getColumns();

        tableViewColumns.add(keyCol);
        tableViewColumns.add(valueCol);

        final ContextMenu menu = new ContextMenu();
        final MenuItem copyMenuItem = new MenuItem(resources.getString("Menu.Copy.Name"));

        copyMenuItem.setOnAction(event -> dumpPropertiesToClipboard(tableView));

        menu.getItems().add(copyMenuItem);
        tableView.setContextMenu(menu);

        tableViewManager = new TableViewManager<>(tableView, PREF_NODE);
        tableViewManager.setColumnWeightFactory(column -> PREF_COLUMN_WEIGHTS[column]);
        tableViewManager.setPreferenceKeyFactory(() -> DEFAULT);

        JavaFXUtils.runLater(tableViewManager::packTable);

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

    public static void showAndWait() {
        JavaFXUtils.runLater(() -> {   // push to EDT to avoid race when loading the html files
            final Pair<AboutDialogController> pair = FXMLUtils.load(AboutDialogController.class.getResource("AboutDialog.fxml"),
                    ResourceUtils.getString("Title.About"));

            pair.getStage().setResizable(false);
            pair.getStage().show();
        });
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) tabbedPane.getScene().getWindow()).close();
    }

    private static class SystemProperty implements Comparable<SystemProperty> {
        private final StringProperty key;
        private final StringProperty value;

        private SystemProperty(final String name, final String value) {
            this.key = new SimpleStringProperty(name);
            this.value = new SimpleStringProperty(value);
        }

        StringProperty keyProperty() {
            return key;
        }

        StringProperty valueProperty() {
            return value;
        }

        @Override
        public int compareTo(@NotNull final SystemProperty o) {
            return keyProperty().get().compareTo(o.keyProperty().get());
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            final SystemProperty that = (SystemProperty) obj;

            return Objects.equals(key, that.key) && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }
    }
}

