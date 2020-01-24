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
package jgnash.uifx.control;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.util.StringConverter;

import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.time.DateUtils;
import jgnash.uifx.Options;
import jgnash.uifx.util.JavaFXUtils;

/**
 * Extends and AreaChart to encapsulate generation of a SecurityNode chart.
 *
 * @author Craig Cavanaugh
 */
public class SecurityNodeAreaChart extends AreaChart<Number, Number> {

    private static final int TICK_MARKS = 14;

    private final SimpleObjectProperty<SecurityNode> securityNodeProperty = new SimpleObjectProperty<>();

    private final NumberAxis xAxis;

    public SecurityNodeAreaChart() {
        super(new NumberAxis(), new NumberAxis());

        setCreateSymbols(false);
        setLegendVisible(false);
        animatedProperty().bind(Options.animationsEnabledProperty());

        xAxis = (NumberAxis) getXAxis();

        xAxis.setTickLabelFormatter(new NumberDateStringConverter());

        xAxis.tickLabelRotationProperty().set(-60);
        xAxis.setAutoRanging(false);

        securityNodeProperty().addListener((observable, oldValue, newValue) -> update());
    }

    public SimpleObjectProperty<SecurityNode> securityNodeProperty() {
        return securityNodeProperty;
    }

    public void update() {
        JavaFXUtils.runLater(getData()::clear); // clear old data

        new Thread(() -> {
            final SecurityNode securityNode = securityNodeProperty().get();

            if (securityNode != null) { // protect against an NPE caused by security rename
                final Optional<LocalDate[]> optional = securityNode.getLocalDateBounds();

                optional.ifPresent(localDates -> {
                    final List<List<SecurityHistoryNode>> groups = securityNode.getHistoryNodeGroupsBySplits();

                    for (int i = 0; i < groups.size(); i++) {
                        final Series<Number, Number> series = new Series<>();
                        series.setName(securityNode.getSymbol() + i);

                        for (final SecurityHistoryNode node : groups.get(i)) {
                            series.getData().add(new Data<>(node.getLocalDate().toEpochDay(), node.getAdjustedPrice()));
                        }

                        JavaFXUtils.runLater(() -> getData().add(series));
                    }

                    xAxis.setLowerBound(localDates[0].toEpochDay());
                    xAxis.setUpperBound(localDates[1].toEpochDay());

                    final long range = localDates[1].toEpochDay() - localDates[0].toEpochDay();

                    if (range > TICK_MARKS) {
                        xAxis.setTickUnit((int)((localDates[1].toEpochDay() - localDates[0].toEpochDay()) / TICK_MARKS));
                    } else {
                        xAxis.setTickUnit(range - 1);
                    }
                });
            }
        }).start();
    }

    private static class NumberDateStringConverter extends StringConverter<Number> {

        final DateTimeFormatter formatter = DateUtils.getShortDateFormatter();

        @Override
        public String toString(final Number value) {
            return formatter.format(LocalDate.ofEpochDay(value.longValue()));
        }

        @Override
        public Number fromString(final String string) {
            return LocalDate.parse(string, formatter).toEpochDay();
        }
    }
}
