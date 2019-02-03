/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
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
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import jgnash.engine.jpa.JpaNetworkServer;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.util.DialogUtils;
import jgnash.resource.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Remote Connection Dialog
 *
 * @author Craig Cavanaugh
 */
public class RemoteConnectionDialog extends JDialog implements ActionListener {

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private final JPasswordField passwordField = new JPasswordField();

    private final JTextField hostField = new JTextFieldEx();

    private JButton okButton;

    private JButton cancelButton;

    private final JIntegerField portField = new JIntegerField();

    private boolean result = false;

    private static final String LAST_PORT = "lastPort";

    private static final String LAST_HOST = "lastHost";

    public RemoteConnectionDialog(final JFrame parent) {
        super(parent, true);
        setTitle(rb.getString("Title.ConnectServer"));
        layoutMainPanel();

        setMinimumSize(getSize());

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        DialogUtils.addBoundsListener(this);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == cancelButton) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getSource() == okButton) {
            result = true;

            Preferences preferences = Preferences.userNodeForPackage(RemoteConnectionDialog.class);
            preferences.put(LAST_HOST, hostField.getText());
            preferences.putInt(LAST_PORT, portField.intValue());

            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
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

    private void setHost(String host) {
        hostField.setText(host);
    }

    private void setPort(int port) {
        portField.setIntValue(port);
    }

    public int getPort() {
        return portField.intValue();
    }

    private void initComponents() {
        Preferences preferences = Preferences.userNodeForPackage(RemoteConnectionDialog.class);

        setPort(preferences.getInt(LAST_PORT, JpaNetworkServer.DEFAULT_PORT));
        setHost(preferences.get(LAST_HOST, "localhost"));

        cancelButton = new JButton(rb.getString("Button.Cancel"));
        okButton = new JButton(rb.getString("Button.Ok"));

        cancelButton.addActionListener(this);
        okButton.addActionListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p, 4dlu, fill:70dlu:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);

        builder.append(rb.getString("Label.DatabaseServer"), hostField);
        builder.append(rb.getString("Label.Port"), portField);
        builder.append(rb.getString("Label.Password"), passwordField);

        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildOKCancelBar(okButton, cancelButton), 3);

        getContentPane().add(builder.getPanel());

        pack();

        setResizable(false);
    }
}
