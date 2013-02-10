/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
package jgnash.ui.components;

import java.awt.EventQueue;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import jgnash.engine.CommodityNode;

/**
 * This class extends JTextField and uses a JavaScript engine to evaluate
 * mathematical expressions in the field. Only simple math operations are
 * supported "+, -, /, *, (, )". Characters are limited to the allowed math
 * operators, decimals, and locale specific numerical grouping separators.
 *
 * @author Craig Cavanaugh
 *
 */
public class JFloatField extends JTextFieldEx {

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
    private volatile boolean keypad = false;

    /**
     * Enable / disable debugging
     */
    private static final boolean DEBUG = false;

    private static final ScriptEngine jsEngine;

    static {
        FLOAT = JFloatField.getAllowedChars();
        jsEngine = new ScriptEngineManager().getEngineByName("JavaScript");
    }

    public JFloatField() {
        this(0, 2, 2);
    }

    public JFloatField(final CommodityNode node) {
        this(0, node.getScale(), node.getScale());
    }

    /**
     * Constructs a new empty JFloatField with the specified number of columns and specified fractional range
     *
     * @param columns  the number of columns to use to calculate the preferred width; if columns is set to zero,
     *                 the preferred width will be whatever naturally results from the component implementation
     * @param maxScale the maximum number of fractional digits allowed.  Another over will be rounded down
     * @param minScale the minimum number of fractional digits allowed.  Another less will be padded
     */
    public JFloatField(final int columns, final int maxScale, final int minScale) {
        super(columns);
        this.scale = maxScale;

        addFocusListener(new EvalFocusListener());
        addKeyListener(new KeyPadListener());

        format = NumberFormat.getInstance();

        if (format instanceof DecimalFormat) {
            format.setMaximumFractionDigits(maxScale);
            format.setMinimumFractionDigits(minScale);
        }

        /* Disable grouping for output formatting on all locales.
         * This solves issues with parsing out group separators.
         * This does not prevent parsing grouping input.
         */
        format.setGroupingUsed(false);
    }

    public void setScale(final CommodityNode node) {
        setScale(node.getScale(), node.getScale());
    }

    /**
     * Change the max and minimum scale allowed for entry
     *
     * @param maxScale max scale
     * @param minScale min scale
     */
    void setScale(final int maxScale, final int minScale) {
        scale = maxScale;

        if (format instanceof DecimalFormat) {
            format.setMaximumFractionDigits(maxScale);
            format.setMinimumFractionDigits(minScale);
        }
    }

    /**
     * Change the max and minimum scale allowed for entry
     *
     * @param scale maximum and minimum scale
     */
    public void setScale(final int scale) {
        setScale(scale, scale);
    }

    /**
     * By default, and numeric values, basic math operators, and '.' and ',' are
     * allowed.
     *
     * @return A string with the characters that are allowed in math expressions
     */
    private static String getAllowedChars() {
        // get grouping and fractional separators
        NumberFormat format = NumberFormat.getInstance();
        if (format instanceof DecimalFormat) {
            group = ((DecimalFormat) format).getDecimalFormatSymbols().getGroupingSeparator();
            fraction = ((DecimalFormat) format).getDecimalFormatSymbols().getDecimalSeparator();
        }

        if (DEBUG) {
            System.out.println("Before fixup");
            System.out.println("fraction " + fraction);
            System.out.println("group " + group);
        }

        // doctor up some locales so numeric pad works
        if (group != '.' && fraction == ',') { // grouping symbol is odd
            group = '.';
        }

        if (DEBUG) {
            System.out.println();
            System.out.println("After fixup");
            System.out.println("fraction " + fraction);
            System.out.println("group " + group);
        }

        return "-0123456789" + group + fraction + MATH_OPERATORS;
    }

    @Override
    protected Document createDefaultModel() {
        return new FloatDocument();
    }

    public BigDecimal getDecimal() {
        if (getText().length() > 0) {
            try {
                return new BigDecimal(eval());
            } catch (NumberFormatException e) {
                if (DEBUG) {
                    System.out.println(e.getLocalizedMessage());
                }
            } // ignore and drop out
        }
        return BigDecimal.ZERO;
    }

    /**
     * Use setDecimal to obtain proper locale formatting
     *
     * @param text decimal text
     * @see #setDecimal(java.math.BigDecimal)
     * @deprecated
     */
    @Override
    @Deprecated
    public void setText(String text) {
        // do nothing
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
    String eval() {
        String text = getText();

        if (text.length() == 0) {
            return "";
        }

        if (DEBUG) {
            System.out.println("eval s0: " + text);
        }

        // strip out any group separators (This could be '.' for certain
        // locales)
        StringBuilder temp = new StringBuilder();
        for (int j = 0; j < text.length(); j++) {
            char c = text.charAt(j);
            if (c != group) {
                temp.append(c);
            }
        }
        text = temp.toString();

        if (DEBUG) {
            System.out.println("eval s1: " + text);
        }

        // replace any ',' with periods so that it parses correctly only if
        // needed
        if (fraction == ',') {
            text = text.replace(',', '.');
        }

        if (DEBUG) {
            System.out.println("eval s2: " + text);
        }

        try {
            BigDecimal d = new BigDecimal(text);
            if (DEBUG) {
                System.out.println("eval result: " + d.toString());
            }
            return d.toString();
        } catch (NumberFormatException nfe) {

            if (DEBUG) {
                System.out.println(nfe.toString());
            }

            try {
                Object o;

                o = jsEngine.eval(text);

                if (o instanceof Number) { // scale the number
                    return new BigDecimal(o.toString()).setScale(scale, BigDecimal.ROUND_HALF_UP).toString();
                }
            } catch (ScriptException ex) {
                if (DEBUG) {               
                    Logger.getLogger(JFloatField.class.getName()).log(Level.SEVERE, null, ex);
                }
                return "";
            }
        }
        return "";
    }

    public void setDecimal(final BigDecimal decimal) {
        if (decimal != null) {
            if (DEBUG) {
                System.out.println("setDecimal:" + format.format(decimal.doubleValue()));
            }
            super.setText(format.format(decimal.doubleValue()));
        } else {
            super.setText("");
        }
    }

    private class FloatDocument extends PlainDocument {

        private static final long serialVersionUID = 6266828450216708242L;

        @Override
        public void insertString(final int offs, final String str, final AttributeSet a)
                throws BadLocationException {
            if (str == null) {
                return;
            }

            /* fraction input is handled as a special case */
            if (keypad) {
                super.insertString(offs, Character.toString(fraction), a);
                keypad = false;
                return;
            }

            /* everything else */
            for (int j = 0; j < str.length(); j++) {
                if (!FLOAT.contains(String.valueOf(str.charAt(j)))) {
                    return;
                }
            }
            super.insertString(offs, str, a);
        }
    }

    /**
     * This FocusListener evaluates the line when the focus is lost
     */
    private final class EvalFocusListener extends FocusAdapter {

        /**
         * Evaluate the field on the event thread
         */
        @Override
        public void focusLost(final FocusEvent e) {
            if (!e.isTemporary()) {
                EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        String t = eval();
                        if (t.length() > 0) {
                            // round the value to scale
                            setDecimal(new BigDecimal(t).setScale(scale, BigDecimal.ROUND_HALF_UP));
                        }
                    }
                });
            }
        }
    }

    /**
     * This is a KeyListener that looks at the number pad decimal and converts a
     * grouping separator into a fraction separator in needed.
     */
    private final class KeyPadListener extends KeyAdapter {

        /**
         * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
         */
        @Override
        public void keyPressed(final KeyEvent e) {
            if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD && e.getKeyCode() == KeyEvent.VK_DECIMAL) {
                keypad = true;
            }
        }
    }
}