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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
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

    private static final String LAST_RECALCULATION = "/recalculation";

    private static final String PREF_NODE_REG_VIS = "/visibility";

    private static final String PREF_NODE_REG_WIDTH = "/width";

    private static final int COLUMN_PADDING = 10; // margins need extra padding to prevent truncated display

    // TODO: Extract or calculate when JavaFX font metrics API improves
    private static final double BOLD_MULTIPLIER = 1.08;  // multiplier for bold width
    private static final double COLUMN_BORDER_WIDTH = 2.0;

    /**
     * Ensure visible columns do not disappear
     */
    private static final int MIN_WIDTH = 30;

    /**
     * JavaFX default value for maximum column width
     */
    private static final int MAX_WIDTH = 4000;

    @NotNull
    private final TableView<S> tableView;

    private final String preferencesUserRoot;

    private final ObjectProperty<Callback<TableColumnBase<S, ?>, Format>> columnFormatFactory = new SimpleObjectProperty<>();

    private final ObjectProperty<Callback<Integer, Boolean>> columnVisibilityFactory = new SimpleObjectProperty<>();

    private final ObjectProperty<Callback<Integer, Double>> columnWeightFactory = new SimpleObjectProperty<>();

    private final ObjectProperty<Callback<Integer, Double>> minimumColumnWidthFactory = new SimpleObjectProperty<>();

    private final ObjectProperty<Supplier<String>> preferenceKeyFactory = new SimpleObjectProperty<>();

    private final ColumnVisibilityListener visibilityListener = new ColumnVisibilityListener();

    private final ColumnWidthListener columnWidthListener = new ColumnWidthListener();

    /**
     * If true, manual packing for the table should be allowed
     */
    private final BooleanProperty manualPacking = new SimpleBooleanProperty(false);

    /**
     * Limits number of processed visibility change events ensuring the most recent is executed.
     */
    private final ThreadPoolExecutor updateColumnVisibilityExecutor;

    /**
     * Limits number of packTable calls while ensuring the most recent is executed.
     */
    private final ThreadPoolExecutor packTableExecutor;

    /**
     * Limits number of saveColumnWidths calls while ensuring the most recent is executed.
     */
    private final ThreadPoolExecutor saveColumnWidthExecutor;

    /**
     * Used to track initialization and pack state
     */
    private final AtomicLong packCounter = new AtomicLong();

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
        updateColumnVisibilityExecutor = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE,
                TimeUnit.DAYS, new ArrayBlockingQueue<>(1));
        updateColumnVisibilityExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        packTableExecutor = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE,
                TimeUnit.DAYS, new ArrayBlockingQueue<>(1));
        packTableExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        saveColumnWidthExecutor = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE,
                TimeUnit.DAYS, new ArrayBlockingQueue<>(1));
        saveColumnWidthExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        // repack and reset column properties if the user changes settings
        manualPacking.addListener((observable, oldValue, newValue) -> {
            if (packCounter.get() > 0) {
                packTable();
            }
        });
    }

    public void restoreLayout() {
        if (packCounter.get() == 0) {
            JavaFXUtils.runAndWait(this::restoreColumnVisibility); // Restore visibility first
            JavaFXUtils.runLater(this::packTable);  // pack the table
        }
    }

    private void installColumnListeners() {
        for (final TableColumnBase<S, ?> tableColumn : tableView.getColumns()) {
            tableColumn.visibleProperty().addListener(visibilityListener);
            tableColumn.widthProperty().addListener(columnWidthListener);
        }
    }

    private void removeColumnListeners() {
        for (final TableColumnBase<S, ?> tableColumn : tableView.getColumns()) {
            tableColumn.visibleProperty().removeListener(visibilityListener);
            tableColumn.widthProperty().removeListener(columnWidthListener);
        }
    }

    public BooleanProperty manualColumnPackingProperty() {
        return manualPacking;
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
        JavaFXUtils.runLater(() -> {
            if (preferenceKeyFactory.get() != null) {
                final double[] columnWidths = tableView.getColumns().filtered(TableColumnBase::isVisible).stream()
                        .mapToDouble(value -> Math.floor(value.getWidth())).toArray();

                final Preferences preferences = Preferences.userRoot().node(preferencesUserRoot + PREF_NODE_REG_WIDTH);

                preferences.put(preferenceKeyFactory.get().get(), EncodeDecode.encodeDoubleArray(columnWidths));
            }
        });
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

    private void clearTimeStamp() {
        if (preferenceKeyFactory.get() != null) {
            final Preferences preferences = Preferences.userRoot().node(preferencesUserRoot + LAST_RECALCULATION);
            preferences.putLong(preferenceKeyFactory.get().get(), 0);
        }
    }

    private void saveTimeStamp() {
        if (preferenceKeyFactory.get() != null) {
            final Preferences preferences = Preferences.userRoot().node(preferencesUserRoot + LAST_RECALCULATION);
            preferences.putLong(preferenceKeyFactory.get().get(), System.currentTimeMillis());
        }
    }

    public long getTimeStamp() {
        if (preferenceKeyFactory.get() != null) {
            final Preferences preferences = Preferences.userRoot().node(preferencesUserRoot + LAST_RECALCULATION);
            return preferences.getLong(preferenceKeyFactory.get().get(), 0);
        }
        return 0;
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

        // no need to retrieve old column widths more than once
        if (preferenceKeyFactory.get() != null) {

            final String uuid = preferenceKeyFactory.get().get();
            final Preferences preferences = Preferences.userRoot().node(preferencesUserRoot + PREF_NODE_REG_WIDTH);

            final String widths = preferences.get(uuid, null);

            if (widths != null) {
                columnWidths = EncodeDecode.decodeDoubleArray(widths);
            }
        }

        return columnWidths;
    }

    /**
     * Called when the table columns need to be repacked because of content change
     */
    public synchronized void packTable() {
        if (tableView.widthProperty().get() == 0) {
            return; // exit right away if the table view is not visible
        }

        packCounter.incrementAndGet();

        packTableExecutor.execute(() -> {   // rate limits with a high rate of transactional changes

            // Create a list of visible columns and column weights
            final List<TableColumn<S, ?>> visibleColumns = new ArrayList<>();
            final List<Double> visibleColumnWeights = new ArrayList<>();

            for (int i = 0; i < tableView.getColumns().size(); i++) {
                if (tableView.getColumns().get(i).isVisible()) {
                    visibleColumns.add(tableView.getColumns().get(i));
                    visibleColumnWeights.add(columnWeightFactory.get().call(i));
                }
                tableView.getColumns().get(i).setMinWidth(0);   // clear minWidth for pack
            }

            final double[] oldWidths = retrieveOldColumnWidths();

            if (oldWidths.length == visibleColumns.size()) {
                double sumOldResizableColumns = 0;

                // calculate the sum of visible resizable columns
                for (int i = 0; i < visibleColumns.size(); i++) {
                    if (visibleColumnWeights.get(i) > 0) {
                        sumOldResizableColumns += oldWidths[i];
                    }
                }

                for (int i = 0; i < visibleColumns.size(); i++) {
                    if (visibleColumnWeights.get(i) > 0) {
                        double oldPercentage = oldWidths[i] / sumOldResizableColumns * 100.0;
                        visibleColumnWeights.set(i, oldPercentage);
                    }
                }
            }

            /* Use the old calculated widths if the count is correct and full initialization has not occurred */
            final double[] calculatedWidths = oldWidths.length == visibleColumns.size()
                    ? oldWidths : new double[visibleColumns.size()];

            /* If any columns are zero width, a full calculation is required */
            boolean forceCalculations = false;

            for (final double width : calculatedWidths) {
                if (width <= 0.0) {
                    forceCalculations = true;
                    break;
                }
            }

            if (forceCalculations || packCounter.get() > 0) {
                double sumFixedColumns = 0; // sum of the fixed width columns

                /* determine if the expensive calculations needs to occur */
                final boolean doExpensiveCalculations = oldWidths.length != visibleColumns.size()
                        || packCounter.get() > 1 || forceCalculations;

                for (int i = 0; i < calculatedWidths.length; i++) {
                    if (visibleColumnWeights.get(i) == 0) {

                        /* expensive operation, don't calculate if we are reusing older values */
                        if (doExpensiveCalculations) {
                            clearTimeStamp();
                            calculatedWidths[i] = getCalculatedColumnWidth(visibleColumns.get(i));
                            saveTimeStamp();    // indicate recalculation is complete
                        }
                        sumFixedColumns += calculatedWidths[i];
                    }
                }

                // leftover visible width of the table.  Column borders need to be considered
                double remainder = tableView.widthProperty().get()
                        - ((visibleColumns.size() - 1) * COLUMN_BORDER_WIDTH) - sumFixedColumns;

                // calculate widths for adjustable columns using the remaining visible width
                for (int i = 0; i < calculatedWidths.length; i++) {
                    if (visibleColumnWeights.get(i) != 0) {
                        calculatedWidths[i] = Math.floor(remainder * (visibleColumnWeights.get(i) / 100.0) - 0.5);
                    }
                }
            }

            JavaFXUtils.runLater(() -> {
                removeColumnListeners();

                // unconstrained is required for resize columns correctly
                tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

                // Force the column widths and let the layout policy do the heavy lifting
                for (int j = 0; j < calculatedWidths.length; j++) {
                    visibleColumns.get(j).setResizable(true);   // allow resizing

                    if (!manualPacking.get()) {
                        if (visibleColumnWeights.get(j) == 0) { // fixed width column
                            visibleColumns.get(j).minWidthProperty().set(calculatedWidths[j]);
                            visibleColumns.get(j).maxWidthProperty().set(calculatedWidths[j]);
                            visibleColumns.get(j).setResizable(false);
                        } else {    // set the preferred size and ensure the column does not disappear
                            visibleColumns.get(j).minWidthProperty().set(MIN_WIDTH);
                            visibleColumns.get(j).prefWidthProperty().set(calculatedWidths[j]);
                        }
                    } else {
                        visibleColumns.get(j).minWidthProperty().set(MIN_WIDTH);
                        visibleColumns.get(j).maxWidthProperty().set(MAX_WIDTH);
                    }
                }

                tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);   // restore the old policy

                // Go back and correct the width of adjustable columns at the end of application thread
                JavaFXUtils.runLater(() -> {
                    // Go back and correct the width of adjustable columns
                    for (int j = 0; j < calculatedWidths.length; j++) {

                        // resize if not a fixed width column or manual packing is enabled
                        if (visibleColumnWeights.get(j) != 0 || manualPacking.get()) {
                            tableView.resizeColumn(visibleColumns.get(j), calculatedWidths[j]
                                    - visibleColumns.get(j).getWidth());
                        }
                    }
                });

                /* save the column widths for next time after fully initialized */
                if (packCounter.get() > 0) {
                    saveColumnWidths();
                }

                installColumnListeners();
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

    private final class ColumnWidthListener implements ChangeListener<Number> {

        private static final int RATE_LIMIT_MILLIS = 175;

        @Override
        public void changed(final ObservableValue<? extends Number> observable, final Number oldValue,
                            final Number newValue) {
            saveColumnWidthExecutor.execute(() -> {
                try {
                    Thread.sleep(RATE_LIMIT_MILLIS);  // rate limit the number of saves during drags
                    saveColumnWidths();
                } catch (final InterruptedException e) {
                    Logger.getLogger(TableViewManager.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            });
        }
    }
}
