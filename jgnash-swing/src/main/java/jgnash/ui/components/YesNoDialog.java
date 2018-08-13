/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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
import java.util.ResourceBundle;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.UIManager;

import jgnash.ui.StaticUIMethods;
import jgnash.resource.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A simple yes/no dialog.  A substitute for JOption pane that improves
 * locale support and creates a more consistent user interface
 *
 * @author Craig Cavanaugh
 */
public class YesNoDialog extends JDialog implements ActionListener {
    private final ResourceBundle rb = ResourceUtils.getBundle();

    private JButton noButton;

    private JButton yesButton;

    private final JComponent component;

    private boolean result;

    /**
     * Used for error messages.
     */
    private static final int ERROR_MESSAGE = 0;

    /**
     * Used for information messages.
     */
    private static final int INFORMATION_MESSAGE = 1;

    /**
     * Used for warning messages.
     */
    public static final int WARNING_MESSAGE = 2;

    /**
     * Used for questions.
     */
    private static final int QUESTION_MESSAGE = 3;


    public static boolean showYesNoDialog(final Frame parent, final JComponent component, final String title) {
        return showYesNoDialog(parent, component, title, QUESTION_MESSAGE);
    }

    public static boolean showYesNoDialog(final Frame parent, final JComponent component, final String title, final int messageType) {
        YesNoDialog d = new YesNoDialog(parent, component, title, messageType);

        d.setVisible(true);
        return d.result;
    }

    private YesNoDialog(final Frame parent, final JComponent component, final String title, final int messageType) {
        super(parent, true);
        setTitle(title);
        this.component = component;
        layoutMainPanel(messageType);
        noButton.addActionListener(this);
        yesButton.addActionListener(this);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);
    }

    private void layoutMainPanel(final int messageType) {
        FormLayout layout = new FormLayout("p, $lcgap, fill:p:g", "f:p:g, $ugap, f:p");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);

        noButton = new JButton(rb.getString("Button.No"));
        yesButton = new JButton(rb.getString("Button.Yes"));

        builder.append(new JLabel(getIconForType(messageType)), component);
        builder.nextLine();
        builder.nextLine();
        builder.append(StaticUIMethods.buildOKCancelBar(yesButton, noButton), 3);

        getContentPane().add(builder.getPanel());
        pack();
    }

    private static Icon getIconForType(int messageType) {
        if (messageType < 0 || messageType > 3) {
            return null;
        }

        switch (messageType) {
            case ERROR_MESSAGE:
                return UIManager.getIcon("OptionPane.errorIcon");
            case INFORMATION_MESSAGE:
                return UIManager.getIcon("OptionPane.informationIcon");
            case WARNING_MESSAGE:
                return UIManager.getIcon("OptionPane.warningIcon");
            case QUESTION_MESSAGE:
                return UIManager.getIcon("OptionPane.questionIcon");
        }
        return null;
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == noButton) {
            result = false;
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getSource() == yesButton) {
            result = true;
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }
}