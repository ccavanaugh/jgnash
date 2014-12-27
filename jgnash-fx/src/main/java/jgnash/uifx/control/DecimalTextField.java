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
package jgnash.uifx.control;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Objects;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jgnash.engine.MathConstants;
import jgnash.util.NotNull;
import jgnash.util.Nullable;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Text field for entering decimal values
 *
 * @author Craig Cavanaugh
 */
public class DecimalTextField extends TextField {

    /**
     * Allowable character in input
     */
    private static final String FLOAT;

    /**
     * Allowable math operators in input
     */
    private static final String MATH_OPERATORS = "()+*/";

    private int scale = 2;

    private static char group = ',';

    private static char fraction = '.';

    /**
     * Used for output of parsed input
     */
    private final NumberFormat format;

    /**
     * Used to track state of fractional separator input on numeric pad
     */
    private volatile boolean forceFraction = false;

    private static final ScriptEngine jsEngine;

    final private ObjectProperty<BigDecimal> decimalProperty = new SimpleObjectProperty<>();

    static {
        FLOAT = getAllowedChars();
        jsEngine = new ScriptEngineManager().getEngineByName("JavaScript");
    }

    public DecimalTextField() {
        final FXMLLoader loader = new FXMLLoader(getClass().getResource("DecimalTextField.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        format = NumberFormat.getInstance();

        /* Disable grouping for output formatting on all locales.
         * This solves issues with parsing out group separators.
         * This does not prevent parsing grouping input.
         */
        format.setGroupingUsed(false);

        setScale(scale);

        // Force evaluation on loss of focus
        focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                evaluateAndSet();
            }
        });

        addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<javafx.scene.input.KeyEvent>() {
            @Override
            public void handle(final KeyEvent event) {

                //Raise a flag the Decimal key has been pressed so it can be
                // forced to a comma or period based on locale
                if (event.getCode() == KeyCode.DECIMAL) {
                    forceFraction = true;
                }

                // For evaluation if the enter key is pressed
                if (event.getCode() == KeyCode.ENTER) {
                    Platform.runLater(() -> {
                        evaluateAndSet();
                        positionCaret(getText().length());
                    });
                }
            }
        });

        setDecimal(BigDecimal.ZERO);

        decimalProperty().addListener(new ChangeListener<BigDecimal>() {
            @Override
            public void changed(ObservableValue<? extends BigDecimal> observable, BigDecimal oldValue, BigDecimal newValue) {
                if (newValue != null && !newValue.equals(oldValue)) {
                    setDecimal(newValue);
                }
            }
        });
    }

    public ObjectProperty<BigDecimal> decimalProperty() {
        return decimalProperty;
    }

    private void evaluateAndSet() {
        final String t = evaluateInput();
        if (!t.isEmpty()) {
            // round the value to scale
            setDecimal(new BigDecimal(t).setScale(scale, MathConstants.roundingMode));
        }
    }

    public void setDecimal(@Nullable final BigDecimal decimal) {
        if (decimal != null) {
            super.setText(format.format(decimal.doubleValue()));
            decimalProperty.setValue(decimal);
        } else {
            super.setText("");
        }
    }

    public BigDecimal getDecimal() {
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
     * Determines if the field is empty
     *
     * @return {@code false} if empty
     */
    public boolean isEmpty() {
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
     * Change the max and minimum scale allowed for entry
     *
     * @param scale number of fractional digits
     */
    public void setScale(final int scale) {
        this.scale = scale;

        if (format instanceof DecimalFormat) {
            format.setMaximumFractionDigits(scale);
            format.setMinimumFractionDigits(scale);
        }

        evaluateAndSet();
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

        // replace any ',' with periods so that it javascript parses it correctly
        if (fraction == ',') {
            text = text.replace(',', '.');
        }

        try {
            return new BigDecimal(text).toString();
        } catch (final NumberFormatException nfe) {
            try {
                final Object o = jsEngine.eval(text);

                if (o instanceof Number) { // scale the number
                    final BigDecimal value = new BigDecimal(o.toString()).setScale(scale, MathConstants.roundingMode);

                    decimalProperty.setValue(value);
                    return value.toString();
                }
            } catch (final ScriptException ex) {
                return "";
            }
        }
        return "";
    }
}
