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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import jgnash.engine.MathConstants;
import jgnash.uifx.util.JavaFXUtils;
import jgnash.util.MathEval;
import jgnash.util.NotNull;

/**
 * Text field for entering decimal values.
 *
 * @author Craig Cavanaugh
 */
public class DecimalTextField extends TextFieldEx {

    private static final int DEFAULT_SCALE = 2;

    /**
     * Allowable character in input.
     */
    private static final String FLOAT;

    /**
     * Allowable math operators in input.
     */
    private static final String MATH_OPERATORS = "()+*/";

    private static char group = ',';

    private static char fraction = '.';

    /**
     * Used for output of parsed input.
     */
    private final NumberFormat format;

    /**
     * Used to track state of fractional SEPARATOR input on numeric pad.
     */
    private volatile boolean forceFraction = false;

    // the property value may be null
    private final ObjectProperty<BigDecimal> decimal = new SimpleObjectProperty<>();

    private final SimpleDoubleProperty doubleValue = new SimpleDoubleProperty();

    /**
     * Controls the maximum number of displayed decimal places.
     */
    private final SimpleIntegerProperty scale = new SimpleIntegerProperty();

    /**
     * Controls the minimum number of displayed decimal places.
     */
    private final SimpleIntegerProperty minScale = new SimpleIntegerProperty();

    /**
     * Displays an empty field if {@code decimalProperty} is zero.
     */
    private final BooleanProperty emptyWhenZero = new SimpleBooleanProperty(true);

    private final BooleanProperty isValid = new SimpleBooleanProperty(false);

    /**
     * Reference is needed to prevent premature garbage collection.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final ChangeListener<Boolean> focusChangeListener;

    @SuppressWarnings("FieldCanBeLocal")
    private final ChangeListener<String> validValueListener;

    static {
        FLOAT = getAllowedChars();
    }

    public DecimalTextField() {
        format = NumberFormat.getInstance();

        /* Disable grouping for output formatting on all locales.
         * This solves issues with parsing out group separators.
         * This does not prevent parsing grouping input.
         */
        format.setGroupingUsed(false);

        // Force evaluation on loss of focus
        focusChangeListener = (observable, oldValue, newValue) -> {
            if (!newValue) {
                evaluateAndSet();
            }
        };

        // Listen to any text changes to determine if it
        validValueListener = (observable, oldValue, newValue) -> {
            try {
                // Replace any commas with decimals before trying to parse the string
                Double.parseDouble(newValue.replace(',', '.'));
                isValid.setValue(true);
            } catch (Exception e) {
                isValid.setValue(false);
            }
        };

        focusedProperty().addListener(new WeakChangeListener<>(focusChangeListener));

        textProperty().addListener(new WeakChangeListener<>(validValueListener));

        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            //Raise a flag the Decimal key has been pressed so it can be
            // forced to a comma or period based on locale
            if (event.getCode() == KeyCode.DECIMAL) {
                forceFraction = true;
            }

            // For evaluation if the enter key is pressed
            if (event.getCode() == KeyCode.ENTER) {
                JavaFXUtils.runLater(() -> {
                    evaluateAndSet();
                    positionCaret(getText().length());
                });
            }
        });

        decimalProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || (emptyWhenZeroProperty().get() && newValue.compareTo(BigDecimal.ZERO) == 0)) {
                setText("");
            } else {
                setText(format.format(newValue.doubleValue()));
            }
        });

        // Change the max and minimum scale allowed for entry
        scale.addListener((observable, oldValue, newValue) -> {
            if (format instanceof DecimalFormat) {
                format.setMaximumFractionDigits(scale.get());
            }
            evaluateAndSet();
        });

        minScale.addListener((observable, oldValue, newValue) -> {
            if (format instanceof DecimalFormat) {
                format.setMinimumFractionDigits(minScale.get());
            }
            evaluateAndSet();
        });

        scale.set(DEFAULT_SCALE); // trigger update to the format
        minScale.set(DEFAULT_SCALE);
    }

    public ObjectProperty<BigDecimal> decimalProperty() {
        return decimal;
    }

    /**
     * {@code ReadOnlyDoubleProperty} representation of the {@code BigDecimal} property to make bindings and error checking easier
     *
     * @return ReadOnlyDoubleProperty
     */
    public ReadOnlyDoubleProperty doubleProperty() {
        return ReadOnlyDoubleProperty.readOnlyDoubleProperty(doubleValue);
    }

    /**
     * Property indicating if the field contains a valid numeric value
     *
     * @return ReadOnlyBooleanProperty
     */
    public ReadOnlyBooleanProperty validDecimalProperty() {
        return ReadOnlyBooleanProperty.readOnlyBooleanProperty(isValid);
    }

    public IntegerProperty scaleProperty() {
        return scale;
    }

    public IntegerProperty minScaleProperty() {
        return minScale;
    }

    private void evaluateAndSet() {
        final String t = evaluateInput();
        if (!t.isEmpty()) {
            // round the value to scale
            setDecimal(new BigDecimal(t).setScale(scale.get(), MathConstants.roundingMode));
        } else {
            setDecimal(BigDecimal.ZERO);
        }
    }

    /**
     * Sets the decimal value for the field.
     *
     * @param decimal {@code BigDecimal}
     */
    public void setDecimal(@NotNull final BigDecimal decimal) {
        Objects.requireNonNull(decimal);

        this.decimal.set(decimal.setScale(scale.get(), MathConstants.roundingMode));
        doubleValue.setValue(this.decimal.getValue().doubleValue());    // set the double property
    }

    public @NotNull
    BigDecimal getDecimal() {
        if (!isEmpty()) {
            try {
                return new BigDecimal(evaluateInput());
            } catch (final NumberFormatException ignored) {
                // ignore and drop out
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Determines if the field is empty.
     *
     * @return {@code false} if empty
     */
    private boolean isEmpty() {
        boolean result = true;

        if (getText() != null) {
            result = getText().isEmpty();
        }

        return result;
    }

    @Override
    public void deleteText(int start, int end) {
        super.replaceText(start, end, "");
    }

    @Override
    public void replaceText(final int start, final int end, final String text) {
        Objects.requireNonNull(text);

        final String newText = getText().substring(0, start) + text + getText().substring(end);

        /* fraction input is handled as a special case */
        if (forceFraction) {
            super.replaceText(start, end, Character.toString(fraction));
            forceFraction = false;
            return;
        }

        for (int i = 0; i < newText.length(); i++) {
            if (!FLOAT.contains(String.valueOf(newText.charAt(i)))) {
                return;
            }
        }
        super.replaceText(start, end, text);
    }

    @Override
    public void replaceSelection(final String text) {
        final int start = getSelection().getStart();
        final int end = getSelection().getEnd();
        final String newText = getText().substring(0, start) + text + getText().substring(end);

        for (int j = 0; j < newText.length(); j++) {
            if (!FLOAT.contains(String.valueOf(newText.charAt(j)))) {
                return;
            }
        }
        super.replaceSelection(text);

        positionCaret(end);
    }

    /**
     * By default, and numeric values, basic math operators, and '.' and ',' are
     * allowed.
     *
     * @return A string with the characters that are allowed in math expressions
     */
    private static String getAllowedChars() {
        // get grouping and fractional separators
        final NumberFormat format = NumberFormat.getInstance();

        if (format instanceof DecimalFormat) {
            group = ((DecimalFormat) format).getDecimalFormatSymbols().getGroupingSeparator();
            fraction = ((DecimalFormat) format).getDecimalFormatSymbols().getDecimalSeparator();
        }

        // doctor up some locales so numeric pad works
        if (group != '.' && fraction == ',') { // grouping symbol is odd
            group = '.';
        }

        return "-0123456789" + group + fraction + MATH_OPERATORS;
    }

    /**
     * BigDecimal and the interpreter cannot parse ',' in string
     * representations of decimals. This method will replace any ',' with '.'
     * and then try parsing with BigDecimal. If this fails, then it is assumed
     * that the user has used mathematical operators and then evaluates the
     * string as a mathematical expression.
     *
     * @return A string representation of the resulting decimal
     */
    @NotNull
    private String evaluateInput() {
        String text = getText();

        if (text == null || text.isEmpty()) {
            return "";
        }

        // strip out any group separators (This could be '.' for certain locales)
        final StringBuilder temp = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != group) {
                temp.append(c);
            }
        }

        text = temp.toString();

        // replace any ',' with periods so that it can be parsed correctly
        if (fraction == ',') {
            text = text.replace(',', '.');
        }

        try {
            doubleValue.setValue(new BigDecimal(text).doubleValue());   // try to set the double property
            return new BigDecimal(text).toString();
        } catch (final NumberFormatException nfe) {
            try {
                final double val = MathEval.eval(text);

                if (!Double.isNaN(val)) {
                    final BigDecimal value = new BigDecimal(val);

                    setDecimal(value);
                    return value.toString();
                }
                doubleValue.set(Double.NaN);
                return "";
            } catch (final ArithmeticException ex) {
                return "";
            }
        }
    }

    public BooleanProperty emptyWhenZeroProperty() {
        return emptyWhenZero;
    }
}
