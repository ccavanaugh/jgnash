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

import java.awt.EventQueue;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SwingWorker;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

import jgnash.engine.Account;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.util.ResourceUtils;

/**
 * Enhanced JComboBox; provides a UI to the user for entering transaction
 * numbers in a consistent manner.
 *
 * @author Craig Cavanaugh
 */
public class TransactionNumberComboBox extends JComboBox<String> {

    private static final String[] defaultItems;

    private static final String nextNumberItem;

    // setup default item numbers
    static {
        ResourceBundle rb = ResourceUtils.getBundle();

        nextNumberItem = rb.getString("Item.NextNum");

        defaultItems = new String[] { "", nextNumberItem, rb.getString("Item.Print") };
    }

    private final Account account;

    public TransactionNumberComboBox(final Account a) {
        super(defaultItems);

        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final List<String> items = engine.getTransactionNumberList();
        final ComboBoxModel<String> model = getModel();

        items.forEach(((DefaultComboBoxModel<String>) model)::addElement);

        account = a;

        if (account != null) {
            JTextComponent e = (JTextComponent) getEditor().getEditorComponent();
            // change the editor's document
            e.setDocument(new SelectionDocument());
        }

        setEditable(true);
    }

    /**
     * Helper method for getting a sane String value from the JComboBox
     *
     * @return the current String value of the JComboBox editor
     */
    public String getText() {
        return (String) getEditor().getItem(); // They are all strings
    }

    /**
     * Helper method for setting the text in the JComboBox editor
     *
     * @param text The new String value for the JComboBox editor
     */
    public void setText(final String text) {
        setSelectedItem(text);
    }

    /**
     * Clear any text selection and restore the caret position
     */
    private void clearSelection() {
        EventQueue.invokeLater(() -> {
            JTextComponent e = (JTextComponent) TransactionNumberComboBox.this.getEditor().getEditorComponent();

            int length = e.getText().length();

            int position = e.getCaretPosition();

            e.setSelectionStart(length);
            e.setSelectionEnd(length);
            e.setCaretPosition(position);
        });
    }

    private class SelectionDocument extends PlainDocument {

        @Override
        public void insertString(int offs, String str, AttributeSet a)
                throws BadLocationException {
            super.insertString(offs, str, a);

            final String item = TransactionNumberComboBox.this.getText();

            if (item.equals(nextNumberItem)) { // next check number
                setPopupVisible(false); // Explicitly close the pop-up
                new SetNextNumber().execute();
            } else {
                clearSelection();
            }
        }
    }

    private class SetNextNumber extends SwingWorker<String, Object> {

        @Override
        public String doInBackground() {
            return account.getNextTransactionNumber();
        }

        @Override
        protected void done() {
            try {
                setText(get());

                JTextComponent e = (JTextComponent) TransactionNumberComboBox.this.getEditor().getEditorComponent();

                int length = e.getText().length();

                e.setSelectionStart(length);
                e.setSelectionEnd(length);
                e.setCaretPosition(length);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
