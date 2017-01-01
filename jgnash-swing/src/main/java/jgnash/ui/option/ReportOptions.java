/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2017 Craig Cavanaugh
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
import java.util.ResourceBundle;

import javax.swing.JPanel;

import jgnash.ui.components.FontComboBox;
import jgnash.ui.report.jasper.ReportFactory;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Report options panel
 *
 * @author Craig Cavanaugh
 */
class ReportOptions extends JPanel implements ActionListener {

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private FontComboBox monoCombo;

    private FontComboBox proportionalCombo;

    public ReportOptions() {
        layoutMainPanel();

        monoCombo.addActionListener(this);
        proportionalCombo.addActionListener(this);
    }

    private void initComponents() {
        monoCombo = new FontComboBox(ReportFactory.getMonoFont());
        proportionalCombo = new FontComboBox(ReportFactory.getProportionalFont());

    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("right:p, $lcgap, max(75dlu;p):g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.rowGroupingEnabled(true);
        builder.border(Borders.DIALOG);

        builder.appendSeparator(rb.getString("Title.Fonts"));
        builder.append(rb.getString("Label.Monospace"), monoCombo);
        builder.append(rb.getString("Label.Proportional"), proportionalCombo);
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == monoCombo) {
            ReportFactory.setMonoFont(monoCombo.getSelectedFont());
        } else if (e.getSource() == proportionalCombo) {
            ReportFactory.setProportionalFont(proportionalCombo.getSelectedFont());
        }
    }
}