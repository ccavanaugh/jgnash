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
package jgnash.ui.option;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ResourceBundle;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import jgnash.net.ConnectionFactory;
import jgnash.ui.components.JIntegerField;
import jgnash.ui.components.JTextFieldEx;
import jgnash.ui.net.NetworkAuthenticator;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Panel for general program options
 *
 * @author Craig Cavanaugh
 */
class NetworkOptions extends JPanel implements ActionListener, FocusListener {

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private JCheckBox authCheckBox;

    private JTextField hostField;

    private JTextField nameField;

    private JPasswordField passwordField;

    private JIntegerField portField;

    private JCheckBox proxyCheckBox;

    private JSpinner connectionTimeout;

    NetworkOptions() {

        layoutMainPanel();

        showState(); // before event handlers are installed
        proxyAction(); // sync the button state
        authAction();


        proxyCheckBox.addActionListener(this);
        authCheckBox.addActionListener(this);

        hostField.addFocusListener(this);
        portField.addFocusListener(this);
        nameField.addFocusListener(this);
        passwordField.addFocusListener(this);

        connectionTimeout.addChangeListener(e ->
                ConnectionFactory.setConnectionTimeout(((SpinnerNumberModel)connectionTimeout.getModel())
                        .getNumber().intValue()));
    }

    private void initComponents() {
        proxyCheckBox = new JCheckBox(rb.getString("Button.UseProxy"));
        hostField = new JTextFieldEx();
        portField = new JIntegerField();
        authCheckBox = new JCheckBox(rb.getString("Button.HTTPAuth"));
        nameField = new JTextFieldEx();
        passwordField = new JPasswordField();

        SpinnerNumberModel model = new SpinnerNumberModel(ConnectionFactory.getConnectionTimeout(), 10, 120, 1);
        connectionTimeout = new JSpinner(model);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("right:p, $lcgap, max(55dlu;p), $lcgap, min:g", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);
        builder.rowGroupingEnabled(true);
        builder.border(Borders.DIALOG);

        builder.appendSeparator(rb.getString("Title.HTTPProxy"));
        builder.append(proxyCheckBox, 5);
        builder.append(rb.getString("Label.Host"), hostField, 3);
        builder.append(rb.getString("Label.Port"), portField, 3);
        builder.append(authCheckBox, 5);
        builder.append(rb.getString("Label.UserName"), nameField, 3);
        builder.append(rb.getString("Label.Password"), passwordField, 3);

        builder.appendSeparator(rb.getString("Title.Connection"));
        builder.append(rb.getString("Label.ConnTimeout"), connectionTimeout);
        builder.append(rb.getString("Word.Seconds"));
    }

    private void showState() {

        proxyCheckBox.setSelected(NetworkAuthenticator.isProxyUsed());
        hostField.setText(NetworkAuthenticator.getHost());
        portField.setIntValue(NetworkAuthenticator.getPort());

        authCheckBox.setSelected(NetworkAuthenticator.isAuthenticationUsed());
        nameField.setText(NetworkAuthenticator.getName());
        passwordField.setText(NetworkAuthenticator.getPassword());
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == proxyCheckBox) {
            proxyAction();
        } else if (e.getSource() == authCheckBox) {
            authAction();
        }
    }

    @Override
    public void focusGained(final FocusEvent e) {
    }

    @Override
    public void focusLost(final FocusEvent e) {
        if (e.getSource() == portField) {
            portAction();
        } else if (e.getSource() == hostField) {
            hostAction();
        } else if (e.getSource() == nameField) {
            nameAction();
        } else if (e.getSource() == passwordField) {
            passwordAction();
        }
    }

    private void proxyAction() {
        NetworkAuthenticator.setUseProxy(proxyCheckBox.isSelected());

        if (proxyCheckBox.isSelected()) {
            hostField.setEnabled(true);
            portField.setEnabled(true);

            System.getProperties().put("http.proxyPort", portField.getText());
            System.getProperties().put("http.proxyHost", hostField.getText());
        } else {
            hostField.setEnabled(false);
            portField.setEnabled(false);

            System.getProperties().remove("http.proxyPort");
            System.getProperties().remove("http.proxyHost");
        }
    }

    private void portAction() {
        int port = portField.intValue();

        NetworkAuthenticator.setPort(port);

        if (!portField.getText().isEmpty() && port > 0) {
            System.getProperties().put("http.proxyPort", port);
        } else {
            System.getProperties().remove("http.proxyPort");
        }
    }

    private void hostAction() {
        String host = hostField.getText();

        NetworkAuthenticator.setHost(host);

        if (!host.isEmpty()) {
            System.getProperties().put("http.proxyHost", host);
        } else {
            System.getProperties().remove("http.proxyHost");
        }
    }

    private void authAction() {
        NetworkAuthenticator.setUseAuthentication(authCheckBox.isSelected());

        if (authCheckBox.isSelected()) {
            nameField.setEnabled(true);
            passwordField.setEnabled(true);
        } else {
            nameField.setEnabled(false);
            passwordField.setEnabled(false);
        }
    }

    private void nameAction() {
        NetworkAuthenticator.setName(nameField.getText());
    }

    private void passwordAction() {
        NetworkAuthenticator.setPassword(new String(passwordField.getPassword()));
    }
}
