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
package jgnash.ui.components;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import jgnash.time.DateUtils;

/**
 * This is an extended JTextField for the entry of dates.<br>
 * The + and Up keys will increment the day.
 * The - and Down keys with decrement the day.
 * The PgUp and PgDown keys will increment and decrement the month.
 * T or t will set the field to current date
 *
 * @author Craig Cavanaugh
 */
public final class JDateField extends JTextFieldEx {

    private final String DATE;

    private final DateTimeFormatter formatter;
    
    private static final Logger LOG = Logger.getLogger(JDateField.class.getName());

    public JDateField() {
        super(0);

        formatter = DateUtils.getShortDateTimeEntryFormat();

        StringBuilder buf = new StringBuilder("0123456789");
        char[] chars = formatter.format(LocalDate.now()).toCharArray();

        for (char aChar : chars) {
            if (!Character.isDigit(aChar)) {
                if (buf.indexOf(Character.toString(aChar)) == -1) {
                    buf.append(aChar);
                }
            }
        }

        DATE = buf.toString();

        setInputVerifier(new DateVerifier());
        setValue(new Date());
        addKeyListener(new KeySpinner());
    }

    public void setValue(final Object value) {
        if (value instanceof LocalDate) {
            setText(formatter.format((LocalDate) value));
        } else if (value instanceof Date) {
            setText(formatter.format(DateUtils.asLocalDate((Date)value)));
        } else {
            setText("");
        }
    }

    /*public Object getValue() {
        return dateValue();
    }*/

    private Date dateValue() {
        Date tDate = new Date();
        try {
            tDate = DateUtils.asDate(LocalDate.from(formatter.parse(getText())));
        } catch (final DateTimeParseException e) {
            LOG.log(Level.INFO, e.getLocalizedMessage(), e);
        }
        return tDate;
    }

    public LocalDate localDateValue() {
        return DateUtils.asLocalDate(dateValue());
    }

    @Override
    protected Document createDefaultModel() {
        return new DateDocument();
    }

    private class DateDocument extends PlainDocument {

        @Override
        public void insertString(final int offs, final String str, final AttributeSet a)
                throws BadLocationException {

            if (str == null) {
                return;
            }

            for (int i = 0; i < str.length(); i++) {
                if (!DATE.contains(String.valueOf(str.charAt(i)))) {
                    return;
                }
            }
            super.insertString(offs, str, a);
        }
    }

    private class DateVerifier extends InputVerifier {

        @Override
        public boolean verify(final JComponent input) {
            if (((JTextField) input).getText().isEmpty()) {
                return true;
            }
            try {
                formatter.parse(((JTextField) input).getText());
                return true;
            } catch (final DateTimeParseException e) {
                LOG.log(Level.FINEST, e.getLocalizedMessage(), e);
                return false;
            }
        }
    }

    /**
     * This KeyListener listens for the keys to increment the value of the
     * date.  The caret position is restored
     */
    private final class KeySpinner extends KeyAdapter {

        /**
         * Invoked when a key has been pressed.
         * See the class description for {@link KeyEvent} for a definition of
         * a key pressed event.
         */
        @Override
        public void keyPressed(final KeyEvent e) {
            int pos = getCaretPosition();
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                case KeyEvent.VK_KP_UP:
                    setValue(localDateValue().plusDays(1));
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_KP_DOWN:
                    setValue(localDateValue().minusDays(1));
                    break;
                case KeyEvent.VK_PAGE_UP:
                    setValue(localDateValue().plusMonths(1));
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    setValue(localDateValue().minusMonths(1));
                    break;
                case KeyEvent.VK_T:
                    setValue(LocalDate.now());
                    break;
                default:
                    return;
            }

            if (getText().length() < pos) {
                setCaretPosition(getText().length());
            } else {
                setCaretPosition(pos);
            }
        }

        /**
         * Invoked when a key has been typed.
         * See the class description for {@link KeyEvent} for a definition of
         * a key typed event.
         */
        @Override
        public void keyTyped(final KeyEvent e) {
            int pos = getCaretPosition();
            switch (e.getKeyChar()) {
                case '+':
                    setValue(localDateValue().plusDays(1));
                    break;
                case '-':
                    setValue(localDateValue().minusDays(1));
                    break;
                default:
                    return;
            }
            e.consume();
            if (pos < getDocument().getLength()) {
                setCaretPosition(pos);
            } else {
                setCaretPosition(getDocument().getLength());
            }
        }
    }
}
