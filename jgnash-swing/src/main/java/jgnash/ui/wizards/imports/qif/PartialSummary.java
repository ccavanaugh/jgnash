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
package jgnash.ui.wizards.imports.qif;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jgnash.convert.imports.qif.QifAccount;
import jgnash.ui.components.wizard.WizardPage;
import jgnash.util.ResourceUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Wizard Page for a partial qif import.
 *
 * @author Craig Cavanaugh
 *
 */
public class PartialSummary extends JPanel implements WizardPage {
    private final PartialDialog dlg;

    private final QifAccount qAcc;

    private final ResourceBundle rb = ResourceUtils.getBundle();

    private JLabel destLabel;

    private JLabel transCount;

    public PartialSummary(QifAccount qAcc, PartialDialog dlg) {
        this.qAcc = qAcc;
        this.dlg = dlg;
        layoutMainPanel();
    }

    private void initComponents() {
        destLabel = new JLabel("Account");
        transCount = new JLabel("0");

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent evt) {
                refreshInfo();
            }
        });
    }

    private void layoutMainPanel() {
        initComponents();

        FormLayout layout = new FormLayout("p, 8dlu, d:g", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);

        builder.appendSeparator(rb.getString("Title.ImpSum"));
        builder.nextLine();
        builder.appendRelatedComponentsGapRow();
        builder.nextLine();
        builder.append(rb.getString("Label.DestAccount"), destLabel);
        builder.append(rb.getString("Label.NumTrans"), transCount);
    }

    @Override
    public boolean isPageValid() {
        return true;
    }

    private void refreshInfo() {
        destLabel.setText(dlg.getAccount().getPathName());
        transCount.setText(Integer.toString(qAcc.getTransactions().size()));
    }

    /**
     * toString must return a valid description for this page that will
     * appear in the task list of the WizardDialog
     */
    @Override
    public String toString() {
        return "3. " + rb.getString("Title.ImpSum");
    }

    @Override
    public void getSettings(Map<Enum<?>, Object> map) {

    }

    @Override
    public void putSettings(Map<Enum<?>, Object> map) {

    }
}
