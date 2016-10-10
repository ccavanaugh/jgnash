/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2016 Craig Cavanaugh
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
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.recurring.PendingReminder;
import jgnash.engine.recurring.Reminder;
import jgnash.uifx.Options;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.ShortDateTableCell;

/**
 * Controller for recurring events.
 *
 * @author Craig Cavanaugh
 */
public class RecurringViewController implements MessageListener {

    @FXML
    private Button modifyButton;

    @FXML
    private Button deleteButton;

    @FXML
    private TableView<Reminder> tableView;

    @FXML
    private ResourceBundle resources;

    private final ObservableList<Reminder> observableReminderList = FXCollections.observableArrayList();

    private final SortedList<Reminder> sortedReminderList = new SortedList<>(observableReminderList);

    final private ReadOnlyObjectWrapper<Reminder> selectedReminderProperty = new ReadOnlyObjectWrapper<>();

    private Timer timer = null;

    private static final int START_UP_DELAY = 45 * 1000;   // 45 seconds

    final private AtomicBoolean dialogShowing = new AtomicBoolean(false);

    @FXML
    private void initialize() {
        tableView.setTableMenuButtonVisible(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        final TableColumn<Reminder, String> descriptionColumn = new TableColumn<>(resources.getString("Column.Description"));
        descriptionColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDescription()));
        tableView.getColumns().add(descriptionColumn);

        final TableColumn<Reminder, String> frequencyColumn = new TableColumn<>(resources.getString("Column.Freq"));
        frequencyColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getReminderType().toString()));
        tableView.getColumns().add(frequencyColumn);

        final TableColumn<Reminder, Boolean> enabledColumn = new TableColumn<>(resources.getString("Column.Enabled"));
        enabledColumn.setCellValueFactory(param -> new SimpleBooleanProperty(param.getValue().isEnabled()));
        enabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(enabledColumn));
        tableView.getColumns().add(enabledColumn);

        final TableColumn<Reminder, LocalDate> lastPosted = new TableColumn<>(resources.getString("Column.LastPosted"));
        lastPosted.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getLastDate()));
        lastPosted.setCellFactory(cell -> new ShortDateTableCell<>());
        tableView.getColumns().add(lastPosted);

        final TableColumn<Reminder, LocalDate> due = new TableColumn<>(resources.getString("Column.Due"));
        due.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getIterator().next()));
        due.setCellFactory(cell -> new ShortDateTableCell<>());
        tableView.getColumns().add(due);

        sortedReminderList.comparatorProperty().bind(tableView.comparatorProperty());

        tableView.setItems(sortedReminderList);

        selectedReminderProperty.bind(tableView.getSelectionModel().selectedItemProperty());

        // bind enabled state of the buttons to the selected reminder property
        deleteButton.disableProperty().bind(Bindings.isNull(selectedReminderProperty));
        modifyButton.disableProperty().bind(Bindings.isNull(selectedReminderProperty));

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM, MessageChannel.REMINDER);

        Platform.runLater(this::loadTable);

        startTimer();

        // Update the period when the snooze value changes
        Options.reminderSnoozePeriodProperty().addListener((observable, oldValue, newValue) -> {
            stopTimer();
            startTimer();
        });
    }

    private void loadTable() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

        Objects.requireNonNull(engine);

        observableReminderList.setAll(engine.getReminders());
    }

    private void startTimer() {
        if (timer == null) {
            timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(RecurringViewController.this::showReminderDialog);
                }
            }, START_UP_DELAY, Options.reminderSnoozePeriodProperty().get());
        }
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;

            Logger.getLogger(RecurringViewController.class.getName()).info("Recurring timer stopped");
        }
    }

    private void showReminderDialog() {
        Logger.getLogger(RecurringViewController.class.getName()).info("Show dialog");

        if (dialogShowing.compareAndSet(false, true)) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            final Collection<PendingReminder> pendingReminders = engine.getPendingReminders();

            if (!pendingReminders.isEmpty()) {
                final NotificationDialog notificationDialog = new NotificationDialog();

                notificationDialog.setReminders(pendingReminders);
                notificationDialog.showAndWait();

                engine.processPendingReminders(notificationDialog.getApprovedReminders());
            }

            dialogShowing.getAndSet(false);
        }
    }

    @Override
    public void messagePosted(final Message message) {
        switch (message.getEvent()) {
            case FILE_CLOSING:
                stopTimer();
                observableReminderList.removeAll();
                MessageBus.getInstance().unregisterListener(this, MessageChannel.SYSTEM, MessageChannel.REMINDER);
                break;
            case REMINDER_ADD:
            case REMINDER_UPDATE:
            case REMINDER_REMOVE:
                loadTable();
                break;
            default:
        }
    }

    @FXML
    private void handleDeleteAction() {
        if (selectedReminderProperty.get() != null) {
            if (Options.confirmOnDeleteReminderProperty().get()) {
                if (StaticUIMethods.showConfirmationDialog(resources.getString("Title.Confirm"),
                        resources.getString("Message.ConfirmReminderDelete"))
                        .getButtonData() != ButtonBar.ButtonData.YES) {
                    return;
                }
            }

            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            engine.removeReminder(selectedReminderProperty.get());
        }
    }

    @FXML
    private void handleRefreshAction() {
        showReminderDialog();
    }

    @FXML
    private void handleNewAction() {
        final Optional<Reminder> reminder = RecurringEntryDialog.showAndWait(null);

        if (reminder.isPresent()) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            if (!engine.addReminder(reminder.get())) {
                StaticUIMethods.displayError(resources.getString("Message.Error.ReminderAdd"));
            }
        }
    }

    @FXML
    private void handleModifyAction() {

        final Reminder old = selectedReminderProperty.get();

        try {
            final Optional<Reminder> reminder = RecurringEntryDialog.showAndWait((Reminder) old.clone());

            if (reminder.isPresent()) {
                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);

                if (engine.removeReminder(old)) { // remove the old
                    if (!engine.addReminder(reminder.get())) { // add the new
                        StaticUIMethods.displayError(resources.getString("Message.Error.ReminderUpdate"));
                    }
                } else {
                    StaticUIMethods.displayError(resources.getString("Message.Error.ReminderUpdate"));
                }

            }
        } catch (final CloneNotSupportedException e) {
            Logger.getLogger(RecurringViewController.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
