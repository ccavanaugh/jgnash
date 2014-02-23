/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jgnash.engine.EngineFactory;
import jgnash.net.currency.CurrencyUpdateFactory;
import jgnash.net.security.SecurityUpdateFactory;
import jgnash.util.Resource;

/**
 * Startup options panel
 * 
 * @author Craig Cavanaugh
 */
class StartupOptions extends JPanel implements ActionListener {

    private final Resource rb = Resource.get();

    private JCheckBox updateCurrenciesButton;

    private JCheckBox updateSecuritiesButton;

    private JCheckBox saveBackupButton;

    private JCheckBox removeBackupButton;

    private JCheckBox openLastOnStartup;

    private JSpinner removeBackupCountSpinner;

    public StartupOptions() {
        layoutMainPanel();
    }

    private void initComponents() {

        updateCurrenciesButton = new JCheckBox(rb.getString("Button.UpdateCurrenciesStartup"));
        updateSecuritiesButton = new JCheckBox(rb.getString("Button.UpdateSecuritiesStartup"));
        openLastOnStartup = new JCheckBox(rb.getString("Button.OpenLastOnStartup"));
        removeBackupButton = new JCheckBox(rb.getString("Button.RemoveOldBackups"));
        saveBackupButton = new JCheckBox(rb.getString("Button.CreateTimeFile"));

        removeBackupCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));

        updateCurrenciesButton.setSelected(CurrencyUpdateFactory.getUpdateOnStartup());
        updateSecuritiesButton.setSelected(SecurityUpdateFactory.getUpdateOnStartup());
        saveBackupButton.setSelected(EngineFactory.exportXMLOnClose());
        removeBackupButton.setSelected(EngineFactory.removeOldBackups());
        openLastOnStartup.setSelected(EngineFactory.openLastOnStartup());

        removeBackupCountSpinner.setValue(EngineFactory.maximumBackups());

        updateCurrenciesButton.addActionListener(this);
        updateSecuritiesButton.addActionListener(this);
        saveBackupButton.addActionListener(this);
        removeBackupButton.addActionListener(this);
        openLastOnStartup.addActionListener(this);

        removeBackupCountSpinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                SpinnerModel dateModel = removeBackupCountSpinner.getModel();
                if (dateModel instanceof SpinnerNumberModel) {
                    Number count = ((SpinnerNumberModel) dateModel).getNumber();

                    EngineFactory.setMaximumBackups((Integer) count);
                }
            }
        });
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p, $lcgap, f:p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.rowGroupingEnabled(true);
        builder.border(Borders.DIALOG);

        builder.appendSeparator(rb.getString("Title.Startup"));
        builder.append(openLastOnStartup, 3);

        builder.appendSeparator(rb.getString("Title.Shutdown"));
        builder.append(saveBackupButton, 3);
        builder.append(removeBackupButton, 3);
        builder.append(rb.getString("Label.MaxBackupCount"), removeBackupCountSpinner);

        builder.appendSeparator(rb.getString("Title.BackgroundUpdate"));
        builder.append(updateCurrenciesButton, 3);
        builder.append(updateSecuritiesButton, 3);
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == updateCurrenciesButton) {
            CurrencyUpdateFactory.setUpdateOnStartup(updateCurrenciesButton.isSelected());
        } else if (e.getSource() == updateSecuritiesButton) {
            SecurityUpdateFactory.setUpdateOnStartup(updateSecuritiesButton.isSelected());
        } else if (e.getSource() == saveBackupButton) {
            EngineFactory.setExportXMLOnClose(saveBackupButton.isSelected());
        } else if (e.getSource() == removeBackupButton) {
            removeBackupCountSpinner.setEnabled(removeBackupButton.isSelected());
            EngineFactory.setRemoveOldBackups(removeBackupButton.isSelected());
        } else if (e.getSource() == openLastOnStartup) {
            EngineFactory.setOpenLastOnStartup(openLastOnStartup.isSelected());
        }
    }
}
