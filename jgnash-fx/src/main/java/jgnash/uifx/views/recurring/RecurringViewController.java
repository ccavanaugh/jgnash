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
package jgnash.uifx.views.recurring;

import java.util.Objects;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageListener;
import jgnash.engine.recurring.Reminder;

/**
 * Controller for recurring events
 *
 * @author Craig Cavanaugh
 */
public class RecurringViewController implements MessageListener {

    @FXML
    private TableView<Reminder> tableView;

    @FXML
    private ResourceBundle resources;

    private final ObservableList<Reminder> observableReminderList = FXCollections.observableArrayList();

    private final SortedList<Reminder> sortedReminderList = new SortedList<>(observableReminderList);

    @FXML
    @SuppressWarnings("unchecked")
    private void initialize() {
        tableView.setTableMenuButtonVisible(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        final TableColumn<Reminder, String> descriptionColumn = new TableColumn<>(resources.getString("Column.Description"));
        descriptionColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDescription()));

        final TableColumn<Reminder, String> frequencyColumn = new TableColumn<>(resources.getString("Column.Freq"));
        frequencyColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getReminderType().toString()));

        final TableColumn<Reminder, Boolean> enabledColumn = new TableColumn<>(resources.getString("Column.Enabled"));
        enabledColumn.setCellValueFactory(param -> new SimpleBooleanProperty(param.getValue().isEnabled()));
        enabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(enabledColumn));

        tableView.getColumns().addAll(descriptionColumn, frequencyColumn, enabledColumn);

        sortedReminderList.comparatorProperty().bind(tableView.comparatorProperty());

        tableView.setItems(sortedReminderList);

        Platform.runLater(this::loadTable);
    }

    private void loadTable() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        Objects.requireNonNull(engine);

        observableReminderList.addAll(engine.getReminders());
    }

    @Override
    public void messagePosted(Message event) {

    }
}
