/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jgnash.ui.StaticUIMethods;
import jgnash.ui.util.DialogUtils;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A simple exception dialog with a close button
 *
 * @author Craig Cavanaugh
 *
 */
public class ExceptionDialog extends JDialog implements ActionListener {

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private JButton closeButton;

    private final Throwable throwable;

    private JTextArea textArea;

    private JButton copyButton;

    public ExceptionDialog(Frame parent, Throwable throwable) {
        super(parent);
        setTitle(rb.getString("Title.UncaughtException"));
        setModal(true);
        this.throwable = throwable;
        layoutMainPanel();

        closeButton.addActionListener(this);
        copyButton.addActionListener(this);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        DialogUtils.addBoundsListener(this);
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("fill:max(80dlu;p):g", "f:max(120dlu;p):g, 6dlu, f:p");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);

        closeButton = new JButton(rb.getString("Button.Close"));

        copyButton = new JButton(rb.getString("Button.CopyToClip"));

        textArea = getTextArea(throwable);

        builder.append(new JScrollPane(textArea));
        builder.nextLine();
        builder.nextLine();
        builder.append(StaticUIMethods.buildRightAlignedBar(copyButton, closeButton));

        getContentPane().add(builder.getPanel());
        pack();
        setMinimumSize(getSize());
    }

    private static JTextArea getTextArea(final Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        t.printStackTrace(out);

        JTextArea textArea = new JTextArea(10, 30);
        textArea.append(sw.toString());

        return textArea;
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e action event
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == closeButton) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getSource() == copyButton) {
            textArea.selectAll();
            textArea.copy();
        }
    }
}
