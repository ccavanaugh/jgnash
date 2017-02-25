/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.text.Format;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

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

/**
 * TableView manager.  Handles persistence of column sizes, visibility and will optimize column widths
 *
 * @author Craig Cavanaugh
 */
public class TableViewManager<S> {

    //private static final String PREF_NODE_REG_POS = "/positions";

    private static final String PREF_NODE_REG_VIS = "/visibility";

    private static final String PREF_NODE_REG_WIDTH = "/width";

    private static final int COLUMN_PADDING = 10; // margins need extra padding to prevent truncated display

    // TODO: Extract or calculate when JavaFX font metrics API improves
    private static final double BOLD_MULTIPLIER = 1.08;  // multiplier for bold width

    @NotNull
    private final TableView<S> tableView;

    private final String preferencesUserRoot;

    private final ObjectProperty<Callback<TableColumnBase<S, ?>, Format>> columnFormatFactory = new SimpleObjectProperty<>();

    private final ObjectProperty<Callback<Integer, Boolean>> columnVisibilityFactory = new SimpleObjectProperty<>();

    private final ObjectProperty<Callback<Integer, Double>> columnWeightFactory = new SimpleObjectProperty<>();

    private final ObjectProperty<Callback<Integer, Double>> minimumColumnWidthFactory = new SimpleObjectProperty<>();

    private final ObjectProperty<Supplier<String>> preferenceKeyFactory = new SimpleObjectProperty<>();

    private final ColumnVisibilityListener visibilityListener = new ColumnVisibilityListener();

    /**
     * Limits number of processed visibility change events ensuring the most recent is executed.
     */
    private final ThreadPoolExecutor updateColumnVisibilityExecutor;

    /**
     * Limits number of packTable calls while ensuring the most recent is executed.
     */
    private final ThreadPoolExecutor packTableExecutor;

    /**
     * Used to track initialization.  If false, old column widths should be restored.
     */
    private boolean isFullyInitialized = false;

    public TableViewManager(@NotNull final TableView<S> tableView, @NotNull final String preferencesUserRoot) {
        this.tableView = tableView;
        this.preferencesUserRoot = preferencesUserRoot;

        // Set a default factories
        setColumnFormatFactory(param -> null);
        setMinimumColumnWidthFactory(param -> 0.0);

         /* At least 2 updates need to be allowed.  The update in process and any potential updates requested
         * that occur when an update is already in process.  Limited to 1 thread
         *
         * Excess execution requests will be silently discarded
         */
        updateColumnVisibilityExecutor = new ThreadPoolExecutor(0, 1, 0,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
        updateColumnVisibilityExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        packTableExecutor = new ThreadPoolExecutor(0, 1, 0,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
        packTableExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    public void restoreLayout() {
        restoreColumnVisibility();  // Restore visibility first

        packTable();                // pack the table
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
     * Determines the preferred width of the column including contents.
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

            final OptionalDouble max = cellItems.parallelStream().filter(Objects::nonNull).mapToDouble(o -> {
                final Format format = columnFormatFactory.get().call(column);   // thread local format per thread
                return JavaFXUtils.getDisplayedTextWidth(format != null ? format.format(o) : o.toString(), column.getStyle());
            }).max();

            maxWidth = max.isPresent() ? max.getAsDouble() : 0;
        }

        //noinspection SuspiciousMethodCalls
        maxWidth = Math.max(maxWidth, Math.max(column.getMinWidth(), minimumColumnWidthFactory
                .get().call(tableView.getColumns().indexOf(column))));

        // header text width
        maxWidth = Math.max(maxWidth,
                JavaFXUtils.getDisplayedTextWidth(column.getText(), column.getStyle()) * BOLD_MULTIPLIER);

        return Math.ceil(maxWidth + COLUMN_PADDING);
    }

    private void saveColumnWidths() {
        if (preferenceKeyFactory.get() != null) {

            final double[] columnWidths = tableView.getColumns().filtered(TableColumnBase::isVisible)
                    .stream().mapToDouble(TableColumnBase::getWidth).toArray();

            final Preferences preferences = Preferences.userRoot().node(preferencesUserRoot + PREF_NODE_REG_WIDTH);

            preferences.put(preferenceKeyFactory.get().get(), EncodeDecode.encodeDoubleArray(columnWidths));
        }
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

            // Remove listeners while state is being restored so states are not saved during state changes
            removeColumnListeners();

            final String uuid = preferenceKeyFactory.get().get();
            final Preferences preferences = Preferences.userRoot().node(preferencesUserRoot + PREF_NODE_REG_VIS);

            final String result = preferences.get(uuid, null);
            if (result != null) {
                final boolean[] columnVisibility = EncodeDecode.decodeBooleanArray(result);

                tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

                if (columnVisibility.length == tableView.getColumns().size()) {
                    for (int i = 0; i < columnVisibility.length; i++) {
                        tableView.getColumns().get(i).visibleProperty().set(columnVisibility[i]);
                    }
                }

                tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            } else {  // no preference has been set, so force all columns to be the default
                final Callback<Integer, Boolean> visibilityCallBack = columnVisibilityFactory.get();

                if (visibilityCallBack != null) {   // use a factory method
                    int i = 0;

                    for (final TableColumnBase<S, ?> column : tableView.getColumns()) {
                        column.visibleProperty().set(visibilityCallBack.call(i++));
                    }
                } else {
                    for (final TableColumnBase<S, ?> column : tableView.getColumns()) {
                        column.visibleProperty().set(true);
                    }
                }
            }

            // restore listeners
            installColumnListeners();
        }
    }

    @NotNull
    private double[] retrieveOldColumnWidths() {
        double[] columnWidths = new double[0];  // zero length array instead of null to protect against NPE

        if (!isFullyInitialized) {  // no need to retrieve old column widths more than once
            if (preferenceKeyFactory.get() != null) {
                final String uuid = preferenceKeyFactory.get().get();
                final Preferences preferences = Preferences.userRoot().node(preferencesUserRoot + PREF_NODE_REG_WIDTH);

                final String widths = preferences.get(uuid, null);

                if (widths != null) {
                    columnWidths = EncodeDecode.decodeDoubleArray(widths);
                }
            }
        }

        return columnWidths;
    }

    /**
     * Called when the table columns need to be repacked because of content change
     */
    public synchronized void packTable() {

        packTableExecutor.execute(() -> {

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

            final double[] oldWidths = retrieveOldColumnWidths();
            double sumFixedColumns = 0; // sum of the fixed width columns

            /* Use the old calculated widths if the count is correct and full initialization has not occurred */
            final double[] calculatedWidths = !isFullyInitialized && oldWidths.length == visibleColumns.size()
                    ? oldWidths : new double[visibleColumns.size()];

            /* determine if the expensive calculations needs to occur */
            final boolean doExpensiveCalculations = isFullyInitialized || oldWidths.length != visibleColumns.size();

            for (int i = 0; i < calculatedWidths.length; i++) {
                if (visibleColumnWeights.get(i) == 0) {

                    /* expensive operation, don't calculate if we are reusing older values */
                    if (doExpensiveCalculations) {
                        calculatedWidths[i] = getCalculatedColumnWidth(visibleColumns.get(i));
                    }
                    sumFixedColumns += calculatedWidths[i];
                } else {
                    calculatedWidths[i] = Double.MAX_VALUE;
                }
            }

            double remainder = tableView.widthProperty().get() - sumFixedColumns;   // leftover visible table width

            // calculate widths for adjustable columns using the remaining visible width
            for (int i = 0; i < calculatedWidths.length; i++) {
                if (doExpensiveCalculations && visibleColumnWeights.get(i) != 0) {
                    calculatedWidths[i] = remainder * (visibleColumnWeights.get(i) / 100.0);
                }
            }

            Platform.runLater(() -> {
                removeColumnListeners();

                // unconstrained is required for resize columns correctly
                tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

                // Force the column widths and let the layout policy do the heavy lifting
                for (int j = 0; j < calculatedWidths.length; j++) {
                    visibleColumns.get(j).setResizable(true);   // allow resizing

                    if (visibleColumnWeights.get(j) == 0) { // fixed width column
                        visibleColumns.get(j).minWidthProperty().set(calculatedWidths[j]);
                        visibleColumns.get(j).maxWidthProperty().set(calculatedWidths[j]);
                        visibleColumns.get(j).setResizable(false);
                    } else {
                        visibleColumns.get(j).prefWidthProperty().set(calculatedWidths[j]);
                    }
                }

                tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);   // restore the old policy

                saveColumnWidths(); // save column widths for next time

                installColumnListeners();

                /* set the initialization flag and start a full calculated pack of the table if not initialized */
                if (!isFullyInitialized) {
                    isFullyInitialized = true;

                    // rerun the pack process to perform a fully calculated pack
                    Platform.runLater(TableViewManager.this::packTable);
                }
            });
        });
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

    /**
     * Enforces a minimum width width for a column
     *
     * @param widthFactory {@code Callback} returning a non-zero width of a given column
     */
    public void setMinimumColumnWidthFactory(final Callback<Integer, Double> widthFactory) {
        this.minimumColumnWidthFactory.setValue(widthFactory);
    }

    public void setDefaultColumnVisibilityFactory(final Callback<Integer, Boolean> visibilityFactoryCallback) {
        columnVisibilityFactory.set(visibilityFactoryCallback);
    }

    public void setPreferenceKeyFactory(final Supplier<String> keyFactory) {
        this.preferenceKeyFactory.set(keyFactory);
    }

    private final class ColumnVisibilityListener implements ChangeListener<Boolean> {
        @Override
        public void changed(final ObservableValue<? extends Boolean> observable, final Boolean oldValue,
                            final Boolean newValue) {
            updateColumnVisibilityExecutor.execute(TableViewManager.this::saveColumnVisibility);
            packTable();
        }
    }
}
