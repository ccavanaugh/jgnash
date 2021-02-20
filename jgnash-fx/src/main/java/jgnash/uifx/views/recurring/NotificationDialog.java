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
package jgnash.uifx.views.recurring;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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

import jgnash.engine.message.ChannelEvent;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.recurring.PendingReminder;
import jgnash.resource.util.ResourceUtils;
import jgnash.time.DateUtils;
import jgnash.uifx.Options;
import jgnash.uifx.control.TimePeriodComboBox;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.JavaFXUtils;

/**
 * A dialog for displaying recurring event / transactions when they occur.
 *
 * @author Craig Cavanaugh
 */
class NotificationDialog extends Stage implements MessageListener {

    @FXML
    private Button cancelButton;

    @FXML
    private Button okButton;

    @FXML
    private Button selectAllButton;

    @FXML
    private Button clearAllButton;

    @FXML
    private Button invertButton;

    @FXML
    private TableView<PendingReminder> tableView;

    @FXML
    private ResourceBundle resources;

    @FXML
    private TimePeriodComboBox snoozeComboBox;

    private final ObservableList<PendingReminder> observableReminderList = FXCollections.observableArrayList();


    NotificationDialog() {
        FXMLUtils.loadFXML(this, "NotificationDialog.fxml", ResourceUtils.getBundle());
        setTitle(ResourceUtils.getString("Title.Reminder"));
    }

    void setReminders(final Collection<PendingReminder> pendingReminders) {
        observableReminderList.setAll(pendingReminders);
    }

    Collection<PendingReminder> getApprovedReminders() {
        return observableReminderList.stream().filter(PendingReminder::isApproved).collect(Collectors.toList());
    }

    @FXML
    private void initialize() {
        final TableColumn<PendingReminder, Boolean> enabledColumn = new TableColumn<>(resources.getString("Column.Approve"));
        enabledColumn.setCellValueFactory(param -> new SimpleBooleanProperty(param.getValue().isApproved()));
        enabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(enabledColumn));
        tableView.getColumns().add(enabledColumn);

        final TableColumn<PendingReminder, LocalDate> dateColumn = new TableColumn<>(resources.getString("Column.Date"));
        dateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getCommitDate()));
        dateColumn.setCellFactory(cell -> new DateTableCell());
        tableView.getColumns().add(dateColumn);

        final TableColumn<PendingReminder, String> descriptionColumn = new TableColumn<>(resources.getString("Column.Description"));
        descriptionColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getReminder().getDescription()));
        tableView.getColumns().add(descriptionColumn);

        tableView.setItems(observableReminderList);

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Toggle the selection
        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                newValue.setApproved(!newValue.isApproved());
                tableView.refresh();
                JavaFXUtils.runLater(() -> tableView.getSelectionModel().clearSelection());
            }
        });

        okButton.onActionProperty().set(event -> handleOkayAction());
        cancelButton.onActionProperty().set(event -> handleCancelAction());
        selectAllButton.onActionProperty().set(event -> handleSelectAllAction());
        clearAllButton.onActionProperty().set(event -> handleClearAllAction());
        invertButton.onActionProperty().set(event -> handleInvertSelectionAction());

        // configure the combo box and bind the property
        snoozeComboBox.setSelectedPeriod(Options.reminderSnoozePeriodProperty().get());
        Options.reminderSnoozePeriodProperty().bind(snoozeComboBox.periodProperty());

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM);

        // unregister the listener when closing
        setOnHiding(event -> MessageBus.getInstance().unregisterListener(NotificationDialog.this,
                MessageChannel.SYSTEM));
    }

    private void handleSelectAllAction() {
        for (final PendingReminder pendingReminder : observableReminderList) {
            pendingReminder.setApproved(true);
        }
        tableView.refresh();
    }

    private void handleClearAllAction() {
        for (final PendingReminder pendingReminder : observableReminderList) {
            pendingReminder.setApproved(false);
        }
        tableView.refresh();
    }

    private void handleInvertSelectionAction() {
        for (final PendingReminder pendingReminder : observableReminderList) {
            pendingReminder.setApproved(!pendingReminder.isApproved());
        }
        tableView.refresh();
    }

    private void handleCancelAction() {
        observableReminderList.clear(); // dump the list so caller sees nothing
        close();
    }

    private void handleOkayAction() {
        close();
    }


    @Override
    public void messagePosted(final Message message) {
        if (message.getEvent() == ChannelEvent.FILE_CLOSING) {  // close the dialog automatically
            handleCancelAction();   // cancel and pending changes because close was forced
        }
    }

    private static class DateTableCell extends TableCell<PendingReminder, LocalDate> {
        private final DateTimeFormatter formatter = DateUtils.getShortDateFormatter();

        @Override
        protected void updateItem(final LocalDate date, final boolean empty) {
            super.updateItem(date, empty);  // required

            if (!empty && date != null) {
                setText(formatter.format(date));
            } else {
                setText(null);
            }
        }
    }
}
