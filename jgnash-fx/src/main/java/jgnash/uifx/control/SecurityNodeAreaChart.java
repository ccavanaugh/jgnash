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
package jgnash.uifx.control;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.util.StringConverter;

import jgnash.engine.SecurityHistoryNode;
import jgnash.engine.SecurityNode;
import jgnash.uifx.Options;
import jgnash.util.DateUtils;

/**
 * Extends and AreaChart to encapsulate generation of a SecurityNode chart
 *
 * @author Craig Cavanaugh
 */
public class SecurityNodeAreaChart extends AreaChart<Number, Number> {

    private static final int TICK_MARKS = 14;

    private final SimpleObjectProperty<SecurityNode> securityNode = new SimpleObjectProperty<>();

    private final NumberAxis xAxis;

    public SecurityNodeAreaChart() {
        super(new NumberAxis(), new NumberAxis());

        setCreateSymbols(false);
        setLegendVisible(false);
        animatedProperty().bind(Options.animationsEnabledProperty());

        xAxis = (NumberAxis) getXAxis();

        xAxis.setTickLabelFormatter(new StringConverter<Number>() {

            final DateTimeFormatter formatter = DateUtils.getShortDateTimeFormat();

            @Override
            public String toString(final Number value) {
                return formatter.format(LocalDate.ofEpochDay(value.longValue()));
            }

            @Override
            public Number fromString(final String string) {
                return LocalDate.parse(string, formatter).toEpochDay();
            }
        });

        xAxis.tickLabelRotationProperty().setValue(-60);
        xAxis.setAutoRanging(false);

        securityNodeProperty().addListener((observable, oldValue, newValue) -> {
            update();
        });
    }

    public SimpleObjectProperty<SecurityNode> securityNodeProperty() {
        return securityNode;
    }

    public void update() {
        if (securityNodeProperty() != null) {
            new Thread(() -> {
                final SecurityNode securityNode = securityNodeProperty().get();
                final List<List<SecurityHistoryNode>> groups = securityNode.getHistoryNodeGroupsBySplits();

                final Optional<LocalDate[]> bounds = securityNode.getLocalDateBounds();

                if (bounds.isPresent()) {
                    Platform.runLater(() -> getData().clear());

                    for (int i = 0; i < groups.size(); i++) {
                        final AreaChart.Series<Number, Number> series = new AreaChart.Series<>();
                        series.setName(securityNode.getSymbol() + i);

                        for (final SecurityHistoryNode node : groups.get(i)) {
                            series.getData().add(new AreaChart.Data<>(node.getLocalDate().toEpochDay(), node.getAdjustedPrice()));
                        }

                        Platform.runLater(() -> getData().add(series));
                    }

                    xAxis.setLowerBound(bounds.get()[0].toEpochDay());
                    xAxis.setUpperBound(bounds.get()[1].toEpochDay());

                    final long range = bounds.get()[1].toEpochDay() - bounds.get()[0].toEpochDay();

                    if (range > TICK_MARKS) {
                        xAxis.setTickUnit((bounds.get()[1].toEpochDay() - bounds.get()[0].toEpochDay()) / TICK_MARKS);
                    } else {
                        xAxis.setTickUnit(range - 1);
                    }
                }
            }).start();
        }
    }
}
