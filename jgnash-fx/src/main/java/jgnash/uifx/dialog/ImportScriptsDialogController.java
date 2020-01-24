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
package jgnash.uifx.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Stage;

import jgnash.convert.importat.ImportFilter;
import jgnash.uifx.Options;
import jgnash.uifx.skin.StyleClass;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.util.TableViewManager;

/**
 * Controller for managing import scripts
 *
 * @author Craig Cavanaugh
 */
public class ImportScriptsDialogController {

    private static final String PREF_NODE = "/jgnash/uifx/dialog/ImportScriptsDialogController";

    private static final double[] PREF_COLUMN_WEIGHTS = {0, 50, 50};

    private static final String DEFAULT = "default";

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private ButtonBar buttonBar;

    @FXML
    private Button upButton;

    @FXML
    private Button downButton;

    @FXML
    private ResourceBundle resources;

    @FXML
    private TableView<Script> tableView;

    private final IntegerProperty scriptCountProperty = new SimpleIntegerProperty();

    private final IntegerProperty selectedIndexProperty = new SimpleIntegerProperty();

    private final ObjectProperty<Consumer<List<ImportFilter>>> acceptanceConsumerProperty = new SimpleObjectProperty<>();

    @SuppressWarnings("FieldCanBeLocal")
    private TableViewManager<Script> tableViewManager;

    @FXML
    private void initialize() {

        buttonBar.buttonOrderProperty().bind(Options.buttonOrderProperty());

        // simplify binding process
        scriptCountProperty.bind(Bindings.size(tableView.getItems()));
        selectedIndexProperty.bind(tableView.selectionModelProperty().get().selectedIndexProperty());

        final TableColumn<Script, Boolean> enabledColumn = new TableColumn<>(resources.getString("Column.Enabled"));
        enabledColumn.setCellValueFactory(param -> param.getValue().enabledProperty);
        enabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(enabledColumn));

        enabledColumn.setEditable(true);
        tableView.getColumns().add(enabledColumn);

        final TableColumn<Script, String> descriptionColumn = new TableColumn<>(resources.getString("Column.Description"));
        descriptionColumn.setCellValueFactory(param -> param.getValue().descriptionProperty);
        tableView.getColumns().add(descriptionColumn);

        final TableColumn<Script, String> scriptColumn = new TableColumn<>(resources.getString("Column.Script"));
        scriptColumn.setCellValueFactory(param -> param.getValue().scriptProperty);
        tableView.getColumns().add(scriptColumn);

        tableView.setEditable(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.getStylesheets().addAll(StyleClass.HIDE_HORIZONTAL_CSS);

        upButton.disableProperty().bind(scriptCountProperty.lessThan(2)
                .or(selectedIndexProperty.lessThan(1)));

        downButton.disableProperty().bind(scriptCountProperty.lessThan(2)
                .or(selectedIndexProperty.isEqualTo(scriptCountProperty.subtract(1)))
                .or(selectedIndexProperty.lessThan(0)));


        tableViewManager = new TableViewManager<>(tableView, PREF_NODE);
        tableViewManager.setColumnWeightFactory(column -> PREF_COLUMN_WEIGHTS[column]);
        tableViewManager.setPreferenceKeyFactory(() -> DEFAULT);

        JavaFXUtils.runLater(tableViewManager::restoreLayout);
        JavaFXUtils.runLater(tableViewManager::packTable);
    }

    /**
     * Loads remain scripts that have not been enabled
     */
    private void loadScripts() {
        for (final ImportFilter importFilter : ImportFilter.getImportFilters()) {

            boolean exists = false;

            for (Script script : tableView.getItems()) {
                if (script.scriptProperty.get().equals(importFilter.getScript())) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                final Script script = new Script();

                script.descriptionProperty.setValue(importFilter.getDescription());
                script.scriptProperty.setValue(importFilter.getScript());
                script.importFilter = importFilter;

                tableView.getItems().add(script);
            }
        }
    }

    public void setEnabledScripts(final List<ImportFilter> importFilters) {
        for (final ImportFilter importFilter : importFilters) {
            final Script script = new Script();

            script.descriptionProperty.setValue(importFilter.getDescription());
            script.scriptProperty.setValue(importFilter.getScript());
            script.importFilter = importFilter;
            script.enabledProperty.setValue(true);

            tableView.getItems().add(script);
        }

        loadScripts();
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parent.get().getWindow()).close();
    }

    @FXML
    private  void handleOkayCloseAction() {
        ((Stage) parent.get().getWindow()).close();

        if (acceptanceConsumerProperty.get() != null) {
            final List<ImportFilter> filterList = new ArrayList<>();

            for (final Script script : tableView.getItems()) {
                if (script.enabledProperty.get()) {
                    filterList.add(script.importFilter);
                }
            }

            acceptanceConsumerProperty.get().accept(filterList);
        }
    }

    @FXML
    private void handleUpAction() {
        Collections.swap(tableView.getItems(), selectedIndexProperty.get(), selectedIndexProperty.get() - 1);
    }

    @FXML
    private void handleDownAction() {
        Collections.swap(tableView.getItems(), selectedIndexProperty.get(), selectedIndexProperty.get() + 1);
    }

    public void setAcceptanceConsumer(final Consumer<List<ImportFilter>> acceptanceConsumer) {
        acceptanceConsumerProperty.setValue(acceptanceConsumer);
    }

    private static class Script {
        final BooleanProperty enabledProperty = new SimpleBooleanProperty(false);

        final StringProperty descriptionProperty = new SimpleStringProperty();

        final StringProperty scriptProperty = new SimpleStringProperty();

        ImportFilter importFilter;
    }
}
