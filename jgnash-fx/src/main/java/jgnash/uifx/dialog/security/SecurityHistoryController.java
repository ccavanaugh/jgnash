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
package jgnash.uifx.dialog.security;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
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
import jgnash.engine.QuoteSource;
import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.engine.message.MessageProperty;
import jgnash.net.security.UpdateFactory;
import jgnash.text.CommodityFormat;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.IntegerTextField;
import jgnash.uifx.control.LocalDateAxis;
import jgnash.uifx.control.SecurityComboBox;
import jgnash.uifx.control.ShortDateTableCell;
import jgnash.uifx.util.InjectFXML;
import jgnash.util.DateUtils;
import jgnash.util.ResourceUtils;

/**
 * Security history controller
 *
 * @author Craig Cavanaugh
 */
public class SecurityHistoryController implements MessageListener {

    @InjectFXML
    private final ObjectProperty<Scene> parentProperty = new SimpleObjectProperty<>();

    @FXML
    private StackPane chartPane;

    @FXML
    private Button addButton;

    @FXML
    private Button updateButton;

    @FXML
    private TableView<SecurityHistoryNode> tableView;

    @FXML
    private SecurityComboBox securityComboBox;

    @FXML
    private DatePickerEx datePicker;

    @FXML
    private DecimalTextField closeTextField;

    @FXML
    private IntegerTextField volumeTextField;

    @FXML
    private DecimalTextField highTextField;

    @FXML
    private DecimalTextField lowTextField;

    @FXML
    private Button deleteButton;

    @FXML
    private ResourceBundle resources;

    private AreaChart<LocalDate, Number> chart;

    private final SimpleObjectProperty<SecurityHistoryNode> selectedSecurityHistoryNode = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<SecurityNode> selectedSecurityNode = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<NumberFormat> numberFormatProperty = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<QuoteSource> quoteSourceProperty = new SimpleObjectProperty<>();

    private final ObservableList<SecurityHistoryNode> observableHistoryNodes = FXCollections.observableArrayList();

    private final SortedList<SecurityHistoryNode> sortedList = new SortedList<>(observableHistoryNodes);

    @FXML
    void initialize() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        MessageBus.getInstance().registerListener(this, MessageChannel.COMMODITY);

        numberFormatProperty.setValue(CommodityFormat.getShortNumberFormat(engine.getDefaultCurrency()));

        selectedSecurityHistoryNode.bind(tableView.getSelectionModel().selectedItemProperty());
        selectedSecurityNode.bind(securityComboBox.getSelectionModel().selectedItemProperty());

        deleteButton.disableProperty().bind(Bindings.isNull(selectedSecurityHistoryNode));

        // Disabled the update button if a security is not selected, or it does not have a quote source
        updateButton.disableProperty().bind(Bindings.or(Bindings.isNull(selectedSecurityNode),
                Bindings.equal(QuoteSource.NONE, quoteSourceProperty)));

        // Can't add if a security is not selected
        addButton.disableProperty().bind(Bindings.isNull(selectedSecurityNode));

        tableView.setTableMenuButtonVisible(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        final TableColumn<SecurityHistoryNode, Date> dateColumn = new TableColumn<>(resources.getString("Column.Date"));
        dateColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getDate()));
        dateColumn.setCellFactory(cell -> new ShortDateTableCell());
        tableView.getColumns().add(dateColumn);

        final TableColumn<SecurityHistoryNode, BigDecimal> closeColumn = new TableColumn<>(resources.getString("Column.Close"));
        closeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getPrice()));
        closeColumn.setCellFactory(cell -> new BigDecimalFormatTableCell());
        tableView.getColumns().add(closeColumn);

        final TableColumn<SecurityHistoryNode, BigDecimal> lowColumn = new TableColumn<>(resources.getString("Column.Low"));
        lowColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getLow()));
        lowColumn.setCellFactory(cell -> new BigDecimalFormatTableCell());
        tableView.getColumns().add(lowColumn);

        final TableColumn<SecurityHistoryNode, BigDecimal> highColumn = new TableColumn<>(resources.getString("Column.High"));
        highColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getHigh()));
        highColumn.setCellFactory(cell -> new BigDecimalFormatTableCell());
        tableView.getColumns().add(highColumn);

        final TableColumn<SecurityHistoryNode, Long> volumeColumn = new TableColumn<>(resources.getString("Column.Volume"));
        volumeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getVolume()));
        volumeColumn.setCellFactory(cell -> new LongFormatTableCell());
        tableView.getColumns().add(volumeColumn);

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        sortedList.comparatorProperty().bind(tableView.comparatorProperty());

        tableView.setItems(sortedList);

        chart = new AreaChart<>(new LocalDateAxis(), new NumberAxis());
        chart.setCreateSymbols(false);

        chartPane.getChildren().addAll(chart);

        securityComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                numberFormatProperty.setValue(CommodityFormat.getShortNumberFormat(newValue.getReportedCurrencyNode()));

                closeTextField.scaleProperty().setValue(newValue.getScale());
                lowTextField.scaleProperty().setValue(newValue.getScale());
                highTextField.scaleProperty().setValue(newValue.getScale());

                quoteSourceProperty.setValue(newValue.getQuoteSource());

                Platform.runLater(() -> {
                    loadTable();
                    loadChart();
                });
            }
        });

        selectedSecurityHistoryNode.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadForm();
            } else {
                clearForm();
            }
        });

        // Install a listener to unregister from the message bus when the window closes
        parentProperty.addListener((observable, oldValue, scene) -> {
            if (scene != null) {
                scene.windowProperty().addListener((observable1, oldValue1, window) -> {
                    window.addEventHandler(WindowEvent.WINDOW_HIDING, event -> {
                        Logger.getLogger(SecurityHistoryController.class.getName()).info("Unregistered from the message bus");
                        MessageBus.getInstance().unregisterListener(SecurityHistoryController.this, MessageChannel.COMMODITY);
                    });
                });
            }
        });
    }

    private void loadForm() {
        datePicker.setDate(selectedSecurityHistoryNode.get().getDate());
        closeTextField.setDecimal(selectedSecurityHistoryNode.get().getPrice());
        lowTextField.setDecimal(selectedSecurityHistoryNode.get().getLow());
        highTextField.setDecimal(selectedSecurityHistoryNode.get().getHigh());
        volumeTextField.setLong(selectedSecurityHistoryNode.get().getVolume());
    }

    private void clearForm() {
        datePicker.setValue(LocalDate.now());
        closeTextField.setDecimal(BigDecimal.ZERO);
        volumeTextField.setText(null);
        lowTextField.setDecimal(BigDecimal.ZERO);
        highTextField.setDecimal(BigDecimal.ZERO);
    }

    @FXML
    private void handleDeleteAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final List<SecurityHistoryNode> historyNodes = new ArrayList<>(tableView.getSelectionModel().getSelectedItems());

        Collections.reverse(historyNodes);  // work backwards through the deletion list

        for (final SecurityHistoryNode historyNode : historyNodes) {
            engine.removeSecurityHistory(selectedSecurityNode.get(), historyNode.getDate());
        }
    }

    @FXML
    private void handleClearAction() {
        tableView.getSelectionModel().clearSelection();
        clearForm();
    }

    @FXML
    private void handleAddAction() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final SecurityHistoryNode history = new SecurityHistoryNode(datePicker.getDate(), closeTextField.getDecimal(),
                volumeTextField.getLong(), highTextField.getDecimal(), lowTextField.getDecimal());

        engine.addSecurityHistory(selectedSecurityNode.get(), history);

        clearForm();
    }

    @FXML
    private void handelCloseAction() {
        ((Stage)parentProperty.get().getWindow()).close();
    }

    @FXML
    private void handleOnlineUpdate() {
        if (!UpdateFactory.updateOne(selectedSecurityNode.get())) {
            StaticUIMethods.displayWarning(ResourceUtils.getString("Message.Error.SecurityUpdate",
                    selectedSecurityNode.get().getSymbol()));
        }
    }

    private void loadTable() {
        observableHistoryNodes.setAll(securityComboBox.getValue().getHistoryNodes());
    }

    @SuppressWarnings("unchecked")
    private void loadChart() {
        new Thread(() -> {
            final AreaChart.Series series = new AreaChart.Series();

            series.setName(selectedSecurityNode.get().getSymbol());

            for (final SecurityHistoryNode node : selectedSecurityNode.get().getHistoryNodes()) {
                series.getData().add(new AreaChart.Data<>(DateUtils.asLocalDate(node.getDate()), node.getPrice()));
            }

            Platform.runLater(() -> chart.getData().setAll(series));
        }).start();
    }

    @Override
    public void messagePosted(final Message message) {
        final CommodityNode eventNode = message.getObject(MessageProperty.COMMODITY);

        if (eventNode.equals(selectedSecurityNode.get())) {
            switch (message.getEvent()) {
                case SECURITY_HISTORY_REMOVE:
                case SECURITY_HISTORY_ADD:
                    Platform.runLater(this::loadTable);
                    Platform.runLater(this::loadChart);
                    break;
                default:
            }
        }
    }

    private class BigDecimalFormatTableCell extends TableCell<SecurityHistoryNode, BigDecimal> {

        public BigDecimalFormatTableCell() {
            setStyle("-fx-alignment: center-right;");  // Right align
        }

        @Override
        protected void updateItem(final BigDecimal amount, final boolean empty) {
            super.updateItem(amount, empty);  // required

            if (!empty && amount != null) {
                setText(numberFormatProperty.get().format(amount));
            } else {
                setText(null);
            }
        }
    }

    private static class LongFormatTableCell extends TableCell<SecurityHistoryNode, Long> {

        private final NumberFormat volumeFormat = NumberFormat.getIntegerInstance();

        public LongFormatTableCell() {
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
