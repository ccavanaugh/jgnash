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

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Stage;
import jgnash.engine.recurring.PendingReminder;
import jgnash.uifx.control.TimePeriodComboBox;
import jgnash.uifx.util.FXMLUtils;
import jgnash.util.DateUtils;
import jgnash.util.ResourceUtils;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * A dialog for displaying recurring event / transactions when they occur.
 *
 * @author Craig Cavanaugh
 */
public class NotificationDialog extends Stage {

    @FXML
    private Button cancelButton;

    @FXML
    private Button okButton;

    @FXML
    private TableView<PendingReminder> tableView;

    @FXML
    private ResourceBundle resources;

    @FXML
    private TimePeriodComboBox snoozeComboBox;

    private final ObservableList<PendingReminder> observableReminderList = FXCollections.observableArrayList();

    NotificationDialog() {
        FXMLUtils.loadFXML(this, "NotificationDialog.fxml", ResourceUtils.getBundle());
        setTitle(ResourceUtils.getBundle().getString("Title.Reminder"));
    }

    public void setReminders(final Collection<PendingReminder> pendingReminders) {
        observableReminderList.setAll(pendingReminders);
    }

    @FXML
    private void initialize() {
        tableView.setTableMenuButtonVisible(false);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        final TableColumn<PendingReminder, Boolean> enabledColumn = new TableColumn<>(resources.getString("Column.Approve"));
        enabledColumn.setCellValueFactory(param -> new SimpleBooleanProperty(param.getValue().isSelected()));
        enabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(enabledColumn));

        final TableColumn<PendingReminder, Date> dateColumn = new TableColumn<>(resources.getString("Column.Date"));
        dateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getCommitDate()));
        dateColumn.setCellFactory(cell -> new DateTableCell());

        final TableColumn<PendingReminder, String> descriptionColumn = new TableColumn<>(resources.getString("Column.Description"));
        descriptionColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getReminder().getDescription()));

        tableView.getColumns().addAll(enabledColumn, dateColumn, descriptionColumn);

        okButton.onActionProperty().setValue(event -> handleOkayAction());
        cancelButton.onActionProperty().setValue(event -> handleCancelAction());
    }


    private void handleCancelAction() {
        close();
    }


    private void handleOkayAction() {

    }

    private static class DateTableCell extends TableCell<PendingReminder, Date> {
        private final DateFormat dateFormatter = DateUtils.getShortDateFormat();

        @Override
        protected void updateItem(final Date date, final boolean empty) {
            super.updateItem(date, empty);  // required

            if (!empty && date != null) {
                setText(dateFormatter.format(date));
            } else {
                setText(null);
            }
        }
    }
}
