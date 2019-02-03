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
package jgnash.ui.report.compiled;

import java.time.LocalDate;
import java.util.Objects;
import java.util.ResourceBundle;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import jgnash.engine.CurrencyNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.report.ProfitLossTextReport;
import jgnash.ui.components.JDateField;
import jgnash.ui.register.AccountBalanceDisplayManager;
import jgnash.resource.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Profit loss report
 * 
 * @author Michael Mueller
 * @author Craig Cavanaugh
 * @author David Robertson
 */
public class ProfitLossTXT {

    private final ResourceBundle rb = ResourceUtils.getBundle();

    public void run() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final CurrencyNode baseCommodity = engine.getDefaultCurrency();

        final LocalDate[] dates = getDates();

        if (dates != null) {
            final String fileName = getFileName();

            ProfitLossTextReport report = new ProfitLossTextReport(fileName, dates[0], dates[1], baseCommodity,
                    AccountBalanceDisplayManager::convertToSelectedBalanceMode);

            report.run();
        }
    }

    private LocalDate[] getDates() {

        final LocalDate start = LocalDate.now().minusYears(1);

        final JDateField startField = new JDateField();
        final JDateField endField = new JDateField();

        startField.setValue(start);

        final FormLayout layout = new FormLayout("right:p, 4dlu, p:g", "");
        final DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.rowGroupingEnabled(true);

        builder.append(rb.getString("Label.StartDate"), startField);
        builder.append(rb.getString("Label.EndDate"), endField);
        builder.nextLine();
        builder.appendUnrelatedComponentsGapRow();
        builder.nextLine();

        final JPanel panel = builder.getPanel();

        int option = JOptionPane.showConfirmDialog(null, new Object[] { panel }, rb.getString("Message.StartEndDate"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            return new LocalDate[] {startField.localDateValue(), endField.localDateValue()};
        }

        return null;
    }

    private String getFileName() {
        final JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(rb.getString("Message.TXTFile"), "txt"));

        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            String fileName = chooser.getSelectedFile().getAbsolutePath();
            if (!fileName.endsWith(".txt")) {
                fileName = fileName + ".txt";
            }
            return fileName;
        }
        return null;
    }
}
