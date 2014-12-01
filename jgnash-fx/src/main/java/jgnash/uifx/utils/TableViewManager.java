/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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
package jgnash.uifx.utils;

import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import jgnash.util.Callable;
import jgnash.util.EncodeDecode;
import jgnash.util.NotNull;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;
import javafx.util.Callback;

/**
 * TableView manager.  Handles persistence of column sizes, visibility and will optimize column widths
 *
 * @author Craig Cavanaugh
 */
public class TableViewManager<S> {

    //private static final String PREF_NODE_REG_POS = "/positions";

    private static final String PREF_NODE_REG_WIDTH = "/widths";

    private static final String PREF_NODE_REG_VIS = "/visibility";

    @NotNull
    private final TableView<S> tableView;

    private final String preferencesUserRoot;

    private final ObjectProperty<Callback<TableColumnBase<S, ?>, Format>> columnFormatFactory = new SimpleObjectProperty<>();

    private final ObjectProperty<Callback<Integer, Double>> columnWeightFactory = new SimpleObjectProperty<>();

    private final ObjectProperty<Callable<String>> preferenceKeyFactory = new SimpleObjectProperty<>();

    private final ColumnVisibilityListener visibilityListener = new ColumnVisibilityListener();

    private final ColumnWidthListener widthListener = new ColumnWidthListener();

    /**
     * Limits number of processed resize events ensuring the most recent is executed
     */
    private final ThreadPoolExecutor updateColumnSizeExecutor;

    /**
     * Limits number of processed visibility change events ensuring the most recent is executed
     */
    private final ThreadPoolExecutor updateColumnVisibilityExecutor;

    public TableViewManager(@NotNull final TableView<S> tableView, @NotNull final String preferencesUserRoot) {
        this.tableView = tableView;
        this.preferencesUserRoot = preferencesUserRoot;

         /* At least 2 updates need to be allowed.  The update in process and any potential updates requested
         * that occur when an update is already in process.  Limited to 1 thread
         *
         * Excess execution requests will be silently discarded
         */
        updateColumnSizeExecutor = new ThreadPoolExecutor(0, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
        updateColumnSizeExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        updateColumnVisibilityExecutor = new ThreadPoolExecutor(0, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
        updateColumnVisibilityExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    public void restoreLayout() {
        // Remove listeners while state is being restored so states are not saved during state changes
        removeColumnListeners();

        // Restore visibility first
        restoreColumnVisibility();

        // Restore column widths
        restoreColumnWidths();

        // Install listeners and save column states
        installColumnListeners();
    }

    private void installColumnListeners() {
        for (final TableColumnBase<S, ?> tableColumn : tableView.getColumns()) {
            tableColumn.visibleProperty().addListener(visibilityListener);
            tableColumn.widthProperty().addListener(widthListener);
        }
    }

    private void removeColumnListeners() {
        for (final TableColumnBase<S, ?> tableColumn : tableView.getColumns()) {
            tableColumn.visibleProperty().removeListener(visibilityListener);
            tableColumn.widthProperty().removeListener(widthListener);
        }
    }

    /**
     * Determines the preferred width of the column including contents
     *
     * @param column {@code TableColumn} to measure content
     * @return preferred width
     */
    private double getCalculatedColumnWidth(final TableColumnBase<S, ?> column) {
        double maxWidth = column.getMinWidth(); // init with the minimum column width

        for (int i = 0; i < tableView.getItems().size(); i++) {

            final Object object = column.getCellData(i);

            if (object != null) {
                final Format format = columnFormatFactory.get().call(column);
                final String displayString;

                if (format != null) {
                    displayString = format.format(object);
                } else {    // if null, just use toString
                    displayString = object.toString();
                }

                if (!displayString.isEmpty()) {    // ignore empty strings


                    // Text and Scene construction must be done on the Platform thread
                    // Invoke the task on the platform thread and wait until complete
                    FutureTask<Double> futureTask = new FutureTask<>(() -> {
                        final Text text = new Text(displayString);
                        new Scene(new Group(text));

                        text.applyCss();
                        return text.getLayoutBounds().getWidth();
                    });

                    Platform.runLater(futureTask);

                    try {
                        maxWidth = Math.max(maxWidth, futureTask.get());
                    } catch (final InterruptedException | ExecutionException e) {
                        Logger.getLogger(TableViewManager.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                    }
                }
            }
        }

        return Math.ceil(maxWidth + 14); // TODO, extract "14" margin from css
    }

    private void saveColumnWidths() {
        if (preferenceKeyFactory.get() != null) {
            final String uuid = preferenceKeyFactory.get().call();
            final Preferences preferences = Preferences.userRoot().node(preferencesUserRoot + PREF_NODE_REG_WIDTH);

            final double[] columnWidths = new double[tableView.getColumns().size()];

            for (int i = 0; i < columnWidths.length; i++) {
                columnWidths[i] = tableView.getColumns().get(i).getWidth();
            }

            preferences.put(uuid, EncodeDecode.encodeDoubleArray(columnWidths));
        }
    }

    private void restoreColumnWidths() {
        if (preferenceKeyFactory.get() != null) {
            final String uuid = preferenceKeyFactory.get().call();
            final Preferences preferences = Preferences.userRoot().node(preferencesUserRoot + PREF_NODE_REG_WIDTH);

            final String widths = preferences.get(uuid, null);
            if (widths != null) {
                final double[] columnWidths = EncodeDecode.decodeDoubleArray(widths);

                tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

                if (columnWidths.length == tableView.getColumns().size()) {
                    for (int i = 0; i < columnWidths.length; i++) {
                        tableView.getColumns().get(i).prefWidthProperty().setValue(columnWidths[i]);
                    }
                }

                tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            }
        }
    }

    private void saveColumnVisibility() {
        if (preferenceKeyFactory.get() != null) {
            final String uuid = preferenceKeyFactory.get().call();
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
            final String uuid = preferenceKeyFactory.get().call();
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
            }
        }
    }

    public void packTable() {

        new Thread(() -> {

            // Create a list of visible columns and column weights
            final List<TableColumnBase<S, ?>> visibleColumns = new ArrayList<>();
            final List<Double> visibleColumnWeights = new ArrayList<>();

            for (int i = 0; i < tableView.getColumns().size(); i++) {
                if (tableView.getColumns().get(i).isVisible()) {
                    visibleColumns.add(tableView.getColumns().get(i));
                    visibleColumnWeights.add(columnWeightFactory.get().call(i));
                }
            }

            /*
            * The calculated width of all visible columns, tableView.getWidth() does not allocate for the scroll bar if
            * visible within the TableView
            */
            double calculatedTableWidth = 0;

            for (final TableColumnBase<S, ?> column : visibleColumns) {
                calculatedTableWidth += Math.rint(column.getWidth());
            }

            double calculatedWidths[] = new double[visibleColumns.size()];

            for (int i = 0; i < calculatedWidths.length; i++) {
                calculatedWidths[i] = getCalculatedColumnWidth(visibleColumns.get(i));
            }

            double[] optimizedWidths = calculatedWidths.clone();

            // optimize column widths
            Double[] columnWeights = visibleColumnWeights.toArray(new Double[visibleColumnWeights.size()]);

            double fixedWidthColumnTotals = 0; // total fixed width of columns

            for (int i = 0; i < optimizedWidths.length; i++) {
                if (columnWeights[i] == 0) {
                    fixedWidthColumnTotals += optimizedWidths[i];
                }
            }

            double diff = calculatedTableWidth - fixedWidthColumnTotals; // remaining non fixed width that must be compressed
            double totalWeight = 0; // used to calculate percentages

            for (double columnWeight : columnWeights) {
                totalWeight += columnWeight;
            }

            int i = 0;
            while (i < columnWeights.length) {
                if (columnWeights[i] > 0) {
                    double adj = (columnWeights[i] / totalWeight * diff);

                    if (optimizedWidths[i] > adj) { // only change if necessary
                        optimizedWidths[i] = adj;
                    } else {
                        diff -= optimizedWidths[i]; // available difference is reduced
                        totalWeight -= columnWeights[i]; // adjust the weighting
                        optimizedWidths = calculatedWidths.clone(); // reset widths
                        columnWeights[i] = 0d; // do not try to adjust width again
                        i = -1; // restart the loop from the beginning
                    }
                }
                i++;
            }

            final double[] finalWidths = optimizedWidths.clone();

            Platform.runLater(() -> {
                removeColumnListeners();

                @SuppressWarnings("rawtypes")
                Callback<TableView.ResizeFeatures, Boolean> oldResizePolicy = tableView.getColumnResizePolicy();

                // unconstrained is required for resize columns correctly
                tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

                for (int j = 0; j < finalWidths.length; j++) {
                    visibleColumns.get(j).prefWidthProperty().setValue(finalWidths[j]);
                }

                // restore the old policy
                tableView.setColumnResizePolicy(oldResizePolicy);
                installColumnListeners();

                // Save the new state
                updateColumnSizeExecutor.execute(TableViewManager.this::saveColumnWidths);
            });
        }).start();
    }

    public void setColumnFormatFactory(final Callback<TableColumnBase<S, ?>, Format> cellFormat) {
        this.columnFormatFactory.set(cellFormat);
    }

    public void setColumnWeightFactory(final Callback<Integer, Double> weightFactory) {
        this.columnWeightFactory.set(weightFactory);
    }

    public void setPreferenceKeyFactory(final Callable<String> keyFactory) {
        this.preferenceKeyFactory.set(keyFactory);
    }

    private final class ColumnVisibilityListener implements ChangeListener<Boolean> {
        @Override
        public void changed(final ObservableValue<? extends Boolean> observable, final Boolean oldValue, final Boolean newValue) {
            updateColumnVisibilityExecutor.execute(TableViewManager.this::saveColumnVisibility);
        }
    }

    private final class ColumnWidthListener implements ChangeListener<Number> {
        @Override
        public void changed(final ObservableValue<? extends Number> observable, final Number oldValue, final Number newValue) {
            updateColumnSizeExecutor.execute(TableViewManager.this::saveColumnWidths);
        }
    }
}
