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
package jgnash.uifx.views.budget;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import jgnash.uifx.skin.ThemeManager;

/**
 * Spark line Canvas for budgets.
 *
 * @author Craig Cavanaugh
 */
class BudgetSparkLine extends Canvas {

    private static final double DEFAULT_WIDTH = 180;

    private static final double MAX_WIDTH = 400;

    private static final double DEFAULT_PERIOD_GAP = 1.0;

    private static final double DEFAULT_BAR_WIDTH = 2.0;

    private static final double MARGIN = 6.0;

    private static final double HEIGHT_MULTIPLIER = 1.5;

    private final ObservableList<BigDecimal> amounts = FXCollections.observableArrayList();

    private double barWidth = DEFAULT_BAR_WIDTH;

    private double periodGap = DEFAULT_PERIOD_GAP;

    /**
     * Listens for changes to the font scale
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final ChangeListener<Number> fontScaleListener;

    BudgetSparkLine(final List<BigDecimal> amounts) {

        fontScaleListener = (observable, oldValue, newValue) -> draw();
        ThemeManager.fontScaleProperty().addListener(new WeakChangeListener<>(fontScaleListener));

        setAmounts(amounts);
    }

    private void setAmounts(final List<BigDecimal> amounts) {
        this.amounts.clear();
        this.amounts.addAll(amounts);

        draw();
    }

    private double calculateWidth() {
        return (amounts.size() * barWidth) + (amounts.size() * periodGap) + (MARGIN * 2) + barWidth;
    }

    private void draw() {
        if (calculateWidth() < DEFAULT_WIDTH || calculateWidth() > MAX_WIDTH) {
            final double scale = DEFAULT_WIDTH / calculateWidth();
            barWidth = barWidth * scale;
            periodGap = periodGap * scale;
        }

        setWidth(calculateWidth());
        setHeight(ThemeManager.getBaseTextHeight() * HEIGHT_MULTIPLIER);

        final GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        gc.setLineWidth(barWidth);

        final double centerLine = getHeight() / 2d;
        final double max = Collections.max(amounts).doubleValue();
        final double min = Collections.min(amounts).doubleValue();
        final double scale = centerLine / Math.max(max, Math.abs(min));

        double x = MARGIN + barWidth / 2d;

        for (final BigDecimal amount : amounts) {
            if (amount.signum() > 0) {
                gc.setStroke(Color.BLACK);
                gc.strokeLine(x, centerLine - (barWidth / 2d), x, centerLine - amount.abs().doubleValue() * scale);
            } else if (amount.signum() < 0) {
                gc.setStroke(Color.RED);
                gc.strokeLine(x, centerLine + (barWidth / 2d), x, centerLine + amount.abs().doubleValue() * scale);
            }

            x += (barWidth + periodGap);
        }
    }
}
