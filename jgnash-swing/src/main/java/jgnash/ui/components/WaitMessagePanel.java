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
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.geom.Ellipse2D;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.icon.EmptyIcon;
import org.jdesktop.swingx.painter.BusyPainter;

/**
 * Animated panel intended for use as a glass pane to block application and display a message
 * 
 * @author Craig Cavanaugh
 * @version $Id: WaitMessagePanel.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public final class WaitMessagePanel extends JXPanel {

    private JLabel messageLabel;

    private JXBusyLabel busyLabel;

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

        busyLabel = new JXBusyLabel(new Dimension(100, 100));

        BusyPainter painter = new BusyPainter(new Ellipse2D.Float(0, 0, 22f, 12f), new Ellipse2D.Float(15f, 15f, 70f, 70f));
        painter.setTrailLength(4);
        painter.setPoints(10);
        busyLabel.setPreferredSize(new Dimension(100, 100));
        busyLabel.setIcon(new EmptyIcon(100, 100));
        busyLabel.setBusyPainter(painter);

        addMouseListener(new MouseAdapter() {
            // empty adapter to block input
        });
    }

    /**
     * Builds the panel with the labels in the center.
     */
    void layoutPanel() {

        initComponents();

        FormLayout layout = new FormLayout("c:p:g", "b:p:g, 10dlu, t:p:g");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.append(busyLabel);
        builder.nextLine();
        builder.nextLine();
        builder.append(messageLabel);

        setLayout(new BorderLayout());

        JPanel panel = builder.getPanel();

        add(panel, BorderLayout.CENTER);

        setAlpha(0.4f);
    }

    public void setMessage(final String messages) {
        assert messages != null;

        messageLabel.setText(messages);
        validate();
    }

    public void setWaiting(final boolean busy) {
        setVisible(busy);
        busyLabel.setBusy(busy);
    }
}
