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

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import jgnash.plugin.Plugin;
import jgnash.plugin.PluginFactory;
import jgnash.ui.StaticUIMethods;
import jgnash.ui.ThemeManager;
import jgnash.ui.UIApplication;
import jgnash.ui.util.DialogUtils;
import jgnash.util.Resource;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Options Dialog
 * 
 * @author Craig Cavanaugh
 *
 */
public class OptionDialog extends JDialog implements ActionListener {

    private final Resource rb = Resource.get();

    private JTabbedPane tabbedPane;

    private JButton closeButton;

    public static void showDialog() {
        EventQueue.invokeLater(() -> {
            OptionDialog d = new OptionDialog();
            DialogUtils.addBoundsListener(d);
            d.setVisible(true);
        });
    }

    private OptionDialog() {
        super(UIApplication.getFrame(), true);
        layoutMainPanel();
        
        final int oldNimbusFontSize = ThemeManager.getNimbusFontSize();

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(final WindowEvent e) {
                if (ThemeManager.getNimbusFontSize() != oldNimbusFontSize && ThemeManager.isLookAndFeelNimbus()) {
                    EventQueue.invokeLater(UIApplication::restartUI);
                }
            }
        });
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(rb.getString("Title.Options"));

        tabbedPane = new JTabbedPane();

        tabbedPane.addTab(rb.getString("Tab.Accounts"), new AccountOptions());
        tabbedPane.addTab(rb.getString("Tab.General"), new GeneralOptions(this));
        tabbedPane.addTab(rb.getString("Tab.Network"), new NetworkOptions());
        tabbedPane.addTab(rb.getString("Tab.Register"), new RegisterOptions());
        tabbedPane.addTab(rb.getString("Tab.Reminders"), new ReminderOptions());
        tabbedPane.addTab(rb.getString("Tab.Report"), new ReportOptions());
        tabbedPane.addTab(rb.getString("Tab.StartupShutdown"), new StartupOptions());

        for (Plugin plugin : PluginFactory.getPlugins()) {
            JPanel optionsPanel = plugin.getOptionsPanel();

            if (optionsPanel != null) {
                Object name = optionsPanel.getClientProperty(Plugin.OPTIONSNAME);

                if (name != null) {
                    tabbedPane.addTab((String) name, optionsPanel);
                }
            }
        }

        closeButton = new JButton(rb.getString("Button.Close"));
        closeButton.addActionListener(this);
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p:g", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(Borders.DIALOG);

        builder.append(tabbedPane);
        builder.nextLine();              
        builder.appendGlueRow();
        builder.nextLine();
        builder.append(StaticUIMethods.buildCloseBar(closeButton));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        pack();

        setMinimumSize(getSize());
    }

    /**
     * Invoked when an action occurs.
     * 
     * @param e action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == closeButton) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }
}
