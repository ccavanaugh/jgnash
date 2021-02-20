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
package jgnash.uifx.dialog.security;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import jgnash.engine.CommodityNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.MathConstants;
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityHistoryEvent;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.net.security.UpdateFactory;
import jgnash.resource.util.ResourceUtils;
import jgnash.text.NumericFormats;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.BigDecimalTableCell;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.IntegerTextField;
import jgnash.uifx.control.SecurityComboBox;
import jgnash.uifx.control.SecurityHistoryEventTypeComboBox;
import jgnash.uifx.control.SecurityNodeAreaChart;
import jgnash.uifx.control.ShortDateTableCell;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Security history controller.
 *
 * @author Craig Cavanaugh
 */
public class SecurityHistoryController implements MessageListener {

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private SecurityHistoryEventTypeComboBox securityEventTypeComboBox;

    @FXML
    private DatePickerEx eventDatePicker;

    @FXML
    private DecimalTextField eventValueTextField;

    @FXML
    private Button deleteEventButton;

    @FXML
    private Button addEventButton;

    @FXML
    private StackPane chartPane;

    @FXML
    private Button addPriceButton;

    @FXML
    private Button updatePriceButton;

    @FXML
    private Button updateEventButton;

    @FXML
    private TableView<SecurityHistoryNode> priceTableView;

    @FXML
    private TableView<SecurityHistoryEvent> eventTableView;

    @FXML
    private SecurityComboBox securityComboBox;

    @FXML
    private DatePickerEx historyDatePicker;

    @FXML
    private DecimalTextField closeTextField;

    @FXML
    private IntegerTextField volumeTextField;

    @FXML
    private DecimalTextField highTextField;

    @FXML
    private DecimalTextField lowTextField;

    @FXML
    private Button deletePriceButton;

    @FXML
    private ResourceBundle resources;

    private SecurityNodeAreaChart chart;

    private final SimpleObjectProperty<SecurityHistoryNode> selectedSecurityHistoryNode = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<SecurityHistoryEvent> selectedSecurityHistoryEvent = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<SecurityNode> selectedSecurityNode = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<NumberFormat> numberFormat = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<QuoteSource> quoteSource = new SimpleObjectProperty<>();

    private final ObservableList<SecurityHistoryNode> observableHistoryNodes = FXCollections.observableArrayList();

    private final SortedList<SecurityHistoryNode> sortedHistoryList = new SortedList<>(observableHistoryNodes);

    private final ObservableList<SecurityHistoryEvent> observableHistoryEventList = FXCollections.observableArrayList();

    private final SortedList<SecurityHistoryEvent> sortedHistoryEventList = new SortedList<>(observableHistoryEventList);

    @FXML
    void initialize() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        numberFormat.set(NumericFormats.getShortCommodityFormat(engine.getDefaultCurrency()));

        selectedSecurityHistoryNode.bind(priceTableView.getSelectionModel().selectedItemProperty());
        selectedSecurityNode.bind(securityComboBox.getSelectionModel().selectedItemProperty());

        deletePriceButton.disableProperty().bind(Bindings.isNull(selectedSecurityHistoryNode));

        selectedSecurityHistoryEvent.bind(eventTableView.getSelectionModel().selectedItemProperty());
        deleteEventButton.disableProperty().bind(Bindings.isNull(selectedSecurityHistoryEvent));

        // Disabled the update button if a security is not selected, or it does not have a quote source
        updatePriceButton.disableProperty().bind(Bindings.or(Bindings.isNull(selectedSecurityNode),
                Bindings.equal(QuoteSource.NONE, quoteSource)));

        // Disabled the update button if a security is not selected, or it does not have a quote source
        updateEventButton.disableProperty().bind(Bindings.or(Bindings.isNull(selectedSecurityNode),
                Bindings.equal(QuoteSource.NONE, quoteSource)));

        // Can't add if a security is not selected
        addPriceButton.disableProperty().bind(Bindings.isNull(selectedSecurityNode));

        // Can't add if a security is not selected and a value is not set
        addEventButton.disableProperty().bind(Bindings.isNull(selectedSecurityNode)
                .or(Bindings.isEmpty(eventValueTextField.textProperty())));


        priceTableView.setTableMenuButtonVisible(false);
        priceTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        final TableColumn<SecurityHistoryNode, LocalDate> priceDateColumn = new TableColumn<>(resources.getString("Column.Date"));
        priceDateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getLocalDate()));
        priceDateColumn.setCellFactory(cell -> new ShortDateTableCell<>());
        priceTableView.getColumns().add(priceDateColumn);

        final TableColumn<SecurityHistoryNode, BigDecimal> priceCloseColumn = new TableColumn<>(resources.getString("Column.Close"));
        priceCloseColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getPrice()));
        priceCloseColumn.setCellFactory(cell -> new BigDecimalTableCell<>(numberFormat));
        priceTableView.getColumns().add(priceCloseColumn);

        final TableColumn<SecurityHistoryNode, BigDecimal> priceLowColumn = new TableColumn<>(resources.getString("Column.Low"));
        priceLowColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getLow()));
        priceLowColumn.setCellFactory(cell -> new BigDecimalTableCell<>(numberFormat));
        priceTableView.getColumns().add(priceLowColumn);

        final TableColumn<SecurityHistoryNode, BigDecimal> priceHighColumn = new TableColumn<>(resources.getString("Column.High"));
        priceHighColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getHigh()));
        priceHighColumn.setCellFactory(cell -> new BigDecimalTableCell<>(numberFormat));
        priceTableView.getColumns().add(priceHighColumn);

        final TableColumn<SecurityHistoryNode, Long> priceVolumeColumn = new TableColumn<>(resources.getString("Column.Volume"));
        priceVolumeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getVolume()));
        priceVolumeColumn.setCellFactory(cell -> new LongFormatTableCell());
        priceTableView.getColumns().add(priceVolumeColumn);

        priceTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        sortedHistoryList.comparatorProperty().bind(priceTableView.comparatorProperty());

        priceTableView.setItems(sortedHistoryList);

        final TableColumn<SecurityHistoryEvent, LocalDate> eventDateColumn = new TableColumn<>(resources.getString("Column.Date"));
        eventDateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDate()));
        eventDateColumn.setCellFactory(cell -> new ShortDateTableCell<>());
        eventTableView.getColumns().add(eventDateColumn);

        final TableColumn<SecurityHistoryEvent, String> eventActionColumn = new TableColumn<>(resources.getString("Column.Event"));
        eventActionColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getType().toString()));
        eventTableView.getColumns().add(eventActionColumn);

        final NumberFormat decimalFormat = NumberFormat.getInstance();
        if (decimalFormat instanceof DecimalFormat) {
            decimalFormat.setMinimumFractionDigits(MathConstants.SECURITY_PRICE_ACCURACY);
            decimalFormat.setMaximumFractionDigits(MathConstants.SECURITY_PRICE_ACCURACY);
        }

        final TableColumn<SecurityHistoryEvent, BigDecimal> eventValueColumn = new TableColumn<>(resources.getString("Column.Value"));
        eventValueColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue()));
        eventValueColumn.setCellFactory(cell -> new BigDecimalTableCell<>(decimalFormat));
        eventTableView.getColumns().add(eventValueColumn);

        eventTableView.setTableMenuButtonVisible(false);
        eventTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        eventTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        sortedHistoryEventList.comparatorProperty().bind(eventTableView.comparatorProperty());

        eventTableView.setItems(sortedHistoryEventList);

        eventValueTextField.scaleProperty().set(MathConstants.SECURITY_PRICE_ACCURACY);

        chart = new SecurityNodeAreaChart();
        chart.securityNodeProperty().bind(selectedSecurityNode);

        chartPane.getChildren().addAll(chart);

        securityComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                numberFormat.set(NumericFormats.getShortCommodityFormat(newValue.getReportedCurrencyNode()));

                closeTextField.scaleProperty().set(newValue.getScale());
                lowTextField.scaleProperty().set(newValue.getScale());
                highTextField.scaleProperty().set(newValue.getScale());

                quoteSource.set(newValue.getQuoteSource());

                JavaFXUtils.runLater(this::loadTables);
            }
        });

        selectedSecurityHistoryNode.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadPriceForm();
            } else {
                clearPriceForm();
            }
        });

        selectedSecurityHistoryEvent.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadEventForm();
            } else {
                clearEventForm();
            }
        });

        // Install a listener to unregister from the message bus when the window closes
        parent.addListener((observable, oldValue, scene) -> {
            if (scene != null) {
                scene.windowProperty().get().addEventHandler(WindowEvent.WINDOW_HIDING, event -> {
                    Logger.getLogger(SecurityHistoryController.class.getName()).info("Unregistered from the message bus");
                    MessageBus.getInstance().unregisterListener(SecurityHistoryController.this, MessageChannel.COMMODITY);
                });
            }
        });

        JavaFXUtils.runLater(()
                -> MessageBus.getInstance().registerListener(SecurityHistoryController.this, MessageChannel.COMMODITY));
    }

    private void loadPriceForm() {
        historyDatePicker.setValue(selectedSecurityHistoryNode.get().getLocalDate());
        closeTextField.setDecimal(selectedSecurityHistoryNode.get().getPrice());
        lowTextField.setDecimal(selectedSecurityHistoryNode.get().getLow());
        highTextField.setDecimal(selectedSecurityHistoryNode.get().getHigh());
        volumeTextField.setLong(selectedSecurityHistoryNode.get().getVolume());
    }

    private void loadEventForm() {
        JavaFXUtils.runLater(() -> {
            eventDatePicker.setValue(selectedSecurityHistoryEvent.get().getDate());
            eventValueTextField.setDecimal(selectedSecurityHistoryEvent.get().getValue());
            securityEventTypeComboBox.setValue(selectedSecurityHistoryEvent.get().getType());
        });

    }

    private void clearPriceForm() {
        historyDatePicker.setValue(LocalDate.now());
        closeTextField.setDecimal(BigDecimal.ZERO);
        volumeTextField.setText(null);
        lowTextField.setDecimal(BigDecimal.ZERO);
        highTextField.setDecimal(BigDecimal.ZERO);
    }

    private void clearEventForm() {
        JavaFXUtils.runLater(() -> {
            eventDatePicker.setValue(LocalDate.now());
            eventValueTextField.setDecimal(BigDecimal.ZERO);
        });
    }

    @FXML
    private void handleDeletePriceAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final List<SecurityHistoryNode> historyNodes = new ArrayList<>(priceTableView.getSelectionModel().getSelectedItems());

        Collections.reverse(historyNodes);  // work backwards through the deletion list

        for (final SecurityHistoryNode historyNode : historyNodes) {
            engine.removeSecurityHistory(selectedSecurityNode.get(), historyNode.getLocalDate());
        }
    }

    @FXML
    private void handleClearPriceAction() {
        priceTableView.getSelectionModel().clearSelection();
        clearPriceForm();
    }

    @FXML
    private void handleAddPriceAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final SecurityHistoryNode history = new SecurityHistoryNode(historyDatePicker.getValue(), closeTextField.getDecimal(),
                volumeTextField.getLong(), highTextField.getDecimal(), lowTextField.getDecimal());

        engine.addSecurityHistory(selectedSecurityNode.get(), history);

        clearPriceForm();
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parent.get().getWindow()).close();
    }

    @FXML
    private void handleOnlineUpdate() {
        if (!UpdateFactory.updateOne(selectedSecurityNode.get())) {
            StaticUIMethods.displayWarning(ResourceUtils.getString("Message.Error.SecurityUpdate",
                    selectedSecurityNode.get().getSymbol()));
        }
    }

    @FXML
    private void handleDeleteEventAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final List<SecurityHistoryEvent> events = new ArrayList<>(eventTableView.getSelectionModel().getSelectedItems());

        Collections.reverse(events);  // work backwards through the deletion list

        for (final SecurityHistoryEvent securityHistoryEvent : events) {
            engine.removeSecurityHistoryEvent(selectedSecurityNode.get(), securityHistoryEvent);
        }
    }

    @FXML
    private void handleAddEventAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final SecurityHistoryEvent event = new SecurityHistoryEvent(securityEventTypeComboBox.getValue(),
                eventDatePicker.getValue(), eventValueTextField.getDecimal());

        engine.addSecurityHistoryEvent(selectedSecurityNode.get(), event);

        clearEventForm();
    }

    @FXML
    private void handleOnlineEventUpdate() {
        UpdateFactory.updateSecurityEvents(selectedSecurityNode.get());
    }

    @FXML
    private void handleClearEventAction() {
        eventTableView.getSelectionModel().clearSelection();
        clearEventForm();
    }

    private void loadTables() {
        priceTableView.getSelectionModel().clearSelection();
        observableHistoryNodes.setAll(securityComboBox.getValue().getHistoryNodes());
        priceTableView.scrollTo(observableHistoryNodes.size() - 1);


        final List<SecurityHistoryEvent> events = new ArrayList<>(securityComboBox.getValue().getHistoryEvents());
        Collections.sort(events);   // events are not sorted

        eventTableView.getSelectionModel().clearSelection();
        observableHistoryEventList.setAll(events);
        eventTableView.scrollTo(observableHistoryEventList.size() - 1);
    }

    @Override
    public void messagePosted(final Message message) {
        final CommodityNode eventNode = message.getObject(MessageProperty.COMMODITY);

        if (eventNode.equals(selectedSecurityNode.get())) {
            switch (message.getEvent()) {
                case SECURITY_HISTORY_ADD:
                case SECURITY_HISTORY_REMOVE:
                case SECURITY_HISTORY_EVENT_ADD:
                case SECURITY_HISTORY_EVENT_REMOVE:
                    JavaFXUtils.runLater(this::loadTables);
                    JavaFXUtils.runLater(chart::update);
                    break;
                default:
            }
        }
    }

    @FXML
    private void handleRemoveWeekendsAction() {
        if (StaticUIMethods.showConfirmationDialog(resources.getString("Title.Confirm"),
                resources.getString("Message.ConfirmSecurityHistoryDelete")).getButtonData() == ButtonBar.ButtonData.YES) {

            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            engine.removeSecurityHistoryByDayOfWeek(selectedSecurityNode.get(),
                    Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
        }
    }

    @FXML
    private void handleKeepFridaysOnlyAction() {
        if (StaticUIMethods.showConfirmationDialog(resources.getString("Title.Confirm"),
                resources.getString("Message.ConfirmSecurityHistoryDelete")).getButtonData() == ButtonBar.ButtonData.YES) {

            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            engine.removeSecurityHistoryByDayOfWeek(selectedSecurityNode.get(),
                    Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
        }
    }

    @FXML
    private void handleKeepMondaysOnlyAction() {
        if (StaticUIMethods.showConfirmationDialog(resources.getString("Title.Confirm"),
                resources.getString("Message.ConfirmSecurityHistoryDelete")).getButtonData() == ButtonBar.ButtonData.YES) {

            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            engine.removeSecurityHistoryByDayOfWeek(selectedSecurityNode.get(),
                    Arrays.asList(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
                            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
        }
    }

    private static class LongFormatTableCell extends TableCell<SecurityHistoryNode, Long> {

        private final NumberFormat volumeFormat = NumberFormat.getIntegerInstance();

        LongFormatTableCell() {
            setStyle("-fx-alignment: center-right;"); // Right align
        }

        @Override
        protected void updateItem(final Long amount, final boolean empty) {
            super.updateItem(amount, empty);  // required

            if (!empty && amount != null) {
                setText(volumeFormat.format(amount));
            } else {
                setText(null);
            }
        }
    }
}
