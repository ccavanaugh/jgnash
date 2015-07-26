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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.chart.Axis;
import javafx.util.Duration;

import jgnash.util.DateUtils;

import com.sun.javafx.charts.ChartLayoutAnimator;

/**
 * LocalDate Chart axis
 *
 * @author Craig Cavanaugh
 */
public class LocalDateAxis extends Axis<LocalDate> {

    private static final double AVERAGE_TICK_GAP = 85;

    private static final int ANIMATION_DEFAULT = 700;

    private final ChartLayoutAnimator animator;

    private Object activeAnimationID;

    private final DoubleProperty currentLowerBound = new SimpleDoubleProperty(this, "currentLowerBound");

    private final DoubleProperty currentUpperBound = new SimpleDoubleProperty(this, "currentUpperBound");

    private final ObjectProperty<DateTimeFormatter> tickLabelFormatter
            = new SimpleObjectProperty<>(this, "dateTimeFormatter");

    private LocalDate minDate;

    private LocalDate maxDate;

    private final ObjectProperty<LocalDate> lowerBoundProperty = new ObjectPropertyBase<LocalDate>() {
        @Override
        protected void invalidated() {
            if (!isAutoRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }

        @Override
        public Object getBean() {
            return LocalDateAxis.this;
        }

        @Override
        public String getName() {
            return "lowerBoundProperty";
        }
    };

    private final ObjectProperty<LocalDate> upperBoundProperty = new ObjectPropertyBase<LocalDate>() {
        @Override
        protected void invalidated() {
            if (!isAutoRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }

        @Override
        public Object getBean() {
            return LocalDateAxis.this;
        }

        @Override
        public String getName() {
            return "upperBoundProperty";
        }
    };

    public LocalDateAxis() {
        this("", LocalDate.now().minusDays(1), LocalDate.now(), true);
    }

    public LocalDateAxis(final String axisLabel, final LocalDate lowerBound, final LocalDate upperBound, final boolean autoRanging) {
        super();

        tickLabelFormatterProperty().setValue(DateUtils.getShortDateTimeFormat());

        if (lowerBound.isAfter(upperBound)) {
            throw new IllegalArgumentException("Bounds are inverted");
        }

        lowerBoundProperty.setValue(lowerBound);
        upperBoundProperty.setValue(upperBound);

        animator = new ChartLayoutAnimator(this);

        setLabel(axisLabel);
        setAutoRanging(false);  // force to false for initiation of the axis

        if (autoRanging) {  // push to the EDT if auto ranging is asked for
            Platform.runLater(() -> autoRangingProperty().setValue(true));
        }
    }

    @Override
    protected Object getRange() {
        return new LocalDate[]{lowerBoundProperty().get(), upperBoundProperty().get()};
    }

    @Override
    protected void setRange(final Object range, final boolean animate) {
        final LocalDate oldLowerBound = lowerBoundProperty.get();
        final LocalDate oldUpperBound = upperBoundProperty.get();

        final LocalDate lower = (LocalDate) ((Object[]) range)[0];
        final LocalDate upper = (LocalDate) ((Object[]) range)[1];

        lowerBoundProperty.setValue(lower);
        upperBoundProperty.setValue(upper);

        if (animate) {
            animator.stop(activeAnimationID);

            activeAnimationID = animator.animate(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(currentLowerBound, toNumericValue(oldLowerBound)),
                            new KeyValue(currentUpperBound, toNumericValue(oldUpperBound))
                    ),
                    new KeyFrame(Duration.millis(ANIMATION_DEFAULT),
                            new KeyValue(currentLowerBound, toNumericValue(lower)),
                            new KeyValue(currentUpperBound, toNumericValue(upper))
                    )
            );

        } else {
            currentLowerBound.set(toNumericValue(lowerBoundProperty.get()));
            currentUpperBound.set(toNumericValue(upperBoundProperty.get()));
        }
    }

    @Override
    public void invalidateRange(final List<LocalDate> list) {
        super.invalidateRange(list);

        Collections.sort(list);

        if (list.isEmpty()) {
            minDate = maxDate = LocalDate.now();
        } else if (list.size() == 1) {
            minDate = maxDate = list.get(0);
        } else if (list.size() > 1) {
            minDate = list.get(0);
            maxDate = list.get(list.size() - 1);
        }
    }

    @Override
    protected Object autoRange(final double length) {
        if (isAutoRanging()) {
            return new LocalDate[]{minDate, maxDate};
        } else {
            Objects.requireNonNull(lowerBoundProperty().get(), "lower bound not set");
            Objects.requireNonNull(upperBoundProperty().get(), "upper bound not set");

            return getRange();
        }
    }

    @Override
    public double getZeroPosition() {
        return 0;
    }

    @Override
    public double getDisplayPosition(final LocalDate date) {
        // Get the difference between the max and min date.
        double diff = currentUpperBound.get() - currentLowerBound.get();

        // scaling factor to convert the date to the visual length
        double scale = (toNumericValue(date) - currentLowerBound.get()) / diff;

        // Multiply this percent value with the range and add the zero offset.
        if (getSide().isVertical()) {
            return getHeight() - scale * getVisualRange() + getZeroPosition();
        } else {
            return scale * getVisualRange() + getZeroPosition();
        }
    }

    private double getVisualRange() {
        // length of axis display area
        return (getSide().isHorizontal() ? getWidth() : getHeight()) - getZeroPosition();
    }

    @Override
    public LocalDate getValueForDisplay(final double displayPosition) {
        // Get the difference between the max and min date.
        double diff = currentUpperBound.get() - currentLowerBound.get();

        // scaling factor to convert the visual length to a date
        double scale = getVisualRange() * diff;

        double value;

        if (getSide().isVertical()) {
            value = (displayPosition - getZeroPosition() - getHeight()) / -scale + currentLowerBound.get();
        } else {
            value = (displayPosition - getZeroPosition()) / scale + currentLowerBound.get();
        }

        return toRealValue(value);
    }

    @Override
    public boolean isValueOnAxis(final LocalDate date) {
        return toNumericValue(date) > currentLowerBound.get() && toNumericValue(date) < currentUpperBound.get();
    }

    /**
     * Calculate a list of all the data values for each tick mark in range
     *
     * @param length The length of the axis in display units
     * @param range A range object returned from autoRange()
     * @return A list of tick marks that fit along the axis if it was the given length
     */
    @Override
    protected List<LocalDate> calculateTickValues(final double length, final Object range) {
        final LocalDate lower = (LocalDate) ((Object[]) range)[0];
        final LocalDate upper = (LocalDate) ((Object[]) range)[1];

        final List<LocalDate> dateList = new ArrayList<>();

        // The preferred gap which should be between two tick marks.
        double tickCounts = Math.rint(length / AVERAGE_TICK_GAP);

        double tickGap = ((toNumericValue(upper) - toNumericValue(lower)) / (tickCounts - 1)) + 1;

        double position = toNumericValue(lower);

        while (position < toNumericValue(upper)) {
            dateList.add(toRealValue(position));
            position += tickGap;
        }

        dateList.add(upper);

        return dateList;
    }

    @Override
    protected void layoutChildren() {
        if (!isAutoRanging()) {
            currentLowerBound.set(toNumericValue(lowerBoundProperty.get()));
            currentUpperBound.set(toNumericValue(upperBoundProperty.get()));
        }
        super.layoutChildren();
    }

    @Override
    protected String getTickMarkLabel(final LocalDate date) {
        return tickLabelFormatter.get().format(date);
    }

    @Override
    public double toNumericValue(final LocalDate date) {
        return date.toEpochDay();
    }

    @Override
    public LocalDate toRealValue(final double value) {
        return LocalDate.ofEpochDay((long) value);
    }

    public final ObjectProperty<LocalDate> lowerBoundProperty() {
        return lowerBoundProperty;
    }

    public final ObjectProperty<LocalDate> upperBoundProperty() {
        return upperBoundProperty;
    }

    public final ObjectProperty<DateTimeFormatter> tickLabelFormatterProperty() {
        return tickLabelFormatter;
    }
}
