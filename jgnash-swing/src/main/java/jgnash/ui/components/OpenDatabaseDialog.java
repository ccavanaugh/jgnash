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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import jgnash.ui.StaticUIMethods;
import jgnash.ui.actions.DatabasePathAction;
import jgnash.ui.util.DialogUtils;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Open database dialog
 *
 * @author Craig Cavanaugh
 */
public class OpenDatabaseDialog extends JDialog implements ActionListener {

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private final JPasswordField passwordField = new JPasswordField();

    private final JTextField hostField = new JTextFieldEx();

    private final JTextField fileField = new JTextFieldEx();

    private final JButton fileButton = new JButton("...");

    private JButton okButton;

    private JButton cancelButton;

    private JCheckBox remoteButton = null;

    private final JIntegerField portField = new JIntegerField();

    private boolean result = false;

    public OpenDatabaseDialog(final JFrame parent) {
        super(parent, true);
        setTitle(rb.getString("Title.Open"));
        layoutMainPanel();

        setMinimumSize(getSize());

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        DialogUtils.addBoundsListener(this);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == fileButton) {
            String file = DatabasePathAction.databaseNameAction(this, DatabasePathAction.Type.OPEN);
            if (!file.isEmpty()) {
                fileField.setText(file);
            }
        } else if (e.getSource() == remoteButton) {
            updateForm();
        } else if (e.getSource() == cancelButton) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getSource() == okButton) {
            result = true;
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }

    public String getDatabasePath() {
        return fileField.getText();
    }

    public char[] getPassword() {
        return passwordField.getPassword();
    }

    public boolean getResult() {
        return result;
    }

    public String getHost() {
        return hostField.getText();
    }

    public void setHost(String host) {
        hostField.setText(host);
    }

    public void setPort(int port) {
        portField.setIntValue(port);
    }

    public boolean isRemote() {
        return remoteButton.isSelected();
    }

    public int getPort() {
        return portField.intValue();
    }

    private void initComponents() {
        remoteButton = new JCheckBox(rb.getString("Button.RemoteServer"));

        cancelButton = new JButton(rb.getString("Button.Cancel"));
        okButton = new JButton(rb.getString("Button.Ok"));

        cancelButton.addActionListener(this);
        okButton.addActionListener(this);
        fileButton.addActionListener(this);
        remoteButton.addActionListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p, 4dlu, fill:70dlu:g, 1dlu, d", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);

        builder.append(new JLabel(rb.getString("Label.DatabaseName")), fileField, fileButton);

        builder.append(remoteButton, 4);
        builder.append(rb.getString("Label.DatabaseServer"), hostField, 3);
        builder.append(rb.getString("Label.Port"), portField, 3);

        builder.appendSeparator(rb.getString("Title.FileLoginCredentials"));
        builder.append(rb.getString("Label.Password"), passwordField, 3);

        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildOKCancelBar(okButton, cancelButton), 5);

        getContentPane().add(builder.getPanel());

        updateForm();

        pack();
    }

    public void setDatabasePath(final String dataBase) {
        fileField.setText(dataBase);
    }

    public void setRemote(final boolean remote) {
        remoteButton.setSelected(remote);
        updateForm();
    }

    private void updateForm() {
        boolean remote = remoteButton.isSelected();

        fileField.setEnabled(!remote);
        fileButton.setEnabled(!remote);

        hostField.setEnabled(remote);
        portField.setEnabled(remote);
    }
}
