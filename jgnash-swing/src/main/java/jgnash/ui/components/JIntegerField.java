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

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

/**
 * Text field for entering integer values
 * 
 * @author Craig Cavanaugh
 */
public class JIntegerField extends JTextFieldEx {

    private static final NumberFormat formatter = NumberFormat.getNumberInstance();

    private static final String INTEGER = "0123456789";
    
    private static final Logger LOG = Logger.getLogger(JIntegerField.class.getName());

    public JIntegerField() {
        this(0);
    }

    private JIntegerField(final int columnSize) {
        super(columnSize);
        setInputVerifier(new IntegerVerifier());
    }

    /*public void setLongValue(final long val) {
        setText(Long.toString(val));
    }*/

    public void setIntValue(final int val) {
        setText(Integer.toString(val));
    }

    public long longValue() {
        try {
            return Long.parseLong(getText());
        } catch (NumberFormatException e) {
            LOG.log(Level.FINEST, e.getLocalizedMessage(), e);
            return 0;
        }
    }

    public int intValue() {
        try {
            return Integer.parseInt(getText());
        } catch (NumberFormatException e) {
            LOG.log(Level.FINEST, e.getLocalizedMessage(), e);
            return 0;
        }
    }

    @Override
    protected Document createDefaultModel() {
        return new IntegerDocument();
    }

    private static class IntegerDocument extends PlainDocument {

        @Override
        public void insertString(final int offs, final String str, final AttributeSet a)
                throws BadLocationException {

            if (str == null) {
                return;
            }

            for (int i = 0; i < str.length(); i++) {
                if (!INTEGER.contains(String.valueOf(str.charAt(i)))) {
                    return;
                }
            }

            super.insertString(offs, str, a);
        }
    }

    private static class IntegerVerifier extends InputVerifier {

        @Override
        public boolean verify(final JComponent input) {
            if (((JTextField) input).getText().isEmpty()) {
                return true;
            }
            try {
                formatter.parse(((JTextField) input).getText());
                return true;
            } catch (ParseException e) {
                LOG.log(Level.FINEST, e.getLocalizedMessage(), e);
                return false;
            }
        }
    }
}