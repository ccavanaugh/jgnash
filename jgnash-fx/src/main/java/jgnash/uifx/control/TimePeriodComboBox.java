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
package jgnash.uifx.control;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import jgnash.util.ResourceUtils;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * ComboBox that allows for selection of predetermined periods of times
 * The period returned is in milliseconds.  getPeriods and getDescriptions
 * can be overridden to change the available periods
 *
 * @author Craig Cavanaugh
 */
public class TimePeriodComboBox extends ComboBox<String> {

    private int[] periods = new int[0];

    public TimePeriodComboBox() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("TimePeriodComboBox.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        Platform.runLater(this::loadModel); // lazy load to let the ui build happen faster
    }

    private void loadModel() {
        periods = getPeriods();
        String[] descriptions = getDescriptions();

        assert periods.length == descriptions.length;

        getItems().addAll(getDescriptions());

        Platform.runLater(() -> setValue(getItems().get(0)));
    }

    /**
     * Sets the selected period.  Period must be a valid period
     * or no change will occur.
     *
     * @param period period in milliseconds to select
     */
    public void setSelectedPeriod(final int period) {
        for (int i = 0; i < periods.length; i++) {
            if (period == periods[i]) {
                getSelectionModel().select(i);
                break;
            }
        }
    }

    /**
     * Returns the selected period in milliseconds
     *
     * @return selected period in milliseconds
     */
    public int getSelectedPeriod() {
        return periods[getSelectionModel().getSelectedIndex()];
    }

    private static int[] getPeriods() {
        return new int[]{300000, 600000, 900000, 1800000, 3600000, 7200000, 28800000, 86400000, 0};
    }

    private static String[] getDescriptions() {

        final ResourceBundle rb = ResourceUtils.getBundle();

        return new String[]{rb.getString("Period.5Min"), rb.getString("Period.10Min"), rb.getString("Period.15Min"),
                rb.getString("Period.30Min"), rb.getString("Period.1Hr"), rb.getString("Period.2Hr"),
                rb.getString("Period.8Hr"), rb.getString("Period.1Day"), rb.getString("Period.NextStart")};
    }
}
