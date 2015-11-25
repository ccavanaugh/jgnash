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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import jgnash.engine.Account;
import jgnash.engine.Transaction;
import jgnash.engine.recurring.*;
import jgnash.uifx.control.AccountComboBox;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.IntegerTextField;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.views.register.TransactionDialog;
import jgnash.util.DateUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for creating and modifying a reminder
 *
 * @author Craig Cavanaugh
 */
public class RecurringPropertiesController {

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private ResourceBundle resources;

    @FXML
    private DatePickerEx startDatePicker;

    @FXML
    private TabPane tabs;

    @FXML
    private CheckBox enabledCheckBox;

    @FXML
    private TextField lastOccurrenceTextField;

    @FXML
    private CheckBox autoEnterCheckBox;

    @FXML
    private IntegerTextField daysBeforeTextField;

    @FXML
    private AccountComboBox accountComboBox;

    @FXML
    private TextField descriptionTextField;

    @FXML
    private TextField payeeTextField;

    @FXML
    private TextArea notesTextArea;

    private Transaction transaction;

    private final DateTimeFormatter dateFormatter = DateUtils.getShortDateTimeFormat();

    private final HashMap<Class<?>, Integer> tabMap = new HashMap<>();

    private Optional<Reminder> optionalReminder = Optional.empty();

    @FXML
    private void initialize() {
        tabMap.put(OneTimeReminder.class, 0);
        tabMap.put(DailyReminder.class, 1);
        tabMap.put(WeeklyReminder.class, 2);
        tabMap.put(MonthlyReminder.class, 3);
        tabMap.put(YearlyReminder.class, 4);

        daysBeforeTextField.disableProperty().bind(autoEnterCheckBox.selectedProperty().not());

        loadTab("NoneTab.fxml", "Tab.None");
        loadTab("DayTab.fxml", "Tab.Day");
        loadTab("WeekTab.fxml", "Tab.Week");
        loadTab("MonthTab.fxml", "Tab.Month");
        loadTab("YearTab.fxml", "Tab.Year");
    }

    private void loadTab(final String fxml, final String name) {
        final Tab tab = new Tab();
        tab.setText(resources.getString(name));

        final RecurringTabController controller = FXMLUtils.loadFXML(o -> tab.setContent((Node) o), fxml, resources);
        tab.setUserData(controller);
        tabs.getTabs().addAll(tab);
    }

    void showReminder(final Reminder reminder) {
        final Account a = reminder.getAccount();

        if (a != null) {
            accountComboBox.setValue(a);
        } else {
            System.err.println("did not find account");
        }

        transaction = reminder.getTransaction();

        if (transaction != null) {
            payeeTextField.setText(transaction.getPayee());
        }

        descriptionTextField.setText(reminder.getDescription());
        enabledCheckBox.setSelected(reminder.isEnabled());
        startDatePicker.setValue(reminder.getStartDate());
        notesTextArea.setText(reminder.getNotes());

        tabs.getSelectionModel().select(tabMap.get(reminder.getClass()));
        ((RecurringTabController)tabs.getSelectionModel().getSelectedItem().getUserData()).setReminder(reminder);

        if (reminder.getLastDate() != null) {
            final LocalDate lastOccurrence = reminder.getLastDate();
            lastOccurrenceTextField.setText(dateFormatter.format(lastOccurrence));
        }

        autoEnterCheckBox.setSelected(reminder.isAutoCreate());
        daysBeforeTextField.setInteger(reminder.getDaysAdvance());
    }

    private Reminder generateReminder() {
        final RecurringTabController tab = (RecurringTabController) tabs.getSelectionModel().getSelectedItem().getUserData();

        final Reminder reminder = tab.getReminder();

        reminder.setDescription(descriptionTextField.getText());
        reminder.setEnabled(enabledCheckBox.isSelected());
        reminder.setStartDate(startDatePicker.getValue());
        reminder.setNotes(notesTextArea.getText());

        reminder.setAutoCreate(autoEnterCheckBox.isSelected());

        reminder.setDaysAdvance(daysBeforeTextField.getInteger());
        reminder.setAccount(accountComboBox.getValue());
        reminder.setTransaction(transaction);

        return reminder;
    }

    public Optional<Reminder> getReminder() {
        return optionalReminder;
    }

    @FXML
    private void okAction() {
        optionalReminder = Optional.of(generateReminder());

        ((Stage) parentProperty.get().getWindow()).close();
    }

    @FXML
    private void cancelAction() {
        ((Stage) parentProperty.get().getWindow()).close();
    }

    @FXML
    private void handleDeleteTransaction() {
        transaction = null;
        payeeTextField.setText(null);
    }

    @FXML
    private void handleEditTransaction() {
        TransactionDialog.showAndWait(accountComboBox.getValue(), transaction, optional -> {
            if (optional.isPresent()) {
                transaction = optional.get();
                payeeTextField.setText(transaction.getPayee());
            }
        });
    }
}
