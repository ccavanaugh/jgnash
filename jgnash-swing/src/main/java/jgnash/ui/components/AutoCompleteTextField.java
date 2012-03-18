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

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import jgnash.ui.components.autocomplete.AutoCompleteModel;

/**
 * An extended JTextField that performs text auto completion
 * 
 * @author Craig Cavanaugh
 *
 */
@SuppressWarnings({ "RedundantStringConstructorCall" })
public class AutoCompleteTextField extends JTextFieldEx {

    private static final long serialVersionUID = -7122559240136664049L;

    private volatile boolean setText = false;

    private AutoCompleteModel model;

    public AutoCompleteTextField(final AutoCompleteModel model) {
        super();
        this.model = model;

        addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(final FocusEvent e) {
                String text = getText();
                if (text != null && text.length() > 0) {
                    setCaretPosition(text.length());
                }
            }
        });
    }

    public AutoCompleteModel getModel() {
        return model;
    }

    @Override
    protected Document createDefaultModel() {
        return new AutoCompleteDocument();
    }

    @Override
    public void setText(final String t) {
        setText = true;

        super.setText(t);

        setText = false;
    }

    protected class AutoCompleteDocument extends PlainDocument {

        private static final long serialVersionUID = -1369307873189366733L;

        String currText = null;

        String newText = null;

        @Override
        public void insertString(final int offs, final String str, final AttributeSet a) throws BadLocationException {

            super.insertString(offs, str, a); // insert the typed characters

            if (!setText) { // only if not trigger by setText method

                currText = this.getText(0, getLength()); // get the full string
                newText = model.doLookAhead(currText); // look for a match               

                if (newText != null) { // found a match
                    remove(0, currText.length()); // clear existing text                   
                    super.insertString(0, currText.substring(0, offs + 1) + newText.substring(offs + 1), a); // replace with the new text string
                    setCaretPosition(newText.length()); // move to the end of the field
                    moveCaretPosition(offs + 1); // move and highlight the new portion of text
                }
            }
        }
    }
}
