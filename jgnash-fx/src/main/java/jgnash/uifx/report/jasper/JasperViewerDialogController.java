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
package jgnash.uifx.report.jasper;

import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;

import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.BusyPane;
import jgnash.uifx.report.PortfolioReportController;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.views.main.MainView;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.FileUtils;
import jgnash.util.ResourceUtils;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.PrintPageFormat;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXlsReportConfiguration;

/**
 * Viewer controller for Jasper Reports.
 *
 * @author Craig Cavanaugh
 */
public final class JasperViewerDialogController {

    private static final String LAST_DIR = "lastDir";

    private static final int REPORT_RESOLUTION = 72;

    private static final float MIN_ZOOM = 0.5f;

    private static final float MAX_ZOOM = 10f;

    private static final int DEFAULT_ZOOMS[] = {50, 75, 100, 125, 150, 175, 200};

    private static final int DEFAULT_ZOOM_INDEX = 2;

    private static final int PAGE_BORDER = 8;

    private static final int UPDATE_PERIOD = 1500; // update period in milliseconds

    private final DoubleProperty zoomProperty = new SimpleDoubleProperty();

    private final DecimalFormat zoomDecimalFormat = new DecimalFormat("#.#");

    private double screenResolution = REPORT_RESOLUTION;

    private double zoom = 0;

    @FXML
    private StackPane reportControllerPane;

    @FXML
    private ToggleButton fitPageButton;

    @FXML
    private ToggleButton fitHeightButton;

    @FXML
    private ToggleButton fitWidthButton;

    @FXML
    private Button zoomInButton;

    @FXML
    private ComboBox<String> zoomComboBox;

    @FXML
    private Button zoomOutButton;

    @FXML
    private VBox pagePane;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private DropShadow dropShadow;

    @FXML
    private Label statusLabel;

    @FXML
    private Button firstButton;

    @FXML
    private Button previousButton;

    @FXML
    private Button nextButton;

    @FXML
    private Button lastButton;

    @FXML
    private Spinner<Integer> fontSizeSpinner;

    @FXML
    private Button reportFormatButton;

    @FXML
    private Button printButton;

    @FXML
    private Button saveButton;

    @FXML
    private StackPane stackPane;

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private ResourceBundle resources;

    private BusyPane busyPane;

    private final SimpleObjectProperty<DynamicJasperReport> report = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<JasperPrint> jasperPrint = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<Pane> reportControllerPaneProperty = new SimpleObjectProperty<>();

    private final IntegerProperty pageIndex = new SimpleIntegerProperty();

    private final IntegerProperty pageCount = new SimpleIntegerProperty();

    /**
     * Used to limit report update rates.
     */
    private final ScheduledThreadPoolExecutor reportExecutor = new ScheduledThreadPoolExecutor(1,
            new DefaultDaemonThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());

    @FXML
    private void initialize() {
        busyPane = new BusyPane();
        stackPane.getChildren().add(busyPane);

        screenResolution = Screen.getPrimary().getDpi();

        saveButton.disableProperty().bind(jasperPrint.isNull());
        printButton.disableProperty().bind(jasperPrint.isNull());
        reportFormatButton.disableProperty().bind(jasperPrint.isNull());
        fontSizeSpinner.disableProperty().bind(jasperPrint.isNull());

        firstButton.disableProperty().bind(jasperPrint.isNull().or(pageCount.isEqualTo(0))
                .or(pageIndex.isEqualTo(0)));
        previousButton.disableProperty().bind(jasperPrint.isNull().or(pageCount.isEqualTo(0))
                .or(pageIndex.isEqualTo(0)));

        nextButton.disableProperty().bind(jasperPrint.isNull().or(pageCount.isEqualTo(0))
                .or(pageIndex.isEqualTo(pageCount.subtract(1))));
        lastButton.disableProperty().bind(jasperPrint.isNull().or(pageCount.isEqualTo(0))
                .or(pageIndex.isEqualTo(pageCount.subtract(1))));

        fitPageButton.disableProperty().bind(jasperPrint.isNull());
        fitHeightButton.disableProperty().bind(jasperPrint.isNull());
        fitWidthButton.disableProperty().bind(jasperPrint.isNull());

        zoomComboBox.disableProperty().bind(jasperPrint.isNull());
        zoomInButton.disableProperty().bind(jasperPrint.isNull()
                .or(zoomProperty.greaterThanOrEqualTo(DEFAULT_ZOOMS[DEFAULT_ZOOMS.length - 1] / 100)));

        zoomOutButton.disableProperty().bind(jasperPrint.isNull()
                .or(zoomProperty.lessThanOrEqualTo(DEFAULT_ZOOMS[0] / 100)));

        fitPageButton.setSelected(true);

        firstButton.prefHeightProperty().bind(saveButton.heightProperty());
        previousButton.prefHeightProperty().bind(saveButton.heightProperty());
        nextButton.prefHeightProperty().bind(saveButton.heightProperty());
        lastButton.prefHeightProperty().bind(saveButton.heightProperty());
        zoomInButton.prefHeightProperty().bind(saveButton.heightProperty());
        zoomOutButton.prefHeightProperty().bind(saveButton.heightProperty());
        fitHeightButton.prefHeightProperty().bind(saveButton.heightProperty());
        fitWidthButton.prefHeightProperty().bind(saveButton.heightProperty());
        fitPageButton.prefHeightProperty().bind(saveButton.heightProperty());

        fontSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 15, 7));

        report.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                jasperPrint.set(newValue.createJasperPrint(false));

                fontSizeSpinner.valueFactoryProperty().get().setValue(newValue.getBaseFontSize());

                newValue.refreshCallBackProperty().set(() ->
                        createJasperPrint(newValue));
            } else {
                jasperPrint.set(null);
            }
        });

        jasperPrint.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                pageCount.set(newValue.getPages().size());
            } else {
                pageCount.set(0);
            }

            Platform.runLater(this::refresh);
        });

        reportControllerPaneProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                reportControllerPane.getChildren().addAll(newValue);
            }
        });

        fontSizeSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            report.get().setBaseFontSize(newValue);

            Platform.runLater(() -> createJasperPrint(report.get()));
        });

        pagePane.setSpacing(PAGE_BORDER);
        pagePane.setPadding(new Insets(PAGE_BORDER));
        pagePane.setAlignment(Pos.CENTER);

        scrollPane.viewportBoundsProperty().addListener((observable, oldValue, newValue) -> {
            if (fitWidthButton.isSelected()) {
                handleFitPageWidthAction();
            }

            scrollPane.setFitToWidth(pagePane.prefWidth(-1) < newValue.getWidth());
            scrollPane.setFitToHeight(pagePane.prefHeight(-1) < newValue.getHeight());
        });

        scrollPane.vvalueProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {

                    final double interval = 1d / pageCount.get();
                    double low = pageIndex.get() * interval;
                    double hi = low + interval;

                    int newPageIndex = pageIndex.get();

                    if (hi < newValue.doubleValue() && pageIndex.get() < pageCount.get()) {
                        while (hi < newValue.doubleValue()) {
                            newPageIndex++;
                            hi += interval;
                        }

                        setPageIndex(newPageIndex); // increase the page index to match the scroll position
                    } else if (low > newValue.doubleValue() && pageIndex.get() > 0) {
                        while (low > newValue.doubleValue()) {
                            newPageIndex--;
                            low -= interval;
                        }

                        setPageIndex(newPageIndex); // decrease the page index to match the scroll position
                    }
                }
        );

        for (int zoom : DEFAULT_ZOOMS) {
            zoomComboBox.getItems().add(zoom + "%");
        }
        zoomComboBox.getSelectionModel().select(DEFAULT_ZOOM_INDEX);
        zoomComboBox.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent e) -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleZoomChangedAction();
            }
        });

        setZoomRatio(1);
    }

    private void createJasperPrint(final DynamicJasperReport dynamicJasperReport) {

        // rate limit creation when print options are occurring quickly
        reportExecutor.schedule(() -> {
            if (reportExecutor.getQueue().size() < 1) {   // ignore if we already have one waiting in the queue
                final Task<Void> task = new Task<Void>() {
                    @Override
                    protected Void call() {
                        updateMessage(resources.getString("Message.CompilingReport"));
                        updateProgress(-1, Long.MAX_VALUE);

                        jasperPrint.set(dynamicJasperReport.createJasperPrint(false));
                        return null;
                    }
                };

                Platform.runLater(() -> {
                    busyPane.setTask(task);
                    new Thread(task).start();
                });

            }
        }, UPDATE_PERIOD, TimeUnit.MILLISECONDS);
    }

    public <T extends DynamicJasperReport> T loadReportController(final String fxmlResource) {
        try {
            final FXMLLoader fxmlLoader =
                    new FXMLLoader(PortfolioReportController.class.getResource(fxmlResource), resources);

            reportControllerPaneProperty.setValue(fxmlLoader.load());

            T controller = fxmlLoader.getController();
            report.setValue(controller);

            return controller;
        } catch (IOException e) {
            StaticUIMethods.displayException(e);
            return null;
        }
    }

    /**
     * Updates the status line and {@code pageIndex property} with the correct page index.
     *
     * @param index active page index
     */
    private void setPageIndex(final int index) {
        final JasperPrint jasperPrint = this.jasperPrint.get();

        if (jasperPrint != null && jasperPrint.getPages() != null && !jasperPrint.getPages().isEmpty()) {
            if (index >= 0 && index < pageCount.get()) {
                pageIndex.setValue(index);
                updateStatus(MessageFormat.format(resources.getString("Pattern.Pages"), index + 1, pageCount.get()));
            }
        } else {
            updateStatus("");
        }
    }

    private void setZoomRatio(final double newZoom) {
        if (newZoom > 0) {
            zoomComboBox.getEditor().setText(zoomDecimalFormat.format(newZoom * 100) + "%");

            if (zoomProperty.doubleValue() != newZoom) {
                zoomProperty.setValue(newZoom);
                zoom = (float) (zoomProperty.doubleValue() * screenResolution / REPORT_RESOLUTION);
                refresh();
            }
        }
    }

    private void setActualZoomRatio(final double newZoom) {
        if (newZoom > 0 && zoom != newZoom) {
            zoomProperty.set(newZoom * REPORT_RESOLUTION / screenResolution);
            zoom = newZoom;

            zoomComboBox.getEditor().setText(zoomDecimalFormat.format(zoomProperty.doubleValue() * 100) + "%");
            refresh();
        }
    }

    private void refresh() {
        final List<Node> children = pagePane.getChildren();
        children.clear();

        for (int i = 0; i < pageCount.get(); i++) {
            try {
                final BufferedImage bufferedImage =
                        (BufferedImage) JasperPrintManager.printPageToImage(jasperPrint.get(), i, (float) zoom);

                final ImageView imageView = new ImageView(SwingFXUtils.toFXImage(bufferedImage, null));
                imageView.setEffect(dropShadow);

                children.add(imageView);
            } catch (final JRException ex) {
                StaticUIMethods.displayException(ex);
            }
        }

        setPageIndex(0);
    }

    private void updateStatus(final String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }

    @FXML
    private void handleSaveAction() {
        Preferences preferences = Preferences.userNodeForPackage(JasperViewerDialogController.class);

        final String lastDir = preferences.get(LAST_DIR, null);

        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(ResourceUtils.getString("Title.SaveFile"));

        if (lastDir != null) {
            fileChooser.setInitialDirectory(new File(lastDir));
        }

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Document", "*.pdf", "*.PDF"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ODT Document", "*.odt", "*.ODT"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("DOCX Document", "*.docx", "*.DOCX"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XLSX Document", "*.xlsx", "*.XLSX"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Document", "*.csv", "*.CSV"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("RTF Document", "*.rtf", "*.RTF"));

        final File file = fileChooser.showSaveDialog(MainView.getPrimaryStage());

        if (file != null) {
            preferences.put(LAST_DIR, file.getParent());

            switch (FileUtils.getFileExtension(file.getAbsolutePath()).toLowerCase(Locale.ROOT)) {
                case "pdf":
                    try {
                        JasperExportManager.exportReportToPdfFile(jasperPrint.get(), file.getAbsolutePath());
                    } catch (final JRException e) {
                        StaticUIMethods.displayException(e);
                    }
                    break;
                case "odt":
                    try {
                        final JROdtExporter exporter = new JROdtExporter();
                        exporter.setExporterInput(new SimpleExporterInput(jasperPrint.get()));
                        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(file));
                        exporter.exportReport();
                    } catch (final JRException e) {
                        StaticUIMethods.displayException(e);
                    }
                    break;
                case "docx":
                    try {
                        final JRDocxExporter exporter = new JRDocxExporter();
                        exporter.setExporterInput(new SimpleExporterInput(jasperPrint.get()));
                        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(file));
                        exporter.exportReport();
                    } catch (final JRException e) {
                        StaticUIMethods.displayException(e);
                    }
                    break;
                case "xlsx":
                    try {
                        final JRXlsExporter exporter = new JRXlsExporter();
                        exporter.setExporterInput(new SimpleExporterInput(jasperPrint.get()));
                        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(file));
                        final SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
                        configuration.setOnePagePerSheet(false);
                        exporter.setConfiguration(configuration);
                        exporter.exportReport();
                    } catch (final JRException e) {
                        StaticUIMethods.displayException(e);
                    }
                    break;
                case "csv":
                    try {
                        final JRCsvExporter exporter = new JRCsvExporter();
                        exporter.setExporterInput(new SimpleExporterInput(jasperPrint.get()));
                        exporter.setExporterOutput(new SimpleWriterExporterOutput(file));
                        exporter.exportReport();
                    } catch (final JRException e) {
                        StaticUIMethods.displayException(e);
                    }
                    break;
                case "rtf":
                    try {
                        final JRRtfExporter exporter = new JRRtfExporter();
                        exporter.setExporterInput(new SimpleExporterInput(jasperPrint.get()));
                        exporter.setExporterOutput(new SimpleWriterExporterOutput(file));
                        exporter.exportReport();
                    } catch (final JRException e) {
                        StaticUIMethods.displayException(e);
                    }
                    break;
                default:
            }
        }

    }

    private void setPage(final int index) {
        double contentsHeight = pagePane.getBoundsInLocal().getHeight();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        ImageView iv = (ImageView) pagePane.getChildren().get(index);
        scrollPane.setVvalue(iv.getBoundsInParent().getMinY() / (contentsHeight - viewportHeight));
        setPageIndex(index);
    }

    @FXML
    private void handlePrintAction() {
        final Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    parent.get().setCursor(Cursor.WAIT);

                    JasperPrintManager.printReport(jasperPrint.get(), true);
                } catch (final JRException e) {
                    StaticUIMethods.displayException(e);
                } finally {
                    parent.get().setCursor(Cursor.DEFAULT);
                }

                return null;
            }
        };

        task.run();
    }

    @FXML
    private void handleFormatAction() {
        final PageFormat oldFormat = report.get().getPageFormat();
        final PrinterJob job = PrinterJob.getPrinterJob();

        final PageFormat format = job.pageDialog(oldFormat);

        if (format != oldFormat) {
            report.get().setPageFormat(format);

            createJasperPrint(report.get());

            Platform.runLater(this::refresh);
        }
    }

    @FXML
    private void handleFirstAction() {
        setPage(0);
    }

    @FXML
    private void handlePreviousAction() {
        setPage(pageIndex.get() - 1);
    }

    @FXML
    private void handleNextAction() {
        setPage(pageIndex.get() + 1);
    }

    @FXML
    private void handleLastAction() {
        setPage(jasperPrint.get().getPages().size() - 1);
    }

    private int getZoomRatio() {
        try {
            return zoomDecimalFormat.parse(zoomComboBox.getEditor().getText()).intValue();
        } catch (final ParseException e) {
            StaticUIMethods.displayException(e);
        }

        return zoomProperty.intValue();
    }

    @FXML
    private void handleZoomInAction() {
        fitPageButton.setSelected(false);
        fitHeightButton.setSelected(false);
        fitWidthButton.setSelected(false);

        int index = Arrays.binarySearch(DEFAULT_ZOOMS, getZoomRatio());

        if (index < 0) {
            zoomComboBox.getSelectionModel().select(zoomDecimalFormat.format(DEFAULT_ZOOMS[-index - 1]) + "%");
        } else if (index < zoomComboBox.getItems().size() - 1) {
            zoomComboBox.getSelectionModel().select(zoomDecimalFormat.format(DEFAULT_ZOOMS[index + 1]) + "%");
        }
    }

    @FXML
    private void handleZoomOutAction() {
        fitPageButton.setSelected(false);
        fitHeightButton.setSelected(false);
        fitWidthButton.setSelected(false);

        int index = Arrays.binarySearch(DEFAULT_ZOOMS, getZoomRatio());

        if (index > 0) {
            zoomComboBox.getSelectionModel().select(zoomDecimalFormat.format(DEFAULT_ZOOMS[index - 1]) + "%");
        } else if (index < -1) {
            zoomComboBox.getSelectionModel().select(zoomDecimalFormat.format(DEFAULT_ZOOMS[-index - DEFAULT_ZOOM_INDEX]) + "%");
        }
    }

    @FXML
    private void handleZoomChangedAction() {
        try {
            if (zoomComboBox.getValue() != null) { // Can be null when clearSelection() triggers the action
                float newZoom = zoomDecimalFormat.parse(zoomComboBox.getValue()).floatValue() / 100f;

                if (newZoom < MIN_ZOOM)
                    newZoom = MIN_ZOOM;

                if (newZoom > MAX_ZOOM)
                    newZoom = MAX_ZOOM;

                setZoomRatio(newZoom);
            }
        } catch (final ParseException e) {
            StaticUIMethods.displayException(e);
        }
    }

    @FXML
    private void handleFitHeightAction() {
        final PrintPageFormat pageFormat = jasperPrint.get().getPageFormat(pageIndex.get());

        final double heightRatio = (scrollPane.getViewportBounds().getHeight() - (2 * PAGE_BORDER))
                / pageFormat.getPageHeight();

        final double widthRatio = (scrollPane.getViewportBounds().getWidth() - (2 * PAGE_BORDER))
                / pageFormat.getPageWidth();

        zoomComboBox.getSelectionModel().clearSelection();

        setActualZoomRatio(heightRatio < widthRatio ? heightRatio : widthRatio);
    }

    @FXML
    private void handleFitPageWidthAction() {
        zoomComboBox.getSelectionModel().clearSelection();

        setActualZoomRatio((scrollPane.getViewportBounds().getWidth() - (2 * PAGE_BORDER)) /
                jasperPrint.get().getPageFormat(pageIndex.get()).getPageWidth());
    }

    @FXML
    private void handleActualSizeAction() {
        zoomComboBox.getSelectionModel().select(DEFAULT_ZOOM_INDEX); // 100% size
    }
}
