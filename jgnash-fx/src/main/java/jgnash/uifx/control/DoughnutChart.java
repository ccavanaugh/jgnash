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

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Doughnut Chart implementation.
 *
 * @author Craig Cavanaugh
 */
public class DoughnutChart extends PieChart {

    private static final int RING_WIDTH = 3;

    private final Circle hole;

    private final Text titleText;

    private final Text subTitleText;

    private final StringProperty centerTitle = new SimpleStringProperty();

    private final StringProperty centerSubTitle = new SimpleStringProperty();

    @SuppressWarnings("unused")
    public DoughnutChart() {
        this(FXCollections.observableArrayList());
    }

    private DoughnutChart(final ObservableList<Data> data) {
        super(data);

        hole = new Circle();
        hole.setStyle("-fx-fill: -fx-background");

        titleText = new Text();
        titleText.setStyle("-fx-font-size: 1.4em");
        titleText.setTextAlignment(TextAlignment.JUSTIFY);
        titleText.textProperty().bind(centerTitle);

        subTitleText = new Text();
        subTitleText.setStyle("-fx-font-size: 1.0em");
        subTitleText.setTextAlignment(TextAlignment.JUSTIFY);
        subTitleText.textProperty().bind(centerSubTitle);
    }

    public StringProperty centerTitleProperty() {
        return centerTitle;
    }

    public StringProperty centerSubTitleProperty() {
        return centerSubTitle;
    }

    @Override
    protected void layoutChartChildren(final double top, final double left, final double contentWidth,
                                       final double contentHeight) {

        super.layoutChartChildren(top, left, contentWidth, contentHeight);

        installContent();
        updateLayout();
    }

    private void installContent() {
        if (!getData().isEmpty()) {
            final Node node = getData().get(0).getNode();
            if (node.getParent() instanceof Pane) {
                final Pane parent = (Pane) node.getParent();

                // The content needs to be reordered after data has changed
                if (parent.getChildren().contains(hole)) {
                    parent.getChildren().remove(hole);
                    parent.getChildren().remove(titleText);
                    parent.getChildren().remove(subTitleText);
                }

                if (!parent.getChildren().contains(hole)) {
                    parent.getChildren().add(hole);
                    parent.getChildren().add(titleText);
                    parent.getChildren().add(subTitleText);
                }
            }
        }
    }

    private void updateLayout() {

        // Determine maximums and minimums, make use of available processors
        final double minX = getData().parallelStream().mapToDouble(value -> value.getNode().getBoundsInParent()
                .getMinX()).min().orElse(0);

        final double minY = getData().parallelStream().mapToDouble(value -> value.getNode().getBoundsInParent()
                .getMinY()).min().orElse(0);

        final double maxX = getData().parallelStream().mapToDouble(value -> value.getNode().getBoundsInParent()
                .getMaxX()).max().orElse(Double.MAX_VALUE);

        final double maxY = getData().parallelStream().mapToDouble(value -> value.getNode().getBoundsInParent()
                .getMaxY()).max().orElse(Double.MAX_VALUE);

        // center the hole and set radius
        hole.setCenterX(minX + (maxX - minX) / 2);
        hole.setCenterY(minY + (maxY - minY) / 2);
        hole.setRadius((maxX - minX) / RING_WIDTH);

        // center the title and subtitle
        titleText.setX((minX + (maxX - minX) / 2) - titleText.getLayoutBounds().getWidth() / 2);
        titleText.setY(minY + (maxY - minY) / 2);

        subTitleText.setX((minX + (maxX - minX) / 2) - subTitleText.getLayoutBounds().getWidth() / 2);
        subTitleText.setY((minY + (maxY - minY) / 2) + subTitleText.getLayoutBounds().getHeight());
    }
}
