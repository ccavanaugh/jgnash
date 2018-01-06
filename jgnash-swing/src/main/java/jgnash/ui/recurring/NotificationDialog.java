/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2018 Craig Cavanaugh
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
package jgnash.ui.recurring;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.recurring.PendingReminder;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.UIApplication;
import jgnash.ui.components.FormattedJTable;
import jgnash.ui.components.TimePeriodCombo;
import jgnash.ui.util.DialogUtils;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * A dialog for displaying recurring event / transactions when they occur.
 *
 * @author Craig Cavanaugh
 * @author Clemens Wacha
 */
class NotificationDialog extends JDialog implements ActionListener, ListSelectionListener {

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private JButton cancelButton;

    private JButton okButton;

    private final TimePeriodCombo periodCombo = new TimePeriodCombo();

    private final ReminderObjectTableModel model;

    private JTable table;

    private final List<PendingReminder> reminders;

    static int showDialog(final List<PendingReminder> reminders, final int snoozeTime) {
        NotificationDialog d = new NotificationDialog(reminders);

        d.setSnoozeTime(snoozeTime);
        d.setVisible(true);
        d.toFront();

        return d.getSnoozeTime();
    }

    private NotificationDialog(final List<PendingReminder> reminders) {
        super(UIApplication.getFrame(), true);

        this.reminders = reminders;

        model = new ReminderObjectTableModel(reminders);

        if (!new JLabel().getFont().canDisplay(model.getEnabledSymbol())) {
            model.setEnabledSymbol('X');
        }

        setTitle(rb.getString("Title.Reminder"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        layoutMainPanel();

        DialogUtils.addBoundsListener(this);

        cancelButton.addActionListener(this);
        okButton.addActionListener(this);
    }

    private int getSnoozeTime() {
        return periodCombo.getSelectedPeriod();
    }

    private void setSnoozeTime(final int snooze) {
        EventQueue.invokeLater(() -> periodCombo.setSelectedPeriod(snooze));
    }

    private void layoutMainPanel() {
        FormLayout layout = new FormLayout("right:p, 4dlu, fill:p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);

        cancelButton = new JButton(rb.getString("Button.RemindLater"));
        okButton = new JButton(rb.getString("Button.AckSel"));

        table = new FormattedJTable(model);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(this);
        table.setColumnSelectionAllowed(false);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setAutoscrolls(true);

        builder.appendRow(RowSpec.decode("fill:80dlu:g"));

        builder.append(scrollPane, 3);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();
        builder.append(rb.getString("Label.RemindLater"), periodCombo);
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildOKCancelBar(okButton, cancelButton), 3);

        getContentPane().add(builder.getPanel());
        pack();

        setMinimumSize(getSize());
    }

    /**
     * @param e action event
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == cancelButton) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } else if (e.getSource() == okButton) {
            final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
            Objects.requireNonNull(engine);

            engine.processPendingReminders(reminders);

            // close the dialog
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    @Override
    public void valueChanged(final ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            if (e.getSource() == table.getSelectionModel()) {
                final int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {

                    EventQueue.invokeLater(() -> {
                        model.toggleSelectedState(table.convertRowIndexToModel(selectedRow));
                        table.clearSelection(); // clear the selection
                    });
                }
            }
        }
    }
}
