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
package jgnash.uifx.report.pdf;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.math.BigDecimal;
import java.util.prefs.Preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.stage.Stage;

import jgnash.report.pdf.Constants;
import jgnash.report.pdf.PageSize;
import jgnash.report.ui.ReportPrintFactory;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.util.InjectFXML;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.util.Nullable;

import org.apache.commons.math3.util.Precision;

/**
 * Page Format Dialog Controller
 *
 * @author Craig Cavanaugh
 */
public class PageFormatDialogController {

    private static final String DEFAULT_MARGIN = "0.5";

    private static final String LAST_UNIT = "lastUnit";

    private static final float EPSILON = 0.5f;  // round error occur due to saved precision of the page size

    @InjectFXML
    private final ObjectProperty<Scene> parent = new SimpleObjectProperty<>();

    @FXML
    private DecimalTextField leftMarginField;

    @FXML
    private DecimalTextField rightMarginField;

    @FXML
    private DecimalTextField topMarginField;

    @FXML
    private DecimalTextField bottomMarginField;

    @FXML
    private DecimalTextField widthField;

    @FXML
    private DecimalTextField heightField;

    @FXML
    private RadioButton portraitRadioButton;

    @FXML
    private RadioButton landscapeRadioButton;

    @FXML
    private ComboBox<PageSize> pageSizeComboBox;

    @FXML
    private ComboBox<Unit> unitsComboBox;

    @FXML
    private Button okayButton;

    private final BooleanProperty invalidPageFormatProperty = new SimpleBooleanProperty();

    private final DoubleProperty unitScaleProperty = new SimpleDoubleProperty(Unit.INCHES.scale);

    private Unit lastUnit = Unit.INCHES;

    private PageFormat pageFormat = ReportPrintFactory.getDefaultPage();

    private final DoubleProperty minWidth = new SimpleDoubleProperty();

    // anything less than 2 inches is considered bad
    private final DoubleProperty minWidthPoints = new SimpleDoubleProperty(Constants.POINTS_PER_INCH * 2);

    private final Preferences preferences = Preferences.userNodeForPackage(PageFormatDialogController.class);

    @FXML
    void initialize() {

        // setup the defaults
        unitsComboBox.getItems().setAll(Unit.POINTS, Unit.MM, Unit.INCHES);
        unitsComboBox.setValue(Unit.INCHES);

        pageSizeComboBox.getItems().setAll(PageSize.values());
        pageSizeComboBox.setValue(PageSize.LETTER);

        widthField.emptyWhenZeroProperty().setValue(false);
        heightField.emptyWhenZeroProperty().setValue(false);
        leftMarginField.emptyWhenZeroProperty().setValue(false);
        rightMarginField.emptyWhenZeroProperty().setValue(false);
        topMarginField.emptyWhenZeroProperty().setValue(false);
        bottomMarginField.emptyWhenZeroProperty().setValue(false);

        // inches
        widthField.setDecimal(new BigDecimal("8.5"));
        heightField.setDecimal(new BigDecimal(11));

        portraitRadioButton.setSelected(true);

        // inches

        final BigDecimal margin = new BigDecimal(DEFAULT_MARGIN);

        leftMarginField.setDecimal(margin);
        rightMarginField.setDecimal(margin);
        topMarginField.setDecimal(margin);
        bottomMarginField.setDecimal(margin);

        // install the listeners
        pageSizeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> handlePageSizeChange());
        unitsComboBox.valueProperty().addListener((observable, oldValue, newValue) -> handleUnitChange());

        // restore the defaults
        unitsComboBox.setValue(Unit.values()[preferences.getInt(LAST_UNIT, Unit.INCHES.ordinal())]);

        // bindings to prevent math mischief from occurring
        minWidth.bind(minWidthPoints.divide(unitScaleProperty));

        invalidPageFormatProperty.bind(widthField.doubleProperty().lessThan(minWidth)
                .or(heightField.doubleProperty().lessThan(minWidth))
                .or(rightMarginField.doubleProperty().greaterThan(widthField.doubleProperty()))
                .or(leftMarginField.doubleProperty().greaterThan(widthField.doubleProperty()))
                .or(topMarginField.doubleProperty().greaterThan(heightField.doubleProperty()))
                .or(bottomMarginField.doubleProperty().greaterThan(heightField.doubleProperty()))
                .or(rightMarginField.doubleProperty().add(leftMarginField.doubleProperty())
                        .greaterThan(widthField.doubleProperty().subtract(minWidth)))
                .or(topMarginField.doubleProperty().add(bottomMarginField.doubleProperty())
                        .greaterThan(heightField.doubleProperty().subtract(minWidth)))
        );

        okayButton.disableProperty().bind(invalidPageFormatProperty);
    }

    void setPageFormat(final PageFormat pageFormat) {

        this.pageFormat = pageFormat;

        if (pageFormat.getOrientation() == PageFormat.LANDSCAPE) {
            landscapeRadioButton.setSelected(true);
        } else {
            portraitRadioButton.setSelected(true);
        }

        // force for correct form orientation
        pageFormat.setOrientation(PageFormat.PORTRAIT);

        final float width = (float) pageFormat.getWidth();
        final float height = (float) pageFormat.getHeight();
        final float imageableX = (float) pageFormat.getImageableX();
        final float imageableY = (float) pageFormat.getImageableY();

        final float rightMargin = width - (float) pageFormat.getImageableWidth() - imageableX;
        final float bottomMargin = height - (float) pageFormat.getImageableHeight() - imageableY;

        final PageSize oldPageSize = matchPageSize(width, height);

        if (oldPageSize != null) {
            pageSizeComboBox.setValue(oldPageSize);
        }

        // load the fields with the new values
        final Unit currentUnit = unitsComboBox.getValue();

        handleUnitChange(widthField, width, currentUnit);
        handleUnitChange(heightField, height, currentUnit);
        handleUnitChange(leftMarginField, imageableX, currentUnit);
        handleUnitChange(topMarginField, imageableY, currentUnit);
        handleUnitChange(rightMarginField, rightMargin, currentUnit);
        handleUnitChange(bottomMarginField, bottomMargin, currentUnit);
    }

    private PageSize matchPageSize(final float width, final float height) {
        for (final PageSize pageSize : PageSize.values()) {
            if ((compare(width, pageSize.width) && compare(height,pageSize.height))
                        || (compare(height, pageSize.width) && compare(width, pageSize.height))) {
                return pageSize;
            }
        }

        return null;
    }

    private boolean compare(float num1, float num2) {
        return Precision.equals(num1, num2, EPSILON);
    }

    @Nullable
    PageFormat getPageFormat() {
        return pageFormat;
    }

    private PageFormat generatePageFormat() {
        final PageFormat pageFormat = new PageFormat();
        final Unit unit = unitsComboBox.getValue();

        if (portraitRadioButton.isSelected()) {
            pageFormat.setOrientation(PageFormat.PORTRAIT);
        } else {
            pageFormat.setOrientation(PageFormat.LANDSCAPE);
        }

        double width = widthField.getDecimal().doubleValue() * unit.scale;
        double height = heightField.getDecimal().doubleValue() * unit.scale;
        double rightMargin = rightMarginField.getDecimal().doubleValue() * unit.scale;
        double bottomMargin = bottomMarginField.getDecimal().doubleValue() * unit.scale;
        double imageableX = leftMarginField.getDecimal().doubleValue() * unit.scale;
        double imageableY = topMarginField.getDecimal().doubleValue() * unit.scale;

        double imageableWidth = width - imageableX - rightMargin;
        double imageableHeight = height - imageableY - bottomMargin;

        final Paper paper = pageFormat.getPaper();
        paper.setSize(width, height);
        paper.setImageableArea(imageableX, imageableY, imageableWidth, imageableHeight);
        pageFormat.setPaper(paper);

        return pageFormat;
    }

    private void handlePageSizeChange() {
        final PageSize pageSize = pageSizeComboBox.getValue();

        final Unit unit = unitsComboBox.getValue();

        // convert points to selected unit of measure
        widthField.setDecimal(new BigDecimal(pageSize.width / unit.scale));
        heightField.setDecimal(new BigDecimal(pageSize.height / unit.scale));
    }

    /**
     * Rescales to a new unit of measure
     */
    private void handleUnitChange() {
        final Unit newUnit = unitsComboBox.getValue();

        preferences.putInt(LAST_UNIT, newUnit.ordinal());

        unitScaleProperty.set(newUnit.scale);   // update binding

        handleUnitChange(widthField, lastUnit, newUnit);
        handleUnitChange(heightField, lastUnit, newUnit);
        handleUnitChange(leftMarginField, lastUnit, newUnit);
        handleUnitChange(rightMarginField, lastUnit, newUnit);
        handleUnitChange(topMarginField, lastUnit, newUnit);
        handleUnitChange(bottomMarginField, lastUnit, newUnit);

        lastUnit = newUnit;
    }

    /**
     * Correctly scales and sets the decimal field
     *
     * @param decimalTextField field to set
     * @param newValue         new value in Points
     * @param newUnit          unit of measure
     */
    private void handleUnitChange(final DecimalTextField decimalTextField, final float newValue, final Unit newUnit) {
        if (decimalTextField.getDecimal().compareTo(BigDecimal.ZERO) != 0) {
            JavaFXUtils.runLater(() -> decimalTextField.setDecimal(new BigDecimal(newValue / newUnit.scale)));
        }
    }

    private void handleUnitChange(final DecimalTextField decimalTextField, final Unit oldUnit, final Unit newUnit) {
        if (decimalTextField.getDecimal().compareTo(BigDecimal.ZERO) != 0) {
            float oldValue = decimalTextField.getDecimal().floatValue() * oldUnit.scale;
            JavaFXUtils.runLater(() -> decimalTextField.setDecimal(new BigDecimal(oldValue / newUnit.scale)));
        }
    }

    @FXML
    private void handleCloseAction() {
        ((Stage) parent.get().getWindow()).close();
    }

    @FXML
    private void handleCancelAction() {
        pageFormat = null;
        handleCloseAction();
    }

    @FXML
    private void handleOkAction() {
        pageFormat = generatePageFormat();

        handleCloseAction();
    }

    private enum Unit {
        POINTS("Points", 1),
        MM("Millimeters", Constants.POINTS_PER_MM),
        INCHES("Inches", Constants.POINTS_PER_INCH);

        Unit(final String description, final float scale) {
            this.description = description;
            this.scale = scale;
        }

        private final transient String description;

        /**
         * Number of points per unit of measure
         */
        final transient float scale;

        @Override
        public String toString() {
            return description;
        }
    }
}
