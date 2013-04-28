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
package jgnash.ui.recurring;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.recurring.PendingReminder;
import jgnash.engine.recurring.Reminder;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;
import jgnash.ui.UIApplication;
import jgnash.ui.components.FormattedJTable;
import jgnash.ui.components.GenericCloseDialog;
import jgnash.ui.components.RollOverButton;
import jgnash.ui.components.YesNoDialog;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

/**
 * A Panel that displays a list of reminders. This panel will listen for recurring events fired by the engine as well.
 * If this panel does not exist in the application, then events will not be fired.
 * 
 * @author Craig Cavanaugh
 */
public class RecurringPanel extends JPanel implements ActionListener, MessageListener {

    private static final int DEFAULT_SNOOZE = 15 * 60 * 1000;

    private static final int START_UP_DELAY = 2 * 60 * 1000;

    private final Resource rb = Resource.get();

    private JButton deleteButton;

    private JButton modifyButton;

    private JButton newButton;

    private JButton remindersButton;

    private JTable reminderTable;

    private Timer timer;

    private static boolean confirmReminderDelete = false;

    private static final String CONFIRM_DELETE = "confirmdelete";

    static {
        Preferences p = Preferences.userNodeForPackage(RecurringPanel.class);
        confirmReminderDelete = p.getBoolean(CONFIRM_DELETE, true);
    }

    /**
     * Creates new form RecurringPanel
     */
    public RecurringPanel() {
        initComponents();

        registerListeners();

        SwingWorker<RecurringTableModel, Void> worker = new SwingWorker<RecurringTableModel, Void>() {

            @Override
            public RecurringTableModel doInBackground() {
                return new RecurringTableModel();
            }

            @Override
            public void done() {
                try {
                    RecurringTableModel model = get();

                    reminderTable.setModel(model);

                    if (!new JLabel().getFont().canDisplay(model.getEnabledSymbol())) {
                        model.setEnabledSymbol('X');
                    }
                } catch (InterruptedException | ExecutionException e) {
                    Logger.getLogger(RecurringPanel.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }
        };

        worker.execute();

        timer = null;
        startTimer();
    }

    private void registerListeners() {
        MessageBus.getInstance().registerListener(this, MessageChannel.SYSTEM);
    }

    /**
     * Show an instance of this panel in a dialog
     * 
     * @param parent parent frame
     */
    public static void showDialog(final Frame parent) {

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                Resource rb = Resource.get();
                GenericCloseDialog d = new GenericCloseDialog(parent, new RecurringPanel(), rb.getString("Title.Reminders"));
                d.pack();
                d.setMinimumSize(d.getSize());
                DialogUtils.addBoundsListener(d, "panelbounds");
                d.setModal(false);
                d.setVisible(true);
            }
        });
    }

    private void initComponents() {
        JPanel toolPanel = new JPanel(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        newButton = new RollOverButton(rb.getString("Button.New"), Resource.getIcon("/jgnash/resource/document-new.png"));
        modifyButton = new RollOverButton(rb.getString("Button.Modify"), Resource.getIcon("/jgnash/resource/document-properties.png"));
        deleteButton = new RollOverButton(rb.getString("Button.Delete"), Resource.getIcon("/jgnash/resource/edit-delete.png"));

        remindersButton = new RollOverButton(rb.getString("Button.CheckReminders"), Resource.getIcon("/jgnash/resource/view-refresh.png"));

        reminderTable = new FormattedJTable();
        reminderTable.setAutoCreateRowSorter(true);
        reminderTable.setFillsViewportHeight(true);
        reminderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        reminderTable.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(final KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteReminder();
                }
            }
        });

        reminderTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showModifyDialog();
                }
            }
        });

        setLayout(new BorderLayout());

        toolBar.add(newButton);
        toolBar.add(modifyButton);
        toolBar.add(deleteButton);
        toolBar.addSeparator();
        toolBar.add(remindersButton);

        toolPanel.add(toolBar, BorderLayout.NORTH);
        toolPanel.add(new JSeparator(), BorderLayout.CENTER);

        add(toolPanel, java.awt.BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(new EmptyBorder(new Insets(0, 0, 0, 0)));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setAutoscrolls(true);
        scrollPane.setViewportView(reminderTable);

        add(scrollPane, java.awt.BorderLayout.CENTER);

        deleteButton.addActionListener(this);
        modifyButton.addActionListener(this);
        newButton.addActionListener(this);
        remindersButton.addActionListener(this);
    }

    /**
     * Sets if confirm on transaction delete is enabled
     * 
     * @param enabled enabled state
     */
    public static void setConfirmReminderDeleteEnabled(final boolean enabled) {
        confirmReminderDelete = enabled;
        Preferences p = Preferences.userNodeForPackage(RecurringPanel.class);
        p.putBoolean(CONFIRM_DELETE, confirmReminderDelete);
    }

    /**
     * Returns the availability of sortable registers
     * 
     * @return true if confirm on transaction delete is enabled, false otherwise
     */
    public static boolean isConfirmReminderDeleteEnabled() {
        return confirmReminderDelete;
    }

    private void deleteReminder() {
        int index = reminderTable.getSelectedRow();
        if (index != -1) {
            if (isConfirmReminderDeleteEnabled()) {
                if (!confirmReminderRemoval()) {
                    return;
                }
            }
            EngineFactory.getEngine(EngineFactory.DEFAULT).removeReminder(getReminderByRow(index));
        }
    }

    private Reminder getReminderByRow(final int row) {
        RecurringTableModel model = (RecurringTableModel) reminderTable.getModel();

        int modelRow = reminderTable.convertRowIndexToModel(row);

        return model.getReminderAt(modelRow);
    }

    private boolean confirmReminderRemoval() {
        return YesNoDialog.showYesNoDialog(UIApplication.getFrame(), new JLabel(rb.getString("Message.ConfirmReminderDelete")), rb.getString("Title.Confirm"));
    }

    private static void showNewDialog() {
        Reminder r = RecurringEntryDialog.showDialog();
        if (r != null) {
            EngineFactory.getEngine(EngineFactory.DEFAULT).addReminder(r);
        }
    }

    private void showModifyDialog() {
        int index = reminderTable.getSelectedRow();
        if (index != -1) {
            final Reminder old = getReminderByRow(index); // get the old reminder

            try {
                Reminder modified = RecurringEntryDialog.showDialog((Reminder) old.clone()); // create a new reminder

                if (modified != null) {

                    Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);

                    if (engine.removeReminder(old)) { // remove the old
                        engine.addReminder(modified); // add the new
                    }
                }
            } catch (CloneNotSupportedException e) {
                Logger.getLogger(RecurringPanel.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == newButton) {
            showNewDialog();
        } else if (e.getSource() == modifyButton) {
            showModifyDialog();
        } else if (e.getSource() == deleteButton) {
            deleteReminder();
        } else if (e.getSource() == timer || e.getSource() == remindersButton) {
            showRecurringDialog();
        }
    }

    private final static String SNOOZE = "snooze";

    private synchronized void showRecurringDialog() {

        // exit if engine is not running or a dialog is already visible
        if (showingDialog || EngineFactory.getEngine(EngineFactory.DEFAULT) == null) {
            return;
        }

        SwingWorker<List<PendingReminder>, Void> worker = new SwingWorker<List<PendingReminder>, Void>() {

            @Override
            protected List<PendingReminder> doInBackground() throws Exception {
                return EngineFactory.getEngine(EngineFactory.DEFAULT).getPendingReminders();
            }

            @Override
            protected void done() {
                try {
                    List<PendingReminder> reminders = get();

                    // Only display one dialog at a time
                    // Event occurs on the EDT, so no need to invoke later
                    if (!reminders.isEmpty()) {
                        showingDialog = true;

                        Preferences p = Preferences.userNodeForPackage(getClass());

                        int snooze = p.getInt(SNOOZE, DEFAULT_SNOOZE);

                        snooze = NotificationDialog.showDialog(reminders, snooze); // display the notification dialog

                        p.putInt(SNOOZE, snooze);

                        if (timer != null) {
                            if (snooze != 0) {
                                timer.setDelay(snooze);
                                timer.setInitialDelay(snooze);
                                timer.restart();
                            } else {
                                timer.stop();
                            }
                        } else {
                            throw new RuntimeException("Lost the timer!");
                        }

                        showingDialog = false;
                    }
                } catch (final InterruptedException | ExecutionException | RuntimeException e) {
                    Logger.getLogger(RecurringPanel.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }
        };

        worker.execute();
    }

    /* prevent duplicate dialogs even if multiple multiple dialogs are open */
    private volatile static boolean showingDialog = false;

    private void startTimer() {
        if (timer == null) {
            Preferences p = Preferences.userNodeForPackage(getClass());

            int snooze = p.getInt(SNOOZE, DEFAULT_SNOOZE);

            timer = new Timer(snooze, this);
            timer.setInitialDelay(START_UP_DELAY);

            timer.start();

            Logger.getLogger(RecurringPanel.class.getName()).info("Recurring timer started");
        }
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;

            Logger.getLogger(RecurringPanel.class.getName()).info("Recurring timer stopped");
        }
    }

    @Override
    public void messagePosted(final Message event) {
        switch (event.getEvent()) {
            case FILE_CLOSING:
            case UI_RESTARTING:
                stopTimer();
                MessageBus.getInstance().unregisterListener(this, MessageChannel.SYSTEM);
                break;
            default:
                break;
        }
    }
}
