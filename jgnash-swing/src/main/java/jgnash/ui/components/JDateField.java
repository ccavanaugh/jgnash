/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
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
import java.text.DateFormat;
import java.text.ParseException;
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

import jgnash.util.DateUtils;

/**
 * This is an extended JTextField for the entry of dates.<br>
 * The + and Up keys will increment the day.
 * The - and Down keys with decrement the day.
 * The PgUp and PgDown keys will increment and decrement the month.
 * T or t will set the field to current date
 *
 * @author Craig Cavanaugh
 * @version $Id: JDateField.java 3128 2012-01-22 19:01:08Z ccavanaugh $
 */
public final class JDateField extends JTextFieldEx {

    private final String DATE;

    private final DateFormat formatter;

    public JDateField() {
        super(0);

        formatter = DateFormat.getDateInstance(DateFormat.SHORT);

        StringBuilder buf = new StringBuilder("0123456789");
        char[] chars = formatter.format(new Date()).toCharArray();

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
        if (value instanceof Date) {
            setText(formatter.format((Date) value));
        } else {
            setText("");
        }
    }

    public Object getValue() {
        return dateValue();
    }

    public Date dateValue() {
        Date tDate = new Date();
        try {
            tDate.setTime(formatter.parse(getText()).getTime());
        } catch (ParseException e) {
            Logger.getLogger(JDateField.class.getName()).log(Level.INFO, e.getLocalizedMessage(), e);
        }
        return tDate;
    }

    @Override
    protected Document createDefaultModel() {
        return new DateDocument();
    }

    class DateDocument extends PlainDocument {

        private static final long serialVersionUID = 7524547195996054009L;

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
            if (((JTextField) input).getText().length() == 0) {
                return true;
            }
            try {
                formatter.parse(((JTextField) input).getText());
                return true;
            } catch (ParseException e) {
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
                    setValue(DateUtils.addDay(dateValue()));
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_KP_DOWN:
                    setValue(DateUtils.subtractDay(dateValue()));
                    break;
                case KeyEvent.VK_PAGE_UP:
                    setValue(DateUtils.addMonth(dateValue()));
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    setValue(DateUtils.subtractMonth(dateValue()));
                    break;
                case KeyEvent.VK_T:
                    setValue(new Date());
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
                    setValue(DateUtils.addDay(dateValue()));
                    break;
                case '-':
                    setValue(DateUtils.subtractDay(dateValue()));
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
