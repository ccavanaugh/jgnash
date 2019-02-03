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
package jgnash.ui.components.wizard;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;

import jgnash.ui.util.IconUtils;
import jgnash.resource.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * Wizard dialog
 * 
 * @author Craig Cavanaugh
 */
public class WizardDialog extends JDialog implements ActionListener {

    private boolean valid = false;

    private final CardLayout layout;

    private final DefaultListModel<WizardPage> model;

    private int selectedIndex = 0;

    private JButton backButton;

    private JButton cancelButton;

    private JButton finishButton;

    private JButton nextButton;

    private JPanel pagePanel;

    private JList<WizardPage> taskList;

    protected final ResourceBundle rb = ResourceUtils.getBundle();

    private final Map<Enum<?>, Object> settings = new HashMap<>();

    private static final String KEY = "TASK";

    protected WizardDialog(final Frame parent) {
        super(parent, true);
        layoutMainPanel();

        backButton.addActionListener(this);
        cancelButton.addActionListener(this);
        finishButton.addActionListener(this);
        nextButton.addActionListener(this);

        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        layout = (CardLayout) pagePanel.getLayout();

        model = new DefaultListModel<>();
        taskList.setModel(model);
        updateButtonState();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    public Object getSetting(final Enum<?> key) {
        return settings.get(key);
    }

    public void setSetting(final Enum<?> key, final Object value) {
        settings.put(key, value);

        /* New setting. Tell each page to read */
        for (int i = 0; i < model.size(); i++) {
            WizardPage p = model.getElementAt(i);
            p.getSettings(settings);
        }
    }

    @Override
    public void setVisible(final boolean visible) {
        if (visible) {
            /*
             * This is a bit of a hack to make sure the dialog packs to handle
             * all the panels in the card layout
             */
            int count = model.size();

            for (int i = count - 1; i >= 0; i--) {
                taskList.setSelectedIndex(i);
                pack();
            }
        }

        setMinimumSize(getSize());

        super.setVisible(visible);
    }

    private void initComponents() {
        pagePanel = new JPanel(new CardLayout());

        backButton = new JButton(rb.getString("Button.Back"));
        backButton.setIcon(IconUtils.getIcon("/jgnash/resource/go-previous.png"));

        nextButton = new JButton(rb.getString("Button.Next"));
        nextButton.setIcon(IconUtils.getIcon("/jgnash/resource/go-next.png"));
        nextButton.setHorizontalTextPosition(SwingConstants.LEADING);

        finishButton = new JButton(rb.getString("Button.Finish"));
        cancelButton = new JButton(rb.getString("Button.Cancel"));

        taskList = new JList<>();
        taskList.setBorder(new EtchedBorder());
        taskList.addListSelectionListener(WizardDialog.this::selectionAction);

        taskList.setCellRenderer(new WizardPageRenderer(taskList.getCellRenderer()));
    }

    private void layoutMainPanel() {
        initComponents();

        CellConstraints cc = new CellConstraints();

        FormLayout lay = new FormLayout("p, $rgap, min(220dlu;d):g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(lay);
        builder.border(Borders.DIALOG);

        builder.appendRow(RowSpec.decode("f:p:g"));
        builder.append(buildTaskPanel(), pagePanel);
        builder.appendSeparator();
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.appendRow("p");
        builder.add(buildButtonPanel(), cc.xyw(1, builder.getRow(), 3));

        getContentPane().add(builder.getPanel());
    }

    private JPanel buildButtonPanel() {
        FormLayout lay = new FormLayout("$glue, $button, $rgap, $button, $rgap, $button, $ugap, $button", "f:p");

        DefaultFormBuilder builder = new DefaultFormBuilder(lay);
        builder.nextColumn();
        builder.append(backButton, nextButton, finishButton);
        builder.append(cancelButton);
        return builder.getPanel();
    }

    private JPanel buildTaskPanel() {
        FormLayout lay = new FormLayout("f:p", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(lay);
        builder.appendSeparator(rb.getString("Title.Steps"));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.appendRow(RowSpec.decode("f:d:g"));
        builder.append(taskList);

        JPanel panel = builder.getPanel();
        panel.setBackground((Color) UIManager.getDefaults().get("List.background"));
        panel.setOpaque(false);

        return panel;
    }

    /**
     * Closes the dialog
     */
    private void closeDialog() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void selectionAction(final ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }

        int oldIndex = selectedIndex;
        int newIndex = taskList.getSelectedIndex();

        if (oldIndex < newIndex) {
            while (oldIndex < newIndex) {
                nextAction();
                oldIndex++;
            }
        }

        if (oldIndex > newIndex) {
            while (oldIndex > newIndex) {
                backAction();
                oldIndex--;
            }
        }
    }

    private void updateButtonState() {
        if (selectedIndex >= 0 && selectedIndex < model.size() - 1) {
            nextButton.setEnabled(true);
        } else {
            nextButton.setEnabled(false);
        }

        if (selectedIndex == model.size() - 1) {
            boolean _valid = true;

            for (int i = 0; i < model.size(); i++) {
                WizardPage page = model.get(i);

                if (!page.isPageValid()) {
                    _valid = false;
                }
            }

            finishButton.setEnabled(_valid);
        } else {
            finishButton.setEnabled(false);
        }

        if (selectedIndex == 0) {
            backButton.setEnabled(false);
        } else {
            backButton.setEnabled(true);
        }
    }

    public boolean isWizardValid() {
        return valid;
    }

    private void nextAction() {
        if (selectedIndex < model.size() - 1) {
            // store an setting on the active page. May be necessary for the
            // next page
            WizardPage page = (WizardPage) pagePanel.getComponent(selectedIndex);
            page.putSettings(settings);

            layout.next(pagePanel);

            selectedIndex++;
            taskList.setSelectedIndex(selectedIndex);
            updateButtonState();

            // tell the active page to update
            page = (WizardPage) pagePanel.getComponent(selectedIndex);
            page.getSettings(settings);
        }
    }

    private void backAction() {
        if (selectedIndex > 0) {
            layout.previous(pagePanel);
            selectedIndex--;
            taskList.setSelectedIndex(selectedIndex);
            updateButtonState();
        }
    }

    private void cancelAction() {
        valid = false;
        closeDialog();
    }

    private void finishAction() {
        valid = true;
        for (int i = 0; i < model.size(); i++) {
            WizardPage p = model.getElementAt(i);
            if (!p.isPageValid()) {
                valid = false;
                break;
            }
            p.putSettings(settings); // let the wizard part store any needed settings
        }
        if (valid) {
            closeDialog();
        }
    }

    protected void addTaskPage(final WizardPage p) {
        p.getSettings(settings); // let the wizard page read any needed settings
        p.putSettings(settings); // let the wizard page put any defaults

        model.addElement(p);
        int size = model.size();
        pagePanel.add((Component) p, KEY + (size - 1));
        updateButtonState();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == backButton) {
            backAction();
        } else if (e.getSource() == cancelButton) {
            cancelAction();
        } else if (e.getSource() == finishButton) {
            finishAction();
        } else if (e.getSource() == nextButton) {
            nextAction();
        }
    }

    /*
     * Color the renderer red if the page is not _valid
     */
    private static final class WizardPageRenderer implements ListCellRenderer<WizardPage> {

        private static final Color inValidColor = Color.RED;

        private Color validColor = Color.BLACK;

        private final ListCellRenderer<? super WizardPage> delegate;

        WizardPageRenderer(final ListCellRenderer<? super WizardPage> listCellRenderer) {
            super();
            this.delegate = listCellRenderer;

            if (listCellRenderer instanceof JLabel) {
                validColor = ((JLabel) listCellRenderer).getForeground();
            }
        }

        @Override
        public Component getListCellRendererComponent(final JList<? extends WizardPage> list, final WizardPage value, final int index, final boolean isSelected, final boolean hasFocus) {

            Component c = delegate.getListCellRendererComponent(list, value, index, isSelected, hasFocus);

            if (delegate instanceof JLabel) {
                if (value.isPageValid()) {
                    ((JLabel) delegate).setForeground(validColor);
                } else {
                    ((JLabel) delegate).setForeground(inValidColor);
                }
            }

            return c;
        }
    }
}
