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
import java.io.File;
import java.util.Arrays;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import jgnash.engine.jpa.SqlUtils;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.actions.DatabasePathAction;
import jgnash.ui.util.DialogUtils;
import jgnash.ui.util.ValidationFactory;
import jgnash.util.FileMagic;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog to change credentials of a database
 *
 * @author Craig Cavanaugh
 */
public class ChangeDatabasePasswordDialog extends JDialog implements ActionListener {

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private final JTextField fileField = new JTextFieldEx();
    private final JButton fileButton = new JButton("...");

    private final JPasswordField passwordField = new JPasswordField();

    private final JPasswordField newPasswordField = new JPasswordField();
    private final JPasswordField newPasswordFieldVal = new JPasswordField();

    private JButton okButton;
    private JButton cancelButton;

    public ChangeDatabasePasswordDialog() {
        super(UIApplication.getFrame(), true);

        setTitle(rb.getString("Title.ChangePassword"));
        layoutMainPanel();

        setMinimumSize(getSize());

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        DialogUtils.addBoundsListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p, 4dlu, fill:70dlu:g, 1dlu, d", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);

        builder.append(new JLabel(rb.getString("Label.DatabaseName")), ValidationFactory.wrap(fileField), fileButton);

        builder.append(rb.getString("Label.Password"), passwordField, 3);

        builder.appendSeparator(rb.getString("Title.NewPassword"));

        builder.append(rb.getString("Label.NewPassword"), newPasswordField, 3);
        builder.append(rb.getString("Label.ConfirmPassword"), ValidationFactory.wrap(newPasswordFieldVal), 3);

        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildOKCancelBar(okButton, cancelButton), 5);

        getContentPane().add(builder.getPanel());

        pack();
    }

    private void initComponents() {
        cancelButton = new JButton(rb.getString("Button.Cancel"));
        okButton = new JButton(rb.getString("Button.Ok"));

        cancelButton.addActionListener(this);
        okButton.addActionListener(this);
        fileButton.addActionListener(this);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == fileButton) {
            String file = DatabasePathAction.databaseNameAction(this, DatabasePathAction.Type.OPEN);
            if (!file.isEmpty()) {
                fileField.setText(file);
            }
        } else if (e.getSource() == cancelButton) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getSource() == okButton) {

            if (fileField.getText().isEmpty()) {
                ValidationFactory.showValidationError(rb.getString("Message.Error.Empty"), fileField);
            } else if (FileMagic.magic(new File(fileField.getText())) != FileMagic.FileType.h2) {
                ValidationFactory.showValidationError(rb.getString("Message.Error.UnsupportedFileType"), fileField);
            } else if (!Arrays.equals(newPasswordField.getPassword(), newPasswordFieldVal.getPassword())) {
                ValidationFactory.showValidationError(rb.getString("Message.Error.PasswordMatch"), newPasswordFieldVal);
            } else {
                boolean result = SqlUtils.changePassword(fileField.getText(), passwordField.getPassword(), newPasswordField.getPassword());

                if (result) {   // display a success message
                    new Thread() {
                        @Override
                        public void run() {
                            StaticUIMethods.displayMessage(rb.getString("Message.CredentialChange"), rb.getString("Title.Success"), JOptionPane.INFORMATION_MESSAGE);
                        }
                    }.start();

                    dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
                } else {
                    setVisible(false);
                    StaticUIMethods.displayError(rb.getString("Message.Error.CredentialChange"));
                    setVisible(true);
                }
            }
        }
    }
}
