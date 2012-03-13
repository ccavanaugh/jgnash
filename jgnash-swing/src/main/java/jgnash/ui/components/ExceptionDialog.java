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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * A simple exception dialog with a close button
 *
 * @author Craig Cavanaugh
 * @version $Id: ExceptionDialog.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class ExceptionDialog extends JDialog implements ActionListener {

    private final Resource rb = Resource.get();

    private JButton closeButton;

    private Throwable throwable;

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
        builder.setDefaultDialogBorder();

        closeButton = new JButton(rb.getString("Button.Close"));

        copyButton = new JButton(rb.getString("Button.CopyToClip"));

        textArea = getTextArea(throwable);

        builder.append(new JScrollPane(textArea));
        builder.nextLine();
        builder.nextLine();
        builder.append(ButtonBarFactory.buildRightAlignedBar(copyButton, closeButton));

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
