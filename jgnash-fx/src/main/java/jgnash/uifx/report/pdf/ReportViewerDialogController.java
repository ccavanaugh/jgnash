/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
package jgnash.uifx.report.pdf;

import jgnash.report.pdf.Report;
import jgnash.report.poi.Workbook;
import jgnash.report.table.AbstractReportTableModel;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.BusyPane;
import jgnash.uifx.report.ReportActions;
import jgnash.uifx.util.FXMLUtils;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.uifx.util.StageUtils;
import jgnash.uifx.views.main.MainView;
import jgnash.util.DefaultDaemonThreadFactory;
import jgnash.util.FileUtils;

import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;

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
import javafx.stage.Stage;

/**
 * Viewer controller for PDFBox Reports.
 *
 * @author Craig Cavanaugh
 */
public class ReportViewerDialogController {

    private static final String LAST_DIR = "lastDir";

    private static final int REPORT_RESOLUTION = 72;

    private static final int UP_SCALING = 2;

    private static final float MIN_ZOOM = 0.5f;

    private static final float MAX_ZOOM = 10f;

    private static final int[] DEFAULT_ZOOMS = {50, 75, 100, 125, 150, 175, 200};

    private static final int DEFAULT_ZOOM_INDEX = 2;

    private static final int PAGE_BORDER = 8;

    private static final int UPDATE_PERIOD = 1500; // update period in milliseconds

    private final DoubleProperty zoomProperty = new SimpleDoubleProperty(1.0);

    private final DecimalFormat zoomDecimalFormat = new DecimalFormat("#.#");

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
    private Spinner<Double> fontSizeSpinner;

    @FXML
    private Button reportFormatButton;

    @FXML
    private Button saveButton;

    @FXML
    private StackPane stackPane;

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private ResourceBundle resources;

    private BusyPane busyPane;

    private final SimpleObjectProperty<Report> report = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<Pane> reportControllerPaneProperty = new SimpleObjectProperty<>();

    private final IntegerProperty pageIndex = new SimpleIntegerProperty();

    private final IntegerProperty pageCount = new SimpleIntegerProperty();

    private ReportController reportController;

    /**
     * Used to limit report update rates.
     */
    private final ScheduledThreadPoolExecutor reportExecutor = new ScheduledThreadPoolExecutor(1,
            new DefaultDaemonThreadFactory("Report View Executor"), new ThreadPoolExecutor.DiscardPolicy());

    @FXML
    private void initialize() {
        busyPane = new BusyPane();
        stackPane.getChildren().add(busyPane);

        saveButton.disableProperty().bind(report.isNull());
        reportFormatButton.disableProperty().bind(report.isNull());
        fontSizeSpinner.disableProperty().bind(report.isNull());

        firstButton.disableProperty().bind(report.isNull().or(pageCount.isEqualTo(0))
                .or(pageIndex.isEqualTo(0)));
        previousButton.disableProperty().bind(report.isNull().or(pageCount.isEqualTo(0))
                .or(pageIndex.isEqualTo(0)));

        nextButton.disableProperty().bind(report.isNull().or(pageCount.isEqualTo(0))
                .or(pageIndex.isEqualTo(pageCount.subtract(1))));
        lastButton.disableProperty().bind(report.isNull().or(pageCount.isEqualTo(0))
                .or(pageIndex.isEqualTo(pageCount.subtract(1))));

        fitPageButton.disableProperty().bind(report.isNull());
        fitHeightButton.disableProperty().bind(report.isNull());
        fitWidthButton.disableProperty().bind(report.isNull());

        zoomComboBox.disableProperty().bind(report.isNull());
        zoomInButton.disableProperty().bind(report.isNull()
                .or(zoomProperty.greaterThanOrEqualTo(DEFAULT_ZOOMS[DEFAULT_ZOOMS.length - 1] / 100)));

        zoomOutButton.disableProperty().bind(report.isNull()
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
        fontSizeSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(5, 15, 7));

        // act when the report property has been set or changed
        report.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                fontSizeSpinner.valueFactoryProperty().get().setValue((double) newValue.getBaseFontSize());
            }
        });

        reportControllerPaneProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                reportControllerPane.getChildren().addAll(newValue);
            }
        });

        fontSizeSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            report.get().setBaseFontSize(newValue.floatValue());

            if (reportController != null) {
                reportController.refreshReport();
            }
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

        // this ensures the report is properly closed when the dialog is closed
        parent.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                parent.get().getWindow().setOnCloseRequest(event -> {
                    try {
                        report.get().close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private void refreshReport() {
        System.out.println("Report was Refreshed!!!!!");
        refresh();
    }

    public <T extends ReportController> T loadReportController(final String fxmlResource) {
        try {
            final FXMLLoader fxmlLoader =
                    new FXMLLoader(ReportActions.class.getResource(fxmlResource), resources);

            reportControllerPaneProperty.setValue(fxmlLoader.load());

            T reportController = fxmlLoader.getController();

            // install handler for refreshing a report
            reportController.setRefreshRunnable(this::refreshReport);

            // save the reference to the report
            reportController.getReport(report::set);

            this.reportController = reportController;

            return reportController;
        } catch (final IOException e) {
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
        if (index >= 0 && index < pageCount.get()) {
            pageIndex.setValue(index);
            updateStatus(MessageFormat.format(resources.getString("Pattern.Pages"), index + 1, pageCount.get()));
        } else {
            updateStatus("");
        }
    }

    private void setZoomRatio(final double newZoom) {
        if (newZoom > 0) {
            zoomComboBox.getEditor().setText(zoomDecimalFormat.format(newZoom * 100) + "%");

            if (zoomProperty.doubleValue() != newZoom) {
                zoomProperty.setValue(newZoom);
            }
        }
    }

    private void setActualZoomRatio(final double newZoom) {
        if (newZoom > 0) {
            zoomProperty.set(newZoom);

            zoomComboBox.getEditor().setText(zoomDecimalFormat.format(zoomProperty.doubleValue() * 100) + "%");
        }
    }

    private void refresh() {
        final List<Node> children = pagePane.getChildren();
        children.clear();
        pageCount.set(0);

        reportExecutor.schedule(() -> {
            if (reportExecutor.getQueue().size() < 1) {   // ignore if we already have one waiting in the queue

                final Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() {
                        updateMessage(resources.getString("Message.CompilingReport"));
                        updateProgress(-1, Long.MAX_VALUE);

                        if (report.get() != null) {

                            for (int i = 0; i < report.get().getPageCount(); i++) {
                                try {

                                    // report resolution is fixed and the ImageView width and height are adjusted to the zoom value
                                    final BufferedImage bufferedImage = report.get().renderImage(i, REPORT_RESOLUTION * UP_SCALING);

                                    JavaFXUtils.runLater(() -> {

                                        final ImageView imageView = new ImageView(SwingFXUtils.toFXImage(bufferedImage, null));

                                        imageView.setEffect(dropShadow);

                                        // bind the width and height to the zoom level
                                        imageView.fitWidthProperty().bind(zoomProperty.multiply(bufferedImage.getWidth() / UP_SCALING));
                                        imageView.fitHeightProperty().bind(zoomProperty.multiply(bufferedImage.getHeight() / UP_SCALING));

                                        children.add(imageView);

                                        pageCount.set(pageCount.get() + 1);
                                    });

                                } catch (final IOException ex) {
                                    StaticUIMethods.displayException(ex);
                                }
                            }
                        }

                        JavaFXUtils.runLater(() -> setPageIndex(0));

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

    private void updateStatus(final String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }

    @FXML
    private void handleSaveAction() {
        final Preferences pref = Preferences.userNodeForPackage(ReportViewerDialogController.class);

        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(ResourceUtils.getString("Title.SaveFile"));

        final File initialDirectory = new File(pref.get(LAST_DIR, System.getProperty("user.home")));

        // Protect against an IllegalArgumentException
        if (initialDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(resources.getString("Label.PDFFiles") + " (.pdf)", "*.pdf", "*.PDF"),
                new FileChooser.ExtensionFilter(resources.getString("Label.SpreadsheetFiles") + " (*.xls, *.xlsx)",
                        "*.xls", "*.xlsx")
        );

        final File file = fileChooser.showSaveDialog(MainView.getPrimaryStage());

        if (file != null) {
            pref.put(LAST_DIR, file.getParent());

            final String extension = FileUtils.getFileExtension(file.getAbsolutePath()).toLowerCase(Locale.ROOT);

            switch (extension) {
                case "pdf":
                    try {
                        report.get().saveToFile(file.toPath());
                    } catch (final IOException ex) {
                        StaticUIMethods.displayException(ex);
                    }
                    break;
                case "xls":
                case "xlsx":
                    final AbstractReportTableModel model = reportController.createReportModel();
                    Workbook.export(model, file);
                    break;
                default:
                    break;
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
    private void handleFormatAction() {
        final PageFormat oldFormat = report.get().getPageFormat();

        final FXMLUtils.Pair<PageFormatDialogController> pair = FXMLUtils.load(PageFormatDialogController.class.getResource("PageFormatDialog.fxml"),
                resources.getString("Title.PageSetup"));

        final PageFormatDialogController controller = pair.getController();
        final Stage stage = pair.getStage();

        StageUtils.addBoundsListener(stage, PageFormatDialogController.class);

        stage.setResizable(false);
        controller.setPageFormat(oldFormat);
        stage.showAndWait();

        final PageFormat newFormat = controller.getPageFormat();

        if (newFormat != null) {
            report.get().setPageFormat(newFormat);
            reportController.refreshReport();
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
        setPage(report.get().getPageCount() - 1);
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
        final PageFormat pageFormat = report.get().getPageFormat();

        final double heightRatio = (scrollPane.getViewportBounds().getHeight() - (2 * PAGE_BORDER))
                / pageFormat.getHeight();

        final double widthRatio = (scrollPane.getViewportBounds().getWidth() - (2 * PAGE_BORDER))
                / pageFormat.getWidth();

        zoomComboBox.getSelectionModel().clearSelection();

        setActualZoomRatio(Math.min(heightRatio, widthRatio));
    }

    @FXML
    private void handleFitPageWidthAction() {
        zoomComboBox.getSelectionModel().clearSelection();

        setActualZoomRatio((scrollPane.getViewportBounds().getWidth() - (2 * PAGE_BORDER)) /
                report.get().getPageFormat().getWidth());
    }

    @FXML
    private void handleActualSizeAction() {
        zoomComboBox.getSelectionModel().select(DEFAULT_ZOOM_INDEX); // 100% size
    }
}
