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
package jgnash.ui.checks;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import jgnash.engine.checks.CheckLayout;
import jgnash.engine.checks.CheckLayoutSerializationFactory;
import jgnash.ui.UIApplication;
import jgnash.ui.components.JTextFieldEx;
import jgnash.util.Resource;

/**
 * Displays a dialog that shows the available options for printing a check.
 *
 * @author Craig Cavanaugh
 *
 */
class PrintCheckDialog extends JDialog implements ActionListener {

    private Resource rb = Resource.get();

    private JButton cancelButton;

    private JCheckBox incCheckBox;

    private JTextField layoutField;

    private JButton printButton;

    private JButton selectButton;

    private JSpinner startSpinner;

    private CheckLayout checkLayout;

    private static final String LAST_LAYOUT = "lastlayout";

    private static final String CURRENT_DIR = "cwd";

    private static final String INC_NUM = "incrementNumbers";

    private boolean returnStatus = false; // return status of dialog

    private Preferences pref = Preferences.userNodeForPackage(PrintCheckDialog.class);

    public PrintCheckDialog() {
        super(UIApplication.getFrame(), true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        layoutMainPanel();

        this.setLocationRelativeTo(UIApplication.getFrame());

        if (pref.get(LAST_LAYOUT, null) != null) {
            loadLayout(pref.get(LAST_LAYOUT, null));
        }
    }

    private void initComponents() {
        layoutField = new JTextFieldEx();
        selectButton = new JButton(rb.getString("Button.Select"));
        startSpinner = new JSpinner();
        incCheckBox = new JCheckBox(rb.getString("Button.IncCheckNums"));
        printButton = new JButton(rb.getString("Button.Print"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));

        // set the spinner value to a safe start value
        startSpinner.getModel().setValue(1);

        cancelButton.addActionListener(this);
        printButton.addActionListener(this);
        selectButton.addActionListener(this);

        incCheckBox.setSelected(pref.getBoolean(INC_NUM, false));
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p, 4dlu, 85dlu:g, 4dlu, p", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.setDefaultDialogBorder();
        builder.setRowGroupingEnabled(true);
        builder.append(rb.getString("Label.CheckLayout"), layoutField, selectButton);
        builder.append(rb.getString("Label.StartPos"), startSpinner);
        builder.nextLine();
        builder.append(incCheckBox, 5);
        builder.setRowGroupingEnabled(false);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(ButtonBarFactory.buildOKCancelBar(printButton, cancelButton), 5);

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        pack();

        setMinimumSize(getSize());
    }

    private void closeDialog() {
        pref.putBoolean(INC_NUM, incCheckBox.isSelected());
        dispatchEvent(new WindowEvent(PrintCheckDialog.this, WindowEvent.WINDOW_CLOSING));
    }

    private void setupSpinner() {
        ((SpinnerNumberModel) startSpinner.getModel()).setMinimum(1);
        ((SpinnerNumberModel) startSpinner.getModel()).setMaximum(checkLayout.getNumberOfChecks());
    }

    /**
     * Creates a <code>JFileChooser</code>
     * directory can be null, The JFileChooser will sort it out
     * @param dir directory to start in
     * @return the JFileChooser
     */
    private JFileChooser createFileChooser(final String dir) {
        JFileChooser chooser = new JFileChooser(dir);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(rb.getString("Label.jGnashFiles") + " (*.xml)", "xml"));
        return chooser;
    }

    /**
     * Show a file selection dialog for the check layout using the last known
     * directory.  If successful, save the working directory and file path.
     */
    private void selectLayout() {
        JFileChooser chooser = createFileChooser(pref.get(CURRENT_DIR, null));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pref.put(CURRENT_DIR, chooser.getCurrentDirectory().getAbsolutePath());
            String file = chooser.getSelectedFile().getAbsolutePath();
            pref.put(LAST_LAYOUT, file);
            loadLayout(file);
        }
    }

    /**
     * Position starts at index 0
     *
     * @return start position
     */
    public int getStartPosition() {
        return ((Number) startSpinner.getValue()).intValue() - 1;
    }

    public boolean getReturnStatus() {
        return returnStatus;
    }

    public CheckLayout getCheckLayout() {
        return checkLayout;
    }

    protected boolean incrementCheckNumbers() {
        return incCheckBox.isSelected();
    }

    private void loadLayout(final String file) {
        if (new File(file).exists()) { // check for existence before trying to load
            Object o = CheckLayoutSerializationFactory.loadLayout(file);

            if (o != null) {
                checkLayout = (CheckLayout) o;
                setupSpinner();
                layoutField.setText(file);
            }
        }
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == cancelButton) {
            closeDialog();
        } else if (e.getSource() == printButton) {
            returnStatus = checkLayout != null;
            closeDialog();
        } else if (e.getSource() == selectButton) {
            selectLayout();
        }
    }
}
