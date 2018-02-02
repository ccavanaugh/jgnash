/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.util.Callback;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.recurring.PendingReminder;
import jgnash.engine.recurring.Reminder;
import jgnash.text.CommodityFormat;
import jgnash.uifx.Options;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.ShortDateTableCell;
import jgnash.uifx.skin.StyleClass;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Controller for recurring events.
 *
 * @author Craig Cavanaugh
 */
public class RecurringViewController implements MessageListener {

    @FXML
    private Button deleteButton;

    @FXML
    private Button modifyButton;

    @FXML
    private Button nowButton;

    @FXML
    private TableView<Reminder> tableView;

    @FXML
    private ResourceBundle resources;

    private final ObservableList<Reminder> observableReminderList = FXCollections.observableArrayList();

    private final SortedList<Reminder> sortedReminderList = new SortedList<>(observableReminderList);

    final private ReadOnlyObjectWrapper<Reminder> selectedReminder = new ReadOnlyObjectWrapper<>();

    private Timer timer = null;

    private static final int START_UP_DELAY = 45 * 1000;   // 45 seconds

    final private AtomicBoolean dialogShowing = new AtomicBoolean(false);
    private boolean isSnoozed = false;

    @FXML
    private void initialize() {
        tableView.setTableMenuButtonVisible(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // hide the horizontal scrollbar and prevent ghosting
        tableView.getStylesheets().addAll(StyleClass.HIDE_HORIZONTAL_CSS);

        final TableColumn<Reminder, String> descriptionColumn = new TableColumn<>(resources.getString("Column.Description"));
        descriptionColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDescription()));
        tableView.getColumns().add(descriptionColumn);

        final TableColumn<Reminder, String> accountColumn = new TableColumn<>(resources.getString("Column.Account"));
        accountColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDescription()));
        accountColumn.setCellValueFactory(param -> {
            if (param.getValue().getAccount() != null) {
                return new SimpleObjectProperty<>(param.getValue().getAccount().toString());
            }
            return null;
        });
        tableView.getColumns().add(accountColumn);

        final TableColumn<Reminder, BigDecimal> amountColumn = new TableColumn<>(resources.getString("Column.Amount"));
        amountColumn.setCellValueFactory(param -> {
            if (param.getValue().getTransaction() != null) {
                return new SimpleObjectProperty<>(param.getValue().getTransaction().getAmount(param.getValue().getAccount()));
            }
            return null;
        });

        amountColumn.setCellFactory(new Callback<TableColumn<Reminder, BigDecimal>, TableCell<Reminder, BigDecimal>>() {
            @Override
            public TableCell<Reminder, BigDecimal> call(TableColumn<Reminder, BigDecimal> param) {
                return new TableCell<Reminder, BigDecimal>() {
                    {
                        setStyle("-fx-alignment: center-right;");
                    }

                    @Override
                    protected void updateItem(final BigDecimal amount, final boolean empty) {
                        super.updateItem(amount, empty);

                        if (!empty && getTableRow() != null && getTableRow().getItem() != null) {
                            final Reminder reminder = (Reminder) getTableRow().getItem();

                            if (reminder.getTransaction() != null) {
                                setText(CommodityFormat.getFullNumberFormat(reminder.getAccount().getCurrencyNode())
                                        .format(amount));

                                if (amount != null && amount.signum() < 0) {
                                    setId(StyleClass.NORMAL_NEGATIVE_CELL_ID);
                                } else {
                                    setId(StyleClass.NORMAL_CELL_ID);
                                }
                            } else {
                                setText(CommodityFormat.getFullNumberFormat(reminder.getAccount().getCurrencyNode())
                                        .format(BigDecimal.ZERO));
                            }
                        }
                    }
                };
            }
        });
        tableView.getColumns().add(amountColumn);

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

        selectedReminder.bind(tableView.getSelectionModel().selectedItemProperty());

        // bind enabled state of the buttons to the selected reminder property
        deleteButton.disableProperty().bind(Bindings.isNull(selectedReminder));
        modifyButton.disableProperty().bind(Bindings.isNull(selectedReminder));
        nowButton.disableProperty().bind(Bindings.isNull(selectedReminder));

        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM, MessageChannel.REMINDER);

        JavaFXUtils.runLater(this::loadTable);

        startTimer();

        // Update the period when the snooze value changes
        Options.reminderSnoozePeriodProperty().addListener((observable, oldValue, newValue) -> {
            stopTimer();

            // Don't start the timer if the dialog is up, otherwise events may get backed up
            // while the dialog is up.
            if (!dialogShowing.get()) {
                startTimer();
            }
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
            if (isSnoozed) {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        JavaFXUtils.runLater(RecurringViewController.this::showReminderDialog);
                    }
                }, Options.reminderSnoozePeriodProperty().get());

                Logger.getLogger(RecurringViewController.class.getName()).info("Recurring timer started snooze: " +  Options.reminderSnoozePeriodProperty().get() / 1000 + " secs");
            }
            else {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        JavaFXUtils.runLater(RecurringViewController.this::showReminderDialog);
                    }
                }, START_UP_DELAY, Options.reminderSnoozePeriodProperty().get());

                Logger.getLogger(RecurringViewController.class.getName()).info("Recurring timer started: " +  Options.reminderSnoozePeriodProperty().get() / 1000 + " secs");
            }
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

                // Stop the timer so events don't back up if the dialog is up too long.
                stopTimer();

                notificationDialog.setReminders(pendingReminders);
                notificationDialog.showAndWait();

                engine.processPendingReminders(notificationDialog.getApprovedReminders());

                isSnoozed = notificationDialog.isSnoozed();
                startTimer();
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
        if (selectedReminder.get() != null) {
            if (Options.confirmOnDeleteReminderProperty().get()) {
                if (StaticUIMethods.showConfirmationDialog(resources.getString("Title.Confirm"),
                        resources.getString("Message.ConfirmReminderDelete"))
                        .getButtonData() != ButtonBar.ButtonData.YES) {
                    return;
                }
            }

            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            engine.removeReminder(selectedReminder.get());
        }
    }

    @FXML
    private void handleRefreshAction() {
        showReminderDialog();
    }

    @FXML
    private void handleNewAction() {
        final Optional<Reminder> optional = RecurringEntryDialog.showAndWait(null);

        optional.ifPresent(reminder -> {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            if (!engine.addReminder(reminder)) {
                StaticUIMethods.displayError(resources.getString("Message.Error.ReminderAdd"));
            }
        });
    }

    @FXML
    private void handleModifyAction() {

        final Reminder old = selectedReminder.get();

        try {
            final Optional<Reminder> optional = RecurringEntryDialog.showAndWait((Reminder) old.clone());

            optional.ifPresent(reminder -> {
                final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
                Objects.requireNonNull(engine);

                if (engine.removeReminder(old)) { // remove the old
                    if (!engine.addReminder(reminder)) { // add the new
                        StaticUIMethods.displayError(resources.getString("Message.Error.ReminderUpdate"));
                    }
                } else {
                    StaticUIMethods.displayError(resources.getString("Message.Error.ReminderUpdate"));
                }

            });
        } catch (final CloneNotSupportedException e) {
            Logger.getLogger(RecurringViewController.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    @FXML
    private void handleNowAction() {
        if (selectedReminder.get().isEnabled()) {
            if (StaticUIMethods.showConfirmationDialog(resources.getString("Title.Confirm"),
                    resources.getString("Message.Confirm.ExecuteReminder"))
                    .getButtonData() != ButtonBar.ButtonData.YES) {
                return;
            }

            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            final PendingReminder pendingReminder = engine.getPendingReminder(selectedReminder.get());
            pendingReminder.setApproved(true);

            engine.processPendingReminders(Collections.singletonList(pendingReminder));
        }
    }
}
