/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import org.jdesktop.swingx.JXPanel;

/**
 * Animated panel intended for use as a glass pane to block application and display a message
 * 
 * @author Craig Cavanaugh
 */
public final class WaitMessagePanel extends JXPanel {

    private JLabel messageLabel;    

    public WaitMessagePanel() {
        layoutPanel();
    }

    /**
     * Creates and configures the UI components.
     */
    private void initComponents() {
        messageLabel = new JLabel();
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD, 20f));
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);        

        addMouseListener(new EmptyMouseAdapter());
    }

    /**
     * Builds the panel with the labels in the center.
     */
    void layoutPanel() {

        initComponents();

        FormLayout layout = new FormLayout("c:p:g", "c:p:g");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);
       
        builder.append(messageLabel);

        setLayout(new BorderLayout());

        JPanel panel = builder.getPanel();

        add(panel, BorderLayout.CENTER);

        setAlpha(0.4f);
    }

    public void setMessage(final String messages) {      
        if (messages ==  null) {
            throw new IllegalArgumentException();
        }

        messageLabel.setText(messages);
        validate();
    }

    public void setWaiting(final boolean busy) {
        setVisible(busy);        
    }

    private static class EmptyMouseAdapter extends MouseAdapter {
        // empty adapter to block input
    }
}
