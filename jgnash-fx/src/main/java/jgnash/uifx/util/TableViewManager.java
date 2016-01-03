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
package jgnash.uifx.util;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import jgnash.util.EncodeDecode;
import jgnash.util.NotNull;

import java.text.Format;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * TableView manager.  Handles persistence of column sizes, visibility and will optimize column widths
 *
 * @author Craig Cavanaugh
 */
public class TableViewManager<S> {

    //private static final String PREF_NODE_REG_POS = "/positions";

    private static final String PREF_NODE_REG_VIS = "/visibility";

    private static final int COLUMN_PADDING = 16; // margins need padding to prevent truncated display

    @NotNull
    private final TableView<S> tableView;

    private final String preferencesUserRoot;

    private final ObjectProperty<Callback<TableColumnBase<S, ?>, Format>> columnFormatFactory = new SimpleObjectProperty<>();

    private final ObjectProperty<Callback<Integer, Double>> columnWeightFactory = new SimpleObjectProperty<>();

    private final ObjectProperty<Supplier<String>> preferenceKeyFactory = new SimpleObjectProperty<>();

    private final ColumnVisibilityListener visibilityListener = new ColumnVisibilityListener();

    private static final Logger logger = Logger.getLogger(TableViewManager.class.getName());

    /**
     * Limits number of processed visibility change events ensuring the most recent is executed
     */
    private final ThreadPoolExecutor updateColumnVisibilityExecutor;

    public TableViewManager(@NotNull final TableView<S> tableView, @NotNull final String preferencesUserRoot) {
        this.tableView = tableView;
        this.preferencesUserRoot = preferencesUserRoot;

        // Set a default format factory
        setColumnFormatFactory(param -> null);

         /* At least 2 updates need to be allowed.  The update in process and any potential updates requested
         * that occur when an update is already in process.  Limited to 1 thread
         *
         * Excess execution requests will be silently discarded
         */
        updateColumnVisibilityExecutor = new ThreadPoolExecutor(0, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
        updateColumnVisibilityExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    public void restoreLayout() {
        // Remove listeners while state is being restored so states are not saved during state changes
        removeColumnListeners();

        // Restore visibility first
        restoreColumnVisibility();

        // Install listeners and save column states
        installColumnListeners();

        packTable();    // repack the table
    }

    private void installColumnListeners() {
        for (final TableColumnBase<S, ?> tableColumn : tableView.getColumns()) {
            tableColumn.visibleProperty().addListener(visibilityListener);
        }
    }

    private void removeColumnListeners() {
        for (final TableColumnBase<S, ?> tableColumn : tableView.getColumns()) {
            tableColumn.visibleProperty().removeListener(visibilityListener);
        }
    }

    /**
     * Determines the preferred width of the column including contents
     *
     * @param column {@code TableColumn} to measure content
     * @return preferred width
     */
    private double getCalculatedColumnWidth(final TableColumnBase<S, ?> column) {
        double maxWidth = 0;

        /* Collect all the unique cell items and remove null*/
        final Set<Object> cellItems = new HashSet<>();

        for (int i = 0; i < tableView.getItems().size(); i++) {
            cellItems.add(column.getCellData(i));
        }
        cellItems.remove(null);

        if (cellItems.size() > 0) { // don't try if there is no data or the stream function will throw an error
            maxWidth = cellItems.parallelStream().filter(s -> s != null).mapToDouble(o -> {
                final Format format = columnFormatFactory.get().call(column);   // thread local format per thread
                return JavaFXUtils.getDisplayedTextWidth(format != null ? format.format(o) : o.toString(), column.getStyle());
            }).max().getAsDouble();
        }

        maxWidth = Math.max(maxWidth, column.getMinWidth());
        maxWidth = Math.max(maxWidth, JavaFXUtils.getDisplayedTextWidth(column.getText(), column.getStyle()));

        return Math.ceil(maxWidth + COLUMN_PADDING);
    }

    private void saveColumnVisibility() {
        if (preferenceKeyFactory.get() != null) {
            final String uuid = preferenceKeyFactory.get().get();
            final Preferences preferences = Preferences.userRoot().node(preferencesUserRoot + PREF_NODE_REG_VIS);

            final boolean[] columnVisibility = new boolean[tableView.getColumns().size()];

            for (int i = 0; i < columnVisibility.length; i++) {
                columnVisibility[i] = tableView.getColumns().get(i).isVisible();
            }

            preferences.put(uuid, EncodeDecode.encodeBooleanArray(columnVisibility));
        }
    }

    private void restoreColumnVisibility() {
        if (preferenceKeyFactory.get() != null) {
            final String uuid = preferenceKeyFactory.get().get();
            final Preferences preferences = Preferences.userRoot().node(preferencesUserRoot + PREF_NODE_REG_VIS);

            final String result = preferences.get(uuid, null);
            if (result != null) {
                final boolean[] columnVisibility = EncodeDecode.decodeBooleanArray(result);

                tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

                if (columnVisibility.length == tableView.getColumns().size()) {
                    for (int i = 0; i < columnVisibility.length; i++) {
                        tableView.getColumns().get(i).visibleProperty().setValue(columnVisibility[i]);
                    }
                }

                tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            } else {  // no preference has been set, so force all columns to be visible
                for (final TableColumnBase<S, ?> column : tableView.getColumns()) {
                    column.visibleProperty().setValue(true);
                }
            }
        }
    }

    public void packTable() {

        // Prevent packing if the containing window is not showing
        /*if (!tableView.getScene().getWindow().isShowing()) {
            logger.log(Level.WARNING, "tried to pack a table that is not visible", new Throwable());
            return;
        }*/

        new Thread(() -> {

            LocalTime start = LocalTime.now();

            // Create a list of visible columns and column weights
            final List<TableColumnBase<S, ?>> visibleColumns = new ArrayList<>();
            final List<Double> visibleColumnWeights = new ArrayList<>();

            for (int i = 0; i < tableView.getColumns().size(); i++) {
                if (tableView.getColumns().get(i).isVisible()) {
                    visibleColumns.add(tableView.getColumns().get(i));
                    visibleColumnWeights.add(columnWeightFactory.get().call(i));
                }
                tableView.getColumns().get(i).setMinWidth(0);   // clear minWidth for pack
            }

            double calculatedWidths[] = new double[visibleColumns.size()];

            for (int i = 0; i < calculatedWidths.length; i++) {
                if (visibleColumnWeights.get(i) == 0) {
                    calculatedWidths[i] = getCalculatedColumnWidth(visibleColumns.get(i));
                } else {
                    calculatedWidths[i] = Double.MAX_VALUE;
                }
            }

            logger.info("Pack Calculation time was " + Duration.between(start, LocalTime.now()).toMillis() + " millis");

            Platform.runLater(() -> {
                removeColumnListeners();

                // unconstrained is required for resize columns correctly
                tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

                // Force the column widths and let the layout policy do the heavy lifting
                for (int j = 0; j < calculatedWidths.length; j++) {
                    visibleColumns.get(j).setResizable(true);   // allow resizing

                    if (visibleColumnWeights.get(j) == 0) { // fixed width column
                        visibleColumns.get(j).minWidthProperty().setValue(calculatedWidths[j]);
                        visibleColumns.get(j).maxWidthProperty().setValue(calculatedWidths[j]);
                        visibleColumns.get(j).setResizable(false);
                    } else {
                        visibleColumns.get(j).prefWidthProperty().setValue(calculatedWidths[j]);
                    }
                }

                // restore the old policy
                tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

                installColumnListeners();
            });
        }).start();
    }

    /**
     * Sets a {@code Format} factory.  A Format will be returned for each column.  A null value may
     * be returned as well.
     * <p>
     * The returned Format must be thread safe.
     *
     * @param cellFormat Callback to generate a {@code Format} given a {@code TableColumnBase}
     */
    public void setColumnFormatFactory(final Callback<TableColumnBase<S, ?>, Format> cellFormat) {
        this.columnFormatFactory.set(cellFormat);
    }

    public void setColumnWeightFactory(final Callback<Integer, Double> weightFactory) {
        this.columnWeightFactory.set(weightFactory);
    }

    public void setPreferenceKeyFactory(final Supplier<String> keyFactory) {
        this.preferenceKeyFactory.set(keyFactory);
    }

    private final class ColumnVisibilityListener implements ChangeListener<Boolean> {
        @Override
        public void changed(final ObservableValue<? extends Boolean> observable, final Boolean oldValue, final Boolean newValue) {
            updateColumnVisibilityExecutor.execute(TableViewManager.this::saveColumnVisibility);
            packTable();
        }
    }
}
