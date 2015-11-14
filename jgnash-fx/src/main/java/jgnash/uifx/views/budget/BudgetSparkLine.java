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
package jgnash.uifx.views.budget;

import java.math.BigDecimal;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import jgnash.uifx.skin.ThemeManager;

/**
 * Spark line Canvas for budgets
 *
 * @author Craig Cavanaugh
 */
class BudgetSparkLine extends Canvas {

    private static final int DEFAULT_WIDTH = 140;

    private static final double PERIOD_GAP = 1.0;

    private static final double BAR_WIDTH = 2.0;

    private static final double MARGIN = 5.0;

    private final ObservableList<BigDecimal> amounts = FXCollections.observableArrayList();

    public BudgetSparkLine(final List<BigDecimal> amounts) {
        setHeight(ThemeManager.getBaseTextHeight() * 1.5);
        setWidth(DEFAULT_WIDTH);

        setAmounts(amounts);
    }

    public void setAmounts(final List<BigDecimal> amounts) {
        this.amounts.clear();
        this.amounts.addAll(amounts);

        setWidth(amounts.size() * BAR_WIDTH + (amounts.size() * (PERIOD_GAP - 1)) + (MARGIN * 2) + BAR_WIDTH);
        setHeight(ThemeManager.getBaseTextHeight() * 1.5);

        draw();
    }

    private void draw() {
        final GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        gc.setLineWidth(BAR_WIDTH);

        final double centerLine = getHeight() / 2d;
        final double max = amounts.stream().max(BigDecimal::compareTo).get().doubleValue();
        final double min = amounts.stream().min(BigDecimal::compareTo).get().doubleValue();
        final double scale = centerLine / Math.max(max, Math.abs(min));

        double x = MARGIN;

        for (final BigDecimal amount : amounts) {
            if (amount.signum() > 0) {
                gc.setStroke(Color.BLACK);
                gc.strokeLine(x, centerLine - (BAR_WIDTH / 2d), x, centerLine - amount.abs().doubleValue() * scale);
            } else if (amount.signum() < 0) {
                gc.setStroke(Color.RED);
                gc.strokeLine(x, centerLine + (BAR_WIDTH / 2d), x, centerLine + amount.abs().doubleValue() * scale);
            }

            x += (BAR_WIDTH + PERIOD_GAP);
        }
    }
}
