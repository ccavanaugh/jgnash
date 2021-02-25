/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2020 Craig Cavanaugh
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
package jgnash.bootloader;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jgnash.resource.util.ResourceUtils;

/**
 * UI for displaying the boot loader download status.
 *
 * @author Craig Cavanaugh
 */
public class BootLoaderDialog extends JFrame {

    private JLabel activeDownloadLabel;
    private JProgressBar progressBar;

    private final Consumer<String> activeFileConsumer;

    private final IntConsumer percentCompleteConsumer;

    public BootLoaderDialog() {

        ImageIcon icon = new ImageIcon(Objects.requireNonNull(
                BootLoaderDialog.class.getResource("/jgnash/resource/gnome-money.png")));

        setIconImage(icon.getImage());

        this.activeFileConsumer = s -> EventQueue.invokeLater(() -> activeDownloadLabel.setText(s));

        this.percentCompleteConsumer = value -> EventQueue.invokeLater(() -> {
            progressBar.setValue(value);

            if (value >= 100) {
                final Thread thread = new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        System.exit(BootLoader.REBOOT_EXIT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

                thread.start();
            }
        });

        initComponents();
    }

    public Consumer<String> getActiveFileConsumer() {
        return activeFileConsumer;
    }

    public IntConsumer getPercentCompleteConsumer() {
        return percentCompleteConsumer;
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        JScrollPane jScrollPane = new JScrollPane();
        JTextArea messageArea = new JTextArea();

        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setStringPainted(true);

        JButton cancelButton = new JButton(ResourceUtils.getString("Button.Cancel"));
        cancelButton.addActionListener(evt -> cancelButtonActionPerformed());

        activeDownloadLabel = new JLabel();
        activeDownloadLabel.setFocusable(false);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Please Wait");
        setAlwaysOnTop(true);
        setMinimumSize(new java.awt.Dimension(430, 220));
        getContentPane().setLayout(new GridBagLayout());

        jScrollPane.setBorder(null);

        messageArea.setEditable(false);
        messageArea.setColumns(20);
        messageArea.setRows(3);
        messageArea.setText(ResourceUtils.getString("Message.OpenJfxDownload"));
        messageArea.setWrapStyleWord(true);
        messageArea.setFocusable(false);
        messageArea.setOpaque(false);
        messageArea.setRequestFocusEnabled(false);
        jScrollPane.setViewportView(messageArea);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(12, 12, 12, 12);
        getContentPane().add(jScrollPane, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 677;
        gridBagConstraints.insets = new Insets(6, 12, 12, 12);
        getContentPane().add(progressBar, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 12, 12);
        getContentPane().add(cancelButton, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(12, 12, 6, 12);
        getContentPane().add(activeDownloadLabel, gridBagConstraints);

        pack();
        setLocationRelativeTo(null);
    }

    private void cancelButtonActionPerformed() {
        System.exit(progressBar.getValue() == 100 ? BootLoader.REBOOT_EXIT : -1);
    }
}
