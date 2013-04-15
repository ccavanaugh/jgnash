/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2013 Craig Cavanaugh
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
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.text.DecimalFormat;
import java.util.prefs.Preferences;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import jgnash.engine.checks.CheckLayout;
import jgnash.engine.checks.CheckLayoutSerializationFactory;
import jgnash.engine.checks.CheckObject;
import jgnash.engine.checks.CheckObject.CheckObjectType;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.components.JTextFieldEx;
import jgnash.ui.components.RollOverButton;
import jgnash.util.Resource;

/**
 * Check design dialog.
 *
 * @author Craig Cavanaugh
 */
public class CheckDesignDialog extends JDialog implements ActionListener, ListSelectionListener, FocusListener {

    private PrintPreviewPanel previewPanel;

    private DefaultListModel<CheckObject> model;

    private JFormattedTextField xPosField;

    private JFormattedTextField yPosField;

    private JFormattedTextField heightField;

    private JFormattedTextField countField;

    private JButton addButton;

    private JButton applyButton;

    private JButton clearButton;

    private JButton closeButton;

    private JTextField nameField;

    private JList<CheckObject> objectList;

    private JButton openButton;

    private JButton printButton;

    private JButton removeButton;

    private JButton saveButton;

    private JButton setupButton;

    private JToolBar toolBar;

    private JComboBox<CheckObjectType> typeCombo;

    private CheckObject activeObject = null;

    private CheckLayout checkLayout;

    private PrintableCheckLayout layout;

    private final Preferences pref = Preferences.userNodeForPackage(CheckDesignDialog.class);

    private final Resource rb = Resource.get();

    public CheckDesignDialog(final Frame parent) {
        super(parent, true);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(rb.getString("Title.CheckDesign"));
        layoutMainPanel();
        setLocationRelativeTo(parent);
        clear();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String args[]) {

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CheckDesignDialog(null).setVisible(true);
            }
        });
    }

    private void initComponents() {
        toolBar = new JToolBar();
        toolBar.setRollover(true);

        openButton = new RollOverButton(rb.getString("Menu.Open.Name"), Resource.getIcon("/jgnash/resource/document-open.png"));
        openButton.setMnemonic(rb.getMnemonic("Menu.Open.Mnemonic"));

        saveButton = new RollOverButton(rb.getString("Menu.Save.Name"), Resource.getIcon("/jgnash/resource/document-save.png"));
        saveButton.setMnemonic(rb.getMnemonic("Menu.Save.Mnemonic"));

        toolBar.add(openButton);
        toolBar.add(saveButton);

        checkLayout = new CheckLayout();

        layout = new PrintableCheckLayout(checkLayout);

        countField = getIntegerField();
        xPosField = getFloatField();
        yPosField = getFloatField();
        heightField = getFloatField();

        previewPanel = new PrintPreviewPanel(layout, layout.getPageFormat());

        objectList = new JList<>();
        //objectList.setPrototypeCellValue("Some dummy text");

        setupButton = new JButton(rb.getString("Button.PageSetup"));
        addButton = new JButton(rb.getString("Button.Add"));
        removeButton = new JButton(rb.getString("Button.Remove"));
        clearButton = new JButton(rb.getString("Button.Clear"));
        applyButton = new JButton(rb.getString("Button.Apply"));
        printButton = new JButton(rb.getString("Button.PrintSample"));
        closeButton = new JButton(rb.getString("Button.Close"));

        nameField = new JTextFieldEx();
        typeCombo = new JComboBox<>();

        DefaultComboBoxModel<CheckObjectType> comboModel = new DefaultComboBoxModel<>(CheckObjectType.values());

        typeCombo.setModel(comboModel);
        model = new DefaultListModel<>();

        objectList.setModel(model);
        objectList.addListSelectionListener(this);

        addButton.addActionListener(this);
        applyButton.addActionListener(this);
        closeButton.addActionListener(this);
        clearButton.addActionListener(this);
        countField.addActionListener(this);
        removeButton.addActionListener(this);
        setupButton.addActionListener(this);
        heightField.addActionListener(this);
        saveButton.addActionListener(this);
        openButton.addActionListener(this);
        printButton.addActionListener(this);

        heightField.addFocusListener(this);
        countField.addFocusListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        getContentPane().add(toolBar, BorderLayout.NORTH);

        FormLayout formLayout = new FormLayout("p:g, 8dlu, f:$lcgap, 8dlu, p", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(formLayout);
        builder.border(Borders.DIALOG);
        builder.appendRow(RowSpec.decode("f:p:g"));
        builder.append(buildLeftPanel());
        builder.append(new JSeparator(SwingConstants.VERTICAL));
        builder.append(buildRightPanel());
        builder.appendSeparator();
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildHelpCloseBar(printButton, closeButton), 5);

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        pack();

        setMinimumSize(getSize());
    }

    private JPanel buildRightPanel() {
        FormLayout formLayout = new FormLayout("right:p, $lcgap, p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(formLayout);

        builder.appendRow(RowSpec.decode("f:p:g")); // JScrollPane fills and grows
        builder.append(new JScrollPane(objectList), 3);
        builder.append(StaticUIMethods.buildAddRemoveBar(addButton, removeButton), 3);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.rowGroupingEnabled(true);
        builder.append(rb.getString("Label.Type"), typeCombo);
        builder.append(rb.getString("Label.Name"), nameField);
        builder.append(rb.getString("Label.XPos"), xPosField);
        builder.append(rb.getString("Label.YPos"), yPosField);
        builder.rowGroupingEnabled(false);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildOKCancelBar(clearButton, applyButton), 3);
        return builder.getPanel();
    }

    private JPanel buildLeftPanel() {
        FormLayout formLayout = new FormLayout("p, $lcgap, max(55dlu;p):g(0.5), 8dlu, p, $lcgap, max(55dlu;p):g(0.5)", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(formLayout);

        builder.appendRow(RowSpec.decode("f:p:g")); // previewPanel fills and
        // grows
        builder.append(previewPanel, 7);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(rb.getString("Label.Height"), heightField);
        builder.append(rb.getString("Label.Count"), countField);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(setupButton, 4);
        return builder.getPanel();
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == applyButton) {
            applyChanges();
        } else if (e.getSource() == addButton) {
            addObject();
        } else if (e.getSource() == removeButton) {
            removeObject();
        } else if (e.getSource() == clearButton) {
            clear();
        } else if (e.getSource() == closeButton) {
            dispatchEvent(new WindowEvent(CheckDesignDialog.this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getSource() == setupButton) {
            pageSetup();
        } else if (e.getSource() == heightField) {
            updateHeight();
        } else if (e.getSource() == countField) {
            updateCount();
        } else if (e.getSource() == printButton) {
            printSample();
        } else if (e.getSource() == saveButton) {
            saveLayout();
        } else if (e.getSource() == openButton) {
            openLayout();
        }
    }

    private static final String CURRENT_DIR = "cwd";

    private void saveLayout() {
        String fn = null;

        JFileChooser chooser = createFileChooser(pref.get(CURRENT_DIR, null));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            pref.put(CURRENT_DIR, chooser.getCurrentDirectory().getAbsolutePath());
            fn = chooser.getSelectedFile().getAbsolutePath();
            if (!fn.endsWith(".lay.xml")) {
                if (!fn.endsWith(".lay")) {
                    fn += ".lay.xml";
                } else {
                    fn += ".xml";
                }
            }
        }

        // must have a valid filename
        if (fn != null) {
            CheckLayoutSerializationFactory.saveLayout(fn, layout.getCheckLayout());
        }
    }

    private void openLayout() {
        JFileChooser chooser = createFileChooser(pref.get(CURRENT_DIR, null));

        String fn = null;
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pref.put(CURRENT_DIR, chooser.getCurrentDirectory().getAbsolutePath());
            fn = chooser.getSelectedFile().getAbsolutePath();
        }

        if (fn != null) {

            Object o = CheckLayoutSerializationFactory.loadLayout(fn);

            if (o != null) {
                checkLayout = (CheckLayout) o;
                layout.setCheckLayout(checkLayout);
                layout.setTestPrint(true);
                loadModel();
                previewPanel.setPrintable(layout);
                clear();
            }

        }
    }

    private void loadModel() {
        model.removeAllElements();

        for (CheckObject object : layout.getCheckObjects()) {
            model.addElement(object);
        }
    }

    /**
     * directory can be null, The JFileChooser will sort it out
     *
     * @param dir base directory
     * @return new JFileChooser
     */
    private static JFileChooser createFileChooser(final String dir) {
        JFileChooser chooser = new JFileChooser(dir);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("jGnash Files(*.xml)", "xml"));
        return chooser;
    }

    private void pageSetup() {
        PageFormat pageFormat = layout.pageSetup();
        if (pageFormat != null) {
            previewPanel.setPageFormat(pageFormat);
        }
    }

    private void printSample() {
        layout.print(); // print a sheet of test transactions
    }

    private void applyChanges() {
        if (activeObject != null) {
            CheckObject o = activeObject;

            int index = objectList.getSelectedIndex();
            model.remove(index);

            // change object here
            o.setName(nameField.getText());
            o.setX(Float.parseFloat(xPosField.getText()));
            o.setY(Float.parseFloat(yPosField.getText()));
            o.setType((CheckObjectType) typeCombo.getSelectedItem());

            // add it back to the list
            model.insertElementAt(o, index);
            clear();
            previewPanel.repaint();
        } else {
            addObject();
        }
    }

    private void showObject() {
        activeObject = objectList.getSelectedValue();
        if (activeObject != null) {
            typeCombo.setSelectedItem(activeObject.getType());
            nameField.setText(activeObject.getName());
            xPosField.setText(Float.toString(activeObject.getX()));
            yPosField.setText(Float.toString(activeObject.getY()));
        }
    }

    private void addObject() {
        if (activeObject == null) {
            CheckObject o = new CheckObject();

            if (!nameField.getText().isEmpty()) {
                o.setName(nameField.getText());
            }
            if (!xPosField.getText().isEmpty()) {
                o.setX(Float.parseFloat(xPosField.getText()));
            }
            if (!yPosField.getText().isEmpty()) {
                o.setY(Float.parseFloat(yPosField.getText()));
            }
            o.setType((CheckObjectType) typeCombo.getSelectedItem());
            model.addElement(o);
            checkLayout.add(o);
            previewPanel.repaint();
        }
    }

    private void clear() {
        nameField.setText("");
        xPosField.setText("0.0");
        yPosField.setText("0.0");
        countField.setText(Integer.toString(layout.getNumChecks()));
        heightField.setText(Double.toString(layout.getCheckHeight()));
        previewPanel.setPageFormat(layout.getPageFormat());
        activeObject = null;
    }

    private void removeObject() {
        if (activeObject != null) {
            checkLayout.remove(activeObject); // remove first
            model.removeElement(activeObject);
            previewPanel.repaint();
            clear();
        }
    }

    private void updateHeight() {
        if (!heightField.getText().isEmpty()) {
            checkLayout.setCheckHeight(Float.parseFloat(heightField.getText()));
            previewPanel.repaint();
        }
    }

    private void updateCount() {
        if (!countField.getText().isEmpty()) {
            layout.setNumChecks(Integer.parseInt(countField.getText()));
            previewPanel.repaint();
        }
    }

    private static JFormattedTextField getFloatField() {
        NumberFormatter df = new NumberFormatter(new DecimalFormat("#.##"));
        NumberFormatter ef = new NumberFormatter(new DecimalFormat("#.##"));
        return new JFormattedTextField(new DefaultFormatterFactory(df, df, ef));
    }

    private static JFormattedTextField getIntegerField() {
        NumberFormatter df = new NumberFormatter(new DecimalFormat("#"));
        NumberFormatter ef = new NumberFormatter(new DecimalFormat("#"));
        return new JFormattedTextField(new DefaultFormatterFactory(df, df, ef));
    }

    /**
     * Called whenever the value of the selection changes.
     *
     * @param e the event that characterizes the change.
     */
    @Override
    public void valueChanged(final ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        showObject();
    }

    /**
     * Invoked when a component gains the keyboard focus.
     *
     * @param e focus event
     */
    @Override
    public void focusGained(final FocusEvent e) {
    }

    /**
     * Invoked when a component loses the keyboard focus.
     *
     * @param e focus event
     */
    @Override
    public void focusLost(final FocusEvent e) {
        if (e.getSource() == heightField) {
            updateHeight();
        } else if (e.getSource() == countField) {
            updateCount();
        }
    }
}