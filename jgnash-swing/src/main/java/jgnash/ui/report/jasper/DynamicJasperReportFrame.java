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
package jgnash.ui.report.jasper;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import jgnash.ui.components.WaitMessagePanel;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

import org.jdesktop.swingx.JXStatusBar;

/**
 * @author Craig Cavanaugh
 *
 */
final class DynamicJasperReportFrame extends JFrame {

    protected DynamicJasperReportPanel viewer = null;

    private JPanel mainPanel;

    private WaitMessagePanel waitPanel;

    private LogHandler logHandler = new LogHandler();

    private JTextField statusField;

    /**
     * Frame for viewing Jasper reports
     * 
     * @param report DynamicJasperReport
     */
    private DynamicJasperReportFrame(final DynamicJasperReport report) {
        setReportName(report.getReportName());

        init();

        registerLogHandler();

        viewer = new DynamicJasperReportPanel(this, report);
        mainPanel.add(viewer, BorderLayout.CENTER);
    }

    void setReportName(final String name) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                setTitle("jGnash - " + name);
            }
        });
    }

    private void unregisterLogHandler() {
        Logger.getLogger(DynamicJasperReport.class.getName()).removeHandler(logHandler);
    }

    private void registerLogHandler() {
        Logger.getLogger(DynamicJasperReport.class.getName()).addHandler(logHandler);
    }

    private void init() {

        statusField = new JTextField();
        statusField.setEditable(false);
        statusField.setFont(statusField.getFont().deriveFont(statusField.getFont().getSize2D() - 1f));
        statusField.setHorizontalAlignment(SwingConstants.CENTER);

        JXStatusBar statusBar = new JXStatusBar();
        JXStatusBar.Constraint c1 = new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL);
        statusBar.add(statusField, c1);
        statusBar.setResizeHandleEnabled(true);

        setIconImage(Resource.getImage("/jgnash/resource/gnome-money.png"));

        waitPanel = new WaitMessagePanel();

        setGlassPane(waitPanel);

        mainPanel = new JPanel();

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent evt) {
                unregisterLogHandler();
                viewer.clear();
                getContentPane().removeAll();
            }
        });

        mainPanel.setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        setMinimumSize(new Dimension(500, 440));
        pack();

        DialogUtils.addBoundsListener(this);
    }

    protected static void viewReport(final DynamicJasperReport report) {
        DynamicJasperReportFrame jasperViewer = new DynamicJasperReportFrame(report);
        jasperViewer.setVisible(true);
    }

    protected void displayWaitMessage(final String message) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                waitPanel.setMessage(message);
                waitPanel.setWaiting(true);
            }
        });
    }

    protected void stopWaitMessage() {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                waitPanel.setWaiting(false);
            }
        });
    }

    protected void setStatus(final String status) {
        statusField.setText(status);
    }

    private class LogHandler extends Handler {

        @Override
        public void publish(final LogRecord record) {
            // update on the event thread to prevent display corruption
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    waitPanel.setMessage(record.getMessage());
                }
            });
        }

        /**
         * Flush any buffered output.
         */
        @Override
        public void flush() {

        }

        @Override
        public void close() throws SecurityException {

        }
    }
}
