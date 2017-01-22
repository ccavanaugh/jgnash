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
package jgnash.ui.report.compiled;

import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.filechooser.FileNameExtensionFilter;

import jgnash.report.BalanceByMonthCSVReport;
import jgnash.ui.components.DatePanel;
import jgnash.ui.register.AccountBalanceDisplayManager;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Export monthly balance information as a CSV (comma-separated variable) file
 *
 * @author Craig Cavanaugh
 * @author Tom Edelson
 */
public final class MonthBalanceCSV {
    private final ResourceBundle rb = ResourceUtils.getBundle();

    private boolean vertical = true;

    private LocalDate startDate;

    private LocalDate endDate;

    private MonthBalanceCSV() {

        try {
            getOptions();

            if (startDate != null && endDate != null) {
                final String fileName = getFileName();

                if (fileName != null) {
                    BalanceByMonthCSVReport report = new BalanceByMonthCSVReport(fileName, startDate, endDate, null,
                            vertical, AccountBalanceDisplayManager::convertToSelectedBalanceMode);
                    report.run();
                }
            }

        } catch (Exception e) {
            Logger.getLogger(MonthBalanceCSV.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void getOptions() {
        final DatePanel startField = new DatePanel();
        final DatePanel endField = new DatePanel();

        ButtonGroup group = new ButtonGroup();
        JRadioButton vButton = new JRadioButton(rb.getString("Button.Vertical"));
        JRadioButton hButton = new JRadioButton(rb.getString("Button.Horizontal"));

        group.add(vButton);
        group.add(hButton);
        vButton.setSelected(true);

        FormLayout layout = new FormLayout("right:p, 4dlu, p:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.rowGroupingEnabled(true);

        builder.append(rb.getString("Label.StartDate"), startField);
        builder.append(rb.getString("Label.EndDate"), endField);
        builder.append(rb.getString("Label.Layout"), vButton);
        builder.append("", hButton);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();

        JPanel panel = builder.getPanel();

        int option = JOptionPane.showConfirmDialog(null, new Object[]{panel}, rb.getString("Message.StartEndDate"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            startDate = startField.getLocalDate();
            endDate = endField.getLocalDate();
        }

        vertical = vButton.isSelected();
    }

    private String getFileName() {
        final JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(rb.getString("Message.CSVFile"), "csv"));

        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            String fileName = chooser.getSelectedFile().getAbsolutePath();
            if (!fileName.endsWith(".csv")) {
                fileName = fileName + ".csv";
            }
            return fileName;
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static void run() {
        new MonthBalanceCSV();
    }
}
